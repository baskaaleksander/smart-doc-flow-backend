package com.baskaaleksander.smartdocflowbackend.modules.documents.application.conversation;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.logging.LoggingPort;
import com.baskaaleksander.smartdocflowbackend.common.logging.Slf4jLoggingAdapter;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.common.util.MakeConversationId;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.ConversationMessageApiMapper;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.api.dto.ConversationMessageResponse;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.*;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationEncryptionServicePort conversationEncryptionServicePort;
    private final DocumentQueryPort documentQueryPort;
    private final ConversationMessageQueryPort conversationMessageQueryPort;
    private final ConversationMessageCommandPort conversationMessageCommandPort;
    private final ConversationMessageApiMapper mapper;
    private final VectorQueryPort vectorQueryPort;
    private final ChatCompletionPort chatCompletionPort;
    private final LoggingPort logger;

    public String askQuestion(String question, UUID docId, UUID userId) {
        long start = System.currentTimeMillis();
        logger.info("CONV_ASK START docId=" + Slf4jLoggingAdapter.shortId(docId)
                + " userId=" + Slf4jLoggingAdapter.shortId(userId)
                + " qLen=" + (question == null ? 0 : question.length()));

        Document doc = documentQueryPort.getDocumentById(docId)
                .orElseThrow(() -> {
                    logger.warn("CONV_ASK FAILED reason=document_not_found docId=" + Slf4jLoggingAdapter.shortId(docId)
                            + " userId=" + Slf4jLoggingAdapter.shortId(userId));
                    return new ResourceNotFoundException("Document not found");
                });

        EnumSet<DocumentStatus> allowedStatuses = EnumSet.of(
                DocumentStatus.PROCESSED,
                DocumentStatus.IN_REVIEW,
                DocumentStatus.REVIEW_PENDING
        );

        if (!allowedStatuses.contains(doc.getStatus())) {
            logger.warn("CONV_ASK FAILED reason=invalid_status status=" + doc.getStatus()
                    + " docId=" + Slf4jLoggingAdapter.shortId(docId)
                    + " userId=" + Slf4jLoggingAdapter.shortId(userId));
            throw new ResourceConflictException("Document is not processed yet");
        }

        String conversationId = MakeConversationId.makeConversationId(docId.toString(), userId);
        logger.info("CONV_ASK CONTEXT_READY convoId=" + Slf4jLoggingAdapter.shortId(UUID.fromString(conversationId))
                + " status=" + doc.getStatus());

        saveMessage(question, ConversationSide.USER, conversationId, userId, docId);
        logger.info("CONV_ASK USER_MESSAGE_SAVED convoId=" + Slf4jLoggingAdapter.shortId(UUID.fromString(conversationId)));

        var filter = Map.<String, Object>of("docId", docId.toString());
        double threshold = 0.35;
        int topK = 12;

        logger.info("VECTOR_QUERY START docId=" + Slf4jLoggingAdapter.shortId(docId)
                + " topK=" + topK + " threshold=" + threshold);
        var hits = vectorQueryPort.searchByQuery(question, threshold, topK, filter);
        logger.info("VECTOR_QUERY SUCCESS docId=" + Slf4jLoggingAdapter.shortId(docId)
                + " hits=" + hits.size());

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

        logger.info("AI_CHAT REQUEST convoId=" + Slf4jLoggingAdapter.shortId(UUID.fromString(conversationId))
                + " ctxCount=" + context.size());
        var response = chatCompletionPort.askWithContext(
                question,
                conversationId,
                docId,
                context,
                params
        );

        logger.info("AI_CHAT SUCCESS convoId=" + Slf4jLoggingAdapter.shortId(UUID.fromString(conversationId))
                + " respLen=" + (response == null ? 0 : response.length()));

        saveMessage(response, ConversationSide.SYSTEM, conversationId, userId, docId);
        logger.info("CONV_ASK SYSTEM_MESSAGE_SAVED convoId=" + Slf4jLoggingAdapter.shortId(UUID.fromString(conversationId)));

        long took = System.currentTimeMillis() - start;
        logger.info("CONV_ASK SUCCESS docId=" + Slf4jLoggingAdapter.shortId(docId)
                + " userId=" + Slf4jLoggingAdapter.shortId(userId)
                + " took=" + took + "ms");

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
        logger.info("CONV_MSG SAVE_SUCCESS convoId=" + Slf4jLoggingAdapter.shortId(UUID.fromString(convoId))
                + " side=" + type + " fpr=" + fingerprint);
    }

    public void deleteConversation(UUID documentId, UUID userId) {
        logger.info("CONV_DELETE START docId=" + Slf4jLoggingAdapter.shortId(documentId)
                + " userId=" + Slf4jLoggingAdapter.shortId(userId));

        UUID conversationId = conversationMessageQueryPort.getIdByUserIdAndDocId(documentId, userId)
                .orElseThrow(() -> {
                    logger.warn("CONV_DELETE FAILED reason=conversation_not_found docId=" + Slf4jLoggingAdapter.shortId(documentId)
                            + " userId=" + Slf4jLoggingAdapter.shortId(userId));
                    return new ResourceNotFoundException("Conversation not found");
                });

        conversationMessageCommandPort.deleteAllByConversationId(conversationId);
        logger.info("CONV_DELETE SUCCESS convoId=" + Slf4jLoggingAdapter.shortId(conversationId));
    }

    public PagingResult<ConversationMessageResponse> getAllConversationMessages(
            UUID documentId,
            UUID userId,
            PaginationRequest request
    ) {
        logger.info("CONV_LIST START docId=" + Slf4jLoggingAdapter.shortId(documentId)
                + " userId=" + Slf4jLoggingAdapter.shortId(userId)
                + " page=" + request.getPage()
                + " size=" + request.getSize());

        PagingResult<ConversationMessage> conversationMessages =
                conversationMessageQueryPort.findAllByDocumentIdAndUserId(request, userId, documentId);

        List<ConversationMessageResponse> messageList = conversationMessages
                .content()
                .stream()
                .map(mapper::toResponse)
                .toList();

        messageList.forEach(m ->
                m.setContent(conversationEncryptionServicePort.decrypt(m.getContent()))
        );

        logger.info("CONV_LIST SUCCESS docId=" + Slf4jLoggingAdapter.shortId(documentId)
                + " userId=" + Slf4jLoggingAdapter.shortId(userId)
                + " count=" + messageList.size()
                + " totalElements=" + conversationMessages.totalElements()
                + " totalPages=" + conversationMessages.totalPages());

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