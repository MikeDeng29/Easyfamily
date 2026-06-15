package com.easyfamily.auth.service;

import com.easyfamily.auth.dto.AuthDtos.CaptchaVerifyRequest;
import com.easyfamily.auth.dto.AuthDtos.AdminLoginRequest;
import com.easyfamily.auth.dto.AuthDtos.LoginRequest;
import com.easyfamily.auth.dto.AuthDtos.LoginResponse;
import com.easyfamily.auth.dto.AuthDtos.RefreshRequest;
import com.easyfamily.auth.dto.AuthDtos.RefreshResponse;
import com.easyfamily.auth.dto.AuthDtos.SlideCaptchaInitRequest;
import com.easyfamily.auth.dto.AuthDtos.SlideCaptchaInitResponse;
import com.easyfamily.auth.dto.AuthDtos.SlideCaptchaVerifyRequest;
import com.easyfamily.auth.dto.AuthDtos.SmsSendRequest;
import com.easyfamily.common.exception.BusinessException;
import com.easyfamily.security.JwtService;
import com.easyfamily.security.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {

    private static final int CAPTCHA_TTL_SECONDS = 300;
    private static final int SLIDE_CHALLENGE_TTL_SECONDS = 120;
    private static final int SLIDE_VERIFY_TOLERANCE_PX = 6;
    private static final int SLIDE_MIN_SOLVE_MS = 500;
    private static final int SLIDE_MAX_SOLVE_MS = 15_000;
    private static final int SLIDE_MIN_TRACK_POINTS = 4;
    private static final int SLIDE_MAX_BACKTRACK_PX = 8;
    private static final int SMS_CODE_TTL_SECONDS = 300;
    private static final String TEST_PHONE = "13800000000";
    private static final int SMS_SEND_RISK_WINDOW_SECONDS = 600;
    private static final int SMS_SEND_RISK_THRESHOLD = 2;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final LoginAuditLogService loginAuditLogService;
    private final JdbcTemplate jdbcTemplate;
    private final SmsSender smsSender;
    private final String adminUsername;
    private final String adminPassword;

    private final String mockSmsCode;

    public AuthService(
            JwtService jwtService,
            TokenBlacklistService tokenBlacklistService,
            LoginAuditLogService loginAuditLogService,
            JdbcTemplate jdbcTemplate,
            SmsSender smsSender,
            @Value("${easyfamily.security.admin-username:admin}") String adminUsername,
            @Value("${easyfamily.security.admin-password:change-me-in-vault}") String adminPassword,
            @Value("${easyfamily.sms.mock-code:}") String mockSmsCode
    ) {
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.loginAuditLogService = loginAuditLogService;
        this.jdbcTemplate = jdbcTemplate;
        this.smsSender = smsSender;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.mockSmsCode = mockSmsCode;
    }

    public String verifyCaptcha(CaptchaVerifyRequest request) {
        String captchaToken = request.captchaProvider() + "-" + UUID.randomUUID();
        jdbcTemplate.update(
                """
                        INSERT INTO auth_captcha_tokens(token, provider, expire_at, created_at)
                        VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                        ON DUPLICATE KEY UPDATE
                          provider = VALUES(provider),
                          expire_at = VALUES(expire_at)
                        """,
                captchaToken,
                request.captchaProvider(),
                Timestamp.from(Instant.now().plusSeconds(CAPTCHA_TTL_SECONDS))
        );
        return captchaToken;
    }

    public SlideCaptchaInitResponse initSlideCaptcha(SlideCaptchaInitRequest request) {
        String challengeId = "slide-" + UUID.randomUUID();
        int targetX = 40 + Math.abs(UUID.randomUUID().hashCode() % 180);
        Instant expireAt = Instant.now().plusSeconds(SLIDE_CHALLENGE_TTL_SECONDS);
        jdbcTemplate.update(
                """
                        INSERT INTO auth_slide_captcha_challenges(challenge_id, target_x, expire_at, consumed, created_at, updated_at)
                        VALUES (?, ?, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        ON DUPLICATE KEY UPDATE
                          target_x = VALUES(target_x),
                          expire_at = VALUES(expire_at),
                          consumed = 0,
                          updated_at = CURRENT_TIMESTAMP
                        """,
                challengeId,
                targetX,
                Timestamp.from(expireAt)
        );

        String backgroundImage = buildBackgroundImageDataUrl(targetX);
        String sliderImage = buildSliderImageDataUrl();
        return new SlideCaptchaInitResponse(
                challengeId,
                backgroundImage,
                sliderImage,
                expireAt.getEpochSecond()
        );
    }

    public String verifySlideCaptcha(SlideCaptchaVerifyRequest request) {
        var rows = jdbcTemplate.query(
                """
                        SELECT target_x, expire_at, consumed
                        , created_at
                        FROM auth_slide_captcha_challenges
                        WHERE challenge_id = ?
                        """,
                (rs, rowNum) -> new SlideChallenge(
                        rs.getInt("target_x"),
                        rs.getTimestamp("expire_at").toInstant(),
                        rs.getBoolean("consumed"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                request.challengeId()
        );
        if (rows.isEmpty()) {
            throw new BusinessException("CAPTCHA_CHALLENGE_NOT_FOUND", "slide challenge not found");
        }
        SlideChallenge challenge = rows.get(0);
        if (challenge.consumed()) {
            throw new BusinessException("CAPTCHA_CHALLENGE_ALREADY_USED", "slide challenge already used");
        }
        if (challenge.expireAt().isBefore(Instant.now())) {
            throw new BusinessException("CAPTCHA_CHALLENGE_EXPIRED", "slide challenge expired");
        }
        long elapsedMs = Instant.now().toEpochMilli() - challenge.createdAt().toEpochMilli();
        if (elapsedMs < SLIDE_MIN_SOLVE_MS) {
            throw new BusinessException("CAPTCHA_RISK_DETECTED", "slide solved too fast");
        }
        if (request.totalTimeMs() < SLIDE_MIN_SOLVE_MS || request.totalTimeMs() > SLIDE_MAX_SOLVE_MS) {
            throw new BusinessException("CAPTCHA_RISK_DETECTED", "invalid slide duration");
        }
        validateTrajectory(request.tracks(), request.offsetX(), request.totalTimeMs());
        if (Math.abs(request.offsetX() - challenge.targetX()) > SLIDE_VERIFY_TOLERANCE_PX) {
            throw new BusinessException("CAPTCHA_SLIDE_VERIFY_FAILED", "slide verify failed");
        }

        jdbcTemplate.update(
                "UPDATE auth_slide_captcha_challenges SET consumed = 1, updated_at = CURRENT_TIMESTAMP WHERE challenge_id = ?",
                request.challengeId()
        );

        String captchaToken = "slide-" + UUID.randomUUID();
        jdbcTemplate.update(
                """
                        INSERT INTO auth_captcha_tokens(token, provider, expire_at, created_at)
                        VALUES (?, 'slide', ?, CURRENT_TIMESTAMP)
                        ON DUPLICATE KEY UPDATE
                          provider = VALUES(provider),
                          expire_at = VALUES(expire_at)
                        """,
                captchaToken,
                Timestamp.from(Instant.now().plusSeconds(CAPTCHA_TTL_SECONDS))
        );
        return captchaToken;
    }

    public void sendSmsCode(SmsSendRequest request, String clientIp) {
        boolean captchaProvided = request.captchaToken() != null && !request.captchaToken().isBlank();
        if (captchaProvided) {
            Integer captchaValid = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM auth_captcha_tokens WHERE token = ? AND expire_at > CURRENT_TIMESTAMP",
                    Integer.class,
                    request.captchaToken()
            );
            if (captchaValid == null || captchaValid <= 0) {
                throw new BusinessException("INVALID_CAPTCHA_TOKEN", "captcha token invalid or expired");
            }
        } else if (isSmsSendRateLimited(request.phone(), clientIp)) {
            throw new BusinessException("CAPTCHA_REQUIRED", "frequent sms requests detected, captcha required");
        }

        String code = (mockSmsCode != null && !mockSmsCode.isBlank())
                ? mockSmsCode
                : String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        // App "一键登录（测试）" uses this fixed phone number, which isn't a real,
        // deliverable mobile number — skip the provider call so it doesn't fail.
        if (!TEST_PHONE.equals(request.phone())) {
            smsSender.send(request.phone(), code);
        }
        jdbcTemplate.update(
                """
                        INSERT INTO auth_sms_codes(phone, sms_code, expire_at, created_at)
                        VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                        ON DUPLICATE KEY UPDATE
                          sms_code = VALUES(sms_code),
                          expire_at = VALUES(expire_at)
                        """,
                request.phone(),
                code,
                Timestamp.from(Instant.now().plusSeconds(SMS_CODE_TTL_SECONDS))
        );
        jdbcTemplate.update(
                """
                        INSERT INTO auth_sms_send_logs(phone, client_ip, created_at)
                        VALUES (?, ?, CURRENT_TIMESTAMP)
                        """,
                request.phone(),
                clientIp
        );
    }

    /**
     * Returns true if the given phone number or client IP has requested an SMS
     * code {@link #SMS_SEND_RISK_THRESHOLD} times or more within the last
     * {@link #SMS_SEND_RISK_WINDOW_SECONDS} seconds, indicating the current
     * request should be challenged with a slide captcha before sending again.
     */
    private boolean isSmsSendRateLimited(String phone, String clientIp) {
        Timestamp windowStart = Timestamp.from(Instant.now().minusSeconds(SMS_SEND_RISK_WINDOW_SECONDS));
        Integer phoneCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM auth_sms_send_logs WHERE phone = ? AND created_at > ?",
                Integer.class,
                phone,
                windowStart
        );
        if (phoneCount != null && phoneCount >= SMS_SEND_RISK_THRESHOLD) {
            return true;
        }
        if (clientIp == null || clientIp.isBlank()) {
            return false;
        }
        Integer ipCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM auth_sms_send_logs WHERE client_ip = ? AND created_at > ?",
                Integer.class,
                clientIp,
                windowStart
        );
        return ipCount != null && ipCount >= SMS_SEND_RISK_THRESHOLD;
    }

    public LoginResponse login(LoginRequest request, String clientIp, String userAgent) {
        try {
            verifySmsCode(request.phone(), request.smsCode());
            String userId = "U" + request.phone();
            String accessToken = jwtService.issueAccessToken(userId, request.phone());
            String refreshToken = jwtService.issueRefreshToken(userId, request.phone());
            loginAuditLogService.record("USER_PHONE", request.phone(), "SUCCESS", null, clientIp, userAgent);
            return new LoginResponse(
                    userId,
                    accessToken,
                    refreshToken
            );
        } catch (BusinessException ex) {
            loginAuditLogService.record("USER_PHONE", request.phone(), "FAIL", ex.getCode(), clientIp, userAgent);
            throw ex;
        }
    }

    public LoginResponse adminLogin(AdminLoginRequest request, String clientIp, String userAgent) {
        try {
            if (!adminUsername.equals(request.username()) || !adminPassword.equals(request.password())) {
                throw new BusinessException("INVALID_ADMIN_CREDENTIALS", "invalid admin username or password");
            }
            String userId = "ADMIN_" + request.username();
            String accessToken = jwtService.issueAdminAccessToken(userId, request.username());
            String refreshToken = jwtService.issueRefreshToken(userId, request.username());
            loginAuditLogService.record("ADMIN_PASSWORD", request.username(), "SUCCESS", null, clientIp, userAgent);
            return new LoginResponse(userId, accessToken, refreshToken);
        } catch (BusinessException ex) {
            loginAuditLogService.record("ADMIN_PASSWORD", request.username(), "FAIL", ex.getCode(), clientIp, userAgent);
            throw ex;
        }
    }

    public void verifySmsCode(String phone, String smsCode) {
        Integer valid = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1) FROM auth_sms_codes
                        WHERE phone = ? AND sms_code = ? AND expire_at > CURRENT_TIMESTAMP
                        """,
                Integer.class,
                phone,
                smsCode
        );
        if (valid == null || valid <= 0) {
            throw new BusinessException("INVALID_SMS_CODE", "invalid sms code");
        }
        jdbcTemplate.update("DELETE FROM auth_sms_codes WHERE phone = ?", phone);
    }

    public RefreshResponse refresh(RefreshRequest request) {
        if (tokenBlacklistService.isRevoked(request.refreshToken())) {
            throw new BusinessException("TOKEN_REVOKED", "refresh token has been revoked");
        }
        Claims claims;
        try {
            claims = jwtService.parse(request.refreshToken());
        } catch (Exception ex) {
            throw new BusinessException("INVALID_REFRESH_TOKEN", "invalid refresh token");
        }

        String tokenType = claims.get("tokenType", String.class);
        if (!"refresh".equals(tokenType)) {
            throw new BusinessException("INVALID_REFRESH_TOKEN", "token type must be refresh");
        }

        String userId = claims.getSubject();
        String phone = claims.get("phone", String.class);
        tokenBlacklistService.revoke(request.refreshToken(), claims.getExpiration().toInstant());
        String accessToken = jwtService.issueAccessToken(userId, phone);
        String refreshToken = jwtService.issueRefreshToken(userId, phone);
        return new RefreshResponse(accessToken, refreshToken);
    }

    public void logout(String accessToken, String refreshToken) {
        revokeTokenSafely(accessToken);
        revokeTokenSafely(refreshToken);
    }

    private void revokeTokenSafely(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            Claims claims = jwtService.parse(token);
            tokenBlacklistService.revoke(token, claims.getExpiration().toInstant());
        } catch (Exception ignored) {
            // Ignore invalid token on logout to keep endpoint idempotent.
        }
    }

    private void validateTrajectory(List<com.easyfamily.auth.dto.AuthDtos.SlideTrackPoint> tracks, int offsetX, int totalTimeMs) {
        if (tracks.size() < SLIDE_MIN_TRACK_POINTS) {
            throw new BusinessException("CAPTCHA_RISK_DETECTED", "trajectory points too few");
        }
        com.easyfamily.auth.dto.AuthDtos.SlideTrackPoint first = tracks.get(0);
        com.easyfamily.auth.dto.AuthDtos.SlideTrackPoint last = tracks.get(tracks.size() - 1);
        if (first.t() != 0) {
            throw new BusinessException("CAPTCHA_RISK_DETECTED", "trajectory start time invalid");
        }

        int prevX = first.x();
        int prevT = first.t();
        for (int i = 1; i < tracks.size(); i++) {
            com.easyfamily.auth.dto.AuthDtos.SlideTrackPoint current = tracks.get(i);
            if (current.t() <= prevT) {
                throw new BusinessException("CAPTCHA_RISK_DETECTED", "trajectory time not increasing");
            }
            if (current.x() < prevX - SLIDE_MAX_BACKTRACK_PX) {
                throw new BusinessException("CAPTCHA_RISK_DETECTED", "trajectory backtrack too large");
            }
            prevX = current.x();
            prevT = current.t();
        }

        int duration = last.t() - first.t();
        if (Math.abs(duration - totalTimeMs) > 600) {
            throw new BusinessException("CAPTCHA_RISK_DETECTED", "duration mismatch");
        }
        if (Math.abs(last.x() - offsetX) > SLIDE_VERIFY_TOLERANCE_PX) {
            throw new BusinessException("CAPTCHA_RISK_DETECTED", "trajectory and offset mismatch");
        }
    }

    private String buildBackgroundImageDataUrl(int targetX) {
        String svg = """
                <svg xmlns='http://www.w3.org/2000/svg' width='320' height='160'>
                  <defs>
                    <linearGradient id='g' x1='0' y1='0' x2='1' y2='1'>
                      <stop offset='0%%' stop-color='#eef3ff'/>
                      <stop offset='100%%' stop-color='#dce7ff'/>
                    </linearGradient>
                  </defs>
                  <rect x='0' y='0' width='320' height='160' rx='12' fill='url(#g)'/>
                  <circle cx='%d' cy='80' r='20' fill='#b9c7ea' fill-opacity='0.75'/>
                  <text x='14' y='24' font-size='14' fill='#5f6b8a'>Drag slider to fit puzzle</text>
                </svg>
                """.formatted(targetX);
        return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
    }

    private String buildSliderImageDataUrl() {
        String svg = """
                <svg xmlns='http://www.w3.org/2000/svg' width='40' height='40'>
                  <circle cx='20' cy='20' r='20' fill='#6e86c8'/>
                </svg>
                """;
        return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
    }

    private record SlideChallenge(int targetX, Instant expireAt, boolean consumed, Instant createdAt) {
    }
}
