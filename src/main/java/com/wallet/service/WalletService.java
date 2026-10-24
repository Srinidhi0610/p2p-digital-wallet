package com.wallet.service;

import com.wallet.dto.CreateWalletRequest;
import com.wallet.dto.DemoDepositRequest;
import com.wallet.dto.WalletResponse;
import com.wallet.entity.*;
import com.wallet.exception.DuplicateWalletException;
import com.wallet.exception.IdempotencyConflictException;
import com.wallet.exception.InvalidTransferAmountException;
import com.wallet.exception.UnauthorizedAccessException;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.repository.LedgerEntryRepository;
import com.wallet.repository.TransferRepository;
import com.wallet.repository.UserRepository;
import com.wallet.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class WalletService {

    public static final String DEMO_FAUCET_EMAIL = "demo.faucet@system.local";
    public static final String DEMO_FAUCET_NAME = "Demo Treasury Faucet";
    public static final BigDecimal INITIAL_TREASURY_BALANCE = new BigDecimal("1000000000.0000");

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final TransferRepository transferRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public WalletService(WalletRepository walletRepository, UserRepository userRepository) {
        this(walletRepository, userRepository, null, null);
    }

    @Autowired
    public WalletService(WalletRepository walletRepository,
                         UserRepository userRepository,
                         TransferRepository transferRepository,
                         LedgerEntryRepository ledgerEntryRepository) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.transferRepository = transferRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
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

    /**
     * Development-only demo deposit function. Disburses demo liquidity from the system treasury faucet
     * into the authenticated user's wallet via a fully compliant double-entry transfer and ledger entries.
     *
     * @param authenticatedUserId the authenticated user invoking the deposit
     * @param walletId the destination wallet ID
     * @param request the deposit request details (amount and optional idempotency key)
     * @return the updated wallet state
     */
    @Transactional
    public WalletResponse depositDemoFunds(Long authenticatedUserId, Long walletId, DemoDepositRequest request) {
        if (transferRepository == null || ledgerEntryRepository == null) {
            throw new IllegalStateException("Transfer and LedgerEntry repositories must be configured for deposit operations");
        }
        if (authenticatedUserId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (walletId == null) {
            throw new IllegalArgumentException("Wallet ID cannot be null");
        }
        if (request == null || request.amount() == null) {
            throw new InvalidTransferAmountException("Deposit amount is required");
        }
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferAmountException(String.format(
                    "Deposit amount must be strictly greater than zero (provided: %s)", request.amount()
            ));
        }
        if (request.amount().stripTrailingZeros().scale() > 4) {
            throw new InvalidTransferAmountException(String.format(
                    "Deposit amount cannot have more than 4 decimal places of precision (provided: %s)", request.amount()
            ));
        }
        if (request.amount().compareTo(new BigDecimal("100000.00")) > 0) {
            throw new InvalidTransferAmountException("Demo deposit amount cannot exceed 100,000.00");
        }

        BigDecimal depositAmount = request.amount().setScale(4, RoundingMode.UNNECESSARY);

        // Fetch user wallet initially to check existence, ownership, and currency
        Wallet initialUserWallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + walletId));

        if (!Objects.equals(initialUserWallet.getOwner().getId(), authenticatedUserId)) {
            throw new UnauthorizedAccessException("You do not have permission to deposit into this wallet");
        }

        String currency = initialUserWallet.getCurrency();

        // Retrieve or initialize the Demo Treasury Faucet system user
        User faucetUser = userRepository.findByEmail(DEMO_FAUCET_EMAIL)
                .orElseGet(() -> {
                    User newUser = new User(
                            DEMO_FAUCET_NAME,
                            DEMO_FAUCET_EMAIL,
                            "$2a$10$e8w.YwGq4vLw1c3Z.Dymkeo2R1nQ7Vp4qGk0GfV/x9vW5hYmX9fO2"
                    );
                    try {
                        return userRepository.save(newUser);
                    } catch (DataIntegrityViolationException ex) {
                        return userRepository.findByEmail(DEMO_FAUCET_EMAIL)
                                .orElseThrow(() -> new IllegalStateException("Failed to retrieve treasury faucet user", ex));
                    }
                });

        // Retrieve or initialize the Treasury Faucet wallet for the requested currency
        Wallet initialFaucetWallet = walletRepository.findByOwnerIdAndCurrency(faucetUser.getId(), currency)
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet(faucetUser, currency, INITIAL_TREASURY_BALANCE);
                    try {
                        return walletRepository.save(newWallet);
                    } catch (DataIntegrityViolationException ex) {
                        return walletRepository.findByOwnerIdAndCurrency(faucetUser.getId(), currency)
                                .orElseThrow(() -> new IllegalStateException("Failed to retrieve treasury faucet wallet", ex));
                    }
                });

        if (initialFaucetWallet.getId().equals(initialUserWallet.getId())) {
            throw new IllegalStateException("Faucet and recipient wallet cannot be identical");
        }

        // Idempotency key handling
        String idempotencyKey;
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            idempotencyKey = request.idempotencyKey().trim();
            if (idempotencyKey.length() > 100) {
                throw new IllegalArgumentException("Idempotency key cannot exceed 100 characters");
            }

            Optional<Transfer> existingTransfer = transferRepository.findBySenderWalletIdAndIdempotencyKey(
                    initialFaucetWallet.getId(), idempotencyKey
            );
            if (existingTransfer.isPresent()) {
                Transfer existing = existingTransfer.get();
                if (Objects.equals(existing.getRecipientWallet().getId(), initialUserWallet.getId())
                        && existing.getAmount().compareTo(depositAmount) == 0
                        && existing.getCurrency().equalsIgnoreCase(currency)) {
                    return WalletResponse.fromEntity(initialUserWallet);
                } else {
                    throw new IdempotencyConflictException("Idempotency key was already used for a different deposit payload");
                }
            }
        } else {
            idempotencyKey = "demo-deposit-" + UUID.randomUUID();
        }

        // Deterministic pessimistic locking in ascending wallet ID order
        Long firstId = Math.min(initialFaucetWallet.getId(), initialUserWallet.getId());
        Long secondId = Math.max(initialFaucetWallet.getId(), initialUserWallet.getId());

        Wallet firstWallet = walletRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + firstId));
        Wallet secondWallet = walletRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + secondId));

        Wallet lockedFaucetWallet = firstWallet.getId().equals(initialFaucetWallet.getId()) ? firstWallet : secondWallet;
        Wallet lockedUserWallet = firstWallet.getId().equals(initialUserWallet.getId()) ? firstWallet : secondWallet;

        // Re-verify ownership under lock
        if (!Objects.equals(lockedUserWallet.getOwner().getId(), authenticatedUserId)) {
            throw new UnauthorizedAccessException("You do not have permission to deposit into this wallet");
        }

        // Post-locking idempotency check
        Optional<Transfer> postLockExisting = transferRepository.findBySenderWalletIdAndIdempotencyKey(
                lockedFaucetWallet.getId(), idempotencyKey
        );
        if (postLockExisting.isPresent()) {
            Transfer existing = postLockExisting.get();
            if (Objects.equals(existing.getRecipientWallet().getId(), lockedUserWallet.getId())
                    && existing.getAmount().compareTo(depositAmount) == 0
                    && existing.getCurrency().equalsIgnoreCase(currency)) {
                return WalletResponse.fromEntity(lockedUserWallet);
            } else {
                throw new IdempotencyConflictException("Idempotency key was already used for a different deposit payload");
            }
        }

        // Ensure treasury faucet has sufficient funds to disburse
        if (lockedFaucetWallet.getBalance().compareTo(depositAmount) < 0) {
            lockedFaucetWallet.setBalance(lockedFaucetWallet.getBalance().add(INITIAL_TREASURY_BALANCE));
        }

        // Execute double-entry transfer
        lockedFaucetWallet.setBalance(lockedFaucetWallet.getBalance().subtract(depositAmount));
        lockedUserWallet.setBalance(lockedUserWallet.getBalance().add(depositAmount));

        walletRepository.save(lockedFaucetWallet);
        Wallet savedUserWallet = walletRepository.save(lockedUserWallet);

        Transfer transfer = new Transfer(
                lockedFaucetWallet,
                lockedUserWallet,
                depositAmount,
                currency,
                TransferStatus.COMPLETED,
                idempotencyKey
        );
        Transfer savedTransfer = transferRepository.save(transfer);

        LedgerEntry debitEntry = new LedgerEntry(
                savedTransfer,
                lockedFaucetWallet,
                LedgerEntryType.DEBIT,
                depositAmount,
                currency
        );
        LedgerEntry creditEntry = new LedgerEntry(
                savedTransfer,
                lockedUserWallet,
                LedgerEntryType.CREDIT,
                depositAmount,
                currency
        );

        ledgerEntryRepository.save(debitEntry);
        ledgerEntryRepository.save(creditEntry);

        return WalletResponse.fromEntity(savedUserWallet);
    }
}
