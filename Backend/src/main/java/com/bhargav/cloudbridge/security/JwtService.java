package com.bhargav.cloudbridge.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtService {

    @Value("${cloudbridge.admin.jwt-secret}")
    private String jwtSecret;

    @Value("${cloudbridge.admin.jwt-expiry-hours}")
    private int jwtExpiryHours;

    // ── GENERATE ───────────────────────────────────────────────────────────────

    /**
     * Generate a JWT token for admin.
     *
     * @param username admin username
     * @return signed JWT string
     */
    public String generateToken(String username) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime()
                + (long) jwtExpiryHours * 60 * 60 * 1000);

        String token = Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .claim("role", "ADMIN")
                .signWith(getSigningKey())
                .compact();

        log.info("[JwtService] Token generated for: {} | expires: {}",
                username, expiry);

        return token;
    }

    // ── VALIDATE ───────────────────────────────────────────────────────────────

    /**
     * Validate a JWT token.
     *
     * @param token raw JWT string
     * @return true if valid and not expired
     */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException e) {
            log.warn("[JwtService] Invalid token: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("[JwtService] Token validation error: {}", e.getMessage());
            return false;
        }
    }

    // ── EXTRACT ────────────────────────────────────────────────────────────────

    /**
     * Extract username (subject) from token.
     *
     * @param token raw JWT string
     * @return username stored in token
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extract expiry date from token.
     */
    public Date extractExpiry(String token) {
        return extractAllClaims(token).getExpiration();
    }

    /**
     * Check if token is expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiry(token).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    // ── PRIVATE ────────────────────────────────────────────────────────────────

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        // Secret must be at least 32 characters for HS256
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}