package com.easyfamily.query.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class QueryDtos {

    private QueryDtos() {
    }

    public record BindingQueryRequest(
            @Pattern(regexp = "^1\\d{10}$", message = "invalid mainland phone format")
            String phone,
            @NotBlank String queryType
    ) {
    }

    public record BindingQueryResponse(
            String phone,
            boolean bankBound,
            boolean socialBound,
            String source,
            long queryTimestamp
    ) {
    }
}
