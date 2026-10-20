package com.wallet.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "ledger_entries",
        indexes = {
                @Index(name = "idx_ledger_transfer_id", columnList = "transfer_id"),
                @Index(name = "idx_ledger_wallet_id", columnList = "wallet_id"),
                @Index(name = "idx_ledger_created_at", columnList = "created_at")
        }
)
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Associated transfer is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id", nullable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_ledger_transfer"))
    private Transfer transfer;

    @NotNull(message = "Associated wallet is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_ledger_wallet"))
    private Wallet wallet;

    @NotNull(message = "Entry type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10, updatable = false)
    private LedgerEntryType entryType;

    @NotNull(message = "Ledger amount cannot be null")
    @DecimalMin(value = "0.0001", message = "Ledger amount must be greater than zero")
    @Column(name = "amount", nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal amount;

    @NotBlank(message = "Currency code cannot be blank")
    @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public LedgerEntry() {
    }

    public LedgerEntry(Transfer transfer, Wallet wallet, LedgerEntryType entryType, BigDecimal amount, String currency) {
        this.transfer = transfer;
        this.wallet = wallet;
        this.entryType = entryType;
        this.amount = amount != null ? amount.setScale(4, java.math.RoundingMode.HALF_UP) : null;
        this.currency = currency;
    }

    public LedgerEntry(Transfer transfer, Wallet wallet, LedgerEntryType entryType, BigDecimal amount) {
        this(transfer, wallet, entryType, amount, wallet != null ? wallet.getCurrency() : null);
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.amount != null) {
            this.amount = this.amount.setScale(4, java.math.RoundingMode.HALF_UP);
        }
        if (this.currency == null && this.wallet != null) {
            this.currency = this.wallet.getCurrency();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Transfer getTransfer() {
        return transfer;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public LedgerEntryType getEntryType() {
        return entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LedgerEntry that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "LedgerEntry{" +
                "id=" + id +
                ", entryType=" + entryType +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
