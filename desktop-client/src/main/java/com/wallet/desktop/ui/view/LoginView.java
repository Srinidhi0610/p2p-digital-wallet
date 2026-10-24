package com.wallet.desktop.ui.view;

import com.wallet.desktop.service.AuthService;
import com.wallet.desktop.ui.NavigationManager;
import com.wallet.desktop.ui.component.Icons;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

public class LoginView extends StackPane {

    private final NavigationManager nav;

    private TextField emailField;
    private PasswordField passwordField;
    private TextField plainPasswordField;
    private boolean showPassword = false;

    private Label errorLabel;
    private Button btnSubmit;

    public LoginView(NavigationManager nav) {
        this.nav = nav;
        initUI();
    }

    private void initUI() {
        setAlignment(Pos.CENTER);
        setPadding(new Insets(24));

        GridPane splitPane = new GridPane();
        splitPane.getStyleClass().add("auth-split-wrapper");
        splitPane.setMaxWidth(960);
        splitPane.setPrefWidth(920);

        // --- Left Brand Side ---
        VBox brandSide = new VBox(20);
        brandSide.getStyleClass().add("auth-brand-side");
        brandSide.setPrefWidth(460);

        Label badge = new Label("PEER-TO-PEER LEDGER ENGINE");
        badge.setGraphic(Icons.zap(Icons.LIME, 13));
        badge.getStyleClass().add("auth-brand-badge");

        VBox titleBox = new VBox(0);
        Label h1 = new Label("Money in");
        h1.getStyleClass().add("auth-brand-heading");
        Label h2 = new Label("Motion.");
        h2.getStyleClass().addAll("auth-brand-heading", "text-lime");
        titleBox.getChildren().addAll(h1, h2);

        Label desc = new Label("A real-time digital wallet built on double-entry accounting principles, deterministic row-level locks, and cryptographic idempotency.");
        desc.setWrapText(true);
        desc.getStyleClass().add("auth-brand-desc");

        // Abstract wave graphic
        SVGPath wave = new SVGPath();
        wave.setContent("M0 30 C 60 10, 140 50, 220 25 C 280 5, 340 35, 400 30");
        wave.setStroke(Icons.LIME);
        wave.setStrokeWidth(2.5);
        wave.setFill(null);
        wave.setOpacity(0.65);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Feature Specs Grid
        HBox specs = new HBox(24);
        specs.setStyle("-fx-border-color: rgba(170, 181, 198, 0.16); -fx-border-width: 1 0 0 0; -fx-padding: 16 0 0 0;");

        VBox spec1 = new VBox(3);
        Label s1Lbl = new Label("SETTLEMENT MODEL");
        s1Lbl.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 9.5px; -fx-text-fill: #AAB5C6;");
        Label s1Val = new Label("Double-Entry Ledger");
        s1Val.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #F5F1E8;");
        spec1.getChildren().addAll(s1Lbl, s1Val);

        VBox spec2 = new VBox(3);
        Label s2Lbl = new Label("CONCURRENCY");
        s2Lbl.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 9.5px; -fx-text-fill: #AAB5C6;");
        Label s2Val = new Label("Pessimistic Row Locks");
        s2Val.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #C8F169;");
        spec2.getChildren().addAll(s2Lbl, s2Val);

        specs.getChildren().addAll(spec1, spec2);

        brandSide.getChildren().addAll(badge, titleBox, desc, wave, spacer, specs);

        // --- Right Form Side ---
        VBox formSide = new VBox(18);
        formSide.getStyleClass().add("auth-form-side");
        formSide.setPrefWidth(460);
        formSide.setAlignment(Pos.CENTER_LEFT);

        VBox formHeader = new VBox(4);
        Label formTitle = new Label("Access Account");
        formTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: #F5F1E8;");
        Label formSubtitle = new Label("Enter your credentials to manage balances and transfer funds");
        formSubtitle.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #AAB5C6;");
        formHeader.getChildren().addAll(formTitle, formSubtitle);

        errorLabel = new Label();
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setWrapText(true);
        errorLabel.getStyleClass().addAll("alert-box", "alert-danger");

        // Email field
        VBox emailBox = new VBox(6);
        Label emailLbl = new Label("Email Address");
        emailLbl.getStyleClass().add("form-label");
        emailField = new TextField();
        emailField.setPromptText("alice@example.com");
        emailBox.getChildren().addAll(emailLbl, emailField);

        // Password field with toggle
        VBox passBox = new VBox(6);
        Label passLbl = new Label("Password");
        passLbl.getStyleClass().add("form-label");

        StackPane passStack = new StackPane();
        passwordField = new PasswordField();
        passwordField.setPromptText("••••••••");

        plainPasswordField = new TextField();
        plainPasswordField.setPromptText("••••••••");
        plainPasswordField.setVisible(false);
        plainPasswordField.setManaged(false);

        // Synchronize text
        passwordField.textProperty().bindBidirectional(plainPasswordField.textProperty());

        Button toggleEye = new Button();
        toggleEye.setGraphic(Icons.keyRound(Icons.MUTED, 14));
        toggleEye.getStyleClass().addAll("btn", "btn-ghost", "btn-sm");
        toggleEye.setStyle("-fx-padding: 3 6; -fx-cursor: hand;");
        toggleEye.setOnAction(e -> {
            showPassword = !showPassword;
            if (showPassword) {
                passwordField.setVisible(false);
                passwordField.setManaged(false);
                plainPasswordField.setVisible(true);
                plainPasswordField.setManaged(true);
            } else {
                plainPasswordField.setVisible(false);
                plainPasswordField.setManaged(false);
                passwordField.setVisible(true);
                passwordField.setManaged(true);
            }
        });
        StackPane.setAlignment(toggleEye, Pos.CENTER_RIGHT);
        StackPane.setMargin(toggleEye, new Insets(0, 6, 0, 0));

        passStack.getChildren().addAll(passwordField, plainPasswordField, toggleEye);
        passBox.getChildren().addAll(passLbl, passStack);

        btnSubmit = new Button("Sign In to Wallet");
        btnSubmit.setGraphic(Icons.arrowRight(Icons.NAVY, 15));
        btnSubmit.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        btnSubmit.getStyleClass().addAll("btn", "btn-lime", "btn-lg");
        btnSubmit.setMaxWidth(Double.MAX_VALUE);
        btnSubmit.setOnAction(e -> handleLogin());

        // Enter key submission
        emailField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> handleLogin());
        plainPasswordField.setOnAction(e -> handleLogin());

