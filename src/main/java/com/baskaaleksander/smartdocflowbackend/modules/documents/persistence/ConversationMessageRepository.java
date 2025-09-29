package com.baskaaleksander.smartdocflowbackend.modules.documents.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {

    @Query("select c.userId from ConversationMessage c where c.id = :id")
    UUID getUserIdById(UUID id);

}
