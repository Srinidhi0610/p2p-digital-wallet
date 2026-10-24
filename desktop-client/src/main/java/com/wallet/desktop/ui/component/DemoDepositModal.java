package com.wallet.desktop.ui.component;

import com.wallet.desktop.model.Wallet;
import com.wallet.desktop.service.WalletService;
import com.wallet.desktop.util.Formatters;
import com.wallet.desktop.util.UuidGenerator;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.function.Consumer;

public class DemoDepositModal extends StackPane {

    private final Wallet wallet;
    private final Runnable onClose;
    private final Consumer<Wallet> onSuccess;

    private TextField amountField;
    private Label errorLabel;
    private Button btnSubmit;
    private Button btnCancel;
    private VBox formContainer;
    private VBox successContainer;

    public DemoDepositModal(Wallet wallet, Runnable onClose, Consumer<Wallet> onSuccess) {
        this.wallet = wallet;
        this.onClose = onClose;
        this.onSuccess = onSuccess;

        initUI();
    }

    private void initUI() {
        getStyleClass().add("modal-backdrop");
        setAlignment(Pos.CENTER);
        setOnMouseClicked(e -> {
            if (e.getTarget() == this) onClose.run();
        });

        VBox card = new VBox(18);
        card.getStyleClass().add("modal-card");
        card.setMaxWidth(480);
        card.setPrefWidth(460);
        card.setOnMouseClicked(javafx.scene.input.MouseEvent::consume);

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane coinBox = new StackPane();
        coinBox.setStyle("-fx-background-color: rgba(200, 241, 105, 0.14); -fx-border-color: rgba(200, 241, 105, 0.35); -fx-border-width: 1; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-min-width: 36px; -fx-min-height: 36px;");
        coinBox.getChildren().add(Icons.coins(Icons.LIME, 20));

        VBox titleBox = new VBox(1);
        Label title = new Label("Add Demo Funds");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: 800; -fx-text-fill: #F5F1E8;");
        Label subtitle = new Label("WALLET #" + wallet.getId() + " (" + wallet.getCurrency() + ")");
        subtitle.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 10px; -fx-text-fill: #C8F169; -fx-font-weight: 700;");
        titleBox.getChildren().addAll(title, subtitle);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button closeBtn = new Button();
        closeBtn.setGraphic(Icons.x(Icons.MUTED, 15));
        closeBtn.getStyleClass().addAll("btn", "btn-ghost", "btn-sm");
        closeBtn.setOnAction(e -> onClose.run());

        header.getChildren().addAll(coinBox, titleBox, headerSpacer, closeBtn);

        // Error message
        errorLabel = new Label();
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setWrapText(true);
        errorLabel.getStyleClass().addAll("alert-box", "alert-danger");

        // Form Container
        formContainer = new VBox(14);

        // Current Balance spec
        HBox balBox = new HBox();
        balBox.setAlignment(Pos.CENTER_LEFT);
        balBox.setStyle("-fx-background-color: #101A2B; -fx-border-color: rgba(170, 181, 198, 0.16); -fx-border-width: 1; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 10 14;");
        Label balLbl = new Label("Current Settled Balance:");
        balLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #AAB5C6;");
        Region balSp = new Region();
        HBox.setHgrow(balSp, Priority.ALWAYS);
        Label balVal = new Label(Formatters.formatCurrency(wallet.getBalance(), wallet.getCurrency()));
        balVal.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #F5F1E8;");
        balBox.getChildren().addAll(balLbl, balSp, balVal);

        // Quick amount chips
        VBox quickBox = new VBox(6);
        Label quickLbl = new Label("Quick Amounts (" + wallet.getCurrency() + ")");
        quickLbl.getStyleClass().add("form-label");
        FlowPane chipPane = new FlowPane(8, 8);
        String[] presets = {"50.00", "100.00", "250.00", "500.00", "1000.00"};
        for (String p : presets) {
            Button chip = new Button("+" + p.split("\\.")[0]);
            chip.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
            chip.setStyle("-fx-font-family: 'Consolas', monospace;");
            chip.setOnAction(e -> amountField.setText(p));
            chipPane.getChildren().add(chip);
        }
        quickBox.getChildren().addAll(quickLbl, chipPane);

        // Custom Amount Field
        VBox fieldBox = new VBox(6);
        Label amtLbl = new Label("Deposit Amount (" + wallet.getCurrency() + ")");
        amtLbl.getStyleClass().add("form-label");
        amountField = new TextField("100.00");
        amountField.getStyleClass().add("font-mono");
        fieldBox.getChildren().addAll(amtLbl, amountField);

        // Dev disclaimer banner
        Label disclaimer = new Label("Development Sandbox Faucet: Liquidity is disbursed directly from the system test treasury node to test peer transfers. No actual bank rail or card charges are processed.");
        disclaimer.setWrapText(true);
        disclaimer.setStyle("-fx-background-color: rgba(16, 26, 43, 0.7); -fx-border-color: rgba(170, 181, 198, 0.16); -fx-border-width: 1; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 10 12; -fx-font-size: 11px; -fx-text-fill: #AAB5C6;");

        // Action buttons
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        btnCancel = new Button("Cancel");
        btnCancel.getStyleClass().addAll("btn", "btn-secondary");
        btnCancel.setOnAction(e -> onClose.run());

        btnSubmit = new Button("Disburse Demo Funds");
        btnSubmit.setGraphic(Icons.zap(Icons.NAVY, 14));
        btnSubmit.getStyleClass().addAll("btn", "btn-lime");
        btnSubmit.setOnAction(e -> handleDeposit());

        actions.getChildren().addAll(btnCancel, btnSubmit);

        formContainer.getChildren().addAll(balBox, quickBox, fieldBox, disclaimer, actions);

        // Success Container (Hidden initially)
        successContainer = new VBox(16);
        successContainer.setAlignment(Pos.CENTER);
        successContainer.setVisible(false);
        successContainer.setManaged(false);

        card.getChildren().addAll(header, errorLabel, formContainer, successContainer);
        getChildren().add(card);
    }

