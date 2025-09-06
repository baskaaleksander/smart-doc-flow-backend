package com.baskaaleksander.smartdocflowbackend.service;

import com.baskaaleksander.smartdocflowbackend.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

@Service
public class OcrService {

    @Value(value = "${minio.bucket.name}")
    private String bucket;
    private final S3Client s3Client;
    private final DocumentRepository documentRepository;

    @Autowired
    public OcrService(
            S3Client s3Client,
            DocumentRepository documentRepository
    ) {
        this.s3Client = s3Client;
        this.documentRepository = documentRepository;
    }

    @Async
    public void startAsync(UUID documentId) {


    }

    private File getPdfFromS3(String documentKey) throws IOException {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(documentKey)
                .build();

        File tempFile = File.createTempFile("s3-", "-" + documentKey.replace("/", "_"));
        try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);) {
            FileOutputStream fos = new FileOutputStream(tempFile);
            s3Object.transferTo(fos);
        }

        return tempFile;
    }
}
