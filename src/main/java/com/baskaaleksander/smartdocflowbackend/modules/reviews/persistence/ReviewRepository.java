package com.baskaaleksander.smartdocflowbackend.modules.reviews.persistence;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    @Query("update Review r set r.status = :status where r.id = :reviewId")
    @Modifying
    void updateStatus(UUID reviewId, ReviewStatus status);

    @Query("select r.reviewer.id from Review r where r.document.id = :documentId")
    Optional<UUID> getReviewerIdByDocumentId(UUID documentId);

    @Query("select r from Review r left join fetch r.document left join fetch r.reviewer where r.id = :id")
    Optional<Review> getReviewById(UUID id);

    Page<Review> findAll(Pageable pageable);

    Page<Review> findByStatus(Pageable pageable, ReviewStatus status);
//    void assignUser(long reviewId, long userId);
}
