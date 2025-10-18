package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.spring;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEntity;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataReviewRepository extends JpaRepository<ReviewEntity, UUID> {

    @Query("update ReviewEntity r set r.status = :status where r.id = :reviewId")
    @Modifying
    void updateStatus(UUID reviewId, ReviewStatus status);

    @Query("select r.reviewer.id from ReviewEntity r where r.document.id = :documentId")
    Optional<UUID> getReviewerIdByDocumentId(UUID documentId);

    @Query("select r from ReviewEntity r left join fetch r.document left join fetch r.reviewer where r.id = :id")
    Optional<ReviewEntity> getReviewById(UUID id);

    Page<ReviewEntity> findAll(Pageable pageable);

    Page<ReviewEntity> findByStatus(Pageable pageable, ReviewStatus status);
//    void assignUser(long reviewId, long userId);
}
