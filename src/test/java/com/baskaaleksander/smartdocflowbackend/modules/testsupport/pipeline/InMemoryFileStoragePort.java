package com.baskaaleksander.smartdocflowbackend.modules.testsupport.pipeline;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.FileStoragePort;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryFileStoragePort implements FileStoragePort {
    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();

    @Override
    public void upload(InputStream inputStream, String key, String contentType, long size) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            storage.put(key, bytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store key " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        storage.remove(key);
    }

    @Override
    public String getPresignedUrl(String storageKey, String mime, Long duration) {
        return "http://localhost/mock/" + storageKey;
    }

    @Override
    public String getJsonFileValue(String storageKey) {
        byte[] bytes = storage.get(storageKey);
        if (bytes == null) {
            throw new IllegalStateException("No data stored under key " + storageKey);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public File getPdfFile(String storageKey) {
        byte[] bytes = storage.get(storageKey);
        if (bytes == null) {
            throw new IllegalStateException("No PDF stored under key " + storageKey);
        }
        try {
            File temp = File.createTempFile("pdf-", ".pdf");
            Files.write(temp.toPath(), bytes);
            temp.deleteOnExit();
            return temp;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp file for key " + storageKey, e);
        }
    }

    public void clear() {
        storage.clear();
    }
}
