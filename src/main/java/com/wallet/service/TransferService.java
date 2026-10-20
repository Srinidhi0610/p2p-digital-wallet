package com.wallet.service;

import com.wallet.dto.TransferRequest;
import com.wallet.dto.TransferResponse;
import com.wallet.entity.*;
import com.wallet.exception.*;
import com.wallet.repository.LedgerEntryRepository;
import com.wallet.repository.TransferRepository;
import com.wallet.repository.WalletRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

@Service
public class TransferService {

    private final WalletRepository walletRepository;
    private final TransferRepository transferRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public TransferService(WalletRepository walletRepository,
                           TransferRepository transferRepository,
                           LedgerEntryRepository ledgerEntryRepository) {
        this.walletRepository = walletRepository;
        this.transferRepository = transferRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    /**
     * Executes an atomic, double-entry peer-to-peer wallet transfer within a single database transaction,
     * enforcing that the authenticated user owns the sender wallet under lock.
     *
     * @param authenticatedUserId ID of the authenticated user initiating the transfer (or null for internal calls)
     * @param request the validated transfer request details
     * @return the completed transfer response
     */
    @Transactional
    public TransferResponse transfer(Long authenticatedUserId, TransferRequest request) {
        validateRequest(request);

        // 1. Initial fast idempotency check
        Optional<Transfer> preLockExisting = transferRepository
                .findBySenderWalletIdAndIdempotencyKey(request.senderWalletId(), request.idempotencyKey());
        if (preLockExisting.isPresent()) {
            Transfer existing = preLockExisting.get();
            if (authenticatedUserId != null && !Objects.equals(existing.getSenderWallet().getOwner().getId(), authenticatedUserId)) {
                throw new UnauthorizedAccessException("You do not have permission to access this transfer");
            }
            return handleExistingTransfer(existing, request);
        }

        // 2. Safe, deterministic pessimistic locking in strictly ascending wallet ID order
        Long senderId = request.senderWalletId();
        Long recipientId = request.recipientWalletId();
        Long firstId = Math.min(senderId, recipientId);
        Long secondId = Math.max(senderId, recipientId);

        Wallet firstWallet = walletRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + firstId));
        Wallet secondWallet = walletRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found with id: " + secondId));

        Wallet senderWallet = firstWallet.getId().equals(senderId) ? firstWallet : secondWallet;
        Wallet recipientWallet = firstWallet.getId().equals(recipientId) ? firstWallet : secondWallet;

        // Verify sender wallet ownership under lock within the transaction
        if (authenticatedUserId != null && !Objects.equals(senderWallet.getOwner().getId(), authenticatedUserId)) {
            throw new UnauthorizedAccessException("You do not have permission to transfer funds from this wallet");
        }

        // 3. Post-locking idempotency re-check (guards against race conditions while waiting for wallet locks)
        Optional<Transfer> postLockExisting = transferRepository
                .findBySenderWalletIdAndIdempotencyKey(request.senderWalletId(), request.idempotencyKey());
        if (postLockExisting.isPresent()) {
            Transfer existing = postLockExisting.get();
            if (authenticatedUserId != null && !Objects.equals(existing.getSenderWallet().getOwner().getId(), authenticatedUserId)) {
                throw new UnauthorizedAccessException("You do not have permission to access this transfer");
            }
            return handleExistingTransfer(existing, request);
        }

        // 4. Currency consistency validation
        validateCurrencies(request.currency(), senderWallet, recipientWallet);

        // 5. Normalized scale-4 amount and balance validation
        BigDecimal transferAmount = request.amount().setScale(4, RoundingMode.UNNECESSARY);
        if (senderWallet.getBalance().compareTo(transferAmount) < 0) {
            throw new InsufficientBalanceException(String.format(
                    "Insufficient balance in sender wallet id %d: available %s %s, requested %s",
                    senderWallet.getId(), senderWallet.getBalance(), senderWallet.getCurrency(), transferAmount
            ));
        }

        // 6. Update balances atomically under lock
        senderWallet.setBalance(senderWallet.getBalance().subtract(transferAmount));
        recipientWallet.setBalance(recipientWallet.getBalance().add(transferAmount));

        walletRepository.save(senderWallet);
        walletRepository.save(recipientWallet);

        // 7. Persist completed Transfer entity
        Transfer transfer = new Transfer(
                senderWallet,
                recipientWallet,
                transferAmount,
                senderWallet.getCurrency(),
                TransferStatus.COMPLETED,
                request.idempotencyKey()
        );
        Transfer savedTransfer = transferRepository.save(transfer);

        // 8. Persist matching DEBIT and CREDIT ledger entries
        LedgerEntry debitEntry = new LedgerEntry(
                savedTransfer,
                senderWallet,
                LedgerEntryType.DEBIT,
                transferAmount,
                senderWallet.getCurrency()
        );
        LedgerEntry creditEntry = new LedgerEntry(
                savedTransfer,
                recipientWallet,
                LedgerEntryType.CREDIT,
                transferAmount,
                recipientWallet.getCurrency()
        );

        ledgerEntryRepository.save(debitEntry);
        ledgerEntryRepository.save(creditEntry);

