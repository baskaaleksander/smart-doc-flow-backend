package com.baskaaleksander.smartdocflowbackend.modules.testsupport.pipeline;

import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.messaging.in.EmbeddingRabbitListenerAdapter;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.messaging.in.OcrRabbitListenerAdapter;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Image;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class PipelineTestConfig {

    private static final AtomicBoolean ocrShouldFail = new AtomicBoolean(false);
    private static final AtomicBoolean embedShouldFail = new AtomicBoolean(false);

    @Bean
    @Primary
    public FileStoragePort inMemoryFileStoragePort() {
        return new InMemoryFileStoragePort();
    }

    @Bean
    @Primary
    public PdfRendererPort pdfRendererPort() {
        return (file, dpi) -> List.of(
                new Image("image-page-1".getBytes(StandardCharsets.UTF_8), "image/png", 1),
                new Image("image-page-2".getBytes(StandardCharsets.UTF_8), "image/png", 2)
        );
    }

    @Bean
    @Primary
    public OcrEnginePort ocrEnginePort() {
        return images -> {
            if (ocrShouldFail.get()) {
                throw new RuntimeException("Simulated OCR failure");
            }
            return """
                    {
                      "pages": [
                        {"page": 1, "text": "First page. Important content."},
                        {"page": 2, "text": "Second page. More insights."}
                      ]
                    }
                    """;
        };
    }

    @Bean
    @Primary
    public VectorIndexPort vectorIndexPort() {
        return docs -> {
            if (embedShouldFail.get()) {
                throw new RuntimeException("Simulated embedding failure");
            }
        };
    }

    @Bean
    @Primary
    public ChatCompletionPort chatCompletionPort() {
        return mock(ChatCompletionPort.class);
    }

    @Bean
    @Primary
    public TaskExecutor pipelineTaskExecutor() {
        return new SimpleAsyncTaskExecutor("pipeline-test-");
    }

    @Bean
    @Primary
    public OcrTaskPublisherPort ocrTaskPublisherPort(OcrRabbitListenerAdapter listener, TaskExecutor pipelineTaskExecutor) {
        return task -> pipelineTaskExecutor.execute(() -> listener.onTask(task));
    }

    @Bean
    @Primary
    public EmbeddingTaskPublisherPort embeddingTaskPublisherPort(EmbeddingRabbitListenerAdapter listener, TaskExecutor pipelineTaskExecutor) {
        return task -> pipelineTaskExecutor.execute(() -> listener.onTask(task));
    }

    @Bean
    @Primary
    public RecordingDocumentCommandPort recordingDocumentCommandPort(
            @Qualifier("documentJpaAdapter") DocumentCommandPort delegate
    ) {
        return new RecordingDocumentCommandPort(delegate);
    }

    public static void setOcrFailure(boolean fail) {
        ocrShouldFail.set(fail);
    }

    public static void setEmbedFailure(boolean fail) {
        embedShouldFail.set(fail);
    }
}
