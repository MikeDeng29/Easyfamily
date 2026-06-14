package com.easyfamily.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final int accessTokenMinutes;
    private final int refreshTokenDays;

    public JwtService(
            @Value("${easyfamily.security.jwt-secret}") String jwtSecret,
            @Value("${easyfamily.security.access-token-minutes}") int accessTokenMinutes,
            @Value("${easyfamily.security.refresh-token-days}") int refreshTokenDays
    ) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenMinutes = accessTokenMinutes;
        this.refreshTokenDays = refreshTokenDays;
    }

    public String issueAccessToken(String userId, String phone) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .id(UUID.randomUUID().toString())
                .claims(Map.of("phone", phone, "tokenType", "access"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenMinutes * 60L)))
                .signWith(secretKey)
                .compact();
    }

    public String issueAdminAccessToken(String userId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .id(UUID.randomUUID().toString())
                .claims(Map.of("phone", username, "tokenType", "access", "roles", "ROLE_ADMIN"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenMinutes * 60L)))
                .signWith(secretKey)
                .compact();
    }

    public String issueRefreshToken(String userId, String phone) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .id(UUID.randomUUID().toString())
                .claims(Map.of("phone", phone, "tokenType", "refresh"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(refreshTokenDays * 24L * 3600L)))
                .signWith(secretKey)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }
}
