package com.baskaaleksander.smartdocflowbackend.modules.documents.api;

import com.baskaaleksander.smartdocflowbackend.common.security.CustomUserDetails;
import com.baskaaleksander.smartdocflowbackend.modules.documents.application.conversation.ConversationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/documents/{documentId}/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping()
    public ResponseEntity<String> askQuestion(@RequestParam("question") String question, @PathVariable("documentId") String id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        return new ResponseEntity<>(conversationService.askQuestion(question, id, userDetails.getId()), HttpStatus.OK);
    }
}
