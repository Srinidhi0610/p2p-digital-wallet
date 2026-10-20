package com.wallet.exception;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({WalletNotFoundException.class, TransferNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(RuntimeException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({UnauthorizedAccessException.class, AccessDeniedException.class})
    public ResponseEntity<Map<String, Object>> handleUnauthorizedAccess(Exception ex) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage() != null ? ex.getMessage() : "Access denied");
    }

    @ExceptionHandler({
            InsufficientBalanceException.class,
            CurrencyMismatchException.class,
            SelfTransferException.class,
            InvalidTransferAmountException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(RuntimeException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Validation failed");
        return buildResponse(HttpStatus.BAD_REQUEST, errorMessage);
    }

    @ExceptionHandler({DuplicateEmailException.class, DuplicateWalletException.class, IdempotencyConflictException.class})
    public ResponseEntity<Map<String, Object>> handleConflict(RuntimeException ex) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return buildResponse(HttpStatus.CONFLICT, "Resource conflict or duplicate entry");
    }

    @ExceptionHandler({InvalidCredentialsException.class, BadCredentialsException.class})
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(RuntimeException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
