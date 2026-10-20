package com.wallet.controller;

import com.wallet.dto.AuthResponse;
import com.wallet.dto.LoginRequest;
import com.wallet.dto.RegisterRequest;
import com.wallet.dto.UserResponse;
import com.wallet.exception.InvalidCredentialsException;
import com.wallet.security.UserPrincipal;
import com.wallet.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new InvalidCredentialsException("Unauthenticated request");
        }
        UserResponse response = authService.getUserProfile(principal.getId());
        return ResponseEntity.ok(response);
    }
}
