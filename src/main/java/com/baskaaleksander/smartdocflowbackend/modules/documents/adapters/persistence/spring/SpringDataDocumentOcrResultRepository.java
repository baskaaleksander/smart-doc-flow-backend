package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring;

import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.entity.DocumentOcrResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataDocumentOcrResultRepository extends JpaRepository<DocumentOcrResultEntity, UUID> {

    @Query("select d from DocumentOcrResultEntity d where d.document.id = :docId")
    Optional<DocumentOcrResultEntity> getOcrByDocId(UUID docId);
}
