package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model;

import java.util.Map;

public record VectorDocument(String text, Map<String, Object> metadata) {
}
