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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration request cannot be null");
        }

        String normalizedEmail = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException("Email is already registered: " + normalizedEmail);
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getName().trim(), normalizedEmail, encodedPassword);

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateEmailException("Email is already registered: " + normalizedEmail);
        }

        String token = jwtService.generateToken(user);
        long expiresInSeconds = jwtService.getExpirationMs() / 1000;

        return new AuthResponse(token, "Bearer", expiresInSeconds, UserResponse.fromEntity(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Login request cannot be null");
        }

        String normalizedEmail = normalizeEmail(request.getEmail());

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);
        long expiresInSeconds = jwtService.getExpirationMs() / 1000;

        return new AuthResponse(token, "Bearer", expiresInSeconds, UserResponse.fromEntity(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserProfile(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
        return UserResponse.fromEntity(user);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
