package com.baskaaleksander.smartdocflowbackend.repository;

import com.baskaaleksander.smartdocflowbackend.enums.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.enums.ReviewStatus;
import com.baskaaleksander.smartdocflowbackend.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("select r from Review r left join fetch r.document left join fetch r.reviewer where r.document.id = :documentId")
    Optional<Review> getReviewByDocId(UUID documentId);

    @Query("update Review r set r.status = :status where r.id = :reviewId")
    @Modifying
    void updateStatus(long reviewId, ReviewStatus status);

//    void assignUser(long reviewId, long userId);
}
