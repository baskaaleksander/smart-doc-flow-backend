package com.baskaaleksander.smartdocflowbackend.modules.documents.application.impl;

import com.baskaaleksander.smartdocflowbackend.modules.documents.application.Embedder;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.OpenAiEmbeddingsRequest;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.OpenAiEmbeddingsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
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
        List<float[]> result = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i += batchSize) result.add(null);

        for (int i = 0; i < texts.size(); i += batchSize) {
            int to = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, to);

            var req = new OpenAiEmbeddingsRequest(model, batch);
            var res = openAi.post()
                    .uri("/v1/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(OpenAiEmbeddingsResponse.class)
                    .block();

            if (res == null || res.data() == null || res.data().size() != batch.size()) {
                throw new RuntimeException("OpenAi embeddings: unexpected response size");
            }

            for (int j = 0; j < batch.size(); j++) {
                var d = res.data().get(j);
                float[] vec = new float[(d.embedding().size())];
                for (int k = 0; k < vec.length; k++) vec[k] = d.embedding().get(k).floatValue();
                result.set(i + j, vec);
            }

        }

        return result;
    }
}
