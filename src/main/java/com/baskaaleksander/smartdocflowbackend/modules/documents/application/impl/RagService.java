package com.baskaaleksander.smartdocflowbackend.modules.documents.application.impl;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.ai.vectorstore.VectorStore;
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

        PromptTemplate customPromptTemplate = PromptTemplate.builder()
                .renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
                .template("""
            <query>

            Context information is below.

			---------------------
			<question_answer_context>
			---------------------

			Given the context information and no prior knowledge, answer the query.

			Follow these rules:

			1. If the answer is not in the context, just say that you don't know.
			2. Avoid statements like "Based on the context..." or "The provided information...".
            """)
                .build();


        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .promptTemplate(customPromptTemplate)
                .build();

        Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(0.90)
                        .vectorStore(vectorStore)
                        .build())
                .build();

        return ChatClient.builder(openAiChatModel).build()
                .prompt(question)
                .advisors(qaAdvisor)
                .call()
                .content();
    }
}
