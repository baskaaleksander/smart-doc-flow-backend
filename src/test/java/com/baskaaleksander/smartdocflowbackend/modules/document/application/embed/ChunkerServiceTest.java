package com.baskaaleksander.smartdocflowbackend.modules.document.application.embed;

import com.baskaaleksander.smartdocflowbackend.modules.documents.application.embed.ChunkerService;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.embed.Tokenizer;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.Chunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(MockitoExtension.class)
public class ChunkerServiceTest {

    static class FakeTokenizer implements Tokenizer{

        @Override
        public int count(String rawText) {
            return rawText == null ? 0 : rawText.length();
        }
    }

    private UUID docId;
    private ChunkerService service;

    @BeforeEach
    void setUp() {
        service = new ChunkerService(new FakeTokenizer());
        docId = UUID.randomUUID();
    }

    @Test
    void chunkPage_shouldReturnEmptyList_whenTextIsEmpty() {
        List<Chunk> chunks = service.chunkPage("", docId, 1);
        assertThat(chunks).isEmpty();
    }

    @Test
    void chunkPage_shouldReturnSingleChunk_whenShortTextFitsInLimit() {
        String text = "Hello world. This is short. End.";
        List<Chunk> chunks = service.chunkPage(text, docId, 2);

        assertThat(chunks).hasSize(1);
        Chunk ch = chunks.get(0);
        assertThat(ch.documentId()).isEqualTo(docId);
        assertThat(ch.page()).isEqualTo(2);
        assertThat(text.substring(ch.startOffset(), ch.endOffset())).isEqualTo(ch.content());
        assertThat(ch.content()).isEqualTo(text);
        assertThat(ch.startOffset()).isEqualTo(0);
        assertThat(ch.endOffset()).isEqualTo(text.length());
    }

    @Test
    void chunkPage_shouldFallbackToSingleSpan_whenNoSentenceDelimiters() {
        String text = "No delimiters here but still should be one span and one chunk if under limit";
        List<Chunk> chunks = service.chunkPage(text, docId, 3);

        assertThat(chunks).hasSize(1);
        Chunk ch = chunks.get(0);
        assertThat(ch.content()).isEqualTo(text);
        assertThat(ch.startOffset()).isEqualTo(0);
        assertThat(ch.endOffset()).isEqualTo(text.length());
        assertThat(ch.page()).isEqualTo(3);
    }
}
