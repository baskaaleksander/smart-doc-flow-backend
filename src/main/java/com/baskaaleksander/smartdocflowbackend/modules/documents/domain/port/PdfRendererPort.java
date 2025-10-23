package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Image;

import java.io.File;
import java.util.List;

public interface PdfRendererPort {
    List<Image> render(File file, int dpi);
}
