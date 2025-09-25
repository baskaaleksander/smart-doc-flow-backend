package com.baskaaleksander.smartdocflowbackend.common.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueConfig {

    @Bean
    public Queue ocrQueue() {
        return new Queue("ocrQueue", false);
    }
}
