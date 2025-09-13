package com.baskaaleksander.smartdocflowbackend.repository;

import com.baskaaleksander.smartdocflowbackend.model.Review;
import com.baskaaleksander.smartdocflowbackend.model.ReviewEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewEventRepository extends JpaRepository<ReviewEvent, UUID> {

    @Query("select r from ReviewEvent r left join fetch r.reviewer left join fetch r.review where r.id = :id")
    Optional<ReviewEvent> getReviewEventById(UUID id);

    @Query("select r from ReviewEvent r left join fetch r.reviewer left join fetch r.review where r.review.id = :reviewId")
    List<ReviewEvent> getReviewEventsByReviewId(UUID reviewId);

    @Query("select r from ReviewEvent r left join fetch r.reviewer left join fetch r.review where r.review.id = :reviewId")
    Page<ReviewEvent> findByReviewId(Pageable pageable, UUID reviewId);

}
