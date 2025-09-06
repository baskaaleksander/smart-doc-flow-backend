package com.baskaaleksander.smartdocflowbackend.controller;

import com.baskaaleksander.smartdocflowbackend.dto.response.DocumentResponse;
import com.baskaaleksander.smartdocflowbackend.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/document")
public class DocumentController {

    private final DocumentService documentService;

    @Autowired
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public DocumentResponse uploadDocument(@RequestBody MultipartFile file) throws IOException {
        return documentService.createAndSave(file);
    }
}
