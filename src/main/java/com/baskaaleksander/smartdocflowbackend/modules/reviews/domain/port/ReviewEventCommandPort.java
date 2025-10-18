package com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.port;

import com.baskaaleksander.smartdocflowbackend.modules.reviews.domain.model.ReviewEvent;

public interface ReviewEventCommandPort {
    ReviewEvent save(ReviewEvent reviewEvent);
}
