package com.baskaaleksander.smartdocflowbackend.modules.testsupport;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    protected static final RabbitMQContainer RABBIT =
            new RabbitMQContainer("rabbitmq:3-management");

    protected static final GenericContainer<?> QDRANT =
            new GenericContainer<>("qdrant/qdrant:latest")
                    .withExposedPorts(6333, 6334)
                    .waitingFor(Wait.forHttp("/readyz").forPort(6333));

    protected static final GenericContainer<?> MINIO =
            new GenericContainer<>("minio/minio:latest")
                    .withEnv("MINIO_ROOT_USER", "minio")
                    .withEnv("MINIO_ROOT_PASSWORD", "minio12345")
                    .withCommand("server /data --console-address :9001")
                    .withExposedPorts(9000, 9001)
                    .waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000));

    static {
        POSTGRES.start();
        RABBIT.start();
        QDRANT.start();
        MINIO.start();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);

        r.add("spring.rabbitmq.host", RABBIT::getHost);
        r.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        r.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        r.add("spring.rabbitmq.password", RABBIT::getAdminPassword);

        String minioUrl = "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000);
        r.add("minio.url", () -> minioUrl);
        r.add("minio.access.name", () -> "minio");
        r.add("minio.access.secret", () -> "minio12345");

    }
}