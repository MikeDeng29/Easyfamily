package com.easyfamily.query.dto;

import jakarta.validation.constraints.Pattern;

public final class QueryDtos {

    private QueryDtos() {
    }

    public record RealNameVerifyRequest(
            @Pattern(regexp = "^1\\d{10}$", message = "invalid mainland phone format")
            String phone,
            @Pattern(regexp = "^[\\u4e00-\\u9fa5A-Za-z·\\s]{2,30}$", message = "invalid name format")
            String name,
            @Pattern(
                    regexp = "^(?:\\d{15}|\\d{17}[0-9Xx])$",
                    message = "invalid mainland id card format"
            )
            String idCardNo
    ) {
    }

    public record RealNameVerifyResponse(
            String phone,
            String name,
            String idCardMasked,
            boolean verified,
            String source,
            long queryTimestamp
    ) {
    }
}
