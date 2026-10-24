package com.wallet.desktop.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;

public class Formatters {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("MMM d, yyyy, hh:mm:ss a").withZone(ZoneId.systemDefault());

    public static String formatCurrency(BigDecimal amount, String currency) {
        if (amount == null) {
            return "0.00";
        }
        String currCode = (currency != null && !currency.isBlank()) ? currency.trim().toUpperCase() : "USD";
        try {
            Currency cur = Currency.getInstance(currCode);
            NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.US);
            nf.setCurrency(cur);
            nf.setMinimumFractionDigits(2);
            nf.setMaximumFractionDigits(4);
            return nf.format(amount);
        } catch (Exception e) {
            return String.format(Locale.US, "%,.4f %s", amount, currCode);
        }
    }

    public static String formatBalance(BigDecimal balance) {
        if (balance == null) {
            return "0.0000";
        }
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(4);
        return nf.format(balance);
    }

    public static String formatDateTime(Instant instant) {
        if (instant == null) {
            return "N/A";
        }
        try {
            return DATE_TIME_FORMATTER.format(instant);
        } catch (Exception e) {
            return instant.toString();
        }
    }

    public static String formatDateRelative(Instant instant) {
        if (instant == null) {
            return "N/A";
        }
        try {
            Instant now = Instant.now();
            Duration duration = Duration.between(instant, now);
            long seconds = duration.getSeconds();
            if (seconds < 0) {
                return formatDateTime(instant);
            }
            if (seconds < 60) {
                return "Just now";
            }
            if (seconds < 3600) {
                return (seconds / 60) + "m ago";
            }
            if (seconds < 86400) {
                return (seconds / 3600) + "h ago";
            }
            return formatDateTime(instant);
        } catch (Exception e) {
            return instant.toString();
        }
    }

    public static String formatWalletId(Long id) {
        if (id == null) {
            return "#---";
        }
        return String.format("#W-%04d", id);
    }
}
