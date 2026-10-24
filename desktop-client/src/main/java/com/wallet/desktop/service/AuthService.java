package com.wallet.desktop.service;

import com.wallet.desktop.model.AuthResponse;
import com.wallet.desktop.model.HealthResponse;
import com.wallet.desktop.model.LoginRequest;
import com.wallet.desktop.model.RegisterRequest;
import com.wallet.desktop.model.User;

import java.util.concurrent.CompletableFuture;

public class AuthService {
    private static final AuthService instance = new AuthService();
    private final ApiClient apiClient = ApiClient.getInstance();

    private AuthService() {}

    public static AuthService getInstance() {
        return instance;
    }

    public CompletableFuture<AuthResponse> login(String email, String password) {
        LoginRequest request = new LoginRequest(email, password);
        return apiClient.postAsync("/auth/login", request, AuthResponse.class)
                .thenApply(response -> {
                    if (response != null && response.getToken() != null) {
                        SessionManager.getInstance().setAuth(response.getToken(), response.getUser());
                    }
                    return response;
                });
    }

    public CompletableFuture<AuthResponse> register(String name, String email, String password) {
        RegisterRequest request = new RegisterRequest(name, email, password);
        return apiClient.postAsync("/auth/register", request, AuthResponse.class)
                .thenApply(response -> {
                    if (response != null && response.getToken() != null) {
                        SessionManager.getInstance().setAuth(response.getToken(), response.getUser());
                    }
                    return response;
                });
    }

    public CompletableFuture<User> getCurrentUser() {
        return apiClient.getAsync("/auth/me", User.class)
                .thenApply(user -> {
                    if (user != null) {
                        SessionManager.getInstance().setCurrentUser(user);
                    }
                    return user;
                });
    }

    public CompletableFuture<User> getProfile() {
        return getCurrentUser();
    }

    public CompletableFuture<HealthResponse> checkHealth() {
        return apiClient.getAsync("/health", HealthResponse.class);
    }

    public void logout() {
        SessionManager.getInstance().clearAuth();
    }
}