        HBox registerLinkBox = new HBox(4);
        registerLinkBox.setAlignment(Pos.CENTER);
        registerLinkBox.setStyle("-fx-border-color: rgba(170, 181, 198, 0.16); -fx-border-width: 1 0 0 0; -fx-padding: 16 0 0 0;");

        Label newPeerLbl = new Label("New peer?");
        newPeerLbl.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #AAB5C6;");

        Button linkRegister = new Button("Create an account");
        linkRegister.getStyleClass().addAll("btn", "btn-ghost", "btn-sm");
        linkRegister.setStyle("-fx-font-weight: 800; -fx-text-fill: #C8F169; -fx-padding: 0;");
        linkRegister.setOnAction(e -> nav.showView("register"));

        registerLinkBox.getChildren().addAll(newPeerLbl, linkRegister);

        formSide.getChildren().addAll(formHeader, errorLabel, emailBox, passBox, btnSubmit, registerLinkBox);

        splitPane.add(brandSide, 0, 0);
        splitPane.add(formSide, 1, 0);

        getChildren().add(splitPane);
    }

    private void handleLogin() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        String email = emailField.getText();
        String pass = passwordField.getText();

        if (email == null || email.isBlank()) {
            showError("Email address is required");
            return;
        }
        if (pass == null || pass.isBlank()) {
            showError("Password cannot be blank");
            return;
        }

        btnSubmit.setDisable(true);
        btnSubmit.setText("Authenticating...");

        AuthService.getInstance().login(email.trim(), pass)
                .thenAccept(res -> Platform.runLater(() -> {
                    btnSubmit.setDisable(false);
                    btnSubmit.setText("Sign In to Wallet");
                    nav.showView("dashboard");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        btnSubmit.setDisable(false);
                        btnSubmit.setText("Sign In to Wallet");
                        showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                    });
                    return null;
                });
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
