package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileStoragePort {
    void upload(InputStream inputStream, String key, String contentType, long size);
    void delete(String key);
    String getPresignedUrl(String storageKey, String mime, Long duration);
    String getJsonFileValue(String storageKey);
}
