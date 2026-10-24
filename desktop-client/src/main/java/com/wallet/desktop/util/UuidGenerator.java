package com.wallet.desktop.util;

import java.util.UUID;

public class UuidGenerator {
    /**
     * Generates a standard RFC 4122 UUIDv4 string for transfer idempotency keys.
     */
    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
