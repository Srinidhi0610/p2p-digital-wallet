package com.wallet.desktop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.wallet.desktop.model.CreateWalletRequest;
import com.wallet.desktop.model.DemoDepositRequest;
import com.wallet.desktop.model.Wallet;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class WalletService {
    private static final WalletService instance = new WalletService();
    private final ApiClient apiClient = ApiClient.getInstance();

    private WalletService() {}

    public static WalletService getInstance() {
        return instance;
    }

    public CompletableFuture<List<Wallet>> getWallets() {
        return apiClient.getAsync("/wallets", new TypeReference<List<Wallet>>() {});
    }

    public CompletableFuture<Wallet> getWalletById(Long walletId) {
        return apiClient.getAsync("/wallets/" + walletId, Wallet.class);
    }

    public CompletableFuture<Wallet> createWallet(String currency) {
        CreateWalletRequest request = new CreateWalletRequest(currency.trim().toUpperCase());
        return apiClient.postAsync("/wallets", request, Wallet.class);
    }

    public CompletableFuture<Wallet> depositDemoFunds(Long walletId, BigDecimal amount, String idempotencyKey) {
        DemoDepositRequest request = new DemoDepositRequest(amount, idempotencyKey);
        return apiClient.postAsync("/wallets/" + walletId + "/deposit", request, Wallet.class);
    }
}
