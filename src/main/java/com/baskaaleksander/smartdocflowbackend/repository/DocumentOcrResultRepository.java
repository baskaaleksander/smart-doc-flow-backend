package com.baskaaleksander.smartdocflowbackend.repository;

import com.baskaaleksander.smartdocflowbackend.model.DocumentOcrResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DocumentOcrResultRepository extends JpaRepository<DocumentOcrResult, UUID> {
}
