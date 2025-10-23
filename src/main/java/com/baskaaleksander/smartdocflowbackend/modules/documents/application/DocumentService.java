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
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.DocumentCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.DocumentQueryPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.DocumentUserQueryPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.OcrTaskPublisherPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.view.DocumentStatusCount;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.NotificationEvent;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private final ApplicationEventPublisher publisher;

    private final OcrTaskPublisherPort taskPublisher;
    private final DocumentCommandPort documentCommandPort;
    private final DocumentQueryPort documentQueryPort;
    private final DocumentUserQueryPort documentUserQueryPort;
    private final DocumentApiMapper mapper;

    @Value(value = "${minio.bucket.name}")
    private String s3Bucket;

    public DocumentService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            ApplicationEventPublisher publisher,

            OcrTaskPublisherPort taskPublisher,
            DocumentCommandPort documentCommandPort,
            DocumentQueryPort documentQueryPort,
            DocumentUserQueryPort documentUserQueryPort,
            DocumentApiMapper mapper
            ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.publisher = publisher;

        this.taskPublisher = taskPublisher;
        this.documentCommandPort = documentCommandPort;
        this.documentQueryPort = documentQueryPort;
        this.documentUserQueryPort = documentUserQueryPort;
        this.mapper = mapper;
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

        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(s3Bucket).key(filename).contentType(contentType).build();

        try (var in = file.getInputStream()) {
            s3Client.putObject(req, RequestBody.fromInputStream(in, file.getSize()));
        } catch (Exception e) {
            throw new S3UploadException("Upload to object store failed");
        }

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

        Document saved;
        try {
            saved = documentCommandPort.save(document);
        } catch (RuntimeException ex) {
            try {
                s3Client.deleteObject(b -> b.bucket(s3Bucket).key(filename));
            } catch (Exception ignore) {}
            throw ex;
        }

        publisher.publishEvent(new NotificationEvent(username, "document_uploaded", "Document successfully uploaded!"));
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

        try {
            ObjectIdentifier objectToDelete = ObjectIdentifier.builder().key(document.getStorageKey()).build();
            s3Client.deleteObjects(request ->
                    request
                            .bucket(s3Bucket)
                            .delete(deleteRequest ->
                                    deleteRequest.objects(
                                            objectToDelete
                                    )));
        } catch (Exception ex) {
            log.error("Failed to delete document {} from S3: {}", id, ex.getMessage(), ex);
            throw new S3DeleteException("Couldn't delete document");
        }

        documentCommandPort.deleteById(id);
    }

    public String downloadDocumentById(UUID id) {
        Document document = documentQueryPort.getDocumentById(id).orElseThrow(() -> new ResourceNotFoundException("Document does not exist"));

        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(s3Bucket)
                .key(document.getStorageKey())
                .responseContentType(document.getMime())
                .build();

        var presignedReq = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(3))
                .getObjectRequest(get)
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignedReq);
        return presigned.url().toString();
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
