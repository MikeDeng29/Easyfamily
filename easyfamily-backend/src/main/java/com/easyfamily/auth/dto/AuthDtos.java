package com.easyfamily.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

import java.util.List;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record CaptchaVerifyRequest(
            @NotBlank String captchaProvider,
            @NotBlank String ticket
    ) {
    }

    public record SlideCaptchaInitRequest(
            String clientId
    ) {
    }

    public record SlideCaptchaInitResponse(
            String challengeId,
            String backgroundImageUrl,
            String sliderImageUrl,
            long expireAtEpochSeconds
    ) {
    }

    public record SlideCaptchaVerifyRequest(
            @NotBlank String challengeId,
            int offsetX,
            @NotNull @Min(1) Integer totalTimeMs,
            @NotEmpty List<@Valid SlideTrackPoint> tracks
    ) {
    }

    public record SlideTrackPoint(
            @Min(0) int x,
            @Min(0) int y,
            @Min(0) int t
    ) {
    }

    public record SmsSendRequest(
            @Pattern(regexp = "^1\\d{10}$", message = "invalid mainland phone format")
            String phone,
            String captchaToken
    ) {
    }

    public record LoginRequest(
            @Pattern(regexp = "^1\\d{10}$", message = "invalid mainland phone format")
            String phone,
            @NotBlank String smsCode
    ) {
    }

    public record AdminLoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }

    public record PasswordLoginRequest(
            @Pattern(regexp = "^1\\d{10}$", message = "invalid mainland phone format")
            String phone,
            @NotBlank @Size(min = 6, max = 20) String password
    ) {
    }

    public record LoginResponse(
            String userId,
            String accessToken,
            String refreshToken
    ) {
    }

    public record RefreshRequest(
            @NotBlank String refreshToken
    ) {
    }

    public record RefreshResponse(
            String accessToken,
            String refreshToken
    ) {
    }

    public record LogoutRequest(
            String refreshToken
    ) {
    }
}
