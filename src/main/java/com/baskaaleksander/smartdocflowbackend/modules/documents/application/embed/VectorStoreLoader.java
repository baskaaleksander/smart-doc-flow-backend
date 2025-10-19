package com.baskaaleksander.smartdocflowbackend.modules.documents.application.embed;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Chunk;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VectorStoreLoader {

    private final VectorStore vectorStore;

    public void loadChunks(List<Chunk> chunks) {
        List<Document> documents = chunks.stream().map(
                c -> Document.builder()
                        .text(c.content())
                        .metadata(Map.of(
                                "docId", c.documentId().toString(),
                                "page", c.page(),
                                "startOffset", c.startOffset(),
                                "endOffset", c.endOffset()
                        ))
                        .build()
        ).toList();

        vectorStore.add(documents);
    }
}
