package com.wallet.desktop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.wallet.desktop.model.PageResponse;
import com.wallet.desktop.model.Transfer;
import com.wallet.desktop.model.TransferRequest;
import com.wallet.desktop.util.UuidGenerator;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

public class TransferService {
    private static final TransferService instance = new TransferService();
    private final ApiClient apiClient = ApiClient.getInstance();

    private TransferService() {}

    public static TransferService getInstance() {
        return instance;
    }

    public CompletableFuture<Transfer> createTransfer(Long senderWalletId, Long recipientWalletId,
                                                      BigDecimal amount, String currency, String idempotencyKey) {
        String key = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey
                : UuidGenerator.generate();

        TransferRequest request = new TransferRequest(
                senderWalletId,
                recipientWalletId,
                amount,
                currency.trim().toUpperCase(),
                key
        );

        return apiClient.postAsync("/transfers", request, Transfer.class);
    }

    public CompletableFuture<PageResponse<Transfer>> getTransfers(int page, int size) {
        String endpoint = String.format("/transfers?page=%d&size=%d", page, size);
        return apiClient.getAsync(endpoint, new TypeReference<PageResponse<Transfer>>() {});
    }

    public CompletableFuture<Transfer> getTransferById(Long transferId) {
        return apiClient.getAsync("/transfers/" + transferId, Transfer.class);
    }
}
