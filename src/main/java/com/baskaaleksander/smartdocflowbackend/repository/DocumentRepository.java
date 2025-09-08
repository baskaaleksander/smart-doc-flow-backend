package com.baskaaleksander.smartdocflowbackend.repository;

import com.baskaaleksander.smartdocflowbackend.enums.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    @Query("update Document d set d.status = :status where d.id = :documentId")
    void updateStatus(UUID documentId, DocumentStatus status);

    Document getDocumentById(UUID documentId);

    @Query("select d.owner.id from Document d where d.id = :documentId")
    Optional<Long> getOwnerUUIDById(UUID documentId);
}
