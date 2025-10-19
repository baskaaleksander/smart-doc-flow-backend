package com.baskaaleksander.smartdocflowbackend.modules.document.application.embed;

import com.baskaaleksander.smartdocflowbackend.modules.documents.application.embed.ChunkerService;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.embed.Tokenizer;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Chunk;
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

    @Test
    void chunkPage_shouldHandleVeryLongSingleWord_asSingleHardChunk() {
        String veryLong = "a".repeat(800) + ".";
        List<Chunk> chunks = service.chunkPage(veryLong, docId, 4);

        assertThat(chunks).hasSize(1);
        Chunk ch = chunks.get(0);
        assertThat(ch.content().length()).isEqualTo(veryLong.length());
        assertThat(ch.startOffset()).isEqualTo(0);
        assertThat(ch.endOffset()).isEqualTo(veryLong.length());
        assertThat(ch.page()).isEqualTo(4);
    }

    @Test
    void chunkPage_shouldProduceMultipleOverlappingChunks_whenTextExceedsLimit() {
        String sentence = "x".repeat(198) + ". ";
        String text = sentence.repeat(5); // ~1000+ znaków

        List<Chunk> chunks = service.chunkPage(text, docId, 5);

        assertThat(chunks.size()).isGreaterThanOrEqualTo(2);

        for (Chunk ch : chunks) {
            assertThat(ch.documentId()).isEqualTo(docId);
            assertThat(ch.page()).isEqualTo(5);
            assertThat(text.substring(ch.startOffset(), ch.endOffset())).isEqualTo(ch.content());
            assertThat(ch.content().length()).isLessThanOrEqualTo(700);
        }

        for (int i = 0; i < chunks.size() - 1; i++) {
            Chunk a = chunks.get(i);
            Chunk b = chunks.get(i + 1);
            assertThat(b.startOffset()).isLessThanOrEqualTo(a.endOffset());

            if (b.startOffset() > 0) {
                char prev = text.charAt(b.startOffset() - 1);
                assertThat(Character.isWhitespace(prev)).isTrue();
            }
        }

        int expectedEnd = text.stripTrailing().length();
        assertThat(chunks.get(chunks.size() - 1).endOffset()).isEqualTo(expectedEnd);
    }

    @Test
    void chunkPage_shouldStartNextChunkAtAlignedBoundary_afterSplit() {
        String s1 = ("alpha " + "a".repeat(690) + ". ");
        String s2 = ("beta " + "b".repeat(690) + ". ");
        String text = s1 + s2;

        List<Chunk> chunks = service.chunkPage(text, docId, 6);

        assertThat(chunks.size()).isGreaterThanOrEqualTo(2);

        for (int i = 1; i < chunks.size(); i++) {
            int start = chunks.get(i).startOffset();
            if (start > 0) {
                assertThat(Character.isWhitespace(text.charAt(start - 1))).isTrue();
            }
        }
    }
}
