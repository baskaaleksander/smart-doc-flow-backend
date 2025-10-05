package com.baskaaleksander.smartdocflowbackend.modules.document.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.exception.S3DeleteException;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
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
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


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
    private DocumentService realService;

    private String s3Bucket = "bucket";
    private Document document;
    private Review review;
    private User owner;
    private DocumentResponse mapped;
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = spy(realService);
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

        ReflectionTestUtils.setField(documentService, "s3Bucket", "bucket");
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

    @Test
    void getById_shouldThrowException_whenNotFound() {
        UUID id = UUID.randomUUID();

        when(documentRepository.findbyIdWithReview(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllDocuments_shouldReturnPagedResult_firstPage() {
        Document d1 = document;
        Document d2 = new Document();
        d2.setId(UUID.randomUUID());
        d2.setOwner(owner);
        d2.setReview(review);
        d2.setStatus(DocumentStatus.IN_REVIEW);
        d2.setFilename("file-2");
        d2.setMime("pdf");
        d2.setSize(10);
        d2.setPageSize(2);
        d2.setCreatedAt(Instant.now());

        DocumentResponse r1 = mapped;
        DocumentResponse r2 = new DocumentResponse(
                d2.getId(), d2.getFilename(), d2.getMime(), d2.getSize(), d2.getPageSize(),
                owner.getId(), review.getId(), d2.getStatus(), d2.getCreatedAt()
        );

        PaginationRequest req = new PaginationRequest(0, 2, "createdAt", Sort.Direction.DESC);
        Pageable pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Document> page = new PageImpl<>(List.of(d1, d2), pageable, 5);
        when(documentRepository.findAll(pageable)).thenReturn(page);

        when(documentMapper.toDocumentResponse(any(Document.class))).thenAnswer(inv -> {
            Document arg = inv.getArgument(0);
            if (arg == d1) return r1;
            if (arg == d2) return r2;
            return null;
        });

        PagingResult<DocumentResponse> res = documentService.getAllDocuments(req);

        assertThat(res.content()).containsExactly(r1, r2);
        assertThat(res.totalPages()).isEqualTo(3);
        assertThat(res.totalElements()).isEqualTo(5);
        assertThat(res.size()).isEqualTo(2);
        assertThat(res.page()).isEqualTo(0);
        assertThat(res.last()).isFalse();
        assertThat(res.next()).isTrue();

        verify(documentRepository).findAll(pageable);
    }

    @Test
    void getAllDocuments_shouldReturnPagedResult_lastPage() {
        Document d1 = document;
        DocumentResponse r1 = mapped;

        PaginationRequest req = new PaginationRequest(2, 2, "createdAt", Sort.Direction.DESC);
        Pageable pageable = PageRequest.of(2, 2, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Document> page = new PageImpl<>(List.of(d1), pageable, 5);
        when(documentRepository.findAll(pageable)).thenReturn(page);

        when(documentMapper.toDocumentResponse(d1)).thenReturn(r1);

        PagingResult<DocumentResponse> res = documentService.getAllDocuments(req);

        assertThat(res.content()).containsExactly(r1);
        assertThat(res.totalPages()).isEqualTo(3);
        assertThat(res.totalElements()).isEqualTo(5);
        assertThat(res.size()).isEqualTo(2);
        assertThat(res.page()).isEqualTo(2);
        assertThat(res.last()).isTrue();
        assertThat(res.next()).isFalse();

        verify(documentRepository).findAll(pageable);
    }

    @Test
    void getUserDocuments_shouldReturnPagedResult() {
        UUID ownerId = owner.getId();

        Document only = document;
        DocumentResponse r = mapped;

        PaginationRequest req = new PaginationRequest(0, 3, "updatedAt", Sort.Direction.ASC);
        Pageable pageable = PageRequest.of(0, 3, Sort.by(Sort.Direction.ASC, "updatedAt"));

        Page<Document> page = new PageImpl<>(List.of(only), pageable, 1);
        when(documentRepository.findAllByOwner(ownerId, pageable)).thenReturn(page);
        when(documentMapper.toDocumentResponse(only)).thenReturn(r);

        PagingResult<DocumentResponse> res = documentService.getUserDocuments(req, ownerId);

        assertThat(res.content()).containsExactly(r);
        assertThat(res.totalPages()).isEqualTo(1);
        assertThat(res.totalElements()).isEqualTo(1);
        assertThat(res.size()).isEqualTo(3);
        assertThat(res.page()).isEqualTo(0);
        assertThat(res.last()).isTrue();
        assertThat(res.next()).isFalse();

        verify(documentRepository).findAllByOwner(ownerId, pageable);
    }

    @Test
    void deleteById_shouldDeleteFromS3_andFromDatabase() {
        UUID id = UUID.randomUUID();
        document.setId(id);
        document.setStorageKey("storage-key");
        when(documentRepository.getDocumentById(id)).thenReturn(Optional.of(document));

        documentService.deleteById(id);

        verify(s3Client).deleteObjects(Mockito.<Consumer<DeleteObjectsRequest.Builder>>any());
        verify(documentRepository).deleteById(id);
    }

    @Test
    void deleteById_shouldThrow_whenDocumentNotFound() {
        UUID id = UUID.randomUUID();
        when(documentRepository.getDocumentById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.deleteById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteById_shouldThrowS3DeleteException_whenS3DeletionFails() {
        UUID id = UUID.randomUUID();
        document.setId(id);
        when(documentRepository.getDocumentById(id)).thenReturn(Optional.of(document));

        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class))).thenThrow(new RuntimeException("S3 down"));

        assertThatThrownBy(() -> documentService.deleteById(id))
                .isInstanceOf(S3DeleteException.class)
                .hasMessageContaining("Couldn't delete document");

        verify(documentRepository).getDocumentById(id);
        verify(documentRepository, never()).deleteById(any());
    }

    @Test
    void downloadDocumentById_shouldReturnPresignedUrl() throws Exception {
        UUID id = UUID.randomUUID();
        document.setId(id);
        document.setMime("application/pdf");

        when(documentRepository.getDocumentById(id)).thenReturn(Optional.of(document));

        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URL("https://example.com/presigned/file.pdf"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        String url = documentService.downloadDocumentById(id);

        assertThat(url).isEqualTo("https://example.com/presigned/file.pdf");
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void downloadDocumentById_shouldThrow_whenDocumentNotFound() {
        UUID id = UUID.randomUUID();
        when(documentRepository.getDocumentById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.downloadDocumentById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

}
