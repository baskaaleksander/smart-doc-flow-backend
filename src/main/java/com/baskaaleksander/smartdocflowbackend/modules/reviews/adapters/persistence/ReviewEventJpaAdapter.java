package com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence;

import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationUtil;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEventEntity;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.mapping.ReviewEventPersistenceMapper;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.spring.SpringDataReviewEventRepository;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewEvent;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewEventType;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port.ReviewEventQueryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class ReviewEventJpaAdapter implements ReviewEventQueryPort {

    private final SpringDataReviewEventRepository reviewEventRepo;
    private final ReviewEventPersistenceMapper mapper;

    public ReviewEventJpaAdapter(
            SpringDataReviewEventRepository reviewEventRepo,
            ReviewEventPersistenceMapper mapper
    ) {
        this.reviewEventRepo = reviewEventRepo;
        this.mapper = mapper;
    }

    @Override
    public Optional<ReviewEvent> getReviewEventById(UUID id) {
        return reviewEventRepo.getReviewEventById(id).map(mapper::toDomain);
    }

    @Override
    public PagingResult<ReviewEvent> findByReviewId(PaginationRequest request, UUID reviewId) {
        Pageable pageable = PaginationUtil.getPageable(request);
        Page<ReviewEventEntity> eventEntityPage = reviewEventRepo.findByReviewId(pageable, reviewId);
        List<ReviewEvent> content = eventEntityPage.getContent().stream().map(mapper::toDomain).toList();

        return new PagingResult<>(
                content,
                eventEntityPage.getTotalPages(),
                eventEntityPage.getTotalElements(),
                eventEntityPage.getSize(),
                eventEntityPage.getNumber(),
                eventEntityPage.isLast(),
                eventEntityPage.hasNext()
        );
    }

    @Override
    public PagingResult<ReviewEvent> findByReviewIdWithType(PaginationRequest request, UUID reviewId, ReviewEventType eventType) {
        Pageable pageable = PaginationUtil.getPageable(request);
        Page<ReviewEventEntity> eventEntityPage = reviewEventRepo.findByReviewIdWithType(pageable, reviewId, eventType);
        List<ReviewEvent> content = eventEntityPage.getContent().stream().map(mapper::toDomain).toList();

        return new PagingResult<>(
                content,
                eventEntityPage.getTotalPages(),
                eventEntityPage.getTotalElements(),
                eventEntityPage.getSize(),
                eventEntityPage.getNumber(),
                eventEntityPage.isLast(),
                eventEntityPage.hasNext()
        );
    }
}
