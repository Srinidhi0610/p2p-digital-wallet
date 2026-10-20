package com.wallet.service;

import com.wallet.dto.AuthResponse;
import com.wallet.dto.LoginRequest;
import com.wallet.dto.RegisterRequest;
import com.wallet.dto.UserResponse;
import com.wallet.entity.User;
import com.wallet.exception.DuplicateEmailException;
import com.wallet.exception.InvalidCredentialsException;
import com.wallet.repository.UserRepository;
import com.wallet.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("Registration encodes password with BCrypt and normalizes email")
    void registerShouldEncodePasswordAndNormalizeEmail() {
        RegisterRequest request = new RegisterRequest("  Alice Smith  ", "  Alice.Smith@Example.COM  ", "SecretPassword123!");

        when(userRepository.existsByEmail("alice.smith@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User userToSave = invocation.getArgument(0);
            userToSave.setId(10L);
            userToSave.setCreatedAt(Instant.now());
            return userToSave;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("mock-jwt-token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400L, response.getExpiresInSeconds());

        UserResponse userResponse = response.getUser();
        assertNotNull(userResponse);
        assertEquals(10L, userResponse.getId());
        assertEquals("Alice Smith", userResponse.getName());
        assertEquals("alice.smith@example.com", userResponse.getEmail());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("alice.smith@example.com", savedUser.getEmail());
        assertNotEquals("SecretPassword123!", savedUser.getPasswordHash());
        assertTrue(passwordEncoder.matches("SecretPassword123!", savedUser.getPasswordHash()));
    }

    @Test
    @DisplayName("Registration rejects duplicate email addresses")
    void registerShouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("Bob", "bob@example.com", "Password123!");
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(true);

        DuplicateEmailException ex = assertThrows(DuplicateEmailException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("bob@example.com"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Registration rejects concurrent duplicate email violating database constraint")
    void registerShouldHandleDataIntegrityViolationOnConcurrentAttempt() {
        RegisterRequest request = new RegisterRequest("Bob", "bob@example.com", "Password123!");
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThrows(DuplicateEmailException.class, () -> authService.register(request));
    }

    @Test
    @DisplayName("Login succeeds with valid credentials")
    void loginShouldSucceedWithValidCredentials() {
        String rawPassword = "CorrectPassword123!";
        String encodedHash = passwordEncoder.encode(rawPassword);
        User user = new User("Alice", "alice@example.com", encodedHash);
        user.setId(1L);
        user.setCreatedAt(Instant.now());

        LoginRequest request = new LoginRequest("ALICE@EXAMPLE.COM", rawPassword);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("mock-valid-token");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock-valid-token", response.getToken());
        assertEquals("alice@example.com", response.getUser().getEmail());
    }

    @Test
    @DisplayName("Login fails with generic message when email does not exist")
    void loginShouldFailWithGenericMessageForNonExistentUser() {
        LoginRequest request = new LoginRequest("unknown@example.com", "any-password");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    @DisplayName("Login fails with identical generic message when password is wrong")
    void loginShouldFailWithGenericMessageForWrongPassword() {
        String encodedHash = passwordEncoder.encode("ActualPassword123!");
        User user = new User("Alice", "alice@example.com", encodedHash);
        user.setId(1L);

        LoginRequest request = new LoginRequest("alice@example.com", "WrongPassword456!");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        InvalidCredentialsException ex = assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
        assertEquals("Invalid email or password", ex.getMessage());
    }
}
