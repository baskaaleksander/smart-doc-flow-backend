package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Image;

import java.util.List;

public interface OcrEnginePort {
    String extractText(List<Image> images);
}
