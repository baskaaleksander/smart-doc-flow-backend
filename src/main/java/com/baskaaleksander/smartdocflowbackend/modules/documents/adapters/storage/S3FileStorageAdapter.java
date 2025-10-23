package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.storage;

import com.baskaaleksander.smartdocflowbackend.common.exception.S3DeleteException;
import com.baskaaleksander.smartdocflowbackend.common.exception.S3DownloadException;
import com.baskaaleksander.smartdocflowbackend.common.exception.S3UploadException;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.FileStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

@Component
public class S3FileStorageAdapter implements FileStoragePort {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    @Value(value = "${minio.bucket.name}")
    private String s3Bucket;
    private final Logger log = LoggerFactory.getLogger(S3FileStorageAdapter.class);


    public S3FileStorageAdapter(
            S3Client s3Client,
            S3Presigner s3Presigner
            ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }
    @Override
    public void upload(MultipartFile file, String filename) {

        String contentType = Optional.ofNullable(file.getContentType()).orElse("");

        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(s3Bucket).key(filename).contentType(contentType).build();

        try (var in = file.getInputStream()) {
            s3Client.putObject(req, RequestBody.fromInputStream(in, file.getSize()));
        } catch (Exception e) {
            throw new S3UploadException("Upload to object store failed");
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.deleteObject(b -> b.bucket(s3Bucket).key(key));
        } catch (Exception ex) {
            log.error("Failed to delete document {} from S3: {}", key, ex.getMessage(), ex);
            throw new S3DeleteException("Couldn't delete document");
        }
    }

    @Override
    public String getPresignedUrl(String storageKey, String mime, Long duration) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(s3Bucket)
                .key(storageKey)
                .responseContentType(mime)
                .build();

        var presignedReq = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(duration))
                .getObjectRequest(get)
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignedReq);
        return presigned.url().toString();
    }

    @Override
    public String getJsonFileValue(String storageKey) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(s3Bucket)
                .key(storageKey + ".json")
                .responseContentType("application/json")
                .build();

        var response = s3Client.getObject(get);
        String result;

        try {
            result = new String(response.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new S3DownloadException("Failed to download file");
        }
        return result;
    }
}
