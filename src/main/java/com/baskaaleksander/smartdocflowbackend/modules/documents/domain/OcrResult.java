package com.baskaaleksander.smartdocflowbackend.modules.documents.domain;

import java.util.List;

public record OcrResult(List<OcrResultPage> pages) {
}