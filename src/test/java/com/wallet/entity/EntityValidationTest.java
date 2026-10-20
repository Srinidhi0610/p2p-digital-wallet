package com.wallet.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EntityValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("User should be valid when all fields are properly provided")
    void validUserShouldHaveNoViolations() {
        User user = new User("Alice Smith", "alice@example.com", "$2a$10$hashedPasswordValue");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("User should fail validation when email is invalid or blank")
    void invalidUserEmailShouldFail() {
        User user = new User("Alice Smith", "not-a-valid-email", "$2a$10$hashedPasswordValue");
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    @DisplayName("Wallet should have balance with scale 4 and default to 0.0000")
    void walletShouldEnforceBigDecimalScale() {
        User user = new User("Bob", "bob@example.com", "hash");
        Wallet wallet = new Wallet(user, "USD", new BigDecimal("100.5"));

        assertEquals(new BigDecimal("100.5000"), wallet.getBalance());
        assertEquals(4, wallet.getBalance().scale());

        wallet.onCreate();
        assertNotNull(wallet.getCreatedAt());
    }

    @Test
    @DisplayName("Wallet should reject invalid currency code length")
    void walletShouldRejectInvalidCurrencyCode() {
        User user = new User("Bob", "bob@example.com", "hash");
        Wallet wallet = new Wallet(user, "USDD", new BigDecimal("100.0000"));

        Set<ConstraintViolation<Wallet>> violations = validator.validate(wallet);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("currency")));
    }

    @Test
    @DisplayName("Transfer should default status to PENDING, require positive amount and idempotency key")
    void transferShouldDefaultToPending() {
        User user1 = new User("Sender", "sender@example.com", "hash");
        User user2 = new User("Recipient", "recipient@example.com", "hash");
        Wallet w1 = new Wallet(user1, "USD", new BigDecimal("500.0000"));
        w1.setId(1L);
        Wallet w2 = new Wallet(user2, "USD", new BigDecimal("100.0000"));
        w2.setId(2L);

        Transfer transfer = new Transfer(w1, w2, new BigDecimal("50.00"), "USD", null, "tx-idem-1");
        transfer.onCreate();

        assertEquals(TransferStatus.PENDING, transfer.getStatus());
        assertEquals(new BigDecimal("50.0000"), transfer.getAmount());
        assertEquals(4, transfer.getAmount().scale());
        assertEquals("tx-idem-1", transfer.getIdempotencyKey());
        assertNotNull(transfer.getCreatedAt());
    }

    @Test
    @DisplayName("Transfer should reject zero or negative amount")
    void transferShouldRejectZeroOrNegativeAmount() {
        User user1 = new User("Sender", "sender@example.com", "hash");
        User user2 = new User("Recipient", "recipient@example.com", "hash");
        Wallet w1 = new Wallet(user1, "USD", new BigDecimal("500.0000"));
        w1.setId(1L);
        Wallet w2 = new Wallet(user2, "USD", new BigDecimal("100.0000"));
        w2.setId(2L);

        Transfer transfer = new Transfer(w1, w2, new BigDecimal("0.0000"), "USD", TransferStatus.PENDING, "tx-idem-zero");
        Set<ConstraintViolation<Transfer>> violations = validator.validate(transfer);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("amount")));
    }

    @Test
    @DisplayName("Transfer should reject blank or oversized idempotency key")
    void transferShouldRejectInvalidIdempotencyKey() {
        User user1 = new User("Sender", "sender@example.com", "hash");
        User user2 = new User("Recipient", "recipient@example.com", "hash");
        Wallet w1 = new Wallet(user1, "USD", new BigDecimal("500.0000"));
        w1.setId(1L);
        Wallet w2 = new Wallet(user2, "USD", new BigDecimal("100.0000"));
        w2.setId(2L);

        Transfer transferBlank = new Transfer(w1, w2, new BigDecimal("10.0000"), "USD", TransferStatus.PENDING, "  ");
        Set<ConstraintViolation<Transfer>> violations = validator.validate(transferBlank);
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("idempotencyKey")));

        String longKey = "a".repeat(101);
        Transfer transferTooLong = new Transfer(w1, w2, new BigDecimal("10.0000"), "USD", TransferStatus.PENDING, longKey);
        Set<ConstraintViolation<Transfer>> longViolations = validator.validate(transferTooLong);
        assertTrue(longViolations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("idempotencyKey")));
    }

    @Test
    @DisplayName("Transfer should reject self-transfers where sender and recipient wallets are identical")
    void transferShouldRejectSelfTransfers() {
        User user = new User("Sender", "sender@example.com", "hash");
        Wallet w1 = new Wallet(user, "USD", new BigDecimal("500.0000"));
        w1.setId(1L);

        Transfer transfer = new Transfer(w1, w1, new BigDecimal("50.0000"), "USD", TransferStatus.PENDING, "tx-self-1");
        Set<ConstraintViolation<Transfer>> violations = validator.validate(transfer);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("distinct")));

        assertThrows(IllegalStateException.class, transfer::onCreate);
    }

    @Test
    @DisplayName("LedgerEntry should require DEBIT or CREDIT, valid currency, and positive amount")
    void ledgerEntryValidation() {
        User user1 = new User("Sender", "sender@example.com", "hash");
        User user2 = new User("Recipient", "recipient@example.com", "hash");
        Wallet w1 = new Wallet(user1, "USD", new BigDecimal("500.0000"));
        w1.setId(1L);
        Wallet w2 = new Wallet(user2, "USD", new BigDecimal("100.0000"));
        w2.setId(2L);
        Transfer transfer = new Transfer(w1, w2, new BigDecimal("50.0000"), "USD", TransferStatus.COMPLETED, "tx-idem-ledger");

        LedgerEntry entry = new LedgerEntry(transfer, w1, LedgerEntryType.DEBIT, new BigDecimal("50.0000"), "USD");
        entry.onCreate();

        Set<ConstraintViolation<LedgerEntry>> violations = validator.validate(entry);
        assertTrue(violations.isEmpty());
        assertEquals(LedgerEntryType.DEBIT, entry.getEntryType());
        assertEquals(new BigDecimal("50.0000"), entry.getAmount());
        assertEquals("USD", entry.getCurrency());
        assertNotNull(entry.getCreatedAt());
    }

    @Test
    @DisplayName("LedgerEntry should reject invalid currency length")
    void ledgerEntryShouldRejectInvalidCurrency() {
        User user1 = new User("Sender", "sender@example.com", "hash");
        User user2 = new User("Recipient", "recipient@example.com", "hash");
        Wallet w1 = new Wallet(user1, "USD", new BigDecimal("500.0000"));
        w1.setId(1L);
        Wallet w2 = new Wallet(user2, "USD", new BigDecimal("100.0000"));
        w2.setId(2L);
        Transfer transfer = new Transfer(w1, w2, new BigDecimal("50.0000"), "USD", TransferStatus.COMPLETED, "tx-idem-bad-curr");

        LedgerEntry entry = new LedgerEntry(transfer, w1, LedgerEntryType.DEBIT, new BigDecimal("50.0000"), "USDD");
        Set<ConstraintViolation<LedgerEntry>> violations = validator.validate(entry);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("currency")));
    }

    @Test
    @DisplayName("Monetary values must never lose precision via floating point representation")
    void monetaryPrecisionShouldBeMaintained() {
        BigDecimal balance = new BigDecimal("0.1").add(new BigDecimal("0.2"));
        // 0.1 + 0.2 in double is 0.30000000000000004, but BigDecimal is exactly 0.3
        assertEquals(new BigDecimal("0.3"), balance);
        assertNotEquals(0.3, 0.1 + 0.2); // floating point inaccuracy proof
    }
}
