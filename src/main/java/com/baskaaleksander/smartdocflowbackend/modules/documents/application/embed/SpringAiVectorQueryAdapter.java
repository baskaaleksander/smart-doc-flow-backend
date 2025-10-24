package com.baskaaleksander.smartdocflowbackend.modules.documents.application.embed;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.SearchHit;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.VectorQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SpringAiVectorQueryAdapter implements VectorQueryPort {
    @Override
    public List<SearchHit> searchByQuery(String query, double similarityThreshold, int topK, Map<String, Object> filter) {
        return List.of();
    }
}
