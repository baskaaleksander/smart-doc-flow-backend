package com.baskaaleksander.smartdocflowbackend.service;

import com.baskaaleksander.smartdocflowbackend.dto.response.DocumentResponse;
import com.baskaaleksander.smartdocflowbackend.enums.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.exceptions.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.exceptions.S3UploadException;
import com.baskaaleksander.smartdocflowbackend.model.Document;
import com.baskaaleksander.smartdocflowbackend.model.User;
import com.baskaaleksander.smartdocflowbackend.repository.DocumentRepository;
import com.baskaaleksander.smartdocflowbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final S3Client s3Client;
    private final OcrService ocrService;
    private final UserRepository userRepository;

    @Value(value = "${minio.bucket.name}")
    private String s3Bucket;

    public DocumentService(
            DocumentRepository documentRepository,
            S3Client s3Client,
            OcrService ocrService,
            UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.s3Client = s3Client;
        this.ocrService = ocrService;
        this.userRepository = userRepository;
    }

    public DocumentResponse createAndSave(MultipartFile file) {
        UUID docId = UUID.randomUUID();
        String originalFilename = Objects.requireNonNull(file.getOriginalFilename()).replace(" ", "_");
        String filename = docId + "_" + originalFilename;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!"application/pdf".equalsIgnoreCase(String.valueOf(file.getContentType()))) {
            System.out.println("Incorrect filetype");;
        }

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(filename)
                .contentType(file.getContentType())
                .build();

        try (var in = file.getInputStream()) {
            s3Client.putObject(request, RequestBody.fromInputStream(in, file.getSize()));
        } catch (Exception e) {
            throw new S3UploadException("Upload to object store failed");
        }

        Document document = new Document(
                docId,
                originalFilename,
                "pdf",
                file.getSize(),
                filename,
                1,
                DocumentStatus.UPLOADED,
                LocalDateTime.now(),
                user
        );

        Document doc = documentRepository.save(document);

        //FOR NOW DISABLED
//        ocrService.startAsync(document.getId());

        return new DocumentResponse(
                doc.getId(),
                doc.getFilename(),
                doc.getMime(),
                doc.getSize(),
                doc.getPageSize(),
                doc.getOwner().getId(),
                doc.getStatus(),
                doc.getCreatedAt()
        );
    }

}
