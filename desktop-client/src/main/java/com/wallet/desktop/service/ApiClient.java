package com.wallet.desktop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wallet.desktop.config.AppConfig;
import com.wallet.desktop.model.ErrorResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ApiClient {
    private static final ApiClient instance = new ApiClient();

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;

    private ApiClient() {
        this.executor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "wallet-api-worker");
            t.setDaemon(true);
            return t;
        });

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .executor(executor)
                .build();

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static ApiClient getInstance() {
        return instance;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public <T> CompletableFuture<T> getAsync(String endpoint, Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> execute("GET", endpoint, null, responseType), executor);
    }

    public <T> CompletableFuture<T> getAsync(String endpoint, TypeReference<T> responseType) {
        return CompletableFuture.supplyAsync(() -> execute("GET", endpoint, null, responseType), executor);
    }

    public <T> CompletableFuture<T> postAsync(String endpoint, Object requestBody, Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> execute("POST", endpoint, requestBody, responseType), executor);
    }

    public <T> CompletableFuture<T> postAsync(String endpoint, Object requestBody, TypeReference<T> responseType) {
        return CompletableFuture.supplyAsync(() -> execute("POST", endpoint, requestBody, responseType), executor);
    }

    public <T> CompletableFuture<T> putAsync(String endpoint, Object requestBody, Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> execute("PUT", endpoint, requestBody, responseType), executor);
    }

    public <T> CompletableFuture<T> deleteAsync(String endpoint, Class<T> responseType) {
        return CompletableFuture.supplyAsync(() -> execute("DELETE", endpoint, null, responseType), executor);
    }

    private <T> T execute(String method, String endpoint, Object requestBody, Class<T> responseType) {
        String fullUrl = AppConfig.getApiUrl(endpoint);
        HttpResponse<String> response = sendHttpRequest(method, fullUrl, requestBody);
        return handleResponse(response, responseType, null);
    }

    private <T> T execute(String method, String endpoint, Object requestBody, TypeReference<T> responseType) {
        String fullUrl = AppConfig.getApiUrl(endpoint);
        HttpResponse<String> response = sendHttpRequest(method, fullUrl, requestBody);
        return handleResponse(response, null, responseType);
    }

    private HttpResponse<String> sendHttpRequest(String method, String fullUrl, Object requestBody) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Accept", "application/json");

            String token = SessionManager.getInstance().getToken();
            if (token != null && !token.isBlank()) {
                builder.header("Authorization", "Bearer " + token);
            }

            HttpRequest.BodyPublisher bodyPublisher;
            if (requestBody != null) {
                String jsonBody = objectMapper.writeValueAsString(requestBody);
                bodyPublisher = HttpRequest.BodyPublishers.ofString(jsonBody);
                builder.header("Content-Type", "application/json");
            } else {
                bodyPublisher = HttpRequest.BodyPublishers.noBody();
            }

            switch (method.toUpperCase()) {
                case "GET" -> builder.GET();
                case "POST" -> builder.POST(bodyPublisher);
                case "PUT" -> builder.PUT(bodyPublisher);
                case "DELETE" -> builder.DELETE();
                default -> builder.method(method.toUpperCase(), bodyPublisher);
            }

            return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ApiException(
                    "Unable to connect to wallet server at " + AppConfig.getBaseUrl() +
                            ". Please ensure the backend is running.",
                    0
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Request interrupted: " + e.getMessage(), 0);
        }
    }

    private <T> T handleResponse(HttpResponse<String> response, Class<T> clazz, TypeReference<T> typeRef) {
        int status = response.statusCode();
        String body = response.body();

        if (status == 401) {
            SessionManager.getInstance().clearAuth();
            ErrorResponse error = parseError(body);
            String message = (error != null && error.getMessage() != null)
                    ? error.getMessage()
                    : "Session expired or invalid credentials. Please log in again.";
            throw new ApiException(message, 401, error);
        }

        if (status >= 400) {
            ErrorResponse error = parseError(body);
            String message = null;
            if (error != null) {
                if (error.getMessage() != null && !error.getMessage().isBlank()) {
                    message = error.getMessage();
                } else if (error.getError() != null && !error.getError().isBlank()) {
                    message = error.getError();
                } else if (error.getErrors() != null && !error.getErrors().isEmpty()) {
                    message = String.join(", ", error.getErrors().values());
                }
            }
            if (message == null || message.isBlank()) {
                message = "Request failed with HTTP status " + status;
            }
            throw new ApiException(message, status, error);
        }

        if (clazz == Void.class) {
            return null;
        }

        if (body == null || body.isBlank()) {
            return null;
        }

        try {
            if (clazz != null) {
                return objectMapper.readValue(body, clazz);
            } else {
                return objectMapper.readValue(body, typeRef);
            }
        } catch (Exception e) {
            throw new ApiException("Failed to parse API response: " + e.getMessage(), status);
        }
    }

    private ErrorResponse parseError(String body) {
        if (body == null || body.isBlank()) return null;
        try {
            return objectMapper.readValue(body, ErrorResponse.class);
        } catch (Exception ignored) {
            return null;
        }
    }
}
