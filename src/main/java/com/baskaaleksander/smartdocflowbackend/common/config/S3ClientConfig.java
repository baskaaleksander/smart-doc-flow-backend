package com.baskaaleksander.smartdocflowbackend.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3ClientConfig {

    @Value(value = "${minio.access.name}")
    private String secret;

    @Value(value = "${minio.access.secret}")
    private String accessKey;

    @Value(value = "${minio.url}")
    private String url;

    @Bean
    public S3Presigner s3Presigner() {

        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(accessKey, secret);

        return S3Presigner.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                .build();
    }

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(secret, accessKey);

        return S3Client.builder()
                .endpointOverride(URI.create(url))
                .credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
                .region(Region.US_EAST_1)
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)
                                .build()
                )
                .build();
    }
}
