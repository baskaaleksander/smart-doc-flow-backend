package com.baskaaleksander.smartdocflowbackend.modules.documents.api;

import com.baskaaleksander.smartdocflowbackend.common.security.CustomUserDetails;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.conversation.ConversationService;
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

    @PreAuthorize("@convoAccess.canViewAndModifyConversation(#id, authentication)")
    @GetMapping()
    public ResponseEntity<?> getAllConversationMessages(@PathVariable("documentId") UUID id) {
        return null;
    }

    @PreAuthorize("@convoAccess.canViewAndModifyConversation(#id, authentication)")
    @DeleteMapping()
    public ResponseEntity<?> deleteConversation(@PathVariable("documentId") UUID id) {
        return null;
    }
}
