package com.baskaaleksander.smartdocflowbackend.service;

import com.baskaaleksander.smartdocflowbackend.exceptions.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.model.Document;
import com.baskaaleksander.smartdocflowbackend.repository.DocumentRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    public void startAsync(UUID documentId) throws IOException {

        Document doc = documentRepository.getDocumentById(documentId);

        String documentKey = doc.getStorageKey();

        File file = getPdfFromS3(documentKey);

        convertPdfToImages(file);

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

    private List<BufferedImage> convertPdfToImages(File file) throws IOException {
        PDDocument pdDocument = Loader.loadPDF(file);
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
    }
}
