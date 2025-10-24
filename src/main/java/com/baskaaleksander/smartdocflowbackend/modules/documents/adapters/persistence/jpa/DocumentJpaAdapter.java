package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.jpa;

import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceConflictException;
import com.baskaaleksander.smartdocflowbackend.common.exception.ResourceNotFoundException;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationRequest;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PaginationUtil;
import com.baskaaleksander.smartdocflowbackend.common.pagination.PagingResult;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.entity.DocumentEntity;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.mapping.DocumentPersistenceMapper;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring.SpringDataDocumentRepository;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.Document;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentReviewBasic;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.DocumentCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.DocumentQueryPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.view.DocumentStatusCount;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.adapters.persistence.entity.ReviewEntity;
import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewStatus;
import com.baskaaleksander.smartdocflowbackend.modules.users.adapters.persistence.spring.SpringDataUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DocumentJpaAdapter implements DocumentCommandPort, DocumentQueryPort {

    private final SpringDataDocumentRepository documentRepo;
    private final DocumentPersistenceMapper mapper;
    private final SpringDataUserRepository userRepo;

    @Transactional
    @Override
    public void deleteById(UUID documentId) {
        documentRepo.deleteById(documentId);
    }

    @Override
    public void updatePageCount(UUID documentId, int count) {
        documentRepo.updatePageCount(documentId, count);
    }

    @Transactional
    @Override
    public Document save(Document document) {
        if (document.getReview() == null) {
            throw new ResourceConflictException("Review is required to create or update document");
        }

        DocumentEntity entity = (document.getId() != null)
                ? documentRepo.findById(document.getId())
                .orElseGet(() -> {
                    var e = new DocumentEntity();
                    e.setId(document.getId());
                    return e;
                })
                : new DocumentEntity();

        if (entity.getId() == null) {
            entity.setId(document.getId() != null ? document.getId() : UUID.randomUUID());
        }

        entity.setFilename(document.getFilename());
        entity.setMime(document.getMime());
        entity.setSize(document.getSize());
        entity.setStorageKey(document.getStorageKey());
        entity.setPageSize(document.getPageSize());
        entity.setStatus(document.getStatus());

        if (document.getOwner() == null || document.getOwner().id() == null) {
            throw new IllegalArgumentException("Owner is required");
        }
        entity.setOwner(userRepo.findById(document.getOwner().id())
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found: " + document.getOwner().id())));

        DocumentReviewBasic review = document.getReview();
        ReviewEntity reviewEntity;

        if (entity.getReview() == null) {
            reviewEntity = new ReviewEntity();
            reviewEntity.setStatus(
                    review.getStatus() != null ? ReviewStatus.fromString(review.getStatus()) : ReviewStatus.PENDING
            );
            reviewEntity.setReviewer(null);
            reviewEntity.setComment(review.getComment());
            reviewEntity.setDocument(entity);
            entity.setReview(reviewEntity);
        } else {
            reviewEntity = entity.getReview();
            reviewEntity.setStatus(
                    review.getStatus() != null ? ReviewStatus.fromString(review.getStatus()) : reviewEntity.getStatus()
            );
            reviewEntity.setComment(review.getComment());
        }

        DocumentEntity saved = documentRepo.saveAndFlush(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public void updateStatus(UUID documentId, DocumentStatus status) {
        documentRepo.updateStatus(documentId, status);
    }

    @Override
    public Optional<Document> getDocumentById(UUID documentId) {
        return documentRepo.getDocumentById(documentId).map(mapper::toDomain);
    }

    @Override
    public Optional<String> getOwnerUsernameById(UUID documentId) {
        return documentRepo.getOwnerUsernameById(documentId);
    }

    @Override
    public Optional<Document> findByIdWithReview(UUID documentId) {
        return documentRepo.findbyIdWithReview(documentId).map(mapper::toDomain);
    }

    @Override
    public PagingResult<Document> findAllByOwner(UUID ownerId, PaginationRequest request) {
        Pageable pageable = PaginationUtil.getPageable(request);
        Page<DocumentEntity> entities = documentRepo.findAllByOwner(ownerId, pageable);
        List<Document> content = entities.getContent().stream().map(mapper::toDomain).toList();

        return new PagingResult<>(
                content,
                entities.getTotalPages(),
                entities.getTotalElements(),
                entities.getSize(),
                entities.getNumber(),
                entities.isLast(),
                entities.hasNext()
        );
    }

    @Override
    public PagingResult<Document> findAllByReviewer(UUID reviewerId, PaginationRequest request) {
        Pageable pageable = PaginationUtil.getPageable(request);
        Page<DocumentEntity> entities = documentRepo.findAllByReviewer(reviewerId, pageable);
        List<Document> content = entities.getContent().stream().map(mapper::toDomain).toList();

        return new PagingResult<>(
                content,
                entities.getTotalPages(),
                entities.getTotalElements(),
                entities.getSize(),
                entities.getNumber(),
                entities.isLast(),
                entities.hasNext()
        );
    }

    @Override
    public PagingResult<Document> findAll(PaginationRequest request) {
        Pageable pageable = PaginationUtil.getPageable(request);
        Page<DocumentEntity> entities = documentRepo.findAll(pageable);
        List<Document> content = entities.getContent().stream().map(mapper::toDomain).toList();

        return new PagingResult<>(
                content,
                entities.getTotalPages(),
                entities.getTotalElements(),
                entities.getSize(),
                entities.getNumber(),
                entities.isLast(),
                entities.hasNext()
        );
    }

    @Override
    public List<DocumentStatusCount> countDocumentsByStatus() {
        return documentRepo.countDocumentsByStatus();
    }

    @Override
    public Set<Document> findAllByIdIn(Set<UUID> ids) {
        return documentRepo.findAllByIdIn(ids).stream().map(mapper::toDomain).collect(Collectors.toSet());
    }
}
