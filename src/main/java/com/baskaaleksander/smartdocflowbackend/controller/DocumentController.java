package com.baskaaleksander.smartdocflowbackend.controller;

import com.baskaaleksander.smartdocflowbackend.dto.request.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.dto.response.DocumentResponse;
import com.baskaaleksander.smartdocflowbackend.dto.response.PagingResult;
import com.baskaaleksander.smartdocflowbackend.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/documents")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEW')")
    public ResponseEntity<PagingResult<DocumentResponse>> getAllDocuments(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        PaginationRequest request = new PaginationRequest(page, size, sortField, direction);
        return new ResponseEntity<>(documentService.getAllDocuments(request), HttpStatus.OK);
    }

    @PreAuthorize("@docAccess.canView(#id, authentication)")
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable UUID id) {
        return new ResponseEntity<>(documentService.getById(id), HttpStatus.OK);
    }

    @PreAuthorize("@docAccess.canView(#id, authentication)")
    @GetMapping("/{id}/download")
    public ResponseEntity<String> downloadDocumentById(@PathVariable UUID id) {
        return new ResponseEntity<>(documentService.downloadDocumentById(id), HttpStatus.OK);
    }

    @PreAuthorize("@docAccess.canModify(#id, authentication)")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDocumentById(@PathVariable UUID id) {
        documentService.deleteById(id);
        return new ResponseEntity<>("Document with id " + id + " deleted successfully", HttpStatus.OK);
    }
}
