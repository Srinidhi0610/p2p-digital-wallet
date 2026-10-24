package com.wallet.desktop.ui.component;

import com.wallet.desktop.model.User;
import com.wallet.desktop.service.SessionManager;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public class NavbarView extends HBox {

    private final Consumer<String> navigationHandler;
    private final Runnable logoutHandler;

    private Button btnOverview;
    private Button btnSendMoney;
    private Button btnLedger;
    private Button btnAccounts;

    private Label userNameLabel;
    private Label userIdLabel;
    private Label userAvatarLabel;

    private HBox authNavBox;
    private HBox unauthNavBox;

    public NavbarView(Consumer<String> navigationHandler, Runnable logoutHandler) {
        this.navigationHandler = navigationHandler;
        this.logoutHandler = logoutHandler;

        initUI();
        updateAuthState();
        SessionManager.getInstance().addAuthListener(isAuth -> updateAuthState());
    }

    private void initUI() {
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("navbar");
        setSpacing(20);

        // --- Brand Container ---
        HBox brandContainer = new HBox(12);
        brandContainer.setAlignment(Pos.CENTER_LEFT);
        brandContainer.setStyle("-fx-cursor: hand;");
        brandContainer.setOnMouseClicked(e -> navigationHandler.accept("dashboard"));

        StackPane brandMark = new StackPane();
        brandMark.getStyleClass().add("brand-mark");
        brandMark.getChildren().add(Icons.arrowUpRight(Icons.LIME, 16));

        VBox brandTextBox = new VBox(0);
        HBox titleBox = new HBox(4);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        Label title1 = new Label("pocket");
        title1.getStyleClass().add("brand-title");
        Label slash = new Label("//");
        slash.getStyleClass().add("brand-slash");
        Label title2 = new Label("peer");
        title2.getStyleClass().add("brand-title");
        titleBox.getChildren().addAll(title1, slash, title2);

        Label subtitle = new Label("MONEY IN MOTION");
        subtitle.getStyleClass().add("brand-subtitle");

        brandTextBox.getChildren().addAll(titleBox, subtitle);
        brandContainer.getChildren().addAll(brandMark, brandTextBox);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // --- Authenticated Nav Links ---
        authNavBox = new HBox(8);
        authNavBox.setAlignment(Pos.CENTER_RIGHT);

        btnOverview = createNavLink("Overview", Icons.wallet(Icons.MUTED, 15), "dashboard");
        btnSendMoney = createNavLink("Send Money", Icons.send(Icons.MUTED, 15), "send");
        btnLedger = createNavLink("Ledger", Icons.history(Icons.MUTED, 15), "ledger");
        btnAccounts = createNavLink("Accounts", Icons.user(Icons.MUTED, 15), "accounts");

        // User Chip
        HBox userChip = new HBox(8);
        userChip.setAlignment(Pos.CENTER_LEFT);
        userChip.getStyleClass().add("nav-user-chip");

        userAvatarLabel = new Label("P");
        userAvatarLabel.getStyleClass().add("user-avatar-mark");

        VBox userMeta = new VBox(1);
        userNameLabel = new Label("Peer");
        userNameLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 11.5px; -fx-text-fill: #F5F1E8;");
        userIdLabel = new Label("#UID-0");
        userIdLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 10px; -fx-text-fill: #6B7C96;");
        userMeta.getChildren().addAll(userNameLabel, userIdLabel);

        Button btnLogout = new Button();
        btnLogout.setGraphic(Icons.logOut(Icons.MUTED, 14));
        btnLogout.getStyleClass().addAll("btn", "btn-ghost", "btn-sm");
        btnLogout.setStyle("-fx-padding: 3 6;");
        btnLogout.setOnAction(e -> {
            if (logoutHandler != null) logoutHandler.run();
        });

        userChip.getChildren().addAll(userAvatarLabel, userMeta, btnLogout);

        authNavBox.getChildren().addAll(btnOverview, btnSendMoney, btnLedger, btnAccounts, userChip);

        // --- Unauthenticated Nav Links ---
        unauthNavBox = new HBox(10);
        unauthNavBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnSignIn = new Button("Sign In");
        btnSignIn.getStyleClass().addAll("btn", "btn-ghost", "btn-sm");
        btnSignIn.setOnAction(e -> navigationHandler.accept("login"));

        Button btnRegister = new Button("Create Account");
        btnRegister.getStyleClass().addAll("btn", "btn-primary", "btn-sm");
        btnRegister.setOnAction(e -> navigationHandler.accept("register"));

        unauthNavBox.getChildren().addAll(btnSignIn, btnRegister);

        getChildren().addAll(brandContainer, spacer, authNavBox, unauthNavBox);
    }

    private Button createNavLink(String text, javafx.scene.Node icon, String route) {
        Button btn = new Button(text);
        btn.setGraphic(icon);
        btn.getStyleClass().add("nav-link");
        btn.setOnAction(e -> navigationHandler.accept(route));
        return btn;
    }

    public void setActiveRoute(String route) {
        btnOverview.getStyleClass().remove("nav-link-active");
        btnSendMoney.getStyleClass().remove("nav-link-active");
        btnLedger.getStyleClass().remove("nav-link-active");
        btnAccounts.getStyleClass().remove("nav-link-active");

        if ("dashboard".equals(route)) {
            btnOverview.getStyleClass().add("nav-link-active");
        } else if ("send".equals(route)) {
            btnSendMoney.getStyleClass().add("nav-link-active");
        } else if ("ledger".equals(route)) {
            btnLedger.getStyleClass().add("nav-link-active");
        } else if ("accounts".equals(route)) {
            btnAccounts.getStyleClass().add("nav-link-active");
        }
    }

    public void updateAuthState() {
        boolean isAuth = SessionManager.getInstance().isAuthenticated();
        authNavBox.setVisible(isAuth);
        authNavBox.setManaged(isAuth);
        unauthNavBox.setVisible(!isAuth);
        unauthNavBox.setManaged(!isAuth);

        if (isAuth) {
            User user = SessionManager.getInstance().getCurrentUser();
            if (user != null) {
                String name = user.getName() != null ? user.getName() : "Peer";
                userNameLabel.setText(name);
                userAvatarLabel.setText(name.substring(0, 1).toUpperCase());
                userIdLabel.setText("#UID-" + user.getId());
            }
        }
    }
}
