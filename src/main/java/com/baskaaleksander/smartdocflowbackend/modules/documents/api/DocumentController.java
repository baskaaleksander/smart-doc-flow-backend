package com.baskaaleksander.smartdocflowbackend.modules.documents.api;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.modules.documents.api.dto.DocumentResponse;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.impl.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
