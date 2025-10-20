package com.baskaaleksander.smartdocflowbackend.modules.documents.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.documents.domain.model.DocumentReviewBasic;

public interface DocumentReviewCommandPort {
    DocumentReviewBasic save(DocumentReviewBasic review);

}
