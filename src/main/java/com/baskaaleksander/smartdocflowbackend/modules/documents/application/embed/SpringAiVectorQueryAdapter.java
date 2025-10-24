package com.baskaaleksander.smartdocflowbackend.modules.documents.application.embed;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.SearchHit;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.VectorQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SpringAiVectorQueryAdapter implements VectorQueryPort {

    private final VectorStore vectorStore;

    @Override
    public List<SearchHit> searchByQuery(String queryText, double similarityThreshold, int topK, Map<String, Object> filter) {

        var retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(similarityThreshold)
                .topK(topK)
                .build();

        String filterExp = buildFilterExpression(filter);

        Query query = Query.builder()
                .text(queryText)
                .context(filterExp == null ? Map.of()
                        : Map.of(VectorStoreDocumentRetriever.FILTER_EXPRESSION, filterExp))
                .build();

        List<Document> docs = retriever.retrieve(query);

        for(var doc : docs) {
            System.out.println(doc.getMetadata() + " " + doc.getText());
        }


        return docs.stream()
                .map(d -> new SearchHit(d.getText(), d.getScore(), d.getMetadata()))
                .toList();
    }

    private String buildFilterExpression(Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) return null;

        return filter.entrySet().stream()
                .map(e -> {
                    String key = e.getKey();
                    Object val = e.getValue();
                    if (val == null) return null;
                    if (val instanceof Number || val instanceof Boolean) {
                        return key + " == " + val;
                    } else {
                        String escaped = String.valueOf(val).replace("'", "\\'");
                        return key + " == '" + escaped + "'";
                    }
                })
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(" and "));
    }
}
