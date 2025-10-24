package com.baskaaleksander.smartdocflowbackend.modules.documents.application.job;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.DocumentCommandPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class JobStatusServiceTest {

    @Mock
    private DocumentCommandPort documentCommandPort;

    @InjectMocks
    private JobStatusService service;

    @Test
    void markFailedOcr_updatesStatusToOcrFailed() {
        UUID id = UUID.randomUUID();

        service.markFailedOcr(id);

        verify(documentCommandPort).updateStatus(id, DocumentStatus.OCR_FAILED);
        verifyNoMoreInteractions(documentCommandPort);
    }

    @Test
    void markFailedEmbed_updatesStatusToEmbedFailed() {
        UUID id = UUID.randomUUID();

        service.markFailedEmbed(id);

        verify(documentCommandPort).updateStatus(id, DocumentStatus.EMBED_FAILED);
        verifyNoMoreInteractions(documentCommandPort);
    }

    @Test
    void markTextReady_updatesStatusToTextReady() {
        UUID id = UUID.randomUUID();

        service.markTextReady(id);

        verify(documentCommandPort).updateStatus(id, DocumentStatus.TEXT_READY);
        verifyNoMoreInteractions(documentCommandPort);
    }

    @Test
    void markReviewPending_updatesStatusToReviewPending() {
        UUID id = UUID.randomUUID();

        service.markReviewPending(id);

        verify(documentCommandPort).updateStatus(id, DocumentStatus.REVIEW_PENDING);
        verifyNoMoreInteractions(documentCommandPort);
    }

    @Test
    void markInProgressOcr_updatesStatusToInProgressOcr() {
        UUID id = UUID.randomUUID();

        service.markInProgressOcr(id);

        verify(documentCommandPort).updateStatus(id, DocumentStatus.IN_PROGRESS_OCR);
        verifyNoMoreInteractions(documentCommandPort);
    }

    @Test
    void markInProgressEmbed_updatesStatusToInProgressEmbed() {
        UUID id = UUID.randomUUID();

        service.markInProgressEmbed(id);

        verify(documentCommandPort).updateStatus(id, DocumentStatus.IN_PROGRESS_EMBED);
        verifyNoMoreInteractions(documentCommandPort);
    }
}