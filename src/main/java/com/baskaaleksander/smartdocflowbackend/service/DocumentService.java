package com.baskaaleksander.smartdocflowbackend.service;

import com.baskaaleksander.smartdocflowbackend.dto.request.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.dto.response.DocumentResponse;
import com.baskaaleksander.smartdocflowbackend.dto.response.PagingResult;
import com.baskaaleksander.smartdocflowbackend.enums.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.enums.ReviewStatus;
import com.baskaaleksander.smartdocflowbackend.exceptions.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.exceptions.S3DeleteException;
import com.baskaaleksander.smartdocflowbackend.exceptions.S3UploadException;
import com.baskaaleksander.smartdocflowbackend.mapper.DocumentMapper;
import com.baskaaleksander.smartdocflowbackend.model.Document;
import com.baskaaleksander.smartdocflowbackend.model.Review;
import com.baskaaleksander.smartdocflowbackend.model.User;
import com.baskaaleksander.smartdocflowbackend.repository.DocumentRepository;
import com.baskaaleksander.smartdocflowbackend.repository.ReviewRepository;
import com.baskaaleksander.smartdocflowbackend.repository.UserRepository;
import com.baskaaleksander.smartdocflowbackend.utils.PaginationUtil;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final S3Client s3Client;
    private OcrService ocrService;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final DocumentMapper documentMapper;
    private final NotificationService notificationService;
    private final S3Presigner s3Presigner;
    private final Logger log = LoggerFactory.getLogger(DocumentService.class);

    @Value(value = "${minio.bucket.name}")
    private String s3Bucket;

    public DocumentService(
            DocumentRepository documentRepository,
            S3Client s3Client,
            OcrService ocrService,
            UserRepository userRepository,
            ReviewRepository reviewRepository,
            DocumentMapper documentMapper,
            NotificationService notificationService,
            S3Presigner s3Presigner
    ) {
        this.documentRepository = documentRepository;
        this.s3Client = s3Client;
        this.ocrService = ocrService;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.documentMapper = documentMapper;
        this.notificationService = notificationService;
        this.s3Presigner = s3Presigner;
    }

    public DocumentResponse createAndSave(MultipartFile file) {
        UUID docId = UUID.randomUUID();
        String originalFilename = Objects.requireNonNull(file.getOriginalFilename()).replace(" ", "_");
        String filename = docId + "_" + originalFilename;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found"));

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
            System.out.println(e);
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

        notificationService.sendNotification(username, "document_uploaded", "Document successfully uploaded!");

        ocrService.startAsync(document.getId());

        return documentMapper.toDocumentResponse(document);
    }

    @Transactional
    protected void saveDocToDb(Document document) {
        documentRepository.save(document);

        Review review = new Review();
        review.setStatus(ReviewStatus.PENDING);
        review.setDocument(document);
        review = reviewRepository.save(review);

        document.setReview(review);
    }

    public DocumentResponse getById(UUID id) {
        Document doc = documentRepository.findbyIdWithReview(id).orElseThrow(() -> new ResourceNotFoundException("Document with id " + id + " not found"));

        return documentMapper.toDocumentResponse(doc);
    }

    public PagingResult<DocumentResponse> getAllDocuments(PaginationRequest request) {

        Pageable pageable = PaginationUtil.getPageable(request);

        Page<Document> documents = documentRepository.findAll(pageable);

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

}
