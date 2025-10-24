package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.ai.vector;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.VectorDocument;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class VectorIndexAdapterTest {

    private final VectorStore vectorStore = mock(VectorStore.class);
    private final VectorIndexAdapter adapter = new VectorIndexAdapter(vectorStore);

    @Test
    void addAll_shouldConvertVectorDocumentsAndPassToVectorStore() {
        VectorDocument doc1 = new VectorDocument("text1", Map.of("docId", "1", "page", 2));
        VectorDocument doc2 = new VectorDocument("text2", Map.of("docId", "2", "page", 5));

        adapter.addAll(List.of(doc1, doc2));

        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, times(1)).add(captor.capture());

        List<Document> addedDocs = captor.getValue();
        assertThat(addedDocs).hasSize(2);
        assertThat(addedDocs.get(0).getText()).isEqualTo("text1");
        assertThat(addedDocs.get(1).getText()).isEqualTo("text2");
        assertThat(addedDocs.get(0).getMetadata()).containsEntry("docId", "1").containsEntry("page", 2);
        assertThat(addedDocs.get(1).getMetadata()).containsEntry("docId", "2").containsEntry("page", 5);
    }

    @Test
    void addAll_shouldNotCallVectorStore_whenEmptyList() {
        adapter.addAll(List.of());
        verify(vectorStore, never()).add(any());
    }
}