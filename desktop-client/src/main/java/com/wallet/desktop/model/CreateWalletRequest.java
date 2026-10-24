package com.wallet.desktop.model;

public class CreateWalletRequest {
    private String currency;

    public CreateWalletRequest() {}

    public CreateWalletRequest(String currency) {
        this.currency = currency;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