        return TransferResponse.fromEntity(savedTransfer);
    }

    /**
     * Overload for internal calls or backward compatibility where caller user ID is not supplied.
     */
    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        return transfer(null, request);
    }

    /**
     * Retrieves a transfer by its ID, ensuring the requesting user owns either the sender or recipient wallet.
     *
     * @param authenticatedUserId ID of the requesting user
     * @param transferId ID of the transfer to retrieve
     * @return the transfer details DTO
     */
    @Transactional(readOnly = true)
    public TransferResponse getTransferById(Long authenticatedUserId, Long transferId) {
        if (authenticatedUserId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (transferId == null) {
            throw new IllegalArgumentException("Transfer ID cannot be null");
        }

        Transfer transfer = transferRepository.findById(transferId)
                .orElseThrow(() -> new TransferNotFoundException("Transfer not found with id: " + transferId));

        boolean isSenderOwner = Objects.equals(transfer.getSenderWallet().getOwner().getId(), authenticatedUserId);
        boolean isRecipientOwner = Objects.equals(transfer.getRecipientWallet().getOwner().getId(), authenticatedUserId);

        if (!isSenderOwner && !isRecipientOwner) {
            throw new UnauthorizedAccessException("You do not have permission to access this transfer");
        }

        return TransferResponse.fromEntity(transfer);
    }

    /**
     * Lists all transfers involving the authenticated user's wallets with pagination and stable ordering.
     *
     * @param authenticatedUserId ID of the authenticated user
     * @param pageable pagination parameters
     * @return paginated transfers involving the user's wallets
     */
    @Transactional(readOnly = true)
    public Page<TransferResponse> getUserTransfers(Long authenticatedUserId, Pageable pageable) {
        if (authenticatedUserId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        Pageable effectivePageable = (pageable != null && pageable.getSort().isSorted())
                ? pageable
                : PageRequest.of(pageable != null ? pageable.getPageNumber() : 0,
                                 pageable != null ? pageable.getPageSize() : 20,
                                 Sort.by(Sort.Direction.DESC, "createdAt", "id"));

        return transferRepository.findByUserId(authenticatedUserId, effectivePageable)
                .map(TransferResponse::fromEntity);
    }

    private void validateRequest(TransferRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Transfer request cannot be null");
        }
        if (request.senderWalletId() == null) {
            throw new IllegalArgumentException("Sender wallet ID is required");
        }
        if (request.recipientWalletId() == null) {
            throw new IllegalArgumentException("Recipient wallet ID is required");
        }
        if (request.senderWalletId().equals(request.recipientWalletId())) {
            throw new SelfTransferException(String.format(
                    "Cannot transfer funds to the same wallet (id: %d)", request.senderWalletId()
            ));
        }
        if (request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
            throw new IllegalArgumentException("Idempotency key is required");
        }
        if (request.idempotencyKey().length() > 100) {
            throw new IllegalArgumentException("Idempotency key cannot exceed 100 characters");
        }
        if (request.currency() == null || request.currency().trim().length() != 3) {
            throw new CurrencyMismatchException("Valid 3-character ISO currency code is required");
        }
        if (request.amount() == null) {
            throw new InvalidTransferAmountException("Transfer amount is required");
        }
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferAmountException(String.format(
                    "Transfer amount must be strictly greater than zero (provided: %s)", request.amount()
            ));
        }
        // Enforce maximum 4 decimal places without silent rounding
        if (request.amount().stripTrailingZeros().scale() > 4) {
            throw new InvalidTransferAmountException(String.format(
                    "Transfer amount cannot have more than 4 decimal places of precision (provided: %s)", request.amount()
            ));
        }
    }

    private void validateCurrencies(String requestedCurrency, Wallet senderWallet, Wallet recipientWallet) {
        if (!senderWallet.getCurrency().equalsIgnoreCase(requestedCurrency)) {
            throw new CurrencyMismatchException(String.format(
                    "Requested currency '%s' does not match sender wallet currency '%s'",
                    requestedCurrency, senderWallet.getCurrency()
            ));
        }
        if (!recipientWallet.getCurrency().equalsIgnoreCase(requestedCurrency)) {
            throw new CurrencyMismatchException(String.format(
                    "Requested currency '%s' does not match recipient wallet currency '%s'",
                    requestedCurrency, recipientWallet.getCurrency()
            ));
        }
    }

    private TransferResponse handleExistingTransfer(Transfer existing, TransferRequest request) {
        boolean sameRecipient = Objects.equals(existing.getRecipientWallet().getId(), request.recipientWalletId());
        boolean sameAmount = existing.getAmount().compareTo(request.amount()) == 0;
        boolean sameCurrency = existing.getCurrency().equalsIgnoreCase(request.currency());

        if (sameRecipient && sameAmount && sameCurrency) {
            return TransferResponse.fromEntity(existing);
        } else {
            throw new IdempotencyConflictException(String.format(
                    "Idempotency key '%s' was already used for a different transfer payload (recipient: %d vs %d, amount: %s vs %s, currency: %s vs %s)",
                    request.idempotencyKey(),
                    existing.getRecipientWallet().getId(), request.recipientWalletId(),
                    existing.getAmount(), request.amount(),
                    existing.getCurrency(), request.currency()
            ));
        }
    }
}
