package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.jpa;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationUtil;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.entity.ConversationMessageEntity;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.mapping.ConversationMessagePersistenceMapper;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring.SpringDataConversationMessageRepository;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.ConversationMessage;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.ConversationMessageCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.ConversationMessageQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ConversationMessageJpaAdapter implements ConversationMessageCommandPort, ConversationMessageQueryPort {

    private final SpringDataConversationMessageRepository conversationRepo;
    private final JdbcChatMemoryRepository jdbcChatMemoryRepository;
    private final ConversationMessagePersistenceMapper mapper;

    @Override
    @Transactional
    public ConversationMessage save(ConversationMessage message) {
        ConversationMessageEntity entity = (message.getId() != null) ?
                conversationRepo.findById(message.getId()).orElseGet(ConversationMessageEntity::new)
                : new ConversationMessageEntity();

        entity.setSide(message.getSide());
        entity.setContent(message.getContent());
        entity.setFingerprint(message.getFingerprint());
        entity.setConversationId(message.getConversationId());
        entity.setDocumentId(message.getDocumentId());
        entity.setUserId(message.getUserId());

        ConversationMessageEntity saved = conversationRepo.save(entity);

        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void deleteAllByConversationId(UUID conversationId) {
        conversationRepo.deleteAllByConversationId(conversationId);
        jdbcChatMemoryRepository.deleteByConversationId(conversationId.toString());
    }

    @Override
    public List<UUID> getUserIdByDocumentId(UUID documentId) {
        return conversationRepo.getUserIdByDocumentId(documentId);
    }

    @Override
    public Optional<UUID> getIdByUserIdAndDocId(UUID userId, UUID documentId) {
        return conversationRepo.getIdByUserIdAndDocId(userId, documentId);
    }

    @Override
    public PagingResult<ConversationMessage> findAllByDocumentIdAndUserId(PaginationRequest request, UUID userId, UUID documentId) {
        Pageable pageable = PaginationUtil.getPageable(request);

        Page<ConversationMessageEntity> entities = conversationRepo.findAllByDocumentIdAndUserId(pageable, userId, documentId);
        List<ConversationMessage> content = entities.getContent().stream().map(mapper::toDomain).toList();

        return new PagingResult<>(
                content,
                entities.getTotalPages(),
                entities.getTotalElements(),
                entities.getSize(),
                entities.getNumber(),
                entities.isLast(),
                entities.hasNext()
        );
    }
}
