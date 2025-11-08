package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.common.security.CustomUserDetails;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto.DocumentDownloadResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto.DocumentResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto.DocumentStatsResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.document.DocumentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Endpoints for managing user documents")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadDocument(@RequestBody MultipartFile file) {
        return new ResponseEntity<>(documentService.createAndSave(file), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEW')")
    @GetMapping("/stats")
    public ResponseEntity<DocumentStatsResponse> getDocumentStats() {
        return new ResponseEntity<>(documentService.getDocumentStats(), HttpStatus.OK);
    }

    @GetMapping("/")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEW')")
    public ResponseEntity<PagingResult<DocumentResponse>> getAllDocuments(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(defaultValue = "false") Boolean assignedToMe,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PaginationRequest request = new PaginationRequest(page, size, sortField, direction);
        return new ResponseEntity<>(documentService.getAllDocuments(request, assignedToMe, userDetails.getId()), HttpStatus.OK);
    }

    @PreAuthorize("@docAccess.canView(#id, authentication)")
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable UUID id) {
        return new ResponseEntity<>(documentService.getById(id), HttpStatus.OK);
    }

    @PreAuthorize("@docAccess.canView(#id, authentication)")
    @GetMapping("/{id}/download")
    public ResponseEntity<DocumentDownloadResponse> downloadDocumentById(@PathVariable UUID id) {
        return new ResponseEntity<>(documentService.downloadDocumentById(id), HttpStatus.OK);
    }

    @PreAuthorize("@docAccess.canModify(#id, authentication)")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDocumentById(@PathVariable UUID id) {
        documentService.deleteById(id);
        return new ResponseEntity<>("Document with id " + id + " deleted successfully", HttpStatus.OK);
    }
}
