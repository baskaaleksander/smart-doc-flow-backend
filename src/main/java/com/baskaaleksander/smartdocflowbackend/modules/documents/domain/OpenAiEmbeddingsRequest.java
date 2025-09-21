package com.baskaaleksander.smartdocflowbackend.modules.documents.domain;

import java.util.List;

public record OpenAiEmbeddingsRequest(
        String model,
        List<String> input
) {
}
