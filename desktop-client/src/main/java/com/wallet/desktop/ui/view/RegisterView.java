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

public class RegisterView extends StackPane {

    private final NavigationManager nav;

    private TextField nameField;
    private TextField emailField;
    private PasswordField passwordField;
    private PasswordField confirmPasswordField;
    private Label matchLabel;
    private Label errorLabel;
    private Button btnSubmit;

    public RegisterView(NavigationManager nav) {
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

        Label badge = new Label("JOIN THE PEER NETWORK");
        badge.setGraphic(Icons.shieldCheck(Icons.LIME, 13));
        badge.getStyleClass().add("auth-brand-badge");

        VBox titleBox = new VBox(0);
        Label h1 = new Label("Decentralized");
        h1.getStyleClass().add("auth-brand-heading");
        Label h2 = new Label("Precision.");
        h2.getStyleClass().addAll("auth-brand-heading", "text-lime");
        titleBox.getChildren().addAll(h1, h2);

        Label desc = new Label("Activate your digital wallet account in seconds. Open multi-currency accounts and experience guaranteed double-entry settlement.");
        desc.setWrapText(true);
        desc.getStyleClass().add("auth-brand-desc");

        // Abstract wave graphic
        SVGPath wave = new SVGPath();
        wave.setContent("M0 20 Q 100 60, 200 20 T 400 20");
        wave.setStroke(Icons.LIME);
        wave.setStrokeWidth(2.5);
        wave.setFill(null);
        wave.setOpacity(0.7);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Specs
        HBox specs = new HBox(24);
        specs.setStyle("-fx-border-color: rgba(170, 181, 198, 0.16); -fx-border-width: 1 0 0 0; -fx-padding: 16 0 0 0;");

        VBox spec1 = new VBox(3);
        Label s1Lbl = new Label("ACCOUNT TYPE");
        s1Lbl.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 9.5px; -fx-text-fill: #AAB5C6;");
        Label s1Val = new Label("Peer Settlement Node");
        s1Val.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #F5F1E8;");
        spec1.getChildren().addAll(s1Lbl, s1Val);

        VBox spec2 = new VBox(3);
        Label s2Lbl = new Label("INITIAL BALANCE");
        s2Lbl.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 9.5px; -fx-text-fill: #AAB5C6;");
        Label s2Val = new Label("0.0000 (Strict Audit)");
        s2Val.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #C8F169;");
        spec2.getChildren().addAll(s2Lbl, s2Val);

        specs.getChildren().addAll(spec1, spec2);

        brandSide.getChildren().addAll(badge, titleBox, desc, wave, spacer, specs);

        // --- Right Form Side ---
        VBox formSide = new VBox(16);
        formSide.getStyleClass().add("auth-form-side");
        formSide.setPrefWidth(460);
        formSide.setAlignment(Pos.CENTER_LEFT);

        VBox formHeader = new VBox(3);
        Label formTitle = new Label("Create Account");
        formTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: #F5F1E8;");
        Label formSubtitle = new Label("Set up your identity and credentials");
        formSubtitle.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #AAB5C6;");
        formHeader.getChildren().addAll(formTitle, formSubtitle);

        errorLabel = new Label();
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setWrapText(true);
        errorLabel.getStyleClass().addAll("alert-box", "alert-danger");

        // Name
        VBox nameBox = new VBox(5);
        Label nameLbl = new Label("Full Legal Name");
        nameLbl.getStyleClass().add("form-label");
        nameField = new TextField();
        nameField.setPromptText("Alice Smith");
        nameBox.getChildren().addAll(nameLbl, nameField);

        // Email
        VBox emailBox = new VBox(5);
        Label emailLbl = new Label("Email Address");
        emailLbl.getStyleClass().add("form-label");
        emailField = new TextField();
        emailField.setPromptText("alice@example.com");
        emailBox.getChildren().addAll(emailLbl, emailField);

        // Password
        VBox passBox = new VBox(5);
        HBox passHeader = new HBox(6);
        passHeader.setAlignment(Pos.CENTER_LEFT);
        Label passLbl = new Label("Password");
        passLbl.getStyleClass().add("form-label");
        Region passSp = new Region();
        HBox.setHgrow(passSp, Priority.ALWAYS);
        Label passHint = new Label("Min 8 chars");
        passHint.getStyleClass().add("form-label-hint");
        passHeader.getChildren().addAll(passLbl, passSp, passHint);

        passwordField = new PasswordField();
        passwordField.setPromptText("Create password");
        passBox.getChildren().addAll(passHeader, passwordField);

        // Confirm Password
        VBox confBox = new VBox(5);
        Label confLbl = new Label("Confirm Password");
        confLbl.getStyleClass().add("form-label");
        confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Repeat password");

        matchLabel = new Label();
        matchLabel.setVisible(false);
        matchLabel.setManaged(false);
        matchLabel.setStyle("-fx-font-size: 11px;");

        // Live check matching
        javafx.beans.value.ChangeListener<String> checkMatch = (obs, oldVal, newVal) -> {
            String p1 = passwordField.getText();
            String p2 = confirmPasswordField.getText();
            if (p2 == null || p2.isBlank()) {
                matchLabel.setVisible(false);
                matchLabel.setManaged(false);
            } else if (p1 != null && p1.equals(p2)) {
                matchLabel.setText("✓ Passwords match");
                matchLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #C8F169; -fx-font-weight: 700;");
                matchLabel.setVisible(true);
                matchLabel.setManaged(true);
            } else {
                matchLabel.setText("⚠ Passwords do not match");
                matchLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #F2A88A; -fx-font-weight: 700;");
                matchLabel.setVisible(true);
                matchLabel.setManaged(true);
            }
        };

        passwordField.textProperty().addListener(checkMatch);
        confirmPasswordField.textProperty().addListener(checkMatch);

        confBox.getChildren().addAll(confLbl, confirmPasswordField, matchLabel);

        btnSubmit = new Button("Activate Wallet Node");
        btnSubmit.setGraphic(Icons.arrowRight(Icons.NAVY, 15));
        btnSubmit.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        btnSubmit.getStyleClass().addAll("btn", "btn-lime", "btn-lg");
        btnSubmit.setMaxWidth(Double.MAX_VALUE);
        btnSubmit.setOnAction(e -> handleRegister());

        HBox signInLinkBox = new HBox(4);
        signInLinkBox.setAlignment(Pos.CENTER);
        signInLinkBox.setStyle("-fx-border-color: rgba(170, 181, 198, 0.16); -fx-border-width: 1 0 0 0; -fx-padding: 14 0 0 0;");

        Label alreadyLbl = new Label("Already activated?");
        alreadyLbl.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #AAB5C6;");

        Button linkSignIn = new Button("Sign in");
        linkSignIn.getStyleClass().addAll("btn", "btn-ghost", "btn-sm");
        linkSignIn.setStyle("-fx-font-weight: 800; -fx-text-fill: #C8F169; -fx-padding: 0;");
        linkSignIn.setOnAction(e -> nav.showView("login"));

        signInLinkBox.getChildren().addAll(alreadyLbl, linkSignIn);

        formSide.getChildren().addAll(formHeader, errorLabel, nameBox, emailBox, passBox, confBox, btnSubmit, signInLinkBox);

        splitPane.add(brandSide, 0, 0);
        splitPane.add(formSide, 1, 0);

        getChildren().add(splitPane);
    }

    private void handleRegister() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        String name = nameField.getText();
        String email = emailField.getText();
        String pass = passwordField.getText();
        String conf = confirmPasswordField.getText();

        if (name == null || name.isBlank()) {
            showError("Please provide your full legal name");
            return;
        }
        if (email == null || email.isBlank() || !email.contains("@")) {
            showError("Please provide a valid email address");
            return;
        }
        if (pass == null || pass.length() < 8) {
            showError("Password must be at least 8 characters long");
            return;
        }
        if (!pass.equals(conf)) {
            showError("Passwords do not match");
            return;
        }

        btnSubmit.setDisable(true);
        btnSubmit.setText("Creating Account...");

        AuthService.getInstance().register(name.trim(), email.trim().toLowerCase(), pass)
                .thenAccept(res -> Platform.runLater(() -> {
                    btnSubmit.setDisable(false);
                    btnSubmit.setText("Activate Wallet Node");
                    nav.showView("dashboard");
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        btnSubmit.setDisable(false);
                        btnSubmit.setText("Activate Wallet Node");
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
