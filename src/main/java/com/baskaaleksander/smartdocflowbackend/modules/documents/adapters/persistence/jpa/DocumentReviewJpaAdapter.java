package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.jpa;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring.SpringDataDocumentRepository;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentReviewBasic;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.DocumentReviewCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEntity;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEventEntity;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.spring.SpringDataReviewEventRepository;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.spring.SpringDataReviewRepository;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewStatus;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@Transactional
public class DocumentReviewJpaAdapter implements DocumentReviewCommandPort {

    private final SpringDataReviewRepository reviewRepo;
    private final SpringDataReviewEventRepository reviewEventRepo;
    private final SpringDataDocumentRepository documentRepo;
    private final SpringDataUserRepository userRepo;

    public DocumentReviewJpaAdapter(
            SpringDataReviewRepository reviewRepo,
            SpringDataReviewEventRepository reviewEventRepo,
            SpringDataDocumentRepository documentRepo,
            SpringDataUserRepository userRepo
            ) {
        this.reviewRepo = reviewRepo;
        this.reviewEventRepo = reviewEventRepo;
        this.documentRepo = documentRepo;
        this.userRepo = userRepo;
    }

    @Override
    public DocumentReviewBasic save(DocumentReviewBasic review) {
        final boolean create = (review.getId() == null);

        if (review.getStatus() == null || review.getStatus().isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        if (review.getDocumentId() == null) {
            throw new IllegalArgumentException("documentId is required");
        }
        if (review.getReviewEventIds() == null) {
            throw new IllegalArgumentException("reviewEventIds is required (can be empty)");
        }

        ReviewEntity entity = create
                ? new ReviewEntity()
                : reviewRepo.findById(review.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + review.getId()));

        try {
            entity.setStatus(ReviewStatus.fromString(review.getStatus()));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(review.getStatus());
        }

        var document = documentRepo.findById(review.getDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + review.getDocumentId()));
        entity.setDocument(document);

        if (review.getReviewerId() == null) {
            entity.setReviewer(null);
        } else {
            var reviewer = userRepo.findById(review.getReviewerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found: " + review.getReviewerId()));
            entity.setReviewer(reviewer);
        }

        entity.setComment(review.getComment() == null ? null : review.getComment().trim());

        if (review.getReviewEventIds().isEmpty()) {
            entity.setReviewEvents(List.of());
        } else {
            var events = reviewEventRepo.findAllById(review.getReviewEventIds());
            if (events.size() != review.getReviewEventIds().size()) {
                throw new ResourceNotFoundException("Some review events not found");
            }
            entity.setReviewEvents(events);
        }

        ReviewEntity saved = reviewRepo.saveAndFlush(entity);

        boolean hasReviewer = saved.getReviewer() != null;
        List<UUID> eventIds = (saved.getReviewEvents() == null)
                ? List.of()
                : saved.getReviewEvents().stream().map(ReviewEventEntity::getId).toList();

        return new DocumentReviewBasic(
                saved.getId(),
                hasReviewer ? saved.getReviewer().getUsername() : null,
                hasReviewer ? saved.getReviewer().getId() : null,
                saved.getDocument().getId(),
                eventIds,
                saved.getComment(),
                saved.getStatus().getValue(),
                saved.getUpdatedAt()
        );
    }
}
