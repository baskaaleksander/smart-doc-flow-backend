package com.baskaaleksander.smartdocflowbackend.modules.documents.application;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.Chunk;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class ChunkerService {

    private final Tokenizer tokenizer;

    public ChunkerService(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    private List<String> splitIntoSentences(String rawText) {
        return Arrays.stream(rawText.split("(?<=[.!?])\\\\s+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public List<Chunk> chunkPage(String rawText, UUID documentId, int page) {
        int maxTokens = 700;
        int overlap = 120;

        List<String> sentences = splitIntoSentences(rawText);
        List<Chunk> chunks = new ArrayList<>();

        StringBuilder current = new StringBuilder();
        int tokenCount = 0;
        int startOffset = 0;


        for (String sentence : sentences) {
            int sentenceTokens = tokenizer.count(sentence);

            if (tokenCount + sentenceTokens > maxTokens) {

            }
        }
        return null;
    }
}
