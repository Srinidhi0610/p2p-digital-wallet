package com.wallet.desktop.ui;

import com.wallet.desktop.service.SessionManager;
import com.wallet.desktop.ui.component.NavbarView;
import com.wallet.desktop.ui.component.ToastNotification;
import com.wallet.desktop.ui.view.AccountsView;
import com.wallet.desktop.ui.view.DashboardView;
import com.wallet.desktop.ui.view.LedgerView;
import com.wallet.desktop.ui.view.LoginView;
import com.wallet.desktop.ui.view.RegisterView;
import com.wallet.desktop.ui.view.SendMoneyView;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public class MainLayout extends StackPane implements NavigationManager {

    private final NavbarView navbar;
    private final StackPane contentArea;
    private final StackPane modalOverlay;

    // View instances
    private LoginView loginView;
    private RegisterView registerView;
    private DashboardView dashboardView;
    private SendMoneyView sendMoneyView;
    private LedgerView ledgerView;
    private AccountsView accountsView;

    private String currentRoute = "login";

    public MainLayout() {
        getStyleClass().add("app-container");

        BorderPane layout = new BorderPane();
        layout.getStyleClass().add("app-container");

        navbar = new NavbarView(this::showView, this::handleLogout);
        layout.setTop(navbar);

        contentArea = new StackPane();
        contentArea.getStyleClass().add("app-container");
        layout.setCenter(contentArea);

        // Modal overlay container
        modalOverlay = new StackPane();
        modalOverlay.setAlignment(Pos.CENTER);
        modalOverlay.setVisible(false);
        modalOverlay.setManaged(false);

        getChildren().addAll(layout, modalOverlay);

        initViews();

        // Listen for session state changes (e.g. 401 or logout)
        SessionManager.getInstance().addAuthListener(isAuth -> {
            Platform.runLater(() -> {
                navbar.updateAuthState();
                if (!isAuth && !("login".equals(currentRoute) || "register".equals(currentRoute))) {
                    showView("login");
                }
            });
        });

        // Initial route based on auth
        if (SessionManager.getInstance().isAuthenticated()) {
            showView("dashboard");
        } else {
            showView("login");
        }
    }

    private void initViews() {
        loginView = new LoginView(this);
        registerView = new RegisterView(this);
        dashboardView = new DashboardView(this);
        sendMoneyView = new SendMoneyView(this);
        ledgerView = new LedgerView(this);
        accountsView = new AccountsView(this);
    }

    @Override
    public void showView(String route) {
        Platform.runLater(() -> {
            boolean isAuth = SessionManager.getInstance().isAuthenticated();

            String targetRoute = route;
            if (!isAuth && !("login".equals(targetRoute) || "register".equals(targetRoute))) {
                targetRoute = "login";
            } else if (isAuth && ("login".equals(targetRoute) || "register".equals(targetRoute))) {
                targetRoute = "dashboard";
            }

            this.currentRoute = targetRoute;
            navbar.setActiveRoute(targetRoute);
            closeModal();

            contentArea.getChildren().clear();

            switch (targetRoute) {
                case "login":
                    contentArea.getChildren().add(loginView);
                    break;
                case "register":
                    contentArea.getChildren().add(registerView);
                    break;
                case "dashboard":
                    contentArea.getChildren().add(dashboardView);
                    dashboardView.loadData();
                    break;
                case "send":
                    contentArea.getChildren().add(sendMoneyView);
                    sendMoneyView.loadData();
                    break;
                case "ledger":
                    contentArea.getChildren().add(ledgerView);
                    ledgerView.loadData(0);
                    break;
                case "accounts":
                    contentArea.getChildren().add(accountsView);
                    accountsView.loadData();
                    break;
                default:
                    contentArea.getChildren().add(dashboardView);
                    dashboardView.loadData();
                    break;
            }
        });
    }

    @Override
    public void showSendWithWallet(Long walletId, String currency) {
        Platform.runLater(() -> {
            this.currentRoute = "send";
            navbar.setActiveRoute("send");
            closeModal();

            contentArea.getChildren().clear();
            contentArea.getChildren().add(sendMoneyView);
            sendMoneyView.setPreselectedSenderId(walletId);
            sendMoneyView.loadData();
        });
    }

    @Override
    public void openModal(Node modal) {
        Platform.runLater(() -> {
            modalOverlay.getChildren().clear();
            modalOverlay.getChildren().add(modal);
            modalOverlay.setVisible(true);
            modalOverlay.setManaged(true);
        });
    }

    @Override
    public void closeModal() {
        Platform.runLater(() -> {
            modalOverlay.getChildren().clear();
            modalOverlay.setVisible(false);
            modalOverlay.setManaged(false);
        });
    }

    @Override
    public void showToast(String title, String message, ToastNotification.Type type) {
        ToastNotification.show(this, title, message, type);
    }

    @Override
    public StackPane getRootPane() {
        return this;
    }

    private void handleLogout() {
        SessionManager.getInstance().clearSession();
        showView("login");
        showToast("Signed Out", "Your session has been securely terminated.", ToastNotification.Type.INFO);
    }
}
