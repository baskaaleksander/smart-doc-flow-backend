package com.baskaaleksander.smartdocflowbackend.modules.documents.application.ocr;

import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.entity.DocumentEntity;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.embed.EmbeddingTaskConsumerService;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.EmbedTask;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.event.OcrTask;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentOcrResult;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.common.exception.PdfProcessingException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.exception.S3DownloadException;
import com.baskaaleksander.smartdocflowbackend.common.exception.S3UploadException;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.*;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.entity.DocumentOcrResultEntity;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring.SpringDataDocumentOcrResultRepository;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring.SpringDataDocumentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OcrTaskConsumerService implements OcrTaskConsumerPort {

    private final OpenAiChatModel chatModel;

    private final EmbeddingTaskPublisherPort taskPublisher;
    private final FileStoragePort fileStoragePort;
    private final DocumentQueryPort documentQueryPort;
    private final DocumentCommandPort documentCommandPort;
    private final DocumentOcrResultCommandPort documentOcrResultCommandPort;

    @Value(value = "${spring.ai.openai.chat.options.model}")
    private String model;

    @Autowired
    public OcrTaskConsumerService(
            OpenAiChatModel chatModel,

            EmbeddingTaskPublisherPort taskPublisher,
            FileStoragePort fileStoragePort,
            DocumentQueryPort documentQueryPort,
            DocumentCommandPort documentCommandPort,
            DocumentOcrResultCommandPort documentOcrResultCommandPort
            ) {
        this.chatModel = chatModel;

        this.taskPublisher = taskPublisher;
        this.fileStoragePort = fileStoragePort;
        this.documentQueryPort = documentQueryPort;
        this.documentCommandPort = documentCommandPort;
        this.documentOcrResultCommandPort = documentOcrResultCommandPort;
    }

    @Transactional
    @Override
    public void handle(OcrTask task) {

        UUID documentId = task.documentId();

        Document doc = documentQueryPort.getDocumentById(documentId)
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


                String docOcrKey = saveJsonToS3(rawText, documentId);

                saveOcrResultToDb(docOcrKey, documentId);

                documentCommandPort.updateStatus(documentId, DocumentStatus.TEXT_READY);

                taskPublisher.publish(new EmbedTask(documentId));

            } catch (Exception e) {
                //TODO: change this
                throw new RuntimeException(e);
            }
    }

    private File getPdfFromS3(String documentKey) {
        return fileStoragePort.getPdfFile(documentKey);
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

    private List<Media> convertImages(List<BufferedImage> images) {
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

    private String performOcr(List<Media> images) {
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

        UserMessage userMessage = UserMessage.builder()
                .text("Please OCR each page. Output must follow the schema above.")
                .media(images)
                .build();

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


        return response.getResult().getOutput().getText();

    }

    private String saveJsonToS3(String raw, UUID docId) {

        byte[] bytes = raw.getBytes(StandardCharsets.UTF_8);
        String docKey = docId + "_ocr.json";
        InputStream is = new ByteArrayInputStream(bytes);

        fileStoragePort.upload(is, docKey, "application/json", bytes.length);

        return docKey;
    }

    private void saveOcrResultToDb(String documentKey, UUID documentId) {

        Document document = documentQueryPort.getDocumentById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        DocumentOcrResult ocrResult = new DocumentOcrResult();
        ocrResult.setDocumentId(document.getId());
        ocrResult.setStorageKey(documentKey);

        documentOcrResultCommandPort.save(ocrResult);
    }
}
