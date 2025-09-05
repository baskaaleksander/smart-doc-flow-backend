package com.baskaaleksander.smartdocflowbackend.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value(value = "${minio.access.name}")
    private String secret;

    @Value(value = "${minio.access.secret}")
    private String accessKey;

    @Value(value = "${minio.url}")
    private String url;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secret)
                .build();
    }
}
