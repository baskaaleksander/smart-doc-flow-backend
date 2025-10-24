package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.SearchHit;

import java.util.List;
import java.util.Map;

public interface VectorQueryPort {
    List<SearchHit> searchByQuery(
            String query,
            double similarityThreshold,
            int topK,
            Map<String, Object> filter
    );
}
