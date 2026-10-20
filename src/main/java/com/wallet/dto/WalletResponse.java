package com.wallet.dto;

import com.wallet.entity.Wallet;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletResponse(
        Long id,
        Long userId,
        String currency,
        BigDecimal balance,
        Instant createdAt
) {
    public static WalletResponse fromEntity(Wallet wallet) {
        if (wallet == null) {
            return null;
        }
        return new WalletResponse(
                wallet.getId(),
                wallet.getOwner().getId(),
                wallet.getCurrency(),
                wallet.getBalance(),
                wallet.getCreatedAt()
        );
    }
}
