package com.itmuch.yes.controller;

import com.google.common.collect.Maps;
import com.itmuch.yes.core.constant.ConstantsCode;
import com.itmuch.yes.core.convert.AjaxResult;
import com.itmuch.yes.core.exception.BizRuntimeException;
import com.itmuch.yes.core.page.PageVoWithSort;
import com.itmuch.yes.domain.content.Article;
import com.itmuch.yes.domain.content.AuditEnum;
import com.itmuch.yes.repository.ArticleRepository;
import com.itmuch.yes.util.mapper.BeanMapper;
import com.itmuch.yes.util.snowflake.IDGenerator;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Criteria;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.representations.AccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;

import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/articles")
@Slf4j
public class ArticleController {
    private final ArticleRepository articleRepository;

    @Autowired
    public ArticleController(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @GetMapping("")
    public HashMap<Object, Object> search(
            Principal principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String audit,
            PageVoWithSort pageVo
    ) {

        if (principal instanceof KeycloakPrincipal) {
            AccessToken accessToken = ((KeycloakPrincipal) principal).getKeycloakSecurityContext().getToken();
            String preferredUsername = accessToken.getPreferredUsername();
            AccessToken.Access realmAccess = accessToken.getRealmAccess();
            Set<String> roles = realmAccess.getRoles();
            log.info("当前登录用户：{}, 角色：{}", preferredUsername, roles);
        }


//        long count = this.articleRepository.count();
//        Page<Article> page = null;
//        if (count != 0) {
//            page = this.articleRepository.findByAudit(
//                    AuditEnum.NOT_YET,
//                    new PageRequest(0, 10, Sort.Direction.DESC, "issueDate")
//            );
//        }
//        return page;
//
////        return this.articleRepository.findAll(
////                new PageRequest(pageVo.getPage() - 1, pageVo.getRows())
////        );
        AuditEnum auditEnum = null;
        try {
            auditEnum = AuditEnum.valueOf(audit);
        } catch (Exception ignored) {
        }

        String escape = keyword == null ? "aaaa" : QueryParser.escape(keyword);

        Criteria criteria = new Criteria("title").fuzzy(escape)
                .or(new Criteria("content").fuzzy(escape))
                .and(new Criteria("audit").is(auditEnum));

        CriteriaQuery criteriaQuery = new CriteriaQuery(criteria)
                .setPageable(
                        new PageRequest(pageVo.getPage(), pageVo.getRows(), pageVo.getSpringSort())
                );
        Page<Article> articles = this.elasticsearchTemplate.queryForPage(criteriaQuery,
                Article.class);

        HashMap<Object, Object> map = Maps.newHashMap();
        map.put("content", articles.getContent());
        map.put("totalElements", articles.getTotalElements());
        map.put("totalPages", articles.getTotalPages());
        return map;
    }

    /**
     * 添加文章
     *
     * @param article 文章
     * @return 编辑结果
     */
    @PostMapping("")
    public AjaxResult add(@RequestBody @Validated Article article) {
        article.setId(IDGenerator.genId());
        article.setAudit(AuditEnum.NOT_YET);
        article.setIssueDate(new Date());
        article.setTags(Stream.of("标签1", "标签2", "标签3").collect(Collectors.toList()));

        this.articleRepository.save(article);
        return new AjaxResult().success();
    }

    /**
     * 编辑文章
     *
     * @param article 文章
     * @return 编辑结果
     */
    @PutMapping("")
    public AjaxResult edit(@RequestBody @Validated Article article) {
        Article articleInDB = this.articleRepository.findOne(article.getId());
        if (articleInDB == null) {
            throw new BizRuntimeException(
                    ConstantsCode.DATA_NOT_FOUND,
                    "该文章不存在",
                    "该文章不存在");
        }

        // 目前只要经过人工编辑，就自动审核。防止这边在编辑，那边采集又覆盖掉。
        articleInDB.setAudit(AuditEnum.PASSED);
        BeanMapper.map(article, articleInDB);
        this.articleRepository.save(articleInDB);
        return new AjaxResult().success();
    }


}
