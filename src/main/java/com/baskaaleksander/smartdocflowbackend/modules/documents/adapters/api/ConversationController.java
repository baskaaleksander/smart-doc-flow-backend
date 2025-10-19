package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.common.security.CustomUserDetails;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto.ConversationMessageResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.conversation.ConversationService;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/documents/{documentId}/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PreAuthorize("@convoAccess.canCreateConversation(#id, authentication)")
    @PostMapping()
    public ResponseEntity<String> askQuestion(@RequestParam("question") String question, @PathVariable("documentId") UUID id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        return new ResponseEntity<>(conversationService.askQuestion(question, id, userDetails.getId()), HttpStatus.OK);
    }

//    @PreAuthorize("@convoAccess.canViewAndModifyConversations(#id, authentication)")
    @PreAuthorize("hasAnyRole('ADMIN', 'REVIEW')")
    @GetMapping()
    public ResponseEntity<PagingResult<ConversationMessageResponse>> getAllConversationMessages(
            @PathVariable("documentId") UUID id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction

    ) {
        PaginationRequest request = new PaginationRequest(page, size, sortField, direction);
        return new ResponseEntity<>(conversationService.getAllConversationMessages(id, userDetails.getId(), request), HttpStatus.OK);
    }

    @PreAuthorize("@convoAccess.canViewAndModifyConversations(#id, authentication)")
    @DeleteMapping()
    public ResponseEntity<String> deleteConversation(@PathVariable("documentId") UUID id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        conversationService.deleteConversation(id, userDetails.getId());
        return new ResponseEntity<>("Deleted", HttpStatus.OK);
    }
}
