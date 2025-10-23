package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.jpa;

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
public class DocumentJpaAdapter implements DocumentCommandPort, DocumentQueryPort {

    private final SpringDataDocumentRepository documentRepo;
    private final DocumentPersistenceMapper mapper;

    public DocumentJpaAdapter(
            SpringDataDocumentRepository documentRepo,
            DocumentPersistenceMapper mapper
    ) {
        this.documentRepo = documentRepo;
        this.mapper = mapper;
    }

    @Override
    public Document save(Document document) {
        return null;
    }

    @Override
    public void attachReview(DocumentReviewBasic review) {

    }

    @Override
    public void updateStatus(UUID documentId, DocumentStatus status) {

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
        Page<DocumentEntity> entities = documentRepo.findAllByOwner(ownerId ,pageable);
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
