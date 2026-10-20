package com.wallet.security;

import com.wallet.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final String secretString;
    private final long expirationMs;
    private SecretKey signingKey;

    public JwtService(
            @Value("${jwt.secret:${JWT_SECRET:}}") String secretString,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs) {
        this.secretString = secretString;
        this.expirationMs = expirationMs;
    }

    @PostConstruct
    public void validateConfiguration() {
        if (secretString == null || secretString.trim().length() < 32) {
            throw new IllegalStateException(
                    "JWT secret key must be configured via 'jwt.secret' or 'JWT_SECRET' and must be at least 32 characters (256 bits) long.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretString.trim().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User and user ID must not be null when generating token");
        }
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public boolean isTokenValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Claims claims = extractAllClaims(token);
            Date expiration = claims.getExpiration();
            return expiration != null && expiration.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long extractUserId(String token) {
        String subject = extractAllClaims(token).getSubject();
        return subject != null ? Long.parseLong(subject) : null;
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    public Claims extractAllClaims(String token) {
        if (signingKey == null) {
            validateConfiguration();
        }
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
