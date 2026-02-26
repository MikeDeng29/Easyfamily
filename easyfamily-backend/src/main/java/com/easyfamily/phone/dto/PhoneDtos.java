package com.easyfamily.phone.dto;

import jakarta.validation.constraints.Pattern;

public final class PhoneDtos {

    private PhoneDtos() {
    }

    public record PhoneItem(String phone, boolean isPrimary, String status) {
    }

    public record PhoneBindRequest(
            @Pattern(regexp = "^1\\d{10}$", message = "invalid mainland phone format")
            String phone
    ) {
    }

    public record PhoneUnbindRequest(
            @Pattern(regexp = "^1\\d{10}$", message = "invalid mainland phone format")
            String phone
    ) {
    }
}
