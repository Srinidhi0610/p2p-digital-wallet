package com.wallet.desktop.config;

import java.util.prefs.Preferences;

public class AppConfig {
    private static final String PREF_BASE_URL = "wallet_base_url";
    private static final String DEFAULT_BASE_URL = "http://localhost:8080";
    private static final Preferences prefs = Preferences.userNodeForPackage(AppConfig.class);

    private static String baseUrl;

    static {
        // System property > Env var > User pref > Default
        String sysProp = System.getProperty("wallet.api.url");
        if (sysProp != null && !sysProp.isBlank()) {
            baseUrl = sysProp.trim();
        } else {
            String envVar = System.getenv("WALLET_API_URL");
            if (envVar != null && !envVar.isBlank()) {
                baseUrl = envVar.trim();
            } else {
                baseUrl = prefs.get(PREF_BASE_URL, DEFAULT_BASE_URL);
            }
        }
        // Normalize trailing slash
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
    }

    public static String getBaseUrl() {
        return baseUrl;
    }

    public static void setBaseUrl(String url) {
        if (url != null && !url.isBlank()) {
            baseUrl = url.trim();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            prefs.put(PREF_BASE_URL, baseUrl);
        }
    }

    public static String getApiUrl(String endpoint) {
        String cleanEndpoint = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
        if (!cleanEndpoint.startsWith("/api")) {
            cleanEndpoint = "/api" + cleanEndpoint;
        }
        return baseUrl + cleanEndpoint;
    }
}
