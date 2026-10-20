package com.wallet.dto;

import com.wallet.entity.Transfer;
import com.wallet.entity.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(
        Long id,
        Long senderWalletId,
        Long recipientWalletId,
        BigDecimal amount,
        String currency,
        TransferStatus status,
        String idempotencyKey,
        Instant createdAt
) {
    public static TransferResponse fromEntity(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getSenderWallet().getId(),
                transfer.getRecipientWallet().getId(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getStatus(),
                transfer.getIdempotencyKey(),
                transfer.getCreatedAt()
        );
    }
}
