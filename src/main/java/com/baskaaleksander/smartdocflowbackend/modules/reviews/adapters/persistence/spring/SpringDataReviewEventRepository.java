package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.spring;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEventEntity;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataReviewEventRepository extends JpaRepository<ReviewEventEntity, UUID> {

    @Query("select r from ReviewEventEntity r left join fetch r.reviewer left join fetch r.review where r.id = :id")
    Optional<ReviewEventEntity> getReviewEventById(UUID id);

    @Query("select r from ReviewEventEntity r left join fetch r.reviewer left join fetch r.review where r.review.id = :reviewId")
    List<ReviewEventEntity> getReviewEventsByReviewId(UUID reviewId);

    @Query("select r from ReviewEventEntity r left join fetch r.reviewer left join fetch r.review where r.review.id = :reviewId")
    Page<ReviewEventEntity> findByReviewId(Pageable pageable, UUID reviewId);

    @Query("select r from ReviewEventEntity r left join fetch r.reviewer left join fetch r.review where r.review.id = :reviewId and r.eventType = :eventType")
    Page<ReviewEventEntity> findByReviewIdWithType(Pageable pageable, UUID reviewId, ReviewEventType eventType);

}
