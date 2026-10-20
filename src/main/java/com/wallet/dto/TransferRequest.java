package com.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TransferRequest(
        @NotNull(message = "Sender wallet ID is required")
        Long senderWalletId,

        @NotNull(message = "Recipient wallet ID is required")
        Long recipientWalletId,

        @NotNull(message = "Transfer amount is required")
        @DecimalMin(value = "0.0001", message = "Transfer amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Currency code is required")
        @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
        String currency,

        @NotBlank(message = "Idempotency key is required")
        @Size(max = 100, message = "Idempotency key must not exceed 100 characters")
        String idempotencyKey
) {}
