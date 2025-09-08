package com.baskaaleksander.smartdocflowbackend.controller;

import com.baskaaleksander.smartdocflowbackend.dto.response.DocumentResponse;
import com.baskaaleksander.smartdocflowbackend.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/document")
public class DocumentController {

    private final DocumentService documentService;

    @Autowired
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadDocument(@RequestBody MultipartFile file) {
        return new ResponseEntity<>(documentService.createAndSave(file), HttpStatus.CREATED);
    }

    @GetMapping("/")
    public ResponseEntity<List<DocumentResponse>> getAllDocuments(@AuthenticationPrincipal UserDetails user) {
        return new ResponseEntity<>(documentService.getAllByOwnerUsername(user.getUsername()), HttpStatus.OK);
    }

    @PreAuthorize("@docAccess.canView(#id, authentication)")
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable UUID id) {
        return new ResponseEntity<>(documentService.getById(id), HttpStatus.OK);
    }

    @PreAuthorize("@docAccess.canModify(#id, authentication)")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDocumentById(@PathVariable UUID id) {
        documentService.deleteById(id);
        return new ResponseEntity<>("Document with id " + id + " deleted successfully", HttpStatus.OK);
    }
}
