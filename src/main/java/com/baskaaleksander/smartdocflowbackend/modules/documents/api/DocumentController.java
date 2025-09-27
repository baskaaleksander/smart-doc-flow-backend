package com.baskaaleksander.smartdocflowbackend.modules.documents.api;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.security.CustomUserDetails;
import com.baskaaleksander.smartdocflowbackend.modules.documents.api.dto.DocumentResponse;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.DocumentService;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.embed.EmbeddingService;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final EmbeddingService embeddingService;
    private final RagService ragService;

    @Autowired
    public DocumentController(DocumentService documentService, EmbeddingService embeddingService, RagService ragService) {
        this.documentService = documentService;
        this.embeddingService = embeddingService;
        this.ragService = ragService;
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

    @PostMapping("/{id}/ingest")
    public ResponseEntity<String> ingestDocument(@PathVariable("id") UUID docId) throws IOException {
        embeddingService.ingestDocument(docId);

        return new ResponseEntity<>("OK", HttpStatus.OK);
    }

    @GetMapping("/{id}/ask")
    public ResponseEntity<String> askQuestion(@RequestParam("question") String question, @PathVariable("id") String id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        return new ResponseEntity<>(ragService.askQuestion(question, id, userDetails.getId()), HttpStatus.OK);
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
