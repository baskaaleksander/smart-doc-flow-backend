package com.baskaaleksander.smartdocflowbackend.service;

import com.baskaaleksander.smartdocflowbackend.exceptions.PdfProcessingException;
import com.baskaaleksander.smartdocflowbackend.exceptions.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.exceptions.S3DownloadException;
import com.baskaaleksander.smartdocflowbackend.model.Document;
import com.baskaaleksander.smartdocflowbackend.repository.DocumentRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
    public CompletableFuture<Void> startAsync(UUID documentId) {

        return CompletableFuture.runAsync(() ->{
            Document doc = documentRepository.getDocumentById(documentId);

            if (doc == null) {
                throw new ResourceNotFoundException("Document with ID " + documentId + " not found.");
            }

            String documentKey = doc.getStorageKey();

            File file = getPdfFromS3(documentKey);

            List<BufferedImage> imageList =  convertPdfToImages(file);
        });
    }

    private File getPdfFromS3(String documentKey) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(documentKey)
                    .build();

            File tempFile = File.createTempFile("s3-", "-" + documentKey.replace("/", "_"));
            s3Client.getObject(getObjectRequest, ResponseTransformer.toFile(tempFile));

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
                BufferedImage image = renderer.renderImageWithDPI(i, 300);
                imageList.add(image);
                ImageIO.write(image, "JPEG", new File("image" + i + ".jpg"));

                System.out.println("added page " + 1);
            }

            pdDocument.close();

            return imageList;
        } catch (Exception ex) {
            throw new PdfProcessingException("Failed to process PDF file");
        }
    }
}
