package com.baskaaleksander.smartdocflowbackend.common.pagination;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaginationRequest {

    private Integer page = 1;

    private Integer size = 10;

    private String sortField = "id";

    private Sort.Direction direction = Sort.Direction.DESC;

}
