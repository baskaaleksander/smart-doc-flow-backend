package com.baskaaleksander.smartdocflowbackend.modules.documents.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {

    @Query("select c.userId from ConversationMessage c where c.documentId = :documentId")
    List<UUID> getUserIdByDocumentId(UUID documentId);

    @Query("select c.id from ConversationMessage c where c.userId = :userId and c.documentId = :documentId")
    Optional<UUID> getIdByUserIdAndDocId(UUID userId, UUID documentId);

    void deleteAllByConversationId(UUID conversationId);

}
