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
        name = "wallets",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_wallets_user_currency", columnNames = {"user_id", "currency"})
        },
        indexes = {
                @Index(name = "idx_wallets_user_id", columnList = "user_id"),
                @Index(name = "idx_wallets_user_currency", columnList = "user_id, currency")
        }
)
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Wallet owner is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_wallets_user_id"))
    private User owner;

    @NotBlank(message = "Currency code cannot be blank")
    @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @NotNull(message = "Balance cannot be null")
    @DecimalMin(value = "0.0000", message = "Balance cannot be negative")
    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO.setScale(4);

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Wallet() {
    }

    public Wallet(User owner, String currency, BigDecimal balance) {
        this.owner = owner;
        this.currency = currency;
        this.balance = balance != null ? balance.setScale(4, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4);
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.balance == null) {
            this.balance = BigDecimal.ZERO.setScale(4);
        } else {
            this.balance = this.balance.setScale(4, java.math.RoundingMode.HALF_UP);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance != null ? balance.setScale(4, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(4);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Wallet wallet)) return false;
        if (id != null && wallet.id != null) {
            return Objects.equals(id, wallet.id);
        }
        return Objects.equals(owner, wallet.owner) && Objects.equals(currency, wallet.currency);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(owner, currency);
    }

    @Override
    public String toString() {
        return "Wallet{" +
                "id=" + id +
                ", currency='" + currency + '\'' +
                ", balance=" + balance +
                ", createdAt=" + createdAt +
                '}';
    }
}
