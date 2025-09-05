package com.baskaaleksander.smartdocflowbackend.service;

import com.baskaaleksander.smartdocflowbackend.enums.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.model.Document;
import com.baskaaleksander.smartdocflowbackend.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final S3Client s3Client;

    @Value(value = "${minio.bucket.name}")
    private String s3Bucket;

    public DocumentService(DocumentRepository documentRepository, S3Client s3Client) {
        this.documentRepository = documentRepository;
        this.s3Client = s3Client;
    }

    public Document createAndSave(MultipartFile file) throws IOException {
        UUID docId = UUID.randomUUID();
        String originalFilename = file.getOriginalFilename();
        String filename = docId + "_" + originalFilename;
        String extension = StringUtils.getFilenameExtension(originalFilename);

        Document document = new Document(
                docId,
                originalFilename,
                extension,
                file.getSize(),
                filename,
                1,
                DocumentStatus.PROCESSING,
                LocalDateTime.now()
        );

        documentRepository.save(document);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(filename)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));


        return document;
    }

}
