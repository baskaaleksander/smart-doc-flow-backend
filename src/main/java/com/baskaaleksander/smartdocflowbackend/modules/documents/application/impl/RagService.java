package com.baskaaleksander.smartdocflowbackend.modules.documents.application.impl;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    private final OpenAiChatModel openAiChatModel;
    private final VectorStore vectorStore;

    public RagService(
            OpenAiChatModel openAiChatModel,
            VectorStore vectorStore
    ) {
        this.openAiChatModel = openAiChatModel;
        this.vectorStore = vectorStore;
    }

    public String askQuestion(String question) {

        ChatResponse response = ChatClient.builder(openAiChatModel)
                .build().prompt()
                .advisors(new QuestionAnswerAdvisor(vectorStore))
                .user(question)
                .call()
                .chatResponse();

        return null;
    }
}
