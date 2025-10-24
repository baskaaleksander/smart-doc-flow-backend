package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ChatCompletionPort {
    String askQuestion(
            String question,
            UUID conversationId,
            UUID docId,
            Map<String, Object> params
    );
    String askWithContext(
            String question,
            UUID conversationId,
            UUID docId,
            List<String> context,
            Map<String, Object> params
    );
}
