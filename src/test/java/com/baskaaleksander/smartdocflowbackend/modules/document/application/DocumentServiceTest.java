package com.baskaaleksander.smartdocflowbackend.modules.document.application;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.exception.S3DeleteException;
import com.baskaaleksander.smartdocflowbackend.common.exception.S3UploadException;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.documents.api.dto.DocumentOwnerBasicInfo;
import com.baskaaleksander.smartdocflowbackend.modules.documents.api.dto.DocumentResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.api.dto.DocumentReviewBasicInfo;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.DocumentService;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.ocr.OcrTaskPublisher;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.mapping.DocumentMapper;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.DocumentRepository;
import com.baskaaleksander.smartdocflowbackend.modules.notifications.application.NotificationService;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewStatus;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEntity;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.spring.SpringDataReviewRepository;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.entity.UserEntity;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
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
    private SpringDataUserRepository userRepository;
    @Mock
    private DocumentMapper documentMapper;
    @Mock
    private SpringDataReviewRepository reviewRepository;
    @Mock
    private S3Presigner s3Presigner;
    @Mock
    private ObjectMapper MAPPER;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DocumentService realService;

    private Document document;
    private ReviewEntity review;
    private UserEntity owner;
    private DocumentResponse mapped;
    private DocumentReviewBasicInfo reviewBasic;
    private DocumentOwnerBasicInfo ownerBasic;
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = spy(realService);
        UserEntity reviewer = new UserEntity();
        reviewer.setUsername("reviewer");
        reviewer.setId(UUID.randomUUID());

        review = new ReviewEntity();
        review.setId(UUID.randomUUID());
        review.setStatus(ReviewStatus.IN_PROGRESS);
        review.setUpdatedAt(Instant.now());
        review.setReviewer(reviewer);

        owner = new UserEntity();
        owner.setId(UUID.randomUUID());
        owner.setUsername("owner");

        ownerBasic = new DocumentOwnerBasicInfo(
                owner.getId(),
                owner.getUsername()
        );

        reviewBasic = new DocumentReviewBasicInfo(
                review.getId(),
                review.getReviewer().getUsername(),
                review.getReviewer().getId(),
                review.getStatus(),
                review.getUpdatedAt()
        );

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
                ownerBasic,
                reviewBasic,
                document.getStatus(),
                document.getCreatedAt()
        );

        ReflectionTestUtils.setField(documentService, "s3Bucket", "bucket");
    }

    @Test
    void createAndSave_shouldUploadToS3_SaveInDb_MapAndTriggerSideEffects() throws Exception {
        String username = "alice";
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(username);
        SecurityContext sc = mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        byte[] content = "pdf-bytes".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "my file.pdf", "application/pdf", content
        );

        when(documentRepository.save(any(Document.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(reviewRepository.save(any(ReviewEntity.class)))
                .thenAnswer(inv -> {
                    ReviewEntity r = inv.getArgument(0);
                    if (r.getId() == null) r.setId(UUID.randomUUID());
                    return r;
                });

        when(documentMapper.toDocumentResponse(any(Document.class)))
                .thenAnswer(inv -> {
                    Document d = inv.getArgument(0);

                    DocumentOwnerBasicInfo ownerInfo = new DocumentOwnerBasicInfo(
                            d.getOwner() != null ? d.getOwner().getId() : null,
                            d.getOwner() != null ? d.getOwner().getUsername() : null
                    );

                    DocumentReviewBasicInfo reviewInfo = null;
                    if (d.getReview() != null) {
                        reviewInfo = new DocumentReviewBasicInfo(
                                d.getReview().getId(),
                                d.getReview().getReviewer() != null ? d.getReview().getReviewer().getUsername() : null,
                                d.getReview().getReviewer() != null ? d.getReview().getReviewer().getId() : null,
                                d.getReview().getStatus(),
                                d.getReview().getUpdatedAt()
                        );
                    }

                    return new DocumentResponse(
                            d.getId(),
                            d.getFilename(),
                            d.getMime(),
                            d.getSize(),
                            d.getPageSize(),
                            ownerInfo,
                            reviewInfo,
                            d.getStatus(),
                            d.getCreatedAt()
                    );
                });

        DocumentResponse res = documentService.createAndSave(file);

        ArgumentCaptor<PutObjectRequest> putReqCap = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCap = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client).putObject(putReqCap.capture(), bodyCap.capture());

        PutObjectRequest putReq = putReqCap.getValue();
        assertThat(putReq.bucket()).isEqualTo("bucket");
        assertThat(putReq.contentType()).isEqualTo("application/pdf");

        String key = putReq.key();
        assertThat(key).endsWith("_my_file.pdf");

        verify(documentRepository, times(2)).save(any(Document.class));
        verify(reviewRepository).save(any(ReviewEntity.class));

        verify(notificationService).sendNotification(eq(username), eq("document_uploaded"), contains("successfully"));
        ArgumentCaptor<UUID> enqueueCap = ArgumentCaptor.forClass(UUID.class);
        verify(ocrTaskPublisher).enqueue(enqueueCap.capture());

        ArgumentCaptor<Document> docCap = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository, atLeastOnce()).save(docCap.capture());
        Document lastSaved = docCap.getAllValues().get(docCap.getAllValues().size() - 1);
        assertThat(enqueueCap.getValue()).isEqualTo(lastSaved.getId());

        assertThat(res.id()).isEqualTo(lastSaved.getId());
        assertThat(res.filename()).isEqualTo("my_file.pdf");
        assertThat(res.mime()).isEqualTo("pdf");

        DocumentOwnerBasicInfo expectedOwner = new DocumentOwnerBasicInfo(user.getId(), username);
        assertThat(res.owner()).isEqualTo(expectedOwner);

        DocumentReviewBasicInfo expectedReview = (lastSaved.getReview() == null) ? null :
                new DocumentReviewBasicInfo(
                        lastSaved.getReview().getId(),
                        lastSaved.getReview().getReviewer() != null ? lastSaved.getReview().getReviewer().getUsername() : null,
                        lastSaved.getReview().getReviewer() != null ? lastSaved.getReview().getReviewer().getId() : null,
                        lastSaved.getReview().getStatus(),
                        lastSaved.getReview().getUpdatedAt()
                );

        assertThat(res.review()).isEqualTo(expectedReview);

        assertThat(res.status()).isEqualTo(DocumentStatus.UPLOADED);
    }

    @Test
    void createAndSave_shouldThrow_whenUserNotFound() throws Exception {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("ghost");
        SecurityContext sc = mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile(
                "file", "x.pdf", "application/pdf", new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> documentService.createAndSave(file))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(s3Client, reviewRepository, ocrTaskPublisher, notificationService, documentMapper);
    }

    @Test
    void createAndSave_shouldThrowS3UploadException_whenPutObjectFails() throws Exception {
        String username = "alice";
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(username);
        SecurityContext sc = mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        MockMultipartFile file = new MockMultipartFile(
                "file", "bad.pdf", "application/pdf", new byte[]{1, 2, 3}
        );

        doThrow(new RuntimeException("S3 down"))
                .when(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        assertThatThrownBy(() -> documentService.createAndSave(file))
                .isInstanceOf(S3UploadException.class)
                .hasMessageContaining("Upload to object store failed");

        verify(documentRepository, never()).save(any());
        verify(reviewRepository, never()).save(any());
        verify(ocrTaskPublisher, never()).enqueue(any());
        verify(notificationService, never()).sendNotification(any(), any(), any());
        verify(documentMapper, never()).toDocumentResponse(any());
    }

    @Test
    void createAndSave_shouldAcceptNonPdfContentType_butKeepInternalMimePdf_andReplaceSpaces() throws Exception {
        String username = "bob";
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(username);
        SecurityContext sc = mock(SecurityContext.class);
        when(sc.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(sc);

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        MockMultipartFile file = new MockMultipartFile(
                "file", "scan doc.png", "image/png", "bytes".getBytes()
        );

        when(documentRepository.save(any(Document.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(reviewRepository.save(any(ReviewEntity.class)))
                .thenAnswer(inv -> {
                    ReviewEntity r = inv.getArgument(0);
                    if (r.getId() == null) r.setId(UUID.randomUUID());
                    return r;
                });

        when(documentMapper.toDocumentResponse(any(Document.class)))
                .thenAnswer(inv -> {
                    Document d = inv.getArgument(0);

                    DocumentOwnerBasicInfo ownerInfo = (d.getOwner() == null)
                            ? null
                            : new DocumentOwnerBasicInfo(d.getOwner().getId(), d.getOwner().getUsername());

                    DocumentReviewBasicInfo reviewInfo = null;
                    if (d.getReview() != null) {
                        reviewInfo = new DocumentReviewBasicInfo(
                                d.getReview().getId(),
                                d.getReview().getReviewer() != null ? d.getReview().getReviewer().getUsername() : null,
                                d.getReview().getReviewer() != null ? d.getReview().getReviewer().getId() : null,
                                d.getReview().getStatus(),
                                d.getReview().getUpdatedAt()
                        );
                    }

                    return new DocumentResponse(
                            d.getId(),
                            d.getFilename(),
                            d.getMime(),
                            d.getSize(),
                            d.getPageSize(),
                            ownerInfo,
                            reviewInfo,
                            d.getStatus(),
                            d.getCreatedAt()
                    );
                });

        DocumentResponse res = documentService.createAndSave(file);

        assertThat(res.mime()).isEqualTo("pdf");
        assertThat(res.filename()).isEqualTo("scan_doc.png");

        ArgumentCaptor<PutObjectRequest> putReqCap = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(putReqCap.capture(), any(RequestBody.class));
        assertThat(putReqCap.getValue().contentType()).isEqualTo("image/png");

        assertThat(res.owner()).isEqualTo(new DocumentOwnerBasicInfo(user.getId(), username));


        verify(notificationService).sendNotification(eq(username), eq("document_uploaded"), contains("successfully"));
        verify(ocrTaskPublisher).enqueue(any(UUID.class));
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
        assertThat(response.owner()).isEqualTo(mapped.owner());
        assertThat(response.review()).isEqualTo(mapped.review());
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
                d2.getId(),
                d2.getFilename(),
                d2.getMime(),
                d2.getSize(),
                d2.getPageSize(),
                new DocumentOwnerBasicInfo(owner.getId(), owner.getUsername()),
                new DocumentReviewBasicInfo(
                        review.getId(),
                        review.getReviewer() != null ? review.getReviewer().getUsername() : null,
                        review.getReviewer() != null ? review.getReviewer().getId() : null,
                        review.getStatus(),
                        review.getUpdatedAt()
                ),
                d2.getStatus(),
                d2.getCreatedAt()
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

        PagingResult<DocumentResponse> res = documentService.getAllDocuments(req, false, UUID.randomUUID());

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

        PagingResult<DocumentResponse> res = documentService.getAllDocuments(req, false, UUID.randomUUID());

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
