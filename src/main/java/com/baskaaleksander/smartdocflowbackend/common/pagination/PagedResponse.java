package com.baskaaleksander.smartdocflowbackend.common.pagination;

import java.util.List;

public record PagedResponse<T>(List<T> items, PageMetadata pageMetadata) {
}
