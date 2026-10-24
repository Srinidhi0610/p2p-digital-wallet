package com.wallet.desktop.service;

import com.wallet.desktop.model.ErrorResponse;

public class ApiException extends RuntimeException {
    private final int statusCode;
    private final ErrorResponse errorResponse;

    public ApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorResponse = null;
    }

    public ApiException(String message, int statusCode, ErrorResponse errorResponse) {
        super(message);
        this.statusCode = statusCode;
        this.errorResponse = errorResponse;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public ErrorResponse getErrorResponse() {
        return errorResponse;
    }
}
