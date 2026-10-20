package com.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateWalletRequest(
        @NotBlank(message = "Currency code is required")
        @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency code must consist of 3 letters")
        String currency
) {}
