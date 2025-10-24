package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.ai.vector;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.VectorDocument;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.VectorIndexPort;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class VectorIndexAdapter implements VectorIndexPort {

    private final VectorStore vectorStore;

    @Override
    public void addAll(List<VectorDocument> docs) {
        List<Document> documents = docs.stream().map(
                c -> Document.builder()
                        .text(c.text())
                        .metadata(c.metadata())
                        .build()
        ).toList();

        vectorStore.add(documents);
    }
}
