package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.VectorDocument;

import java.util.List;

public interface VectorIndexPort {
    void addAll(List<VectorDocument> docs);
}
