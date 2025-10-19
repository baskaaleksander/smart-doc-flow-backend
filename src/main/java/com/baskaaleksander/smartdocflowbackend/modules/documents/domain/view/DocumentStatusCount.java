package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.view;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentStatus;

public interface DocumentStatusCount {
    DocumentStatus getStatus();
    Long getCount();
}
