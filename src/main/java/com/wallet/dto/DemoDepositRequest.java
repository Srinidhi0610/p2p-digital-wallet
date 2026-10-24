package com.wallet.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DemoDepositRequest(
        @NotNull(message = "Deposit amount is required")
        @DecimalMin(value = "0.0001", message = "Deposit amount must be strictly greater than zero")
        @DecimalMax(value = "100000.00", message = "Demo deposit amount cannot exceed 100,000.00")
        BigDecimal amount,

        @Size(max = 100, message = "Idempotency key must not exceed 100 characters")
        String idempotencyKey
) {}
