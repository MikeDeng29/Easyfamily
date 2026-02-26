package com.easyfamily.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record CaptchaVerifyRequest(
            @NotBlank String captchaProvider,
            @NotBlank String ticket
    ) {
    }

    public record SmsSendRequest(
            @Pattern(regexp = "^1\\d{10}$", message = "invalid mainland phone format")
            String phone,
            @NotBlank String captchaToken
    ) {
    }

    public record LoginRequest(
            @Pattern(regexp = "^1\\d{10}$", message = "invalid mainland phone format")
            String phone,
            @NotBlank String smsCode
    ) {
    }

    public record LoginResponse(
            String userId,
            String accessToken,
            String refreshToken
    ) {
    }
}
