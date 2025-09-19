package com.baskaaleksander.smartdocflowbackend.modules.documents.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DocumentOcrResultRepository extends JpaRepository<DocumentOcrResult, UUID> {
}
