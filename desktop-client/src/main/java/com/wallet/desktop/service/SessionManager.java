package com.wallet.desktop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.desktop.model.User;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

public class SessionManager {
    private static final String PREF_TOKEN = "apex_wallet_token";
    private static final String PREF_USER = "apex_wallet_user";
    private static final Preferences prefs = Preferences.userNodeForPackage(SessionManager.class);

    private static final SessionManager instance = new SessionManager();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String token;
    private User currentUser;
    private final List<Consumer<Boolean>> authListeners = new ArrayList<>();

    private SessionManager() {
        // Restore session if present
        this.token = prefs.get(PREF_TOKEN, null);
        String userJson = prefs.get(PREF_USER, null);
        if (userJson != null) {
            try {
                this.currentUser = objectMapper.readValue(userJson, User.class);
            } catch (Exception ignored) {
                this.currentUser = null;
            }
        }
    }

    public static SessionManager getInstance() {
        return instance;
    }

    public synchronized void setAuth(String token, User user) {
        this.token = token;
        this.currentUser = user;

        if (token != null) {
            prefs.put(PREF_TOKEN, token);
        } else {
            prefs.remove(PREF_TOKEN);
        }

        if (user != null) {
            try {
                prefs.put(PREF_USER, objectMapper.writeValueAsString(user));
            } catch (Exception ignored) {
            }
        } else {
            prefs.remove(PREF_USER);
        }

        notifyListeners(true);
    }

    public synchronized void clearAuth() {
        this.token = null;
        this.currentUser = null;
        prefs.remove(PREF_TOKEN);
        prefs.remove(PREF_USER);

        notifyListeners(false);
    }

    public synchronized void clearSession() {
        clearAuth();
    }

    public synchronized boolean isAuthenticated() {
        return token != null && !token.isBlank();
    }

    public synchronized String getToken() {
        return token;
    }

    public synchronized User getCurrentUser() {
        return currentUser;
    }

    public synchronized void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            try {
                prefs.put(PREF_USER, objectMapper.writeValueAsString(user));
            } catch (Exception ignored) {
            }
        }
    }

    public synchronized void addAuthListener(Consumer<Boolean> listener) {
        if (!authListeners.contains(listener)) {
            authListeners.add(listener);
        }
    }

    public synchronized void removeAuthListener(Consumer<Boolean> listener) {
        authListeners.remove(listener);
    }

    private void notifyListeners(boolean authenticated) {
        List<Consumer<Boolean>> listenersCopy;
        synchronized (this) {
            listenersCopy = new ArrayList<>(authListeners);
        }
        for (Consumer<Boolean> listener : listenersCopy) {
            if (Platform.isFxApplicationThread()) {
                listener.accept(authenticated);
            } else {
                Platform.runLater(() -> listener.accept(authenticated));
            }
        }
    }
}
