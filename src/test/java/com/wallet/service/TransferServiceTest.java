package com.wallet.service;

import com.wallet.dto.TransferRequest;
import com.wallet.dto.TransferResponse;
import com.wallet.entity.*;
import com.wallet.exception.*;
import com.wallet.repository.LedgerEntryRepository;
import com.wallet.repository.TransferRepository;
import com.wallet.repository.UserRepository;
import com.wallet.repository.WalletRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TransferServiceTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    private User alice;
    private User bob;
    private Wallet aliceWallet;
    private Wallet bobWallet;

    @BeforeEach
    void setUp() {
        cleanupData();

        alice = userRepository.save(new User("Alice", "alice.service@example.com", "hash1"));
        bob = userRepository.save(new User("Bob", "bob.service@example.com", "hash2"));

        aliceWallet = walletRepository.save(new Wallet(alice, "USD", new BigDecimal("1000.0000")));
        bobWallet = walletRepository.save(new Wallet(bob, "USD", new BigDecimal("200.0000")));
    }

    @AfterEach
    void tearDown() {
        cleanupData();
    }

    private void cleanupData() {
        ledgerEntryRepository.deleteAll();
        transferRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Successful transfer updates both balances and returns completed transfer response")
    void testSuccessfulTransferUpdatesBalances() {
        TransferRequest request = new TransferRequest(
                aliceWallet.getId(),
                bobWallet.getId(),
                new BigDecimal("150.0000"),
                "USD",
                "idem-success-1"
        );

        TransferResponse response = transferService.transfer(request);

        assertNotNull(response.id());
        assertEquals(aliceWallet.getId(), response.senderWalletId());
        assertEquals(bobWallet.getId(), response.recipientWalletId());
        assertEquals(new BigDecimal("150.0000"), response.amount());
        assertEquals("USD", response.currency());
        assertEquals(TransferStatus.COMPLETED, response.status());
        assertEquals("idem-success-1", response.idempotencyKey());

        // Verify updated wallet balances in database
        Wallet updatedAlice = walletRepository.findById(aliceWallet.getId()).orElseThrow();
        Wallet updatedBob = walletRepository.findById(bobWallet.getId()).orElseThrow();

        assertEquals(new BigDecimal("850.0000"), updatedAlice.getBalance());
        assertEquals(new BigDecimal("350.0000"), updatedBob.getBalance());
    }

    @Test
    @DisplayName("Successful transfer persists exactly two matching ledger entries (DEBIT and CREDIT)")
    void testSuccessfulTransferPersistsExactlyTwoLedgerEntries() {
        TransferRequest request = new TransferRequest(
                aliceWallet.getId(),
                bobWallet.getId(),
                new BigDecimal("250.0000"),
                "USD",
                "idem-ledger-entries-1"
        );

        TransferResponse response = transferService.transfer(request);

        List<LedgerEntry> entries = ledgerEntryRepository.findByTransferId(response.id());
        assertEquals(2, entries.size(), "Completed transfer must have exactly two ledger entries");

        LedgerEntry debitEntry = entries.stream()
                .filter(e -> e.getEntryType() == LedgerEntryType.DEBIT)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing DEBIT ledger entry"));

        LedgerEntry creditEntry = entries.stream()
                .filter(e -> e.getEntryType() == LedgerEntryType.CREDIT)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing CREDIT ledger entry"));

        // Debit assertions
        assertEquals(aliceWallet.getId(), debitEntry.getWallet().getId());
        assertEquals(new BigDecimal("250.0000"), debitEntry.getAmount());
        assertEquals("USD", debitEntry.getCurrency());
        assertNotNull(debitEntry.getCreatedAt());

        // Credit assertions
        assertEquals(bobWallet.getId(), creditEntry.getWallet().getId());
        assertEquals(new BigDecimal("250.0000"), creditEntry.getAmount());
        assertEquals("USD", creditEntry.getCurrency());
        assertNotNull(creditEntry.getCreatedAt());
    }

    @Test
    @DisplayName("Insufficient funds leaves balances unchanged and persists no transfer or ledger entries")
    void testInsufficientFundsRollsBackEverything() {
        TransferRequest request = new TransferRequest(
                aliceWallet.getId(),
                bobWallet.getId(),
                new BigDecimal("1500.0000"), // Exceeds available 1000.0000
                "USD",
                "idem-insufficient-1"
        );

        assertThrows(InsufficientBalanceException.class, () -> transferService.transfer(request));

        // Balances must remain unchanged
        Wallet unchangedAlice = walletRepository.findById(aliceWallet.getId()).orElseThrow();
        Wallet unchangedBob = walletRepository.findById(bobWallet.getId()).orElseThrow();
        assertEquals(new BigDecimal("1000.0000"), unchangedAlice.getBalance());
        assertEquals(new BigDecimal("200.0000"), unchangedBob.getBalance());

        // No transfer or ledger entries should be persisted
        assertEquals(0, transferRepository.count());
        assertEquals(0, ledgerEntryRepository.count());
    }

    @Test
    @DisplayName("Self-transfer to same wallet is rejected")
    void testSelfTransferIsRejected() {
        TransferRequest request = new TransferRequest(
                aliceWallet.getId(),
                aliceWallet.getId(),
                new BigDecimal("50.0000"),
                "USD",
                "idem-self-1"
        );

        assertThrows(SelfTransferException.class, () -> transferService.transfer(request));

        // State remains unchanged
        Wallet unchangedAlice = walletRepository.findById(aliceWallet.getId()).orElseThrow();
        assertEquals(new BigDecimal("1000.0000"), unchangedAlice.getBalance());
        assertEquals(0, transferRepository.count());
    }

    @Test
    @DisplayName("Currency mismatch between request and wallets is rejected")
    void testCurrencyMismatchIsRejected() {
        // Request currency differs from wallet currencies
        TransferRequest badRequestCurrency = new TransferRequest(
                aliceWallet.getId(),
                bobWallet.getId(),
                new BigDecimal("50.0000"),
                "EUR",
                "idem-curr-mismatch-1"
        );
        assertThrows(CurrencyMismatchException.class, () -> transferService.transfer(badRequestCurrency));

        // Recipient wallet has a different currency
        Wallet bobEurWallet = walletRepository.save(new Wallet(bob, "EUR", new BigDecimal("50.0000")));
        TransferRequest mismatchedRecipient = new TransferRequest(
                aliceWallet.getId(),
                bobEurWallet.getId(),
                new BigDecimal("50.0000"),
                "USD",
                "idem-curr-mismatch-2"
        );
        assertThrows(CurrencyMismatchException.class, () -> transferService.transfer(mismatchedRecipient));

        // No state changes
        assertEquals(0, transferRepository.count());
    }

    @Test
    @DisplayName("Missing sender or recipient wallet is rejected")
    void testMissingWalletIsRejected() {
        TransferRequest missingSender = new TransferRequest(
                999999L,
                bobWallet.getId(),
                new BigDecimal("50.0000"),
                "USD",
                "idem-missing-sender"
        );
        assertThrows(WalletNotFoundException.class, () -> transferService.transfer(missingSender));

        TransferRequest missingRecipient = new TransferRequest(
                aliceWallet.getId(),
                999999L,
                new BigDecimal("50.0000"),
                "USD",
                "idem-missing-recipient"
        );
        assertThrows(WalletNotFoundException.class, () -> transferService.transfer(missingRecipient));
    }

    @Test
    @DisplayName("Repeated identical idempotency request returns original result without duplicate deduction")
    void testIdempotencyReturnsExistingWithoutDuplicateDeduction() {
        TransferRequest request = new TransferRequest(
                aliceWallet.getId(),
                bobWallet.getId(),
                new BigDecimal("100.0000"),
                "USD",
                "idem-repeat-1"
        );

        // First execution
        TransferResponse firstResponse = transferService.transfer(request);

        // Second execution with same payload and key
        TransferResponse secondResponse = transferService.transfer(request);

        // Responses must match
        assertEquals(firstResponse.id(), secondResponse.id());
        assertEquals(firstResponse.createdAt(), secondResponse.createdAt());

        // Balances must be deducted only ONCE
        Wallet updatedAlice = walletRepository.findById(aliceWallet.getId()).orElseThrow();
        Wallet updatedBob = walletRepository.findById(bobWallet.getId()).orElseThrow();
        assertEquals(new BigDecimal("900.0000"), updatedAlice.getBalance());
        assertEquals(new BigDecimal("300.0000"), updatedBob.getBalance());

        // Exactly 1 transfer and exactly 2 ledger entries in database
        assertEquals(1, transferRepository.count());
        assertEquals(2, ledgerEntryRepository.count());
    }

    @Test
    @DisplayName("Reusing same idempotency key with a different payload is rejected with IdempotencyConflictException")
    void testReusingIdempotencyKeyWithDifferentPayloadIsRejected() {
        TransferRequest originalRequest = new TransferRequest(
                aliceWallet.getId(),
                bobWallet.getId(),
                new BigDecimal("100.0000"),
                "USD",
                "idem-conflict-1"
        );
        transferService.transfer(originalRequest);

        // Same sender and key, but DIFFERENT amount
        TransferRequest differentAmount = new TransferRequest(
                aliceWallet.getId(),
                bobWallet.getId(),
                new BigDecimal("200.0000"),
                "USD",
                "idem-conflict-1"
        );
        assertThrows(IdempotencyConflictException.class, () -> transferService.transfer(differentAmount));

        // Same sender and key, but DIFFERENT recipient
        Wallet aliceEur = walletRepository.save(new Wallet(alice, "EUR", new BigDecimal("500.0000")));
        TransferRequest differentRecipient = new TransferRequest(
                aliceWallet.getId(),
                aliceEur.getId(),
                new BigDecimal("100.0000"),
                "USD",
                "idem-conflict-1"
        );
        assertThrows(IdempotencyConflictException.class, () -> transferService.transfer(differentRecipient));
    }

    @Test
    @DisplayName("Invalid monetary amounts are rejected without silent rounding")
    void testInvalidMonetaryAmountsAreRejected() {
        // Null amount
        assertThrows(InvalidTransferAmountException.class, () -> transferService.transfer(
                new TransferRequest(aliceWallet.getId(), bobWallet.getId(), null, "USD", "k1")));

        // Zero amount
        assertThrows(InvalidTransferAmountException.class, () -> transferService.transfer(
                new TransferRequest(aliceWallet.getId(), bobWallet.getId(), BigDecimal.ZERO, "USD", "k2")));

        // Negative amount
        assertThrows(InvalidTransferAmountException.class, () -> transferService.transfer(
                new TransferRequest(aliceWallet.getId(), bobWallet.getId(), new BigDecimal("-10.0000"), "USD", "k3")));

        // Exceeding 4 decimal places of precision (rejected, NOT silently rounded)
        assertThrows(InvalidTransferAmountException.class, () -> transferService.transfer(
                new TransferRequest(aliceWallet.getId(), bobWallet.getId(), new BigDecimal("10.12345"), "USD", "k4")));
    }

    @Test
    @DisplayName("Concurrent bidirectional transfers between two wallets execute without deadlock")
    void testConcurrentBidirectionalTransfersWithoutDeadlock() throws Exception {
        int threads = 10;
        int transfersPerThread = 5;
        BigDecimal transferAmount = new BigDecimal("2.0000");

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            final int threadIdx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < transfersPerThread; j++) {
                        // Alternate directions based on thread index
                        Long senderId = (threadIdx % 2 == 0) ? aliceWallet.getId() : bobWallet.getId();
                        Long recipientId = (threadIdx % 2 == 0) ? bobWallet.getId() : aliceWallet.getId();
                        String key = String.format("conc-tx-t%d-j%d", threadIdx, j);

                        transferService.transfer(new TransferRequest(senderId, recipientId, transferAmount, "USD", key));
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release all threads simultaneously
        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All concurrent transfers should finish within 30 seconds without deadlock");
        assertEquals(threads * transfersPerThread, successCount.get(), "All concurrent transfers should succeed");

        // Verify total money in system is conserved
        Wallet finalAlice = walletRepository.findById(aliceWallet.getId()).orElseThrow();
        Wallet finalBob = walletRepository.findById(bobWallet.getId()).orElseThrow();

        BigDecimal initialTotal = new BigDecimal("1200.0000");
        BigDecimal finalTotal = finalAlice.getBalance().add(finalBob.getBalance());
        assertEquals(initialTotal, finalTotal, "Total money across wallets must be conserved");
    }
}
