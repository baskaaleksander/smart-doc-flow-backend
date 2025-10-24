package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.ai.chunking;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Chunk;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.TokenizerPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkerServiceAdapterTest {

    static class CharCountTokenizer implements TokenizerPort {
        @Override
        public int count(String text) {
            return text == null ? 0 : text.length();
        }
    }

    private final TokenizerPort tokenizer = new CharCountTokenizer();
    private final ChunkerServiceAdapter adapter = new ChunkerServiceAdapter(tokenizer);

    @Test
    void chunkPage_emptyText_returnsEmptyList() {
        List<Chunk> chunks = adapter.chunkPage("", UUID.randomUUID(), 1);
        assertThat(chunks).isEmpty();
    }

    @Test
    void chunkPage_whitespaceOnly_returnsEmptyList() {
        List<Chunk> chunks = adapter.chunkPage("   \n\t  ", UUID.randomUUID(), 2);
        assertThat(chunks).isEmpty();
    }

    @Test
    void chunkPage_shortText_returnsSingleChunk() {
        UUID docId = UUID.randomUUID();
        String text = "Short sentence one. Another short sentence.";
        List<Chunk> chunks = adapter.chunkPage(text, docId, 3);

        assertThat(chunks).hasSize(1);
        Chunk c = chunks.get(0);
        assertThat(c.documentId()).isEqualTo(docId);
        assertThat(c.page()).isEqualTo(3);
        assertThat(c.startOffset()).isEqualTo(0);
        assertThat(c.endOffset()).isEqualTo(text.length());
        assertThat(c.content()).isEqualTo(text);
    }

    @Test
    void chunkPage_longSingleSentence_isSplitWithOverlap() {
        UUID docId = UUID.randomUUID();
        String unit = "lorem ipsum ";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) sb.append(unit);
        sb.append(".");
        String longSentence = sb.toString();

        List<Chunk> chunks = adapter.chunkPage(longSentence, docId, 4);

        assertThat(chunks.size()).isGreaterThan(1);

        Chunk first = chunks.get(0);
        Chunk last = chunks.get(chunks.size() - 1);

        assertThat(first.startOffset()).isEqualTo(0);
        assertThat(first.endOffset()).isLessThan(longSentence.length());
        assertThat(last.endOffset()).isEqualTo(longSentence.length());

        for (int i = 1; i < chunks.size(); i++) {
            Chunk prev = chunks.get(i - 1);
            Chunk curr = chunks.get(i);
            assertThat(curr.startOffset()).isLessThan(prev.endOffset());
            if (curr.startOffset() > 0) {
                char before = longSentence.charAt(Math.max(0, curr.startOffset() - 1));
                assertThat(Character.isWhitespace(before)).isTrue();
            }
        }
    }

    @Test
    void chunkPage_multipleSentences_exceedMax_createsMultipleChunks() {
        UUID docId = UUID.randomUUID();
        String s1 = "A".repeat(300) + ". ";
        String s2 = "B".repeat(300) + ". ";
        String s3 = "C".repeat(300) + ". ";
        String s4 = "D".repeat(300) + ".";
        String text = s1 + s2 + s3 + s4;

        List<Chunk> chunks = adapter.chunkPage(text, docId, 5);

        assertThat(chunks.size()).isGreaterThan(1);

        int coveredEnd = 0;
        for (int i = 0; i < chunks.size(); i++) {
            Chunk c = chunks.get(i);
            assertThat(c.documentId()).isEqualTo(docId);
            assertThat(c.page()).isEqualTo(5);
            assertThat(c.startOffset()).isLessThan(c.endOffset());
            assertThat(c.content()).isEqualTo(text.substring(c.startOffset(), c.endOffset()));
            coveredEnd = Math.max(coveredEnd, c.endOffset());
            if (i > 0) {
                Chunk prev = chunks.get(i - 1);
                assertThat(c.startOffset()).isLessThan(prev.endOffset());
            }
        }
        assertThat(coveredEnd).isEqualTo(text.length());
    }
}