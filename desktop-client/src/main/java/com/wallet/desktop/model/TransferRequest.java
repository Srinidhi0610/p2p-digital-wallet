package com.wallet.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TransferRequest {
    private Long senderWalletId;
    private Long recipientWalletId;
    private BigDecimal amount;
    private String currency;
    private String idempotencyKey;

    public TransferRequest() {
    }

    public TransferRequest(Long senderWalletId, Long recipientWalletId, BigDecimal amount, String currency, String idempotencyKey) {
        this.senderWalletId = senderWalletId;
        this.recipientWalletId = recipientWalletId;
        this.amount = amount;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getSenderWalletId() {
        return senderWalletId;
    }

    public void setSenderWalletId(Long senderWalletId) {
        this.senderWalletId = senderWalletId;
    }

    public Long getRecipientWalletId() {
        return recipientWalletId;
    }

    public void setRecipientWalletId(Long recipientWalletId) {
        this.recipientWalletId = recipientWalletId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
