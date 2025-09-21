package com.baskaaleksander.smartdocflowbackend.modules.documents.application.impl;

import com.baskaaleksander.smartdocflowbackend.modules.documents.application.Embedder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class OpenAiEmbedder implements Embedder {

    private final WebClient openAi;
    private final String model;
    private final int batchSize;

    public OpenAiEmbedder(WebClient openAi,
                          @Value("${openai.embedding.model:text-embedding-3-small}") String model,
                          @Value("${openai.embedding.batch-size:64}") int batchSize) {
        this.openAi = openAi;
        this.model = model;
        this.batchSize = batchSize;
    }


    @Override
    public List<float[]> embedBatch(List<String> texts) {
        return List.of();
    }
}
