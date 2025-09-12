package com.baskaaleksander.smartdocflowbackend.dto.response;

import java.util.Collection;

public record PagingResult<T>(
        Collection<T> content,
        Integer totalPages,
        long totalElements,
        Integer size,
        Integer page,
        boolean last,
        boolean next
) {
}
