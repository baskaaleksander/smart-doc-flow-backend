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
}
