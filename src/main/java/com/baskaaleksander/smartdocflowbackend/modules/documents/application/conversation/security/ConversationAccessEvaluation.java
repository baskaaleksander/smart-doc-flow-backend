package com.baskaaleksander.smartdocflowbackend.modules.documents.application.conversation.security;

import com.baskaaleksander.smartdocflowbackend.common.exception.UnauthorizedAccessException;
import com.baskaaleksander.smartdocflowbackend.common.security.CustomUserDetails;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring.SpringDataConversationMessageRepository;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.spring.SpringDataReviewRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component("convoAccess")
public class ConversationAccessEvaluation {

    private final SpringDataConversationMessageRepository conversationMessageRepository;
    private final SpringDataReviewRepository reviewRepository;

    public ConversationAccessEvaluation(SpringDataConversationMessageRepository conversationMessageRepository, SpringDataReviewRepository reviewRepository) {
        this.conversationMessageRepository = conversationMessageRepository;
        this.reviewRepository = reviewRepository;
    }

    public boolean canViewAndModifyConversations(UUID documentId, Authentication authentication) {
        List<UUID> allUserIds = conversationMessageRepository.getUserIdByDocumentId(documentId);

        var userDetails = (CustomUserDetails) authentication.getPrincipal();

        return allUserIds.contains(userDetails.getId());
    }

    public boolean canCreateConversation(UUID documentId, Authentication authentication) {

        List<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        UUID reviewerId = reviewRepository.getReviewerIdByDocumentId(documentId).orElseThrow(() -> new UnauthorizedAccessException("You are not allowed to operate with this document"));

        var userDetails = (CustomUserDetails) authentication.getPrincipal();

        UUID userId = userDetails.getId();

        return ((roles.contains("ROLE_REVIEW") || roles.contains("ROLE_ADMIN")) && reviewerId.equals(userId));
    }
}
