package com.myapp.authservice.util;

import com.myapp.authservice.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Token structure must match exactly:
 * {
 *   userId: string,
 *   phone: string,
 *   role: string,
 *   iat: number,
 *   exp: number
 * }
 */
@Slf4j
@Component
public class JwtUtils {

    private final SecretKey secretKey;
    private final long accessTokenExpirySeconds;
    private final long refreshTokenExpiryDays;

    public JwtUtils(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiry-seconds}") long accessTokenExpirySeconds,
            @Value("${app.jwt.refresh-token-expiry-days}") long refreshTokenExpiryDays) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirySeconds = accessTokenExpirySeconds;
        this.refreshTokenExpiryDays = refreshTokenExpiryDays;
    }

    /**
     * Generate access token
     * Matches: JwtUtils.generateAccessToken({ userId, phone, role })
     *
     * @param userId User ID
     * @param phone Phone number
     * @param role User role
     * @return JWT access token string
     */
    public String generateAccessToken(String userId, String phone, Role role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("phone", phone);
        claims.put("role", role.name());

        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenExpirySeconds, ChronoUnit.SECONDS);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Generate refresh token
     * Matches: JwtUtils.generateRefreshToken(userId)
     *
     * @param userId User ID
     * @return JWT refresh token string
     */
    public String generateRefreshToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);

        Instant now = Instant.now();
        Instant expiry = now.plus(refreshTokenExpiryDays, ChronoUnit.DAYS);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Verify and parse access token
     * Matches: JwtUtils.verifyAccessToken(token)
     *
     * @param token JWT token string
     * @return Claims if valid
     * @throws JwtException if token is invalid or expired
     */
    public Claims verifyAccessToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Verify refresh token
     * Matches: JwtUtils.verifyRefreshToken(token)
     *
     * @param token JWT token string
     * @return Claims if valid
     * @throws JwtException if token is invalid or expired
     */
    public Claims verifyRefreshToken(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract userId from token without full verification
     * Used for logging/debugging purposes only
     *
     * @param token JWT token
     * @return userId or null if unable to extract
     */
    public String extractUserIdUnsafe(String token) {
        try {
            // Parse without verification for logging
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;

            String payload = new String(
                java.util.Base64.getUrlDecoder().decode(parts[1]),
                StandardCharsets.UTF_8
            );

            // Simple JSON parsing for userId
            if (payload.contains("\"userId\"")) {
                int start = payload.indexOf("\"userId\":\"") + 10;
                int end = payload.indexOf("\"", start);
                if (start > 9 && end > start) {
                    return payload.substring(start, end);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract userId from token", e);
        }
        return null;
    }
}
