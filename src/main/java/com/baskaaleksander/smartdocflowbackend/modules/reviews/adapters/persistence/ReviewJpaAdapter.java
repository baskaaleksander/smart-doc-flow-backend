package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.documents.persistence.DocumentRepository;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.spring.SpringDataReviewRepository;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.Review;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewStatus;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port.ReviewCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port.ReviewQueryPort;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class ReviewJpaAdapter implements ReviewCommandPort, ReviewQueryPort {

    private final SpringDataReviewRepository reviewRepo;
    private final SpringDataUserRepository userRepo;
    private final DocumentRepository documentRepo;

    public ReviewJpaAdapter(
            SpringDataReviewRepository reviewRepo,
            SpringDataUserRepository userRepo,
            DocumentRepository documentRepo
    ) {
        this.reviewRepo = reviewRepo;
        this.userRepo = userRepo;
        this.documentRepo = documentRepo;
    }

    @Override
    public void updateStatus(UUID reviewId, ReviewStatus status) {

    }

    @Override
    public Review save(Review review) {
        return null;
    }

    @Override
    public Optional<UUID> getReviewerIdByDocumentId(UUID documentId) {
        return Optional.empty();
    }

    @Override
    public Optional<Review> getReviewById(UUID id) {
        return Optional.empty();
    }

    @Override
    public PagingResult<Review> findAll(PaginationRequest request) {
        return null;
    }

    @Override
    public PagingResult<Review> findByStatus(PaginationRequest request, ReviewStatus status) {
        return null;
    }
}
