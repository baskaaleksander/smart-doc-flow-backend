package com.baskaaleksander.smartdocflowbackend.modules.documents.application.document;

import com.baskaaleksander.smartdocflowbackend.common.exception.InvalidFileTypeException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.exception.S3UploadException;
import com.baskaaleksander.smartdocflowbackend.common.logging.LoggingPort;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.contracts.NotificationEvent;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.DocumentApiMapper;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto.DocumentResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto.DocumentStatsResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.OcrTask;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentReviewBasic;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentUserBasic;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.*;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.view.DocumentStatusCount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentServiceTest {

    @Mock
    private DocumentDomainEventPublisherPort publisher;
    @Mock
    private OcrTaskPublisherPort taskPublisher;
    @Mock
    private DocumentCommandPort documentCommandPort;
    @Mock
    private DocumentQueryPort documentQueryPort;
    @Mock
    private DocumentUserQueryPort documentUserQueryPort;
    @Mock
    private DocumentApiMapper mapper;
    @Mock
    private FileStoragePort fileStoragePort;
    @Mock
    private LoggingPort logger;

    @InjectMocks
    private DocumentService service;

    private SecurityContext securityContext;
    private Authentication authentication;

    @BeforeEach
    void setupSecurity() {
        securityContext = mock(SecurityContext.class);
        authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn("john");
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createAndSave_success() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("my doc.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(123L);
        InputStream is = new ByteArrayInputStream(new byte[]{1, 2, 3});
        when(file.getInputStream()).thenReturn(is);

        DocumentUserBasic user = new DocumentUserBasic(UUID.randomUUID(), "john");
        when(documentUserQueryPort.findByUsername("john")).thenReturn(Optional.of(user));

        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        when(documentCommandPort.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));
        DocumentResponse dto = mock(DocumentResponse.class);
        when(mapper.toResponse(any(Document.class))).thenReturn(dto);

        DocumentResponse out = service.createAndSave(file);

        assertThat(out).isEqualTo(dto);
        verify(fileStoragePort).upload(any(InputStream.class), matches(".*my_doc.pdf$"), eq("application/pdf"), eq(123L));
        verify(documentCommandPort).save(docCaptor.capture());
        Document saved = docCaptor.getValue();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getFilename()).isEqualTo("my_doc.pdf");
        assertThat(saved.getStorageKey()).contains("_my_doc.pdf");
        assertThat(saved.getMime()).isEqualTo("application/pdf");
        assertThat(saved.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
        assertThat(saved.getOwner()).isEqualTo(user);
        assertThat(saved.getReview()).isInstanceOf(DocumentReviewBasic.class);
        verify(publisher).publish(any(NotificationEvent.class));
        verify(taskPublisher).publish(argThat(t -> t instanceof OcrTask ot && ot.documentId().equals(saved.getId())));
    }

    @Test
    void createAndSave_invalidMime_throws() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("x.png");
        when(file.getContentType()).thenReturn("image/png");

        assertThatThrownBy(() -> service.createAndSave(file))
                .isInstanceOf(InvalidFileTypeException.class);
        verifyNoInteractions(fileStoragePort, documentCommandPort);
    }

    @Test
    void createAndSave_userNotFound_throws() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("a.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(documentUserQueryPort.findByUsername("john")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createAndSave(file))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(fileStoragePort, documentCommandPort);
    }

    @Test
    void createAndSave_inputStreamError_throwsS3UploadException() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("a.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(documentUserQueryPort.findByUsername("john")).thenReturn(Optional.of(new DocumentUserBasic(UUID.randomUUID(), "john")));
        when(file.getInputStream()).thenThrow(new RuntimeException("io"));

        assertThatThrownBy(() -> service.createAndSave(file))
                .isInstanceOf(S3UploadException.class);
        verifyNoInteractions(fileStoragePort, documentCommandPort);
    }

    @Test
    void getById_success() {
        UUID docId = UUID.randomUUID();
        Document doc = new Document.Builder()
                .id(docId)
                .filename("file.pdf")
                .mime("application/pdf")
                .size(2.0)
                .storageKey(docId + "_file.pdf")
                .pageSize(0)
                .status(DocumentStatus.UPLOADED)
                .build();
        when(documentQueryPort.findByIdWithReview(docId)).thenReturn(Optional.of(doc));
        DocumentResponse dto = mock(DocumentResponse.class);
        when(mapper.toResponse(doc)).thenReturn(dto);

        DocumentResponse out = service.getById(docId);

        assertThat(out).isEqualTo(dto);
    }

    @Test
    void getById_notFound() {
        UUID id = UUID.randomUUID();
        when(documentQueryPort.findByIdWithReview(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllDocuments_all() {
        PaginationRequest req = new PaginationRequest(0, 2, null, null);
        UUID docId1 = UUID.randomUUID();
        Document doc1 = new Document.Builder()
                .id(docId1)
                .filename("file.pdf")
                .mime("application/pdf")
                .size(2.0)
                .storageKey(docId1 + "_file.pdf")
                .pageSize(0)
                .status(DocumentStatus.UPLOADED)
                .build();

        UUID docId2 = UUID.randomUUID();
        Document doc2 = new Document.Builder()
                .id(docId2)
                .filename("file.pdf")
                .mime("application/pdf")
                .size(2.0)
                .storageKey(docId2 + "_file.pdf")
                .pageSize(0)
                .status(DocumentStatus.UPLOADED)
                .build();

        PagingResult<Document> page = new PagingResult<>(List.of(doc1), 1, 1L, 2, 0, true, false);
        when(documentQueryPort.findAll(req)).thenReturn(page);
        DocumentResponse r1 = mock(DocumentResponse.class);
        when(mapper.toResponse(doc1)).thenReturn(r1);

        PagingResult<DocumentResponse> out = service.getAllDocuments(req, false, UUID.randomUUID());

        assertThat(out.content()).containsExactly(r1);
        verify(documentQueryPort).findAll(req);
        verify(documentQueryPort, never()).findAllByReviewer(any(), any());
    }

    @Test
    void getAllDocuments_assignedToMe() {
        PaginationRequest req = new PaginationRequest(1, 5, "createdAt,desc", null);
        UUID reviewer = UUID.randomUUID();
        UUID docId1 = UUID.randomUUID();
        Document doc1 = new Document.Builder()
                .id(docId1)
                .filename("file.pdf")
                .mime("application/pdf")
                .size(2.0)
                .storageKey(docId1 + "_file.pdf")
                .pageSize(0)
                .status(DocumentStatus.UPLOADED)
                .build();

        UUID docId2 = UUID.randomUUID();
        Document doc2 = new Document.Builder()
                .id(docId2)
                .filename("file.pdf")
                .mime("application/pdf")
                .size(2.0)
                .storageKey(docId2 + "_file.pdf")
                .pageSize(0)
                .status(DocumentStatus.UPLOADED)
                .build();

        PagingResult<Document> page = new PagingResult<>(List.of(doc1, doc2), 2, 2L, 5, 1, true, false);
        when(documentQueryPort.findAllByReviewer(reviewer, req)).thenReturn(page);
        when(mapper.toResponse(doc1)).thenReturn(mock(DocumentResponse.class));
        when(mapper.toResponse(doc2)).thenReturn(mock(DocumentResponse.class));

        PagingResult<DocumentResponse> out = service.getAllDocuments(req, true, reviewer);

        assertThat(out.totalElements()).isEqualTo(2L);
        verify(documentQueryPort).findAllByReviewer(reviewer, req);
    }

    @Test
    void getUserDocuments_success() {
        PaginationRequest req = new PaginationRequest(0, 3, null, null);
        UUID ownerId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        Document doc = new Document.Builder()
                .id(docId)
                .filename("file.pdf")
                .mime("application/pdf")
                .size(2.0)
                .storageKey(docId + "_file.pdf")
                .pageSize(0)
                .status(DocumentStatus.UPLOADED)
                .build();
        PagingResult<Document> page = new PagingResult<>(List.of(doc), 1, 1L, 3, 0, true, false);
        when(documentQueryPort.findAllByOwner(ownerId, req)).thenReturn(page);
        DocumentResponse r1 = mock(DocumentResponse.class);
        when(mapper.toResponse(doc)).thenReturn(r1);

        PagingResult<DocumentResponse> out = service.getUserDocuments(req, ownerId);

        assertThat(out.content()).containsExactly(r1);
        verify(documentQueryPort).findAllByOwner(ownerId, req);
    }

    @Test
    void deleteById_success() {
        UUID docId = UUID.randomUUID();
        Document doc = new Document.Builder()
                .id(docId)
                .filename("file.pdf")
                .mime("application/pdf")
                .size(2.0)
                .storageKey(docId + "_file.pdf")
                .pageSize(0)
                .status(DocumentStatus.UPLOADED)
                .build();

        when(documentQueryPort.getDocumentById(docId)).thenReturn(Optional.of(doc));

        service.deleteById(docId);

        verify(fileStoragePort).delete(docId + "_file.pdf");
        verify(documentCommandPort).deleteById(docId);
    }

    @Test
    void deleteById_notFound() {
        UUID id = UUID.randomUUID();
        when(documentQueryPort.getDocumentById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteById(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(fileStoragePort);
    }

    @Test
    void downloadDocumentById_success() {
        UUID docId = UUID.randomUUID();
        Document doc = new Document.Builder()
                .id(docId)
                .filename("file.pdf")
                .mime("application/pdf")
                .size(2.0)
                .storageKey(docId + "_file.pdf")
                .pageSize(0)
                .status(DocumentStatus.UPLOADED)
                .build();

        when(documentQueryPort.getDocumentById(docId)).thenReturn(Optional.of(doc));
        when(fileStoragePort.getPresignedUrl(docId + "_file.pdf", "application/pdf", 3L)).thenReturn("url");

        String url = service.downloadDocumentById(docId);

        assertThat(url).isEqualTo("url");
    }

    @Test
    void downloadDocumentById_notFound() {
        UUID id = UUID.randomUUID();
        when(documentQueryPort.getDocumentById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.downloadDocumentById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getDocumentStats_aggregatesFailed() {
        DocumentStatusCount c1 = mock(DocumentStatusCount.class);
        when(c1.getStatus()).thenReturn(DocumentStatus.REVIEW_PENDING);
        when(c1.getCount()).thenReturn(2L);
        DocumentStatusCount c2 = mock(DocumentStatusCount.class);
        when(c2.getStatus()).thenReturn(DocumentStatus.IN_REVIEW);
        when(c2.getCount()).thenReturn(3L);
        DocumentStatusCount c3 = mock(DocumentStatusCount.class);
        when(c3.getStatus()).thenReturn(DocumentStatus.REVIEWED);
        when(c3.getCount()).thenReturn(4L);
        DocumentStatusCount c4 = mock(DocumentStatusCount.class);
        when(c4.getStatus()).thenReturn(DocumentStatus.OCR_FAILED);
        when(c4.getCount()).thenReturn(1L);
        DocumentStatusCount c5 = mock(DocumentStatusCount.class);
        when(c5.getStatus()).thenReturn(DocumentStatus.EMBED_FAILED);
        when(c5.getCount()).thenReturn(5L);
        when(documentQueryPort.countDocumentsByStatus()).thenReturn(List.of(c1, c2, c3, c4, c5));

        DocumentStatsResponse stats = service.getDocumentStats();

        assertThat(stats.pendingReview()).isEqualTo(2L);
        assertThat(stats.inReview()).isEqualTo(3L);
        assertThat(stats.reviewed()).isEqualTo(4L);
        assertThat(stats.failed()).isEqualTo(6L);
    }
}