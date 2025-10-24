package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model;

import java.util.Map;

public record SearchHit(String text, double score, Map<String, Object> metadata) {
}
