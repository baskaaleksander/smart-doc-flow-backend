package com.baskaaleksander.smartdocflowbackend.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OcrService {

    @Async
    public void startAsync(UUID documentId) {

    }
}
