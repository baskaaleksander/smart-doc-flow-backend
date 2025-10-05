package com.baskaaleksander.smartdocflowbackend.modules.document.application;

import com.baskaaleksander.smartdocflowbackend.modules.documents.api.dto.DocumentResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.DocumentService;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.ocr.OcrTaskPublisher;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.mapping.DocumentMapper;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.DocumentRepository;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence.Review;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence.ReviewRepository;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.User;
import com.baskaaleksander.smartdocflowbackend.modules.users.persistence.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class DocumentServiceTest {
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private S3Client s3Client;
    @Mock
    private OcrTaskPublisher ocrTaskPublisher;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DocumentMapper documentMapper;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private S3Presigner s3Presigner;
    @Mock
    private ObjectMapper MAPPER;

    @InjectMocks
    private DocumentService documentService;

    private String s3Bucket = "bucket";
    private Document document;
    private Review review;
    private User owner;
    private DocumentResponse mapped;

    @BeforeEach
    void setUp() {
        review = new Review();
        review.setId(UUID.randomUUID());
        owner = new User();
        owner.setId(UUID.randomUUID());

        document = new Document();
        document.setId(UUID.randomUUID());
        document.setFilename("filename");
        document.setMime("pdf");
        document.setSize(0);
        document.setPageSize(1);
        document.setOwner(owner);
        document.setReview(review);
        document.setStatus(DocumentStatus.IN_REVIEW);
        document.setStorageKey("storage-key");
        document.setCreatedAt(Instant.now());

        mapped = new DocumentResponse(
                document.getId(),
                document.getFilename(),
                document.getMime(),
                document.getSize(),
                document.getPageSize(),
                document.getOwner().getId(),
                document.getReview().getId(),
                document.getStatus(),
                document.getCreatedAt()
        );
    }

    @Test
    void getById_shouldReturnDocumentResponse() {
        UUID id = UUID.randomUUID();
        when(documentRepository.findbyIdWithReview(id)).thenReturn(Optional.of(document));
        when(documentMapper.toDocumentResponse(document))
                .thenReturn(mapped);

        DocumentResponse response = documentService.getById(id);

        assertThat(response.id()).isEqualTo(mapped.id());
        assertThat(response.filename()).isEqualTo(mapped.filename());
        assertThat(response.mime()).isEqualTo(mapped.mime());
        assertThat(response.size()).isEqualTo(mapped.size());
        assertThat(response.pageSize()).isEqualTo(mapped.pageSize());
        assertThat(response.ownerId()).isEqualTo(mapped.ownerId());
        assertThat(response.reviewId()).isEqualTo(mapped.reviewId());
        assertThat(response.status()).isEqualTo(mapped.status());
        assertThat(response.createdAt()).isEqualTo(mapped.createdAt());
    }
}
