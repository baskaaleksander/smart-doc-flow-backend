package com.baskaaleksander.smartdocflowbackend.modules.documents.application.embed;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.ChatCompletionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SpringAiChatCompletionAdapter implements ChatCompletionPort {
    @Override
    public String askQuestion(String question, String conversationId, UUID docId, Map<String, Object> params) {
        return "";
    }

    @Override
    public String askWithContext(String question, String conversationId, UUID docId, List<String> context, Map<String, Object> params) {
        return "";
    }
}
