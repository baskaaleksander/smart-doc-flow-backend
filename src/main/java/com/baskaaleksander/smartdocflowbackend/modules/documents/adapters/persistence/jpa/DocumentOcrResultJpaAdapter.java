package com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.jpa;

import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.entity.DocumentOcrResultEntity;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.mapping.DocumentOcrResultPersistenceMapper;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring.SpringDataDocumentOcrResultRepository;
import com.baskaaleksander.smartdocflowbackend.modules.documents.adapters.persistence.spring.SpringDataDocumentRepository;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentOcrResult;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.DocumentOcrResultCommandPort;
import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port.DocumentOcrResultQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DocumentOcrResultJpaAdapter implements DocumentOcrResultQueryPort, DocumentOcrResultCommandPort {

    private final SpringDataDocumentOcrResultRepository ocrResultRepository;
    private final SpringDataDocumentRepository documentRepository;
    private final DocumentOcrResultPersistenceMapper mapper;

    @Override
    @Transactional
    public DocumentOcrResult save(DocumentOcrResult result) {

        DocumentOcrResultEntity entity = (result.getId() != null) ?
                ocrResultRepository.findById(result.getId()).orElseGet(DocumentOcrResultEntity::new)
                : new DocumentOcrResultEntity();

        entity.setStorageKey(result.getStorageKey());
        entity.setDocument(documentRepository.getReferenceById(result.getDocumentId()));

        DocumentOcrResultEntity saved = ocrResultRepository.save(entity);

        return mapper.toDomain(saved);
    }

    @Override
    public Optional<DocumentOcrResult> getOcrByDocId(UUID documentId) {
        return ocrResultRepository.getOcrByDocId(documentId).map(mapper::toDomain);
    }
}
