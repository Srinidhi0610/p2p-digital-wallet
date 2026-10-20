package com.wallet.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "transfers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_transfers_sender_idempotency", columnNames = {"sender_wallet_id", "idempotency_key"})
        },
        indexes = {
                @Index(name = "idx_transfers_sender_idempotency", columnList = "sender_wallet_id, idempotency_key"),
                @Index(name = "idx_transfers_sender_wallet", columnList = "sender_wallet_id"),
                @Index(name = "idx_transfers_recipient_wallet", columnList = "recipient_wallet_id"),
                @Index(name = "idx_transfers_status", columnList = "status"),
                @Index(name = "idx_transfers_created_at", columnList = "created_at")
        }
)
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Sender wallet is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_wallet_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transfers_sender_wallet"))
    private Wallet senderWallet;

    @NotNull(message = "Recipient wallet is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_wallet_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transfers_recipient_wallet"))
    private Wallet recipientWallet;

    @NotNull(message = "Transfer amount cannot be null")
    @DecimalMin(value = "0.0001", message = "Transfer amount must be greater than zero")
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @NotBlank(message = "Currency code cannot be blank")
    @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @NotNull(message = "Transfer status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransferStatus status = TransferStatus.PENDING;

    @NotBlank(message = "Idempotency key cannot be blank")
    @Size(max = 100, message = "Idempotency key must not exceed 100 characters")
    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Transfer() {
    }

    public Transfer(Wallet senderWallet, Wallet recipientWallet, BigDecimal amount, String currency, TransferStatus status, String idempotencyKey) {
        this.senderWallet = senderWallet;
        this.recipientWallet = recipientWallet;
        this.amount = amount != null ? amount.setScale(4, java.math.RoundingMode.HALF_UP) : null;
        this.currency = currency;
        this.status = status != null ? status : TransferStatus.PENDING;
        this.idempotencyKey = idempotencyKey;
    }

    public Transfer(Wallet senderWallet, Wallet recipientWallet, BigDecimal amount, String currency, TransferStatus status) {
        this(senderWallet, recipientWallet, amount, currency, status, null);
    }

    @PrePersist
    protected void onCreate() {
        if (!isDistinctWallets()) {
            throw new IllegalStateException("Sender and recipient wallets must be distinct");
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = TransferStatus.PENDING;
        }
        if (this.amount != null) {
            this.amount = this.amount.setScale(4, java.math.RoundingMode.HALF_UP);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Wallet getSenderWallet() {
        return senderWallet;
    }

    public void setSenderWallet(Wallet senderWallet) {
        this.senderWallet = senderWallet;
    }

    public Wallet getRecipientWallet() {
        return recipientWallet;
    }

    public void setRecipientWallet(Wallet recipientWallet) {
        this.recipientWallet = recipientWallet;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount != null ? amount.setScale(4, java.math.RoundingMode.HALF_UP) : null;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public void setStatus(TransferStatus status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @AssertTrue(message = "Sender and recipient wallets must be distinct")
    public boolean isDistinctWallets() {
        if (senderWallet == null || recipientWallet == null) {
            return true;
        }
        if (senderWallet.getId() != null && recipientWallet.getId() != null) {
            return !Objects.equals(senderWallet.getId(), recipientWallet.getId());
        }
        return senderWallet != recipientWallet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transfer transfer)) return false;
        return id != null && Objects.equals(id, transfer.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Transfer{" +
                "id=" + id +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