    private void handleDeposit() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        String text = amountField.getText();
        if (text == null || text.isBlank()) {
            showError("Please enter a valid deposit amount.");
            return;
        }

        BigDecimal amt;
        try {
            amt = new BigDecimal(text.trim());
            if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Deposit amount must be strictly greater than 0.");
                return;
            }
            if (amt.compareTo(new BigDecimal("100000.00")) > 0) {
                showError("Maximum demo deposit limit is 100,000.00 per request.");
                return;
            }
        } catch (Exception e) {
            showError("Invalid numeric amount format.");
            return;
        }

        btnSubmit.setDisable(true);
        btnCancel.setDisable(true);
        btnSubmit.setText("Disbursing...");

        String key = "demo-dep-" + wallet.getId() + "-" + System.currentTimeMillis();

        WalletService.getInstance().depositDemoFunds(wallet.getId(), amt, key)
                .thenAccept(updated -> Platform.runLater(() -> {
                    showSuccess(updated);
                    if (onSuccess != null) {
                        onSuccess.accept(updated);
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        btnSubmit.setDisable(false);
                        btnCancel.setDisable(false);
                        btnSubmit.setText("Disburse Demo Funds");
                        showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                    });
                    return null;
                });
    }

    private void showSuccess(Wallet updatedWallet) {
        formContainer.setVisible(false);
        formContainer.setManaged(false);

        VBox successBox = new VBox(10);
        successBox.setAlignment(Pos.CENTER);
        successBox.setStyle("-fx-background-color: #101A2B; -fx-border-color: rgba(200, 241, 105, 0.25); -fx-border-width: 1; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 20px;");

        javafx.scene.Node checkIcon = Icons.checkCircle(Icons.LIME, 36);

        Label heading = new Label("Demo Funds Credited!");
        heading.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #F5F1E8;");

        Label desc = new Label("The double-entry ledger transfer was committed to your wallet.");
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #AAB5C6;");

        Label newBal = new Label("New Balance: " + Formatters.formatCurrency(updatedWallet.getBalance(), updatedWallet.getCurrency()));
        newBal.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #C8F169;");

        successBox.getChildren().addAll(checkIcon, heading, desc, newBal);

        Button btnDone = new Button("Done");
        btnDone.getStyleClass().addAll("btn", "btn-lime");
        btnDone.setMaxWidth(Double.MAX_VALUE);
        btnDone.setOnAction(e -> onClose.run());

        successContainer.getChildren().clear();
        successContainer.getChildren().addAll(successBox, btnDone);
        successContainer.setVisible(true);
        successContainer.setManaged(true);
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
