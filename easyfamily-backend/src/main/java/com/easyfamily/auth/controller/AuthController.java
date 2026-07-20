package com.easyfamily.auth.controller;

import com.easyfamily.auth.dto.AuthDtos.CaptchaVerifyRequest;
import com.easyfamily.auth.dto.AuthDtos.AdminLoginRequest;
import com.easyfamily.auth.dto.AuthDtos.LoginRequest;
import com.easyfamily.auth.dto.AuthDtos.LoginResponse;
import com.easyfamily.auth.dto.AuthDtos.LogoutRequest;
import com.easyfamily.auth.dto.AuthDtos.PasswordLoginRequest;
import com.easyfamily.auth.dto.AuthDtos.RefreshRequest;
import com.easyfamily.auth.dto.AuthDtos.RefreshResponse;
import com.easyfamily.auth.dto.AuthDtos.SlideCaptchaInitRequest;
import com.easyfamily.auth.dto.AuthDtos.SlideCaptchaInitResponse;
import com.easyfamily.auth.dto.AuthDtos.SlideCaptchaVerifyRequest;
import com.easyfamily.auth.dto.AuthDtos.SmsSendRequest;
import com.easyfamily.auth.service.AuthService;
import com.easyfamily.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/captcha/verify")
    public ApiResponse<Map<String, String>> verifyCaptcha(@Valid @RequestBody CaptchaVerifyRequest request) {
        String captchaToken = authService.verifyCaptcha(request);
        return ApiResponse.ok(Map.of("captchaToken", captchaToken));
    }

    @PostMapping("/captcha/slide/init")
    public ApiResponse<SlideCaptchaInitResponse> initSlideCaptcha(@RequestBody(required = false) SlideCaptchaInitRequest request) {
        return ApiResponse.ok(authService.initSlideCaptcha(request));
    }

    @PostMapping("/captcha/slide/verify")
    public ApiResponse<Map<String, String>> verifySlideCaptcha(@Valid @RequestBody SlideCaptchaVerifyRequest request) {
        String captchaToken = authService.verifySlideCaptcha(request);
        return ApiResponse.ok(Map.of("captchaToken", captchaToken));
    }

    @PostMapping("/sms/send")
    public ApiResponse<Void> sendSmsCode(@Valid @RequestBody SmsSendRequest request, HttpServletRequest httpServletRequest) {
        authService.sendSmsCode(request, resolveClientIp(httpServletRequest));
        return ApiResponse.ok(null);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        return ApiResponse.ok(authService.login(request, resolveClientIp(httpServletRequest), resolveUserAgent(httpServletRequest)));
    }

    @PostMapping("/login/password")
    public ApiResponse<LoginResponse> loginWithPassword(@Valid @RequestBody PasswordLoginRequest request, HttpServletRequest httpServletRequest) {
        return ApiResponse.ok(authService.loginWithPassword(request, resolveClientIp(httpServletRequest), resolveUserAgent(httpServletRequest)));
    }

    @PostMapping("/admin/login")
    public ApiResponse<LoginResponse> adminLogin(
            @Valid @RequestBody AdminLoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.ok(authService.adminLogin(request, resolveClientIp(httpServletRequest), resolveUserAgent(httpServletRequest)));
    }

    @PostMapping("/refresh")
    public ApiResponse<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest httpServletRequest, @RequestBody(required = false) LogoutRequest request) {
        String accessToken = extractBearerToken(httpServletRequest.getHeader("Authorization"));
        String refreshToken = request != null ? request.refreshToken() : null;
        authService.logout(accessToken, refreshToken);
        return ApiResponse.ok(null);
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}
