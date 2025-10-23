package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port;

import org.springframework.web.multipart.MultipartFile;

public interface FileStoragePort {
    void upload(MultipartFile file, String filename);
    void delete(String key);
    String getPresignedUrl(String storageKey, String mime, Long duration);
}
