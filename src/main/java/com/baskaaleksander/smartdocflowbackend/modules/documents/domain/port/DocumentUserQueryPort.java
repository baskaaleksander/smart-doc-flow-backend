package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentUserBasic;

public interface DocumentUserQueryPort {
    DocumentUserBasic findByUsername(String username);
}
