package com.baskaaleksander.smartdocflowbackend.modules.documents.application.conversation;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationUtil;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.common.util.MakeConversationId;
import com.baskaaleksander.smartdocflowbackend.modules.documents.api.dto.ConversationMessageResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.ConversationSide;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.mapping.ConversationMessageMapper;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.ConversationMessage;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.ConversationMessageRepository;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.DocumentRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
public class ConversationService {

    private final VectorStore vectorStore;
    private final DocumentRepository documentRepository;
    private final ChatClient chatClient;
    private final ConversationEncryptionService conversationEncryptionService;
    private final ConversationMessageRepository conversationMessageRepository;
    private final JdbcChatMemoryRepository jdbcChatMemoryRepository;
    private final ConversationMessageMapper conversationMessageMapper;

    public ConversationService(
            VectorStore vectorStore,
            DocumentRepository documentRepository,
            ChatClient chatClient, ConversationEncryptionService conversationEncryptionService, ConversationMessageRepository conversationMessageRepository, JdbcChatMemoryRepository jdbcChatMemoryRepository, ConversationMessageMapper conversationMessageMapper) {
        this.vectorStore = vectorStore;
        this.documentRepository = documentRepository;
        this.chatClient = chatClient;
        this.conversationEncryptionService = conversationEncryptionService;
        this.conversationMessageRepository = conversationMessageRepository;
        this.jdbcChatMemoryRepository = jdbcChatMemoryRepository;
        this.conversationMessageMapper = conversationMessageMapper;
    }

    public String askQuestion(String question, UUID docId, UUID userId) {

        System.out.println(docId);

        Document doc = documentRepository.getDocumentById(docId).orElseThrow(() -> new ResourceNotFoundException("Document not found"));

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
                        .similarityThreshold(0.50)
                        .vectorStore(vectorStore)
                        .build())
                .build();


        String response = chatClient
                .prompt(question)
                .advisors(retrievalAugmentationAdvisor)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .advisors(a -> a.param("query", question))
                .advisors(qaAdvisor)
                .advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, "docId == '"+ docId +"'"))
                .call()
                .content();


        saveMessage(response, ConversationSide.SYSTEM, conversationId, userId, docId);

        return response;
    }

    private void saveMessage(String content, ConversationSide type, String convoId, UUID userId, UUID documentId) {
        String encryptedContent = conversationEncryptionService.encrypt(content);
        String fingerprint = conversationEncryptionService.fingerprint(content);

        ConversationMessage message = new ConversationMessage();
        message.setSide(type);
        message.setContent(encryptedContent);
        message.setFingerprint(fingerprint);
        message.setConversationId(UUID.fromString(convoId));
        message.setUserId(userId);
        message.setDocumentId(documentId);

        conversationMessageRepository.save(message);

    }

    public void deleteConversation(UUID documentId, UUID userId) {
        UUID conversationId = conversationMessageRepository.getIdByUserIdAndDocId(documentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        jdbcChatMemoryRepository.deleteByConversationId(conversationId.toString());

        conversationMessageRepository.deleteAllByConversationId(conversationId);
    }

    public PagingResult<ConversationMessageResponse> getAllConversationMessages(
            UUID documentId,
            UUID userId,
            PaginationRequest request
    ) {
        Pageable pageable = PaginationUtil.getPageable(request);

        Page<ConversationMessage> conversationMessages = conversationMessageRepository.findAllByDocumentIdAndUserId(pageable, userId, documentId);

        List<ConversationMessageResponse> messageList = conversationMessages
                .stream()
                .map(conversationMessageMapper::toMessageResponse)
                .toList();

        messageList.forEach(m ->
                m.setContent(conversationEncryptionService.decrypt(m.getContent()))
        );

        Integer currentPage = request.getPage();
        int totalPages = conversationMessages.getTotalPages();

        return new PagingResult<>(
                messageList,
                totalPages,
                conversationMessages.getTotalElements(),
                conversationMessages.getSize(),
                conversationMessages.getNumber(),
                currentPage + 1 == totalPages,
                currentPage + 1 < totalPages
        );
    }
}
