package com.easyfamily.auth.service;

import com.easyfamily.auth.dto.AuthDtos.CaptchaVerifyRequest;
import com.easyfamily.auth.dto.AuthDtos.LoginRequest;
import com.easyfamily.auth.dto.AuthDtos.LoginResponse;
import com.easyfamily.auth.dto.AuthDtos.SmsSendRequest;
import com.easyfamily.common.exception.BusinessException;
import com.easyfamily.security.JwtService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final Map<String, String> phoneCodeStore = new ConcurrentHashMap<>();
    private final Map<String, Instant> captchaTokenStore = new ConcurrentHashMap<>();
    private final JwtService jwtService;

    public AuthService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public String verifyCaptcha(CaptchaVerifyRequest request) {
        String captchaToken = request.captchaProvider() + "-" + UUID.randomUUID();
        captchaTokenStore.put(captchaToken, Instant.now().plusSeconds(300));
        return captchaToken;
    }

    public void sendSmsCode(SmsSendRequest request) {
        Instant expireAt = captchaTokenStore.get(request.captchaToken());
        if (expireAt == null || Instant.now().isAfter(expireAt)) {
            throw new BusinessException("INVALID_CAPTCHA_TOKEN", "captcha token invalid or expired");
        }
        // TODO: Integrate SMS provider (Aliyun/Tencent Cloud) in Sprint 2.
        phoneCodeStore.put(request.phone(), "123456");
    }

    public LoginResponse login(LoginRequest request) {
        String currentCode = phoneCodeStore.get(request.phone());
        if (currentCode == null || !currentCode.equals(request.smsCode())) {
            throw new BusinessException("INVALID_SMS_CODE", "invalid sms code");
        }
        String userId = "U" + request.phone();
        String accessToken = jwtService.issueAccessToken(userId, request.phone());
        String refreshToken = jwtService.issueRefreshToken(userId, request.phone());
        return new LoginResponse(
                userId,
                accessToken,
                refreshToken
        );
    }
}
