package com.baskaaleksander.smartdocflowbackend.modules.documents.application.conversation;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.common.util.MakeConversationId;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.ConversationMessageApiMapper;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto.ConversationMessageResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.*;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ConversationService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    private final ConversationEncryptionServicePort conversationEncryptionServicePort;
    private final DocumentQueryPort documentQueryPort;
    private final ConversationMessageQueryPort conversationMessageQueryPort;
    private final ConversationMessageCommandPort conversationMessageCommandPort;
    private final ConversationMessageApiMapper mapper;
    private final VectorQueryPort vectorQueryPort;
    private final ChatCompletionPort chatCompletionPort;

    public ConversationService(
            VectorStore vectorStore,
            ChatClient chatClient,

            ConversationEncryptionServicePort conversationEncryptionServicePort,
            ConversationMessageQueryPort conversationMessageQueryPort,
            ConversationMessageCommandPort conversationMessageCommandPort,
            DocumentQueryPort documentQueryPort,
            ConversationMessageApiMapper mapper,
            VectorQueryPort vectorQueryPort,
            ChatCompletionPort chatCompletionPort
            ) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;

        this.conversationEncryptionServicePort = conversationEncryptionServicePort;
        this.conversationMessageQueryPort = conversationMessageQueryPort;
        this.conversationMessageCommandPort = conversationMessageCommandPort;
        this.documentQueryPort = documentQueryPort;
        this.mapper = mapper;
        this.vectorQueryPort = vectorQueryPort;
        this.chatCompletionPort = chatCompletionPort;
    }

    public String askQuestion(String question, UUID docId, UUID userId) {

        Document doc = documentQueryPort.getDocumentById(docId).orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        EnumSet<DocumentStatus> allowedStatuses = EnumSet.of(
                DocumentStatus.PROCESSED,
                DocumentStatus.IN_REVIEW,
                DocumentStatus.REVIEW_PENDING
        );

        if (!allowedStatuses.contains(doc.getStatus())) {
            throw new ResourceConflictException("Document is not processed yet");
        }

        String conversationId = MakeConversationId.makeConversationId(docId.toString(), userId);

        saveMessage(question, ConversationSide.USER, conversationId, userId, docId);

//        PromptTemplate customPromptTemplate = PromptTemplate.builder()
//                .renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
//                .template("""
//        <query>
//
//        Context information is below.
//
//        ---------------------
//        <question_answer_context>
//        ---------------------
//
//        Given the context information and no prior knowledge, answer the query.
//
//        Follow these rules:
//        1. If the answer is not in the context, just say that you don't know.
//        2. Avoid statements like "Based on the context..." or "The provided information...".
//        """)
//                .build();
//
//
//        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
//                .promptTemplate(customPromptTemplate)
//                .build();
//
//        Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
//                .documentRetriever(VectorStoreDocumentRetriever.builder()
//                        .similarityThreshold(0.50)
//                        .vectorStore(vectorStore)
//                        .build())
//                .build();
//
//
//        String response = chatClient
//                .prompt(question)
//                .advisors(retrievalAugmentationAdvisor)
//                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
//                .advisors(a -> a.param("query", question))
//                .advisors(qaAdvisor)
//                .advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, "docId == '"+ docId +"'"))
//                .call()
//                .content();

        var filter = Map.<String, Object>of("docId", docId.toString());
        double threshold = 0.50;
        int topK = 8;

        var hits = vectorQueryPort.searchByQuery(question, threshold, topK, filter);

        var context = hits.stream().map(SearchHit::text).toList();

        var params = Map.<String, Object>of(
                "temperature", 0.0,
                "systemPrompt", """
            You are a precise Q&A assistant.

            Follow these rules:
            1. If the answer is not in the provided context, reply: "I don't know."
            2. Do not use phrases like "Based on the context" or "The provided information".
            3. Use only the given context; do not rely on prior knowledge.
            """
        );

        var response = chatCompletionPort.askWithContext(
                question,
                conversationId,
                docId,
                context,
                params
        );


        saveMessage(response, ConversationSide.SYSTEM, conversationId, userId, docId);

        return response;
    }

    private void saveMessage(String content, ConversationSide type, String convoId, UUID userId, UUID documentId) {
        String encryptedContent = conversationEncryptionServicePort.encrypt(content);
        String fingerprint = conversationEncryptionServicePort.fingerprint(content);

        ConversationMessage message = new ConversationMessage();
        message.setSide(type);
        message.setContent(encryptedContent);
        message.setFingerprint(fingerprint);
        message.setConversationId(UUID.fromString(convoId));
        message.setUserId(userId);
        message.setDocumentId(documentId);

        conversationMessageCommandPort.save(message);

    }

    public void deleteConversation(UUID documentId, UUID userId) {
        UUID conversationId = conversationMessageQueryPort.getIdByUserIdAndDocId(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        conversationMessageCommandPort.deleteAllByConversationId(conversationId);
    }

    public PagingResult<ConversationMessageResponse> getAllConversationMessages(
            UUID documentId,
            UUID userId,
            PaginationRequest request
    ) {
        PagingResult<ConversationMessage> conversationMessages = conversationMessageQueryPort.findAllByDocumentIdAndUserId(request, userId, documentId);

        List<ConversationMessageResponse> messageList = conversationMessages
                .content()
                .stream()
                .map(mapper::toResponse)
                .toList();

        messageList.forEach(m ->
                m.setContent(conversationEncryptionServicePort.decrypt(m.getContent()))
        );

        return new PagingResult<>(
                messageList,
                conversationMessages.totalPages(),
                conversationMessages.totalElements(),
                conversationMessages.size(),
                conversationMessages.page(),
                conversationMessages.last(),
                conversationMessages.next()
        );
    }
}
