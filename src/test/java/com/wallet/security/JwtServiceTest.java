package com.wallet.security;

import com.wallet.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String VALID_SECRET = "super-secret-key-that-is-at-least-32-bytes-long!";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(VALID_SECRET, EXPIRATION_MS);
        jwtService.validateConfiguration();
    }

    @Test
    @DisplayName("Startup validation fails if secret key is missing or shorter than 32 characters")
    void startupFailsOnMissingOrShortSecret() {
        JwtService nullSecretService = new JwtService(null, EXPIRATION_MS);
        assertThrows(IllegalStateException.class, nullSecretService::validateConfiguration);

        JwtService shortSecretService = new JwtService("too-short-secret", EXPIRATION_MS);
        assertThrows(IllegalStateException.class, shortSecretService::validateConfiguration);
    }

    @Test
    @DisplayName("generateToken should encode stable user ID as subject and include email claim")
    void generateTokenShouldIncludeSubjectAndClaims() {
        User user = new User("Alice", "alice@example.com", "hashed_pwd");
        user.setId(42L);

        String token = jwtService.generateToken(user);
        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));

        assertEquals(42L, jwtService.extractUserId(token));
        assertEquals("alice@example.com", jwtService.extractEmail(token));
    }

    @Test
    @DisplayName("isTokenValid should reject malformed or blank tokens")
    void isTokenValidShouldRejectMalformedTokens() {
        assertFalse(jwtService.isTokenValid(null));
        assertFalse(jwtService.isTokenValid(""));
        assertFalse(jwtService.isTokenValid("   "));
        assertFalse(jwtService.isTokenValid("not.a.valid.jwt.token"));
    }

    @Test
    @DisplayName("isTokenValid should reject expired tokens")
    void isTokenValidShouldRejectExpiredTokens() {
        // Create an already-expired token directly
        SecretKey key = Keys.hmacShaKeyFor(VALID_SECRET.getBytes(StandardCharsets.UTF_8));
        Instant past = Instant.now().minusSeconds(120);

        String expiredToken = Jwts.builder()
                .subject("42")
                .claim("email", "alice@example.com")
                .issuedAt(Date.from(past.minusSeconds(60)))
                .expiration(Date.from(past))
                .signWith(key)
                .compact();

        assertFalse(jwtService.isTokenValid(expiredToken));
    }

    @Test
    @DisplayName("isTokenValid should reject tokens signed with a different secret key")
    void isTokenValidShouldRejectIncorrectlySignedTokens() {
        String otherSecret = "another-completely-different-signing-secret-key-32-chars!";
        SecretKey otherKey = Keys.hmacShaKeyFor(otherSecret.getBytes(StandardCharsets.UTF_8));

        String foreignToken = Jwts.builder()
                .subject("42")
                .claim("email", "alice@example.com")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(otherKey)
                .compact();

        assertFalse(jwtService.isTokenValid(foreignToken));
    }

    @Test
    @DisplayName("generateToken should reject null user or null user ID")
    void generateTokenShouldRejectNullUserOrId() {
        assertThrows(IllegalArgumentException.class, () -> jwtService.generateToken(null));

        User userWithoutId = new User("Alice", "alice@example.com", "hash");
        assertThrows(IllegalArgumentException.class, () -> jwtService.generateToken(userWithoutId));
    }
}
