package com.baskaaleksander.smartdocflowbackend.service;

import com.baskaaleksander.smartdocflowbackend.enums.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.exceptions.PdfProcessingException;
import com.baskaaleksander.smartdocflowbackend.exceptions.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.exceptions.S3DownloadException;
import com.baskaaleksander.smartdocflowbackend.model.Document;
import com.baskaaleksander.smartdocflowbackend.model.OcrResult;
import com.baskaaleksander.smartdocflowbackend.repository.DocumentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class OcrService {

    @Value(value = "${minio.bucket.name}")
    private String bucket;
    @Value(value = "${spring.ai.openai.chat.options.model}")
    private String model;
    private final S3Client s3Client;
    private final DocumentRepository documentRepository;
    private final OpenAiChatModel chatModel;
    private final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    public OcrService(
            S3Client s3Client,
            DocumentRepository documentRepository,
            OpenAiChatModel chatModel
    ) {
        this.s3Client = s3Client;
        this.documentRepository = documentRepository;
        this.chatModel = chatModel;
    }

    @Async("ocrExecutor")
    public CompletableFuture<Void> startAsync(UUID documentId) {

        System.out.println("im here in start async");

        Document doc = documentRepository.getDocumentById(documentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Document with ID " + documentId + " not found"));


            String documentKey = doc.getStorageKey();

            if (documentKey == null) {
                throw new ResourceNotFoundException("Document " + documentId + " has no key");
            }
            File file = null;

            try {
                file = getPdfFromS3(documentKey);

                List<BufferedImage> imageList = convertPdfToImages(file);

                List<Media> mediaList = convertImages(imageList);

                String rawText = performOcr(mediaList);

                saveJsonToS3(rawText, documentId);

                documentRepository.updateStatus(documentId, DocumentStatus.TEXT_READY);

                return CompletableFuture.completedFuture(null);
            } catch (Exception e) {
                documentRepository.updateStatus(documentId, DocumentStatus.FAILED);
                throw new RuntimeException(e);
            }
    }

    private File getPdfFromS3(String documentKey) {
        try {
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
        } catch (Exception e) {
            throw new S3DownloadException("Failed to download document " + documentKey);
        }
    }

    private List<BufferedImage> convertPdfToImages(File file) {
        try (PDDocument pdDocument = Loader.loadPDF(file);) {
            PDFRenderer renderer = new PDFRenderer(pdDocument);
            int pagesCount = pdDocument.getPages().getCount();
            List<BufferedImage> imageList = new ArrayList<>();

            for (int i = 0; i < pagesCount; i++) {
                imageList.add(renderer.renderImageWithDPI(i, 300));
            }

            return imageList;
        } catch (Exception ex) {
            throw new PdfProcessingException("Failed to process PDF file");
        }
    }

    public List<Media> convertImages(List<BufferedImage> images) {
        List<Media> mediaList = new ArrayList<>();

        for (BufferedImage img : images) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(img, "png", baos);
                baos.flush();

                byte[] bytes = baos.toByteArray();
                ByteArrayResource resource = new ByteArrayResource(bytes) {
                    @Override
                    public String getFilename() {
                        return "test.png";
                    }
                };

                mediaList.add(new Media(MimeTypeUtils.IMAGE_PNG, resource));
            } catch (Exception e) {
                throw new RuntimeException("Failed to convert BufferedImage to Resource", e);
            }
        }

        return mediaList;
    }

    private String performOcr(List<Media> images) throws JsonProcessingException {
        var system = new SystemMessage("""
        You are an OCR engine. Extract plain text from each provided image.
        - Preserve original line breaks and spacing as much as possible.
        - Do not hallucinate missing text. If unreadable, return an empty string.
        - Use UTF-8 with diacritics intact.
        - Return ONLY valid JSON (no markdown). Schema:
          {
            "pages": [
              { "page": <integer>, "text": "<string>" }
            ]
          }
        - Language hint: %s
        """);

        UserMessage userMessage = new UserMessage(
                "Please OCR each page. Output must follow the schema above.",
                images
        );

        var options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(1.0)
                .build();

        ChatResponse response = chatModel.call(
                new Prompt(
                        List.of(system, userMessage),
                        options
                )
        );

        //        OcrResult result = MAPPER.readValue(raw, OcrResult.class);
        

        return response.getResult().getOutput().getText();

    }

    private void saveJsonToS3(String raw, UUID docId) {
        
    }
}
