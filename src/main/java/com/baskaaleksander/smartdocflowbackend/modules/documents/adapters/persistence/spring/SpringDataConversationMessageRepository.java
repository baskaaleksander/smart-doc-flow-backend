package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring;

import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.entity.ConversationMessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataConversationMessageRepository extends JpaRepository<ConversationMessageEntity, UUID> {

    @Query("select c.userId from ConversationMessageEntity c where c.documentId = :documentId")
    List<UUID> getUserIdByDocumentId(UUID documentId);

    @Query("select distinct c.conversationId from ConversationMessageEntity c where c.userId = :userId and c.documentId = :documentId")
    Optional<UUID> getConversationIdByUserIdAndDocId(UUID userId, UUID documentId);

    void deleteAllByConversationId(UUID conversationId);

    @Query("select c from ConversationMessageEntity c where c.userId = :userId and c.documentId = :documentId")
    Page<ConversationMessageEntity> findAllByDocumentIdAndUserId(Pageable pageable, UUID userId, UUID documentId);
}
