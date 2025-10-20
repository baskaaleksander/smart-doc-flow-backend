package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentUserBasic;

import java.util.Optional;

public interface DocumentUserQueryPort {
    Optional<DocumentUserBasic> findByUsername(String username);
}
