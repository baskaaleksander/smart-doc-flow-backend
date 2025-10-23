package com.baskaaleksander.smartdocflowbackend.modules.documents.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.*;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.DocumentApiMapper;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto.DocumentResponse;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto.DocumentStatsResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.OcrTask;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentReviewBasic;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentUserBasic;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.*;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.view.DocumentStatusCount;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.NotificationEvent;
import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private final DocumentDomainEventPublisherPort publisher;
    private final OcrTaskPublisherPort taskPublisher;
    private final DocumentCommandPort documentCommandPort;
    private final DocumentQueryPort documentQueryPort;
    private final DocumentUserQueryPort documentUserQueryPort;
    private final DocumentApiMapper mapper;
    private final FileStoragePort fileStoragePort;

    public DocumentService(
            DocumentDomainEventPublisherPort publisher,
            OcrTaskPublisherPort taskPublisher,
            DocumentCommandPort documentCommandPort,
            DocumentQueryPort documentQueryPort,
            DocumentUserQueryPort documentUserQueryPort,
            DocumentApiMapper mapper,
            FileStoragePort fileStoragePort) {
        this.publisher = publisher;
        this.taskPublisher = taskPublisher;
        this.documentCommandPort = documentCommandPort;
        this.documentQueryPort = documentQueryPort;
        this.documentUserQueryPort = documentUserQueryPort;
        this.mapper = mapper;
        this.fileStoragePort = fileStoragePort;
    }

    public DocumentResponse createAndSave(MultipartFile file) {
        UUID docId = UUID.randomUUID();
        String originalFilename = Objects.requireNonNull(file.getOriginalFilename()).replace(" ", "_");
        String filename = docId + "_" + originalFilename;

        String contentType = Optional.ofNullable(file.getContentType()).orElse("");
        if (!"application/pdf".equalsIgnoreCase(contentType)) {
            throw new InvalidFileTypeException("Only application/pdf is allowed");
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        DocumentUserBasic user = documentUserQueryPort.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        InputStream stream;
        try {
            stream = file.getInputStream();
        } catch (Exception ex) {
            throw new S3UploadException("Failed to upload file");
        }

        fileStoragePort.upload(stream, filename, contentType, file.getSize());

        Document document = new Document();
        document.setId(docId);
        document.setFilename(originalFilename);
        document.setStorageKey(filename);
        document.setMime(contentType);
        document.setSize(file.getSize());
        document.setPageSize(0);
        document.setStatus(DocumentStatus.UPLOADED);
        document.setOwner(user);

        DocumentReviewBasic review = new DocumentReviewBasic();
        document.setReview(review);

        Document saved = null;
        try {
            saved = documentCommandPort.save(document);
        } catch (RuntimeException ex) {
            fileStoragePort.delete(filename);
            //throw some error
        }

        publisher.publish(new NotificationEvent(username, "document_uploaded", "Document successfully uploaded!"));
        taskPublisher.publish(new OcrTask(saved.getId()));

        return mapper.toResponse(saved);
    }

    public DocumentResponse getById(UUID id) {
        Document doc = documentQueryPort.findByIdWithReview(id).orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        return mapper.toResponse(doc);
    }

    public PagingResult<DocumentResponse> getAllDocuments(PaginationRequest request, Boolean assignedToMe, UUID userId) {
        PagingResult<Document> documents;

        if (assignedToMe) {
            documents = documentQueryPort.findAllByReviewer(userId, request);
        } else {
            documents = documentQueryPort.findAll(request);
        }

        List<DocumentResponse> content = documents.content().stream().map(mapper::toResponse).toList();

        return new PagingResult<>(
                content,
                documents.totalPages(),
                documents.totalElements(),
                documents.size(),
                documents.page(),
                documents.last(),
                documents.next()
        );
    }

    public PagingResult<DocumentResponse> getUserDocuments(PaginationRequest request, UUID userId) {

        PagingResult<Document> documents = documentQueryPort.findAllByOwner(userId, request);

        List<DocumentResponse> content = documents.content().stream().map(mapper::toResponse).toList();

        return new PagingResult<>(
                content,
                documents.totalPages(),
                documents.totalElements(),
                documents.size(),
                documents.page(),
                documents.last(),
                documents.next()
        );
    }


    @Transactional
    public void deleteById(UUID id) {
        Document document = documentQueryPort.getDocumentById(id).orElseThrow(() -> new ResourceNotFoundException("Document does not exist"));

        fileStoragePort.delete(document.getStorageKey());

        documentCommandPort.deleteById(id);
    }

    public String downloadDocumentById(UUID id) {
        Document document = documentQueryPort.getDocumentById(id).orElseThrow(() -> new ResourceNotFoundException("Document does not exist"));

        return fileStoragePort.getPresignedUrl(document.getStorageKey(), document.getMime(), 3L);
    }

    public DocumentStatsResponse getDocumentStats() {
        List<DocumentStatusCount> counts = documentQueryPort.countDocumentsByStatus();

        Map<DocumentStatus, Long> stats = counts.stream()
                .collect(Collectors.toMap(DocumentStatusCount::getStatus, DocumentStatusCount::getCount));

        Long failed = Optional.ofNullable(stats.get(DocumentStatus.OCR_FAILED)).orElse(0L)
                + Optional.ofNullable(stats.get(DocumentStatus.EMBED_FAILED)).orElse(0L);

        return new DocumentStatsResponse(
                Optional.ofNullable(stats.get(DocumentStatus.REVIEW_PENDING)).orElse(0L),
                Optional.ofNullable(stats.get(DocumentStatus.IN_REVIEW)).orElse(0L),
                Optional.ofNullable(stats.get(DocumentStatus.REVIEWED)).orElse(0L),
                failed
        );
    }

}
