package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model;

public record Image(byte[] bytes, String mimeType, int pageNumber) {
}
