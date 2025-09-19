package com.baskaaleksander.smartdocflowbackend.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class ChunkerService {

    private List<String> splitIntoSentences(String rawText) {
        return Arrays.stream(rawText.split("(?<=[.!?])\\\\s+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public List<?> chunkPage(String rawText, UUID documentId, int page) {
        return null;
    }
}
