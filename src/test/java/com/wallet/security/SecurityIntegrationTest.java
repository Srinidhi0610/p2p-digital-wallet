package com.wallet.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.dto.LoginRequest;
import com.wallet.dto.RegisterRequest;
import com.wallet.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Health endpoint is public and accessible without authentication")
    void healthEndpointShouldBePublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Registration endpoint is public, hashes password, and never returns password hash")
    void registrationShouldBePublicAndNeverExposePasswordHash() throws Exception {
        RegisterRequest request = new RegisterRequest("Alice Test", "alice.test@example.com", "Password123!");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("alice.test@example.com"))
                .andExpect(jsonPath("$.user.name").value("Alice Test"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertFalse(responseBody.contains("passwordHash"));
        assertFalse(responseBody.contains("Password123!"));
    }

    @Test
    @DisplayName("Registration rejects duplicate email addresses with 409 Conflict")
    void registrationShouldRejectDuplicateEmail() throws Exception {
        RegisterRequest first = new RegisterRequest("Alice", "duplicate@example.com", "Password123!");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        RegisterRequest second = new RegisterRequest("Alice Another", "duplicate@example.com", "DifferentPassword123!");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(containsString("already registered")));
    }

    @Test
    @DisplayName("Registration rejects invalid request parameters with 400 Bad Request")
    void registrationShouldRejectInvalidRequest() throws Exception {
        RegisterRequest invalid = new RegisterRequest("", "invalid-email", "short");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Login authenticates valid credentials and returns JWT")
    void loginShouldAuthenticateValidCredentials() throws Exception {
        RegisterRequest register = new RegisterRequest("Bob", "bob@example.com", "ValidPassword123!");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest("bob@example.com", "ValidPassword123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("bob@example.com"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Login rejects non-existent user with generic 401 error")
    void loginShouldRejectNonExistentUserGenerically() throws Exception {
        LoginRequest login = new LoginRequest("nonexistent@example.com", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("Login rejects wrong password with identical generic 401 error")
    void loginShouldRejectWrongPasswordGenerically() throws Exception {
        RegisterRequest register = new RegisterRequest("Carol", "carol@example.com", "CorrectPassword123!");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest("carol@example.com", "IncorrectPassword123!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("Protected endpoint requires authentication and rejects missing token with 401")
    void protectedEndpointShouldRejectMissingToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Protected endpoint rejects malformed token with 401")
    void protectedEndpointShouldRejectMalformedToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer not-a-valid-jwt-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Protected endpoint rejects expired token with 401")
    void protectedEndpointShouldRejectExpiredToken() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Instant past = Instant.now().minusSeconds(120);

        String expiredToken = Jwts.builder()
                .subject("999")
                .claim("email", "expired@example.com")
                .issuedAt(Date.from(past.minusSeconds(60)))
                .expiration(Date.from(past))
                .signWith(key)
                .compact();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Protected endpoint rejects incorrectly signed token with 401")
    void protectedEndpointShouldRejectIncorrectlySignedToken() throws Exception {
        String foreignSecret = "completely-different-foreign-signing-secret-key-32-chars!";
        SecretKey foreignKey = Keys.hmacShaKeyFor(foreignSecret.getBytes(StandardCharsets.UTF_8));

        String foreignToken = Jwts.builder()
                .subject("999")
                .claim("email", "foreign@example.com")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(foreignKey)
                .compact();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + foreignToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Protected endpoint authenticates successfully with valid JWT")
    void protectedEndpointShouldSucceedWithValidToken() throws Exception {
        RegisterRequest register = new RegisterRequest("David", "david@example.com", "Password123!");
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = regResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseJson).get("token").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("david@example.com"))
                .andExpect(jsonPath("$.name").value("David"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
}
