package com.baskaaleksander.smartdocflowbackend.common.security.access;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("convoAccess")
public class ConversationAccessEvaluation {

    public boolean canViewAndModifyConversation(UUID documentId, Authentication authentication) {
        return true;
    }

    public boolean canCreateConversation(UUID documentId, Authentication authentication) {
        return true;
    }
}
