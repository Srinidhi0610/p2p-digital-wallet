package com.wallet.service;

import com.wallet.dto.CreateWalletRequest;
import com.wallet.dto.WalletResponse;
import com.wallet.entity.User;
import com.wallet.entity.Wallet;
import com.wallet.exception.DuplicateWalletException;
import com.wallet.exception.UnauthorizedAccessException;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.repository.UserRepository;
import com.wallet.repository.WalletRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    public WalletService(WalletRepository walletRepository, UserRepository userRepository) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public WalletResponse createWallet(Long userId, CreateWalletRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (request == null || request.currency() == null) {
            throw new IllegalArgumentException("Currency is required");
        }

        String normalizedCurrency = request.currency().trim().toUpperCase(Locale.ROOT);

        if (walletRepository.findByOwnerIdAndCurrency(userId, normalizedCurrency).isPresent()) {
            throw new DuplicateWalletException(String.format(
                    "A wallet with currency '%s' already exists for this user", normalizedCurrency
            ));
        }

        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Enforce strictly zero starting balance
        Wallet wallet = new Wallet(owner, normalizedCurrency, BigDecimal.ZERO);

        try {
            wallet = walletRepository.save(wallet);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateWalletException(String.format(
                    "A wallet with currency '%s' already exists for this user", normalizedCurrency
            ));
        }

        return WalletResponse.fromEntity(wallet);
    }

    @Transactional(readOnly = true)
    public List<WalletResponse> getUserWallets(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return walletRepository.findByOwnerId(userId).stream()
                .map(WalletResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public WalletResponse getWalletById(Long userId, Long walletId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (walletId == null) {
            throw new IllegalArgumentException("Wallet ID cannot be null");
        }

        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + walletId));

        if (!Objects.equals(wallet.getOwner().getId(), userId)) {
            throw new UnauthorizedAccessException("You do not have permission to access this wallet");
        }

        return WalletResponse.fromEntity(wallet);
    }
}
