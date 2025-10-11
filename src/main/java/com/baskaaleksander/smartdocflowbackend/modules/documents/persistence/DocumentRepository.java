package com.baskaaleksander.smartdocflowbackend.modules.documents.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.DocumentStatusCount;
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

    @Query("select d from Document d where d.owner.id = :ownerId")
    Page<Document> findAllByOwner(UUID ownerId, Pageable pageable);

    @Query("select d from Document d where d.review.reviewer.id = :reviewerId")
    Page<Document> findAllByReviewer(UUID reviewerId, Pageable pageable);

    Page<Document> findAll(Pageable pageable);

    @Query("select d.status as status, count(d) as count from Document d group by d.status")
    List<DocumentStatusCount> countDocumentsByStatus();
}
