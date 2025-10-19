package com.baskaaleksander.smartdocflowbackend.modules.documents.application;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.modules.documents.api.dto.DocumentResponse;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.documents.api.dto.DocumentStatsResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.OcrTask;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.OcrTaskPublisherPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.view.DocumentStatusCount;
import com.baskaaleksander.smartdocflowbackend.modules.documents.mapping.DocumentMapper;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.NotificationEvent;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewStatus;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.exception.S3DeleteException;
import com.baskaaleksander.smartdocflowbackend.common.exception.S3UploadException;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.Document;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.DocumentRepository;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.spring.SpringDataReviewRepository;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
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

    private final DocumentRepository documentRepository;
    private final S3Client s3Client;
    private final SpringDataUserRepository userRepository;
    private final SpringDataReviewRepository reviewRepository;
    private final DocumentMapper documentMapper;
    private final S3Presigner s3Presigner;
    private final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private final ObjectMapper MAPPER = new ObjectMapper();
    private final ApplicationEventPublisher publisher;

    private final OcrTaskPublisherPort taskPublisher;


    @Value(value = "${minio.bucket.name}")
    private String s3Bucket;

    public DocumentService(
            DocumentRepository documentRepository,
            S3Client s3Client,
            SpringDataUserRepository userRepository,
            SpringDataReviewRepository reviewRepository,
            DocumentMapper documentMapper,
            S3Presigner s3Presigner,
            ApplicationEventPublisher publisher,
            OcrTaskPublisherPort taskPublisher
            ) {
        this.documentRepository = documentRepository;
        this.s3Client = s3Client;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.documentMapper = documentMapper;
        this.s3Presigner = s3Presigner;
        this.publisher = publisher;
        this.taskPublisher = taskPublisher;
    }

    public DocumentResponse createAndSave(MultipartFile file) {
        UUID docId = UUID.randomUUID();
        String originalFilename = Objects.requireNonNull(file.getOriginalFilename()).replace(" ", "_");
        String filename = docId + "_" + originalFilename;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        UserEntity user = userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!"application/pdf".equalsIgnoreCase(String.valueOf(file.getContentType()))) {
            System.out.println("Incorrect filetype");;
        }

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(filename)
                .contentType(file.getContentType())
                .build();

        try (var in = file.getInputStream()) {
            s3Client.putObject(request, RequestBody.fromInputStream(in, file.getSize()));
        } catch (Exception e) {
            throw new S3UploadException("Upload to object store failed");
        }

        Document document = new Document();
        document.setId(docId);
        document.setFilename(originalFilename);
        document.setStorageKey(filename);
        document.setMime("pdf");
        document.setSize(file.getSize());
        document.setPageSize(1);
        document.setStatus(DocumentStatus.UPLOADED);
        document.setOwner(user);


        saveDocToDb(document);

        publisher.publishEvent(new NotificationEvent(username, "document_uploaded", "Document successfully uploaded!"));

        taskPublisher.publish(new OcrTask(docId));

        return documentMapper.toDocumentResponse(document);
    }

    @Transactional
    protected void saveDocToDb(Document document) {
        documentRepository.save(document);

        ReviewEntity review = new ReviewEntity();
        review.setStatus(ReviewStatus.PENDING);
        review.setDocument(document);
        review = reviewRepository.save(review);

        document.setReview(review);

        documentRepository.save(document);
    }

    public DocumentResponse getById(UUID id) {
        Document doc = documentRepository.findbyIdWithReview(id).orElseThrow(() -> new ResourceNotFoundException("Document with id " + id + " not found"));

        return documentMapper.toDocumentResponse(doc);
    }

    public PagingResult<DocumentResponse> getAllDocuments(PaginationRequest request, Boolean assignedToMe, UUID userId) {

        Pageable pageable = PaginationUtil.getPageable(request);
        Page<Document> documents;

        if (assignedToMe) {
            documents = documentRepository.findAllByReviewer(userId, pageable);
        } else {
            documents = documentRepository.findAll(pageable);
        }

        return getDocumentResponsePagingResult(request, documents);
    }

    public PagingResult<DocumentResponse> getUserDocuments(PaginationRequest request, UUID userId) {

        Pageable pageable = PaginationUtil.getPageable(request);

        Page<Document> documents = documentRepository.findAllByOwner(userId, pageable);

        return getDocumentResponsePagingResult(request, documents);
    }

    private PagingResult<DocumentResponse> getDocumentResponsePagingResult(PaginationRequest request, Page<Document> documents) {
        List<DocumentResponse> documentsList = documents
                .stream()
                .map(documentMapper::toDocumentResponse)
                .toList();

        Integer currentPage = request.getPage();
        int totalPages = documents.getTotalPages();

        return new PagingResult<>(
                documentsList,
                totalPages,
                documents.getTotalElements(),
                documents.getSize(),
                documents.getNumber(),
                currentPage + 1 == totalPages,
                currentPage + 1 < totalPages
        );
    }

    @Transactional
    public void deleteById(UUID id) {
        Document document = documentRepository.getDocumentById(id).orElseThrow(() -> new ResourceNotFoundException("Document does not exist"));

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

        documentRepository.deleteById(id);
    }

    public String downloadDocumentById(UUID id) {
        Document document = documentRepository.getDocumentById(id).orElseThrow(() -> new ResourceNotFoundException("Document does not exist"));

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
        List<DocumentStatusCount> counts = documentRepository.countDocumentsByStatus();

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
