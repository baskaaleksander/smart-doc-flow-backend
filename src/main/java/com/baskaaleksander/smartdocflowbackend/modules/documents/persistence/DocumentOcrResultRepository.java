package com.baskaaleksander.smartdocflowbackend.modules.documents.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentOcrResultRepository extends JpaRepository<DocumentOcrResult, UUID> {

    @Query("select d from DocumentOcrResult d where d.document.id = :docId")
    Optional<DocumentOcrResult> getOcrByDocId(UUID docId);
}
