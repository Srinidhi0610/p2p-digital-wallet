package com.wallet.desktop.ui;

import com.wallet.desktop.ui.component.ToastNotification;
import javafx.scene.Node;

/**
 * Primary navigation and modal coordinator for pocket // peer JavaFX application.
 */
public interface NavigationManager {

    /**
     * Navigate to a registered route ("login", "register", "dashboard", "send", "ledger", "accounts").
     */
    void showView(String route);

    /**
     * Navigate directly to the Send Liquidity screen with a source wallet partition pre-selected.
     */
    void showSendWithWallet(Long walletId, String currency);

    /**
     * Mount and display a modal dialog above the active view.
     */
    void openModal(Node modal);

    /**
     * Dismiss the currently active modal dialog.
     */
    void closeModal();

    /**
     * Display a floating feedback notification toast.
     */
    void showToast(String title, String message, ToastNotification.Type type);

    /**
     * Get the root layout container.
     */
    javafx.scene.layout.StackPane getRootPane();
}
