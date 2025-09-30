package com.baskaaleksander.smartdocflowbackend.common.security.access;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.security.CustomUserDetails;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.ConversationMessageRepository;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence.ReviewRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component("convoAccess")
public class ConversationAccessEvaluation {

    //fix all that
    private final ConversationMessageRepository conversationMessageRepository;
    private final ReviewRepository reviewRepository;

    public ConversationAccessEvaluation(ConversationMessageRepository conversationMessageRepository, ReviewRepository reviewRepository) {
        this.conversationMessageRepository = conversationMessageRepository;
        this.reviewRepository = reviewRepository;
    }

    public boolean canViewAndModifyConversations(UUID documentId, Authentication authentication) {
        List<UUID> allUserIds = conversationMessageRepository.getUserIdByDocumentId(documentId);

        for (UUID id : allUserIds) {
            System.out.println(id);
        }

        var userDetails = (CustomUserDetails) authentication.getPrincipal();

        return allUserIds.contains(userDetails.getId());
    }

    public boolean canCreateConversation(UUID documentId, Authentication authentication) {

        List<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        UUID reviewerId = reviewRepository.getReviewerIdByDocumentId(documentId).orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        var userDetails = (CustomUserDetails) authentication.getPrincipal();

        UUID userId = userDetails.getId();

        return (roles.contains("ROLE_REVIEWER") && reviewerId.equals(userId));
    }
}
