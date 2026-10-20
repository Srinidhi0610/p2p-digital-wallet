package com.wallet.exception;

public class TransferNotFoundException extends RuntimeException {

    public TransferNotFoundException(String message) {
        super(message);
    }
}
