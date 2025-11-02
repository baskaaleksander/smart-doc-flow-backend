package com.baskaaleksander.smartdocflowbackend.modules.testsupport.pipeline;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.DocumentCommandPort;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RecordingDocumentCommandPort implements DocumentCommandPort {

    private final DocumentCommandPort delegate;
    private final Map<UUID, List<DocumentStatus>> statusHistory = new ConcurrentHashMap<>();
    private final Map<UUID, List<Integer>> pageCounts = new ConcurrentHashMap<>();

    public RecordingDocumentCommandPort(DocumentCommandPort delegate) {
        this.delegate = delegate;
    }

    public void clear() {
        statusHistory.clear();
        pageCounts.clear();
    }

    @Override
    public void deleteById(UUID documentId) {
        delegate.deleteById(documentId);
    }

    @Override
    public Document save(Document document) {
        return delegate.save(document);
    }

    @Override
    public void updateStatus(UUID documentId, DocumentStatus status) {
        statusHistory.computeIfAbsent(documentId, id -> Collections.synchronizedList(new ArrayList<>())).add(status);
        delegate.updateStatus(documentId, status);
    }

    @Override
    public void updatePageCount(UUID documentId, int count) {
        pageCounts.computeIfAbsent(documentId, id -> Collections.synchronizedList(new ArrayList<>())).add(count);
        delegate.updatePageCount(documentId, count);
    }

    public List<DocumentStatus> getStatuses(UUID documentId) {
        List<DocumentStatus> statuses = statusHistory.get(documentId);
        return statuses == null ? List.of() : List.copyOf(statuses);
    }

    public List<Integer> getPageCounts(UUID documentId) {
        List<Integer> counts = pageCounts.get(documentId);
        return counts == null ? List.of() : List.copyOf(counts);
    }
}
