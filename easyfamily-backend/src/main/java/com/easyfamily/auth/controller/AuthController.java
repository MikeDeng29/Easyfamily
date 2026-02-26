package com.easyfamily.auth.controller;

import com.easyfamily.auth.dto.AuthDtos.CaptchaVerifyRequest;
import com.easyfamily.auth.dto.AuthDtos.LoginRequest;
import com.easyfamily.auth.dto.AuthDtos.LoginResponse;
import com.easyfamily.auth.dto.AuthDtos.SmsSendRequest;
import com.easyfamily.auth.service.AuthService;
import com.easyfamily.common.api.ApiResponse;
import jakarta.validation.Valid;
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

    @PostMapping("/sms/send")
    public ApiResponse<Void> sendSmsCode(@Valid @RequestBody SmsSendRequest request) {
        authService.sendSmsCode(request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }
}
