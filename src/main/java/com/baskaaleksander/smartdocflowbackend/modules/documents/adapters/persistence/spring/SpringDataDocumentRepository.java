package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring;

import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.entity.DocumentEntity;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.view.DocumentStatusCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface SpringDataDocumentRepository extends JpaRepository<DocumentEntity, UUID> {

    @Query("update DocumentEntity d set d.status = :status where d.id = :documentId")
    @Modifying
    void updateStatus(UUID documentId, DocumentStatus status);

    @Query("select d.owner.id from DocumentEntity d where d.review.id = :reviewId")
    Optional<UUID> getOwnerIdByReviewId(UUID reviewId);

    @Query("update DocumentEntity d set d.pageSize = :count where d.id = :documentId")
    @Modifying
    void updatePageCount(UUID documentId, int count);

    Optional<DocumentEntity> getDocumentById(UUID documentId);

    @Query("select d.owner.username from DocumentEntity d where d.id = :documentId")
    Optional<String> getOwnerUsernameById(UUID documentId);

    @Query("""
                select d from DocumentEntity d
                left join fetch d.review
                where d.id = :documentId
            """)
    Optional<DocumentEntity> findbyIdWithReview(UUID documentId);

    @Query("select d from DocumentEntity d where d.owner.id = :ownerId")
    Page<DocumentEntity> findAllByOwner(UUID ownerId, Pageable pageable);

    @Query("select d from DocumentEntity d where d.review.reviewer.id = :reviewerId")
    Page<DocumentEntity> findAllByReviewer(UUID reviewerId, Pageable pageable);

    Page<DocumentEntity> findAll(Pageable pageable);

    @Query("select d.status as status, count(d) as count from DocumentEntity d group by d.status")
    List<DocumentStatusCount> countDocumentsByStatus();

    @Query("select d from DocumentEntity d where d.id in :ids")
    Set<DocumentEntity> findAllByIdIn(Set<UUID> ids);
}
