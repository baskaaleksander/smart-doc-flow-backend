package com.baskaaleksander.smartdocflowbackend.repository;

import com.baskaaleksander.smartdocflowbackend.enums.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.enums.ReviewStatus;
import com.baskaaleksander.smartdocflowbackend.model.Document;
import com.baskaaleksander.smartdocflowbackend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    @Query("update Document d set d.status = :status where d.id = :documentId")
    @Modifying
    void updateStatus(UUID documentId, DocumentStatus status);

    Optional<Document> getDocumentById(UUID documentId);

    @Query("select d.owner.username from Document d where d.id = :documentId")
    Optional<String> getOwnerUsernameById(UUID documentId);

    @Query("""
    select d from Document d
    left join fetch d.review
    where d.id = :documentId
""")
    Optional<Document> findbyIdWithReview(UUID documentId);

    @Query("select d from Document d where d.owner.username = :username")
    Page<Document> findAllByOwner(String username, Pageable pageable);

    Page<Document> findAll(Pageable pageable);

}
