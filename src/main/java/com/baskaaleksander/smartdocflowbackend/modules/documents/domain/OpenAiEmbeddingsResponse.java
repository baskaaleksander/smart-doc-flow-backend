package com.baskaaleksander.smartdocflowbackend.modules.documents.domain;

import java.util.List;

public record OpenAiEmbeddingsResponse(
        List<Datum> data
        ) {
    public record Datum(List<Double> embedding) {}
}
