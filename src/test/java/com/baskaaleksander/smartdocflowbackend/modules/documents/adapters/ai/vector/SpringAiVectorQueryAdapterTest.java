package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.ai.vector;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.SearchHit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringAiVectorQueryAdapterTest {

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private SpringAiVectorQueryAdapter adapter;

    @Captor
    private ArgumentCaptor<SearchRequest> requestCaptor;

    @Test
    void searchByQuery_buildsFilterAndMapsResults() {
        Document d1 = Document.builder()
                .text("t1")
                .metadata(Map.of("docId", "123", "page", 5))
                .score(0.91)
                .build();
        Document d2 = Document.builder()
                .text("t2")
                .metadata(Map.of("docId", "123", "page", 6))
                .score(0.72)
                .build();

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(d1, d2));

        Map<String, Object> filter = new LinkedHashMap<>();
        filter.put("docId", "123");
        filter.put("page", 5);
        filter.put("flag", true);

        List<SearchHit> out = adapter.searchByQuery("hello", 0.35, 12, filter);

        verify(vectorStore).similaritySearch(requestCaptor.capture());
        SearchRequest req = requestCaptor.getValue();

        assertThat(req.getQuery()).isEqualTo("hello");
        assertThat(req.getTopK()).isEqualTo(12);
        assertThat(req.getSimilarityThreshold()).isEqualTo(0.35);

        Filter.Expression expr = req.getFilterExpression();
        assertThat(expr).isNotNull();

        String exprStr = expr.toString();
        assertThat(exprStr).contains("Key[key=docId]");
        assertThat(exprStr).contains("Value[value=123]");
        assertThat(exprStr).contains("Key[key=page]");
        assertThat(exprStr).contains("Value[value=5]");
        assertThat(exprStr).contains("Key[key=flag]");
        assertThat(exprStr).contains("Value[value=true]");

        assertThat(out).hasSize(2);
        assertThat(out.get(0).text()).isEqualTo("t1");
        assertThat(out.get(0).score()).isEqualTo(0.91);
        assertThat(out.get(0).metadata()).containsEntry("docId", "123");

        assertThat(out.get(1).text()).isEqualTo("t2");
        assertThat(out.get(1).score()).isEqualTo(0.72);
        assertThat(out.get(1).metadata()).containsEntry("page", 6);
    }

    @Test
    void searchByQuery_escapesSingleQuotesInFilterValues() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        Map<String, Object> filter = Map.of("name", "O'Hara");

        adapter.searchByQuery("q", 0.4, 3, filter);

        verify(vectorStore).similaritySearch(requestCaptor.capture());
        Filter.Expression expr = requestCaptor.getValue().getFilterExpression();
        assertThat(expr).isNotNull();

        String exprStr = expr.toString();
        assertThat(exprStr).contains("Key[key=name]");
        assertThat(exprStr).contains("Value[value=O\\'Hara]");
    }

    @Test
    void searchByQuery_noFilterPassesNullExpression() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        adapter.searchByQuery("q", 0.5, 5, null);

        verify(vectorStore).similaritySearch(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getFilterExpression()).isNull();
    }
}