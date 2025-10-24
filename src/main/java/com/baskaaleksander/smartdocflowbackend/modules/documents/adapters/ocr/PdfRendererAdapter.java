package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.ocr;

import com.baskaaleksander.smartdocflowbackend.common.exception.PdfProcessingException;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Image;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.PdfRendererPort;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfRendererAdapter implements PdfRendererPort {
    @Override
    public List<Image> render(File file, int dpi) {
        try (PDDocument pdDocument = Loader.loadPDF(file);) {
            PDFRenderer renderer = new PDFRenderer(pdDocument);
            int pagesCount = pdDocument.getPages().getCount();
            List<BufferedImage> imageList = new ArrayList<>();

            for (int i = 0; i < pagesCount; i++) {
                imageList.add(renderer.renderImageWithDPI(i, dpi));
            }

            return convertImages(imageList);

        } catch (Exception ex) {
            throw new PdfProcessingException("Failed to process PDF file");
        }
    }

    private List<Image> convertImages(List<BufferedImage> images) {
        List<Image> imageList = new ArrayList<>();

        int pageNumber = 1;
        for (BufferedImage img : images) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(img, "png", baos);
                baos.flush();

                byte[] bytes = baos.toByteArray();
                imageList.add(
                        new Image(
                                bytes,
                                "image/png",
                                pageNumber++
                        )
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to convert BufferedImage to Resource", e);
            }
        }

        return imageList;
    }
}
