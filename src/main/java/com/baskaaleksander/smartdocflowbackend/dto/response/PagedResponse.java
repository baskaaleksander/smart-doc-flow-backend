package com.baskaaleksander.smartdocflowbackend.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record PagedResponse<T>(List<T> items, PageMetadata pageMetadata) {
}
