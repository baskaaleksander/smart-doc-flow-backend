package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model;

import java.util.List;

public record OcrResult(List<OcrResultPage> pages) {
}