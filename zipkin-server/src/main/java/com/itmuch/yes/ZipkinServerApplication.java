package com.itmuch.yes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.sleuth.zipkin.stream.EnableZipkinStreamServer;
import zipkin.server.EnableZipkinServer;

@SpringBootApplication
@EnableZipkinStreamServer
@EnableDiscoveryClient
public class ZipkinServerApplication {
    public static void main(final String[] args) {
        SpringApplication.run(ZipkinServerApplication.class, args);
    }
}
