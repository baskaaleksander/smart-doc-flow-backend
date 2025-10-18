package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationUtil;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.DocumentRepository;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEntity;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEventEntity;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.mapping.ReviewPersistenceMapper;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.spring.SpringDataReviewRepository;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.Review;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewEvent;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewStatus;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port.ReviewCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port.ReviewQueryPort;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class ReviewJpaAdapter implements ReviewCommandPort, ReviewQueryPort {

    private final SpringDataReviewRepository reviewRepo;
    private final SpringDataUserRepository userRepo;
    private final DocumentRepository documentRepo;
    private final ReviewPersistenceMapper mapper;

    public ReviewJpaAdapter(
            SpringDataReviewRepository reviewRepo,
            SpringDataUserRepository userRepo,
            DocumentRepository documentRepo,
            ReviewPersistenceMapper mapper
    ) {
        this.reviewRepo = reviewRepo;
        this.userRepo = userRepo;
        this.documentRepo = documentRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void updateStatus(UUID reviewId, ReviewStatus status) {
        reviewRepo.updateStatus(reviewId, status);
    }

    @Override
    @Transactional
    public Review save(Review review) {
        ReviewEntity entity = (review.getId() != null)
                ? reviewRepo.findById(review.getId()).orElseGet(ReviewEntity::new)
                : new ReviewEntity();

        entity.setStatus(review.getStatus());

        if(review.getComment() != null && !review.getComment().isBlank()) {
            entity.setComment(review.getComment());
        }

        entity.setDocument(documentRepo.getReferenceById(review.getDocumentId()));

        entity.setReviewer(review.getReviewerId() != null ? userRepo.getReferenceById(review.getReviewerId()) : null);

        if (review.getReviewEvents() != null) {
            for (ReviewEvent re : review.getReviewEvents()) {
                if (re.getId() != null) {
                    ReviewEventEntity de = new ReviewEventEntity();
                    de.setEventType(re.getEventType());
                    de.setComment(re.getComment() != null ? re.getComment() : null);
                    de.setReviewer(userRepo.getReferenceById(re.getReviewerId()));
                    de.setReview(entity);
                    entity.getReviewEvents().add(de);
                }
            }
        }

        ReviewEntity saved = reviewRepo.save(entity);

        return mapper.toDomain(saved);
    }

    @Override
    public Optional<UUID> getReviewerIdByDocumentId(UUID documentId) {
        return reviewRepo.getReviewerIdByDocumentId(documentId);
    }

    @Override
    public Optional<Review> getReviewById(UUID id) {
        return reviewRepo.getReviewById(id).map(mapper::toDomain);
    }

    @Override
    public PagingResult<Review> findAll(PaginationRequest request) {
        Pageable pageable = PaginationUtil.getPageable(request);
        Page<ReviewEntity> entityPage = reviewRepo.findAll(pageable);

        List<Review> content = entityPage.getContent().stream().map(mapper::toDomain).toList();

        return new PagingResult<>(
                content,
                entityPage.getTotalPages(),
                entityPage.getTotalElements(),
                entityPage.getSize(),
                entityPage.getNumber(),
                entityPage.isLast(),
                entityPage.hasNext()
        );
    }

    @Override
    public PagingResult<Review> findByStatus(PaginationRequest request, ReviewStatus status) {
        Pageable pageable = PaginationUtil.getPageable(request);
        Page<ReviewEntity> entityPage = reviewRepo.findByStatus(pageable, status);

        List<Review> content = entityPage.getContent().stream().map(mapper::toDomain).toList();

        return new PagingResult<>(
                content,
                entityPage.getTotalPages(),
                entityPage.getTotalElements(),
                entityPage.getSize(),
                entityPage.getNumber(),
                entityPage.isLast(),
                entityPage.hasNext()
        );
    }
}
