package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.ai.chat;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.ChatCompletionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SpringAiChatCompletionAdapter implements ChatCompletionPort {

    private final ChatClient chatClient;

    @Override
    public String askQuestion(String question, String conversationId, UUID docId, Map<String, Object> params) {
        return askWithContext(question, conversationId, docId, List.of(), params);
    }

    @Override
    public String askWithContext(String question, String conversationId, UUID docId, List<String> context, Map<String, Object> params) {
        String systemPrompt = (String) params.getOrDefault("systemPrompt",
                "You are a precise Q&A assistant. If the answer is not in context, say \"I don't know.\"");
        double temperature = ((Number) params.getOrDefault("temperature", 0.0)).doubleValue();

        String contextJoined = String.join("\n---\n", context);
        String finalUserPrompt = """
                <query>
                %s
                
                Context information is below.
                ------------------------
                %s
                ------------------------
                
                Rules:
                1) If the answer is not in the context respond exactly: "I don't know."
                2) Do not say "Based on the context" or similar.
                """.formatted(question, contextJoined);

        return chatClient
                .prompt(finalUserPrompt)
                .system(systemPrompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }
}
