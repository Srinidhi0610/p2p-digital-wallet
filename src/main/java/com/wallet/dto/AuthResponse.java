package com.wallet.dto;

public class AuthResponse {

    private String token;
    private String tokenType;
    private long expiresInSeconds;
    private UserResponse user;

    public AuthResponse() {
    }

    public AuthResponse(String token, String tokenType, long expiresInSeconds, UserResponse user) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresInSeconds = expiresInSeconds;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }
}
