package com.baskaaleksander.smartdocflowbackend.common.config;

import com.baskaaleksander.smartdocflowbackend.modules.documents.application.Embedder;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.impl.OpenAiEmbedder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OpenAiConfig {

    @Bean
    public WebClient openAiWebClient(
            @Value("${openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${openai.api-key}") String apiKey
    ) {

        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

}
