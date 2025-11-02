package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.messaging.in.consumer;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.logging.LoggingPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.EmbedTask;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.OcrTask;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentOcrResult;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Image;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcrTaskConsumerServiceTest {

    @Mock
    private EmbeddingTaskPublisherPort taskPublisher;
    @Mock
    private FileStoragePort fileStoragePort;
    @Mock
    private DocumentQueryPort documentQueryPort;
    @Mock
    private DocumentCommandPort documentCommandPort;
    @Mock
    private DocumentOcrResultCommandPort documentOcrResultCommandPort;
    @Mock
    private OcrEnginePort ocrEnginePort;
    @Mock
    private PdfRendererPort pdfRendererPort;
    @Mock
    private LoggingPort logger;

    @InjectMocks
    private OcrTaskConsumerService service;

    @Test
    void handle_success_processesOcr_savesJson_updatesStatus_andPublishesEmbedTask() {
        UUID docId = UUID.randomUUID();
        Document doc = new Document();
        doc.setId(docId);
        doc.setStorageKey("s3://bucket/file.pdf");

        when(documentQueryPort.getDocumentById(docId)).thenReturn(Optional.of(doc));
        File tmp = new File("dummy.pdf");
        when(fileStoragePort.getPdfFile("s3://bucket/file.pdf")).thenReturn(tmp);

        Image img = mock(Image.class);
        when(pdfRendererPort.render(tmp, 300)).thenReturn(List.of(img, img));

        when(ocrEnginePort.extractText(anyList())).thenReturn("{\"pages\":[]}");

        ArgumentCaptor<InputStream> isCaptor = ArgumentCaptor.forClass(InputStream.class);
        doAnswer(inv -> null).when(fileStoragePort).upload(isCaptor.capture(), eq(docId + "_ocr.json"),
                eq("application/json"), anyLong());

        when(documentQueryPort.getDocumentById(docId)).thenReturn(Optional.of(doc), Optional.of(doc));

        OcrTask task = new OcrTask(docId);
        service.handle(task);

        verify(fileStoragePort).upload(any(InputStream.class), eq(docId + "_ocr.json"),
                eq("application/json"), anyLong());

        verify(documentOcrResultCommandPort).save(argThat(ocr -> {
            return docId.equals(ocr.getDocumentId()) && (docId + "_ocr.json").equals(ocr.getStorageKey());
        }));

        verify(documentCommandPort).updateStatus(docId, DocumentStatus.TEXT_READY);
        verify(taskPublisher).publish(argThat(t -> t instanceof EmbedTask et && et.documentId().equals(docId)));
    }

    @Test
    void handle_documentNotFound_throws() {
        UUID docId = UUID.randomUUID();
        when(documentQueryPort.getDocumentById(docId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(new OcrTask(docId)))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(fileStoragePort, pdfRendererPort, ocrEnginePort, documentOcrResultCommandPort, documentCommandPort, taskPublisher);
    }

    @Test
    void handle_documentHasNoKey_throws() {
        UUID docId = UUID.randomUUID();
        Document doc = new Document();
        doc.setId(docId);
        doc.setStorageKey(null);
        when(documentQueryPort.getDocumentById(docId)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> service.handle(new OcrTask(docId)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("has no key");

        verifyNoInteractions(fileStoragePort, pdfRendererPort, ocrEnginePort, documentOcrResultCommandPort, documentCommandPort, taskPublisher);
    }

    @Test
    void handle_pdfRendererThrows_wrappedInRuntimeException() {
        UUID docId = UUID.randomUUID();
        Document doc = new Document();
        doc.setId(docId);
        doc.setStorageKey("key.pdf");
        when(documentQueryPort.getDocumentById(docId)).thenReturn(Optional.of(doc));
        when(fileStoragePort.getPdfFile("key.pdf")).thenReturn(new File("x"));
        when(pdfRendererPort.render(any(File.class), eq(300))).thenThrow(new RuntimeException("render failed"));

        assertThatThrownBy(() -> service.handle(new OcrTask(docId)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("processing failed");

        verify(documentCommandPort, never()).updateStatus(any(), any());
        verify(taskPublisher, never()).publish(any());
    }

    @Test
    void saveOcrResultToDb_missingOnSecondLookup_throws() {
        UUID docId = UUID.randomUUID();
        Document doc = new Document();
        doc.setId(docId);
        doc.setStorageKey("key.pdf");

        when(documentQueryPort.getDocumentById(docId)).thenReturn(Optional.of(doc));
        when(fileStoragePort.getPdfFile("key.pdf")).thenReturn(new File("x"));
        when(pdfRendererPort.render(any(File.class), eq(300))).thenReturn(List.of(mock(Image.class)));
        when(ocrEnginePort.extractText(anyList())).thenReturn("{\"pages\":[]}");

        when(documentQueryPort.getDocumentById(docId)).thenReturn(Optional.of(doc), Optional.empty());

        assertThatThrownBy(() -> service.handle(new OcrTask(docId)))
                .isInstanceOf(RuntimeException.class);

        verify(documentOcrResultCommandPort, never()).save(any(DocumentOcrResult.class));
        verify(documentCommandPort, never()).updateStatus(any(), any());
        verify(taskPublisher, never()).publish(any());
    }
}