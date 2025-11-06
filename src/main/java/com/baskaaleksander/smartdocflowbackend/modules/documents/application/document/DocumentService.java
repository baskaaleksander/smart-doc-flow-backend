package com.baskaaleksander.smartdocflowbackend.modules.documents.application.document;

import com.baskaaleksander.smartdocflowbackend.common.exception.DocumentUploadException;
import com.baskaaleksander.smartdocflowbackend.common.exception.InvalidFileTypeException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.exception.S3UploadException;
import com.baskaaleksander.smartdocflowbackend.common.logging.LoggingPort;
import com.baskaaleksander.smartdocflowbackend.common.logging.Slf4jLoggingAdapter;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.NotificationEvent;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.DocumentApiMapper;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto.DocumentResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto.DocumentStatsResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.EmbedTask;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.OcrTask;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentReviewBasic;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentUserBasic;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.*;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.view.DocumentStatusCount;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentDomainEventPublisherPort publisher;
    private final OcrTaskPublisherPort taskPublisher;
    private final EmbeddingTaskPublisherPort embeddingTaskPublisher;
    private final DocumentCommandPort documentCommandPort;
    private final DocumentQueryPort documentQueryPort;
    private final DocumentUserQueryPort documentUserQueryPort;
    private final DocumentApiMapper mapper;
    private final FileStoragePort fileStoragePort;
    private final LoggingPort logger;

    public DocumentResponse createAndSave(MultipartFile file) {
        long start = System.currentTimeMillis();
        UUID docId = UUID.randomUUID();
        String originalFilename = Objects.requireNonNull(file.getOriginalFilename()).replace(" ", "_");
        String filename = docId + "_" + originalFilename;

        logger.info("DOC_UPLOAD START docId=" + Slf4jLoggingAdapter.shortId(docId)
                + " name=" + originalFilename
                + " size=" + file.getSize());

        String contentType = Optional.ofNullable(file.getContentType()).orElse("");
        if (!"application/pdf".equalsIgnoreCase(contentType)) {
            logger.warn("DOC_UPLOAD FAILED reason=invalid_mime docId=" + Slf4jLoggingAdapter.shortId(docId)
                    + " mime=" + contentType);
            throw new InvalidFileTypeException("Only application/pdf is allowed");
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        DocumentUserBasic user = documentUserQueryPort.findByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("DOC_UPLOAD FAILED reason=user_not_found docId=" + Slf4jLoggingAdapter.shortId(docId)
                            + " username=" + username);
                    return new ResourceNotFoundException("User not found");
                });

        InputStream stream;
        try {
            stream = file.getInputStream();
        } catch (Exception ex) {
            logger.error("DOC_UPLOAD FAILED reason=input_stream_error docId=" + Slf4jLoggingAdapter.shortId(docId)
                    + " name=" + originalFilename, ex);
            throw new S3UploadException("Failed to upload file");
        }

        try {
            fileStoragePort.upload(stream, filename, contentType, file.getSize());
            logger.info("DOC_UPLOAD S3_SUCCESS docId=" + Slf4jLoggingAdapter.shortId(docId)
                    + " key=" + filename);
        } catch (Exception ex) {
            logger.error("DOC_UPLOAD FAILED reason=s3_upload_error docId=" + Slf4jLoggingAdapter.shortId(docId)
                    + " key=" + filename, ex);
            throw new S3UploadException("Failed to upload file");
        }

        DocumentReviewBasic review = new DocumentReviewBasic();

        Document document = new Document.Builder()
                .id(docId)
                .filename(originalFilename)
                .storageKey(filename)
                .mime(contentType)
                .size(file.getSize())
                .pageSize(0)
                .status(DocumentStatus.UPLOADED)
                .owner(user)
                .review(review)
                .build();

        Document saved;
        try {
            saved = documentCommandPort.save(document);
            logger.info("DOC_UPLOAD DB_SAVE_SUCCESS docId=" + Slf4jLoggingAdapter.shortId(docId));
        } catch (RuntimeException ex) {
            fileStoragePort.delete(filename);
            logger.error("DOC_UPLOAD FAILED reason=db_save_error docId=" + Slf4jLoggingAdapter.shortId(docId)
                    + " key=" + filename, ex);
            throw new DocumentUploadException("Failed to save document. Please try again later.");
        }

        publisher.publish(new NotificationEvent(username, "document_uploaded", "Document successfully uploaded!"));
        logger.info("DOC_UPLOAD NOTIFY_PUBLISHED docId=" + Slf4jLoggingAdapter.shortId(docId)
                + " username=" + username);

        taskPublisher.publish(new OcrTask(saved.getId()));
        long took = System.currentTimeMillis() - start;
        logger.info("DOC_UPLOAD SUCCESS docId=" + Slf4jLoggingAdapter.shortId(docId)
                + " key=" + filename
                + " took=" + took + "ms");

        return mapper.toResponse(saved);
    }

    public DocumentResponse getById(UUID id) {
        logger.info("DOC_GET START docId=" + Slf4jLoggingAdapter.shortId(id));
        Document doc = documentQueryPort.findByIdWithReview(id)
                .orElseThrow(() -> {
                    logger.warn("DOC_GET FAILED reason=not_found docId=" + Slf4jLoggingAdapter.shortId(id));
                    return new ResourceNotFoundException("Document not found");
                });
        logger.info("DOC_GET SUCCESS docId=" + Slf4jLoggingAdapter.shortId(id)
                + " status=" + doc.getStatus());
        return mapper.toResponse(doc);
    }

    public PagingResult<DocumentResponse> getAllDocuments(PaginationRequest request, Boolean assignedToMe, UUID userId) {
        logger.info("DOC_LIST START page=" + request.getPage() + " size=" + request.getSize()
                + " assignedToMe=" + assignedToMe
                + (assignedToMe ? " reviewerId=" + Slf4jLoggingAdapter.shortId(userId) : ""));

        PagingResult<Document> documents = assignedToMe
                ? documentQueryPort.findAllByReviewer(userId, request)
                : documentQueryPort.findAll(request);

        List<DocumentResponse> content = documents.content().stream().map(mapper::toResponse).toList();

        logger.info("DOC_LIST SUCCESS page=" + documents.page()
                + " size=" + documents.size()
                + " totalElements=" + documents.totalElements()
                + " totalPages=" + documents.totalPages());
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
        logger.info("DOC_LIST_USER START userId=" + Slf4jLoggingAdapter.shortId(userId)
                + " page=" + request.getPage() + " size=" + request.getSize());

        PagingResult<Document> documents = documentQueryPort.findAllByOwner(userId, request);
        List<DocumentResponse> content = documents.content().stream().map(mapper::toResponse).toList();

        logger.info("DOC_LIST_USER SUCCESS userId=" + Slf4jLoggingAdapter.shortId(userId)
                + " totalElements=" + documents.totalElements()
                + " totalPages=" + documents.totalPages());
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
        logger.info("DOC_DELETE START docId=" + Slf4jLoggingAdapter.shortId(id));
        Document document = documentQueryPort.getDocumentById(id)
                .orElseThrow(() -> {
                    logger.warn("DOC_DELETE FAILED reason=not_found docId=" + Slf4jLoggingAdapter.shortId(id));
                    return new ResourceNotFoundException("Document does not exist");
                });

        try {
            fileStoragePort.delete(document.getStorageKey());
            logger.info("DOC_DELETE S3_DELETE_SUCCESS docId=" + Slf4jLoggingAdapter.shortId(id)
                    + " key=" + document.getStorageKey());
        } catch (Exception ex) {
            logger.error("DOC_DELETE FAILED reason=s3_delete_error docId=" + Slf4jLoggingAdapter.shortId(id)
                    + " key=" + document.getStorageKey(), ex);
            throw ex;
        }

        documentCommandPort.deleteById(id);
        logger.info("DOC_DELETE SUCCESS docId=" + Slf4jLoggingAdapter.shortId(id));
    }

    public String downloadDocumentById(UUID id) {
        logger.info("DOC_DOWNLOAD START docId=" + Slf4jLoggingAdapter.shortId(id));
        Document document = documentQueryPort.getDocumentById(id)
                .orElseThrow(() -> {
                    logger.warn("DOC_DOWNLOAD FAILED reason=not_found docId=" + Slf4jLoggingAdapter.shortId(id));
                    return new ResourceNotFoundException("Document does not exist");
                });

        String url = fileStoragePort.getPresignedUrl(document.getStorageKey(), document.getMime(), 3L);
        logger.info("DOC_DOWNLOAD SUCCESS docId=" + Slf4jLoggingAdapter.shortId(id)
                + " key=" + document.getStorageKey());
        return url;
    }

    public DocumentStatsResponse getDocumentStats() {
        logger.info("DOC_STATS START");
        List<DocumentStatusCount> counts = documentQueryPort.countDocumentsByStatus();

        Map<DocumentStatus, Long> stats = counts.stream()
                .collect(Collectors.toMap(DocumentStatusCount::getStatus, DocumentStatusCount::getCount));

        Long failed = Optional.ofNullable(stats.get(DocumentStatus.OCR_FAILED)).orElse(0L)
                + Optional.ofNullable(stats.get(DocumentStatus.EMBED_FAILED)).orElse(0L);

        logger.info("DOC_STATS SUCCESS reviewPending=" + Optional.ofNullable(stats.get(DocumentStatus.REVIEW_PENDING)).orElse(0L)
                + " inReview=" + Optional.ofNullable(stats.get(DocumentStatus.IN_REVIEW)).orElse(0L)
                + " reviewed=" + Optional.ofNullable(stats.get(DocumentStatus.REVIEWED)).orElse(0L)
                + " failed=" + failed);

        return new DocumentStatsResponse(
                Optional.ofNullable(stats.get(DocumentStatus.REVIEW_PENDING)).orElse(0L),
                Optional.ofNullable(stats.get(DocumentStatus.IN_REVIEW)).orElse(0L),
                Optional.ofNullable(stats.get(DocumentStatus.REVIEWED)).orElse(0L),
                failed
        );
    }
}