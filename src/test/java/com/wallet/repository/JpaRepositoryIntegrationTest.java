package com.wallet.repository;

import com.wallet.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class JpaRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Test
    @DisplayName("UserRepository should save and find user by email")
    void testUserRepositoryOperations() {
        User user = new User("John Doe", "john@example.com", "$2a$10$hashedPassword");
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("john@example.com");
        assertTrue(found.isPresent());
        assertEquals("John Doe", found.get().getName());
        assertTrue(userRepository.existsByEmail("john@example.com"));
        assertFalse(userRepository.existsByEmail("nonexistent@example.com"));
    }

    @Test
    @DisplayName("WalletRepository should query wallets by owner ID and currency")
    void testWalletRepositoryOperations() {
        User user = entityManager.persist(new User("Jane Doe", "jane@example.com", "hash"));
        Wallet usdWallet = entityManager.persist(new Wallet(user, "USD", new BigDecimal("1000.0000")));
        Wallet eurWallet = entityManager.persist(new Wallet(user, "EUR", new BigDecimal("500.0000")));

        List<Wallet> wallets = walletRepository.findByOwnerId(user.getId());
        assertEquals(2, wallets.size());

        Optional<Wallet> foundUsd = walletRepository.findByOwnerIdAndCurrency(user.getId(), "USD");
        assertTrue(foundUsd.isPresent());
        assertEquals(new BigDecimal("1000.0000"), foundUsd.get().getBalance());

        Optional<Wallet> foundByIdAndOwner = walletRepository.findByIdAndOwnerId(usdWallet.getId(), user.getId());
        assertTrue(foundByIdAndOwner.isPresent());
        assertEquals("USD", foundByIdAndOwner.get().getCurrency());
    }

    @Test
    @DisplayName("TransferRepository should find transfer by sender wallet ID and idempotency key")
    void testFindBySenderWalletIdAndIdempotencyKey() {
        User sender = entityManager.persist(new User("SenderUser", "sender.idem@example.com", "hash"));
        User recipient = entityManager.persist(new User("RecipientUser", "recipient.idem@example.com", "hash"));

        Wallet senderWallet = entityManager.persist(new Wallet(sender, "USD", new BigDecimal("1000.0000")));
        Wallet recipientWallet = entityManager.persist(new Wallet(recipient, "USD", new BigDecimal("200.0000")));

        Transfer transfer = new Transfer(senderWallet, recipientWallet, new BigDecimal("120.0000"), "USD", TransferStatus.COMPLETED, "key-unique-789");
        transferRepository.save(transfer);

        // Found with matching sender wallet ID and key
        Optional<Transfer> found = transferRepository.findBySenderWalletIdAndIdempotencyKey(senderWallet.getId(), "key-unique-789");
        assertTrue(found.isPresent());
        assertEquals(transfer.getId(), found.get().getId());
        assertEquals(new BigDecimal("120.0000"), found.get().getAmount());
        assertEquals("USD", found.get().getCurrency());

        // Not found with wrong key
        Optional<Transfer> notFoundKey = transferRepository.findBySenderWalletIdAndIdempotencyKey(senderWallet.getId(), "wrong-key");
        assertFalse(notFoundKey.isPresent());

        // Not found with different wallet ID
        Optional<Transfer> notFoundWallet = transferRepository.findBySenderWalletIdAndIdempotencyKey(recipientWallet.getId(), "key-unique-789");
        assertFalse(notFoundWallet.isPresent());
    }

    @Test
    @DisplayName("WalletRepository should retrieve and lock wallet by ID using PESSIMISTIC_WRITE within transaction")
    void testFindByIdForUpdateAcquiresLockWithinTransaction() {
        User user = entityManager.persist(new User("LockUser", "lock.user@example.com", "hash"));
        Wallet wallet = entityManager.persist(new Wallet(user, "USD", new BigDecimal("500.0000")));
        entityManager.flush();
        entityManager.clear();

        // Retrieve with pessimistic write lock (SELECT ... FOR UPDATE)
        Optional<Wallet> lockedWalletOpt = walletRepository.findByIdForUpdate(wallet.getId());
        assertTrue(lockedWalletOpt.isPresent());

        Wallet lockedWallet = lockedWalletOpt.get();
        assertEquals(new BigDecimal("500.0000"), lockedWallet.getBalance());
        assertEquals("USD", lockedWallet.getCurrency());

        // Verify entity can be updated under the acquired lock
        lockedWallet.setBalance(new BigDecimal("450.0000"));
        walletRepository.saveAndFlush(lockedWallet);

        entityManager.clear();
        Wallet updated = entityManager.find(Wallet.class, wallet.getId());
        assertEquals(new BigDecimal("450.0000"), updated.getBalance());

        // Non-existent ID returns empty
        Optional<Wallet> nonExistent = walletRepository.findByIdForUpdate(999999L);
        assertFalse(nonExistent.isPresent());
    }

    @Test
    @DisplayName("LedgerEntryRepository should retrieve all entries for a specific transfer")
    void testFindByTransferId() {
        User sender = entityManager.persist(new User("AuditSender", "audit.s@example.com", "hash"));
        User recipient = entityManager.persist(new User("AuditRecipient", "audit.r@example.com", "hash"));

        Wallet senderWallet = entityManager.persist(new Wallet(sender, "USD", new BigDecimal("300.0000")));
        Wallet recipientWallet = entityManager.persist(new Wallet(recipient, "USD", new BigDecimal("100.0000")));

        Transfer transfer = entityManager.persist(
                new Transfer(senderWallet, recipientWallet, new BigDecimal("80.0000"), "USD", TransferStatus.COMPLETED, "tx-audit-88")
        );

        LedgerEntry debit = new LedgerEntry(transfer, senderWallet, LedgerEntryType.DEBIT, new BigDecimal("80.0000"), "USD");
        LedgerEntry credit = new LedgerEntry(transfer, recipientWallet, LedgerEntryType.CREDIT, new BigDecimal("80.0000"), "USD");
        ledgerEntryRepository.save(debit);
        ledgerEntryRepository.save(credit);

        List<LedgerEntry> entries = ledgerEntryRepository.findByTransferId(transfer.getId());
        assertEquals(2, entries.size());

        boolean hasDebit = entries.stream().anyMatch(e -> e.getEntryType() == LedgerEntryType.DEBIT
                && e.getWallet().getId().equals(senderWallet.getId())
                && e.getAmount().compareTo(new BigDecimal("80.0000")) == 0
                && "USD".equals(e.getCurrency()));
        boolean hasCredit = entries.stream().anyMatch(e -> e.getEntryType() == LedgerEntryType.CREDIT
                && e.getWallet().getId().equals(recipientWallet.getId())
                && e.getAmount().compareTo(new BigDecimal("80.0000")) == 0
                && "USD".equals(e.getCurrency()));

        assertTrue(hasDebit, "Expected debit ledger entry for sender wallet");
        assertTrue(hasCredit, "Expected credit ledger entry for recipient wallet");
    }

    @Test
    @DisplayName("Transfer idempotency must be strictly scoped to the sender wallet")
    void testTransferIdempotencyScopedToSenderWallet() {
        User user1 = entityManager.persist(new User("User1", "u1@example.com", "hash"));
        User user2 = entityManager.persist(new User("User2", "u2@example.com", "hash"));

        Wallet wallet1 = entityManager.persist(new Wallet(user1, "USD", new BigDecimal("500.0000")));
        Wallet wallet2 = entityManager.persist(new Wallet(user2, "USD", new BigDecimal("500.0000")));

        // First transfer from wallet1 with key "req-abc"
        Transfer t1 = new Transfer(wallet1, wallet2, new BigDecimal("25.0000"), "USD", TransferStatus.COMPLETED, "req-abc");
        entityManager.persistAndFlush(t1);

        // Duplicate transfer from same wallet1 with same key "req-abc" must fail with constraint violation
        Transfer tDuplicate = new Transfer(wallet1, wallet2, new BigDecimal("25.0000"), "USD", TransferStatus.COMPLETED, "req-abc");
        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(tDuplicate);
        });

        entityManager.clear();

        // Refetch entities after clear
        Wallet refetchedW1 = entityManager.find(Wallet.class, wallet1.getId());
        Wallet refetchedW2 = entityManager.find(Wallet.class, wallet2.getId());

        // Transfer from a DIFFERENT sender wallet (wallet2) with the SAME idempotency key "req-abc" must succeed!
        Transfer tFromOtherWallet = new Transfer(refetchedW2, refetchedW1, new BigDecimal("10.0000"), "USD", TransferStatus.COMPLETED, "req-abc");
        assertDoesNotThrow(() -> {
            entityManager.persistAndFlush(tFromOtherWallet);
        });
    }

    @Test
    @DisplayName("LedgerEntryRepository should persist debit and credit entries with currency for a transfer")
    void testLedgerEntryRepositoryOperations() {
        User sender = entityManager.persist(new User("Sender2", "s2@example.com", "hash"));
        User recipient = entityManager.persist(new User("Recipient2", "r2@example.com", "hash"));

        Wallet senderWallet = entityManager.persist(new Wallet(sender, "USD", new BigDecimal("500.0000")));
        Wallet recipientWallet = entityManager.persist(new Wallet(recipient, "USD", new BigDecimal("100.0000")));

        Transfer transfer = entityManager.persist(
                new Transfer(senderWallet, recipientWallet, new BigDecimal("150.0000"), "USD", TransferStatus.COMPLETED, "tx-ledger-1")
        );

        LedgerEntry debitEntry = new LedgerEntry(transfer, senderWallet, LedgerEntryType.DEBIT, new BigDecimal("150.0000"), "USD");
        LedgerEntry creditEntry = new LedgerEntry(transfer, recipientWallet, LedgerEntryType.CREDIT, new BigDecimal("150.0000"), "USD");

        ledgerEntryRepository.save(debitEntry);
        ledgerEntryRepository.save(creditEntry);

        List<LedgerEntry> transferEntries = ledgerEntryRepository.findByTransferId(transfer.getId());
        assertEquals(2, transferEntries.size());

        List<LedgerEntry> senderEntries = ledgerEntryRepository.findByWalletId(senderWallet.getId());
        assertEquals(1, senderEntries.size());
        assertEquals(LedgerEntryType.DEBIT, senderEntries.getFirst().getEntryType());
        assertEquals("USD", senderEntries.getFirst().getCurrency());

        List<LedgerEntry> recipientEntries = ledgerEntryRepository.findByWalletIdOrderByCreatedAtDesc(recipientWallet.getId());
        assertEquals(1, recipientEntries.size());
        assertEquals(LedgerEntryType.CREDIT, recipientEntries.getFirst().getEntryType());
        assertEquals("USD", recipientEntries.getFirst().getCurrency());
    }
}
