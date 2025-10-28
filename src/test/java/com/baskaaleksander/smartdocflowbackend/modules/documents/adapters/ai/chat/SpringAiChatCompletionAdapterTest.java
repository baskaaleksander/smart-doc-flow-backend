package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.ai.chat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpringAiChatCompletionAdapterTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    @InjectMocks
    private SpringAiChatCompletionAdapter adapter;

    @Test
    void askWithContext_returnsResponse_and_bindsOverloads() {
        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.system(Mockito.anyString())).thenReturn(requestSpec);
        when(requestSpec.advisors(Mockito.<Consumer<ChatClient.AdvisorSpec>>any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("AI stands for Artificial Intelligence.");

        String question = "What is AI?";
        String conversationId = "conv-123";
        UUID docId = UUID.randomUUID();
        List<String> context = List.of("AI stands for Artificial Intelligence.");
        Map<String, Object> params = Map.of("temperature", 0.2, "systemPrompt", "Be concise.");

        String out = adapter.askWithContext(question, conversationId, docId, context, params);

        assertThat(out).isEqualTo("AI stands for Artificial Intelligence.");

        verify(chatClient).prompt(Mockito.<String>argThat(s ->
                s.contains(question) && s.contains("Rules:") && s.contains("Artificial Intelligence.")
        ));
        verify(requestSpec).system("Be concise.");
        verify(requestSpec, atLeastOnce()).advisors(Mockito.<Consumer<ChatClient.AdvisorSpec>>any());
        verify(requestSpec).call();
        verify(responseSpec).content();
        verifyNoMoreInteractions(responseSpec);
    }

    @Test
    void askQuestion_delegatesAndUsesDefaults() {
        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.system(Mockito.anyString())).thenReturn(requestSpec);
        when(requestSpec.advisors(Mockito.<Consumer<ChatClient.AdvisorSpec>>any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("I don't know.");

        UUID docId = UUID.randomUUID();
        String result = adapter.askQuestion("Unknown?", "cid-1", docId, Map.of());

        assertThat(result).isEqualTo("I don't know.");
        verify(chatClient).prompt(Mockito.<String>argThat(s -> s.contains("Unknown?")));
        verify(requestSpec).system(Mockito.<String>argThat(sys -> sys.toLowerCase().contains("precise q&a assistant")));
        verify(requestSpec, atLeastOnce()).advisors(Mockito.<Consumer<ChatClient.AdvisorSpec>>any());
        verify(responseSpec).content();
    }
}