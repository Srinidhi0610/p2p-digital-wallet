package com.wallet.desktop.ui.view;

import com.wallet.desktop.model.Transfer;
import com.wallet.desktop.model.Wallet;
import com.wallet.desktop.service.TransferService;
import com.wallet.desktop.service.WalletService;
import com.wallet.desktop.ui.NavigationManager;
import com.wallet.desktop.ui.component.ConfirmTransferModal;
import com.wallet.desktop.ui.component.Icons;
import com.wallet.desktop.util.Formatters;
import com.wallet.desktop.util.UuidGenerator;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SendMoneyView extends StackPane {

    private final NavigationManager nav;

    private List<Wallet> wallets = new ArrayList<>();
    private Long preselectedSenderId = null;

    private VBox formCard;
    private VBox receiptCard;

    private ComboBox<WalletItem> sourceWalletCombo;
    private Label settledHintLabel;
    private TextField recipientIdField;
    private TextField amountField;
    private Label idempotencyKeyLabel;
    private String currentIdempotencyKey;
    private Label errorLabel;
    private Button btnSubmit;

    public SendMoneyView(NavigationManager nav) {
        this.nav = nav;
        initUI();
    }

    public static class WalletItem {
        public final Wallet wallet;
        public WalletItem(Wallet wallet) { this.wallet = wallet; }
        @Override
        public String toString() {
            return "WALLET #" + wallet.getId() + " — " + wallet.getCurrency() +
                    " (Balance: " + Formatters.formatCurrency(wallet.getBalance(), wallet.getCurrency()) + ")";
        }
    }

    private void initUI() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");

        VBox content = new VBox(20);
        content.setMaxWidth(680);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(24, 20, 48, 20));

        // Header
        VBox header = new VBox(4);
        header.setAlignment(Pos.TOP_LEFT);

        Label badge = new Label("REAL-TIME P2P TRANSIT");
        badge.setGraphic(Icons.zap(Icons.LIME, 13));
        badge.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 10px; -fx-text-fill: #C8F169; -fx-font-weight: 700;");

        Label title = new Label("Send Liquidity");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: #F5F1E8;");

        Label subtitle = new Label("Move funds instantly to any registered peer wallet via atomic double-entry settlement");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #AAB5C6;");

        header.getChildren().addAll(badge, title, subtitle);

        errorLabel = new Label();
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setWrapText(true);
        errorLabel.getStyleClass().addAll("alert-box", "alert-danger");

        // Form Card
        formCard = new VBox(18);
        formCard.getStyleClass().add("surface");

        // Source Wallet
        VBox srcBox = new VBox(6);
        HBox srcHeader = new HBox(6);
        srcHeader.setAlignment(Pos.CENTER_LEFT);
        Label srcLbl = new Label("Source Wallet");
        srcLbl.getStyleClass().add("form-label");
        Region srcSp = new Region();
        HBox.setHgrow(srcSp, Priority.ALWAYS);
        settledHintLabel = new Label();
        settledHintLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px; -fx-text-fill: #C8F169; -fx-font-weight: 700;");
        srcHeader.getChildren().addAll(srcLbl, srcSp, settledHintLabel);

        sourceWalletCombo = new ComboBox<>();
        sourceWalletCombo.setMaxWidth(Double.MAX_VALUE);
        sourceWalletCombo.setOnAction(e -> updateSelectedWalletHint());
        srcBox.getChildren().addAll(srcHeader, sourceWalletCombo);

        // Recipient ID
        VBox rBox = new VBox(6);
        HBox rHeader = new HBox(6);
        rHeader.setAlignment(Pos.CENTER_LEFT);
        Label rLbl = new Label("Recipient Wallet ID");
        rLbl.getStyleClass().add("form-label");
        Region rSp = new Region();
        HBox.setHgrow(rSp, Priority.ALWAYS);
        Label rHint = new Label("Numeric account identifier");
        rHint.getStyleClass().add("form-label-hint");
        rHeader.getChildren().addAll(rLbl, rSp, rHint);

        recipientIdField = new TextField();
        recipientIdField.setPromptText("e.g. 2");
        recipientIdField.getStyleClass().add("font-mono");
        rBox.getChildren().addAll(rHeader, recipientIdField);

        // Amount
        VBox amtBox = new VBox(6);
        HBox amtHeader = new HBox(6);
        amtHeader.setAlignment(Pos.CENTER_LEFT);
        Label amtLbl = new Label("Transfer Amount");
        amtLbl.getStyleClass().add("form-label");
        Region amtSp = new Region();
        HBox.setHgrow(amtSp, Priority.ALWAYS);
        Label amtHint = new Label("Max 4 decimal places");
        amtHint.getStyleClass().add("form-label-hint");
        amtHeader.getChildren().addAll(amtLbl, amtSp, amtHint);

        amountField = new TextField();
        amountField.setPromptText("0.0000");
        amountField.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 17px; -fx-font-weight: 700;");

        // Quick amount presets
        FlowPane quickPane = new FlowPane(6, 6);
        String[] presets = {"10", "50", "100", "250"};
        for (String p : presets) {
            Button qb = new Button("+" + p);
            qb.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
            qb.setStyle("-fx-font-family: 'Consolas', monospace;");
            qb.setOnAction(e -> amountField.setText(p + ".0000"));
            quickPane.getChildren().add(qb);
        }

        Button maxBtn = new Button("MAX");
        maxBtn.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        maxBtn.setStyle("-fx-font-family: 'Consolas', monospace; -fx-text-fill: #C8F169; -fx-border-color: rgba(200, 241, 105, 0.4);");
        maxBtn.setOnAction(e -> {
            Wallet sw = getSelectedWallet();
            if (sw != null && sw.getBalance() != null) {
                amountField.setText(sw.getBalance().toPlainString());
            }
        });
        quickPane.getChildren().add(maxBtn);

        amtBox.getChildren().addAll(amtHeader, amountField, quickPane);

        // Idempotency Key Box
        VBox keyBox = new VBox(6);
        keyBox.setStyle("-fx-background-color: #101A2B; -fx-border-color: rgba(170, 181, 198, 0.16); -fx-border-width: 1; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 10 14;");

        HBox keyHeader = new HBox(6);
        keyHeader.setAlignment(Pos.CENTER_LEFT);
        keyHeader.getChildren().addAll(
                Icons.keyRound(Icons.LIME, 13),
                new Label("Idempotency Hash (Replay Guard):") {{
                    setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px; -fx-text-fill: #AAB5C6;");
                }}
        );
        Region keySp = new Region();
        HBox.setHgrow(keySp, Priority.ALWAYS);

        Button regenBtn = new Button("Regenerate");
        regenBtn.getStyleClass().addAll("btn", "btn-ghost", "btn-sm");
        regenBtn.setStyle("-fx-font-family: 'Consolas', monospace; -fx-text-fill: #C8F169; -fx-padding: 0;");
        regenBtn.setOnAction(e -> regenerateKey());
        keyHeader.getChildren().addAll(keySp, regenBtn);

        idempotencyKeyLabel = new Label();
        idempotencyKeyLabel.setWrapText(true);
        idempotencyKeyLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 10.5px; -fx-text-fill: #6B7C96;");
        regenerateKey();

        keyBox.getChildren().addAll(keyHeader, idempotencyKeyLabel);

        // Submit Button
        btnSubmit = new Button("Review & Commit Transfer");
        btnSubmit.setGraphic(Icons.send(Icons.NAVY, 15));
        btnSubmit.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        btnSubmit.getStyleClass().addAll("btn", "btn-lime", "btn-lg");
        btnSubmit.setMaxWidth(Double.MAX_VALUE);
        btnSubmit.setOnAction(e -> handleReviewAndCommit());

        formCard.getChildren().addAll(srcBox, rBox, amtBox, keyBox, btnSubmit);

        // Verified Settlement Receipt Card (Hidden initially)
        receiptCard = new VBox(18);
        receiptCard.getStyleClass().add("surface");
        receiptCard.setStyle("-fx-border-color: rgba(200, 241, 105, 0.4);");
        receiptCard.setAlignment(Pos.CENTER);
        receiptCard.setVisible(false);
        receiptCard.setManaged(false);

        content.getChildren().addAll(header, errorLabel, formCard, receiptCard);
        scrollPane.setContent(content);

        getChildren().add(scrollPane);
    }

    public void setPreselectedSenderId(Long senderId) {
        this.preselectedSenderId = senderId;
    }

    public void loadData() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        formCard.setVisible(true);
        formCard.setManaged(true);
        receiptCard.setVisible(false);
        receiptCard.setManaged(false);

        WalletService.getInstance().getWallets()
                .thenAccept(wList -> {
                    this.wallets = (wList != null) ? wList : new ArrayList<>();
                    Platform.runLater(this::populateWallets);
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
                    return null;
                });
    }

    private void populateWallets() {
        sourceWalletCombo.getItems().clear();
        Wallet selectedItem = null;

        for (Wallet w : wallets) {
            WalletItem item = new WalletItem(w);
            sourceWalletCombo.getItems().add(item);
            if (preselectedSenderId != null && w.getId().equals(preselectedSenderId)) {
                selectedItem = w;
            }
        }

        if (selectedItem != null) {
            for (WalletItem item : sourceWalletCombo.getItems()) {
                if (item.wallet.getId().equals(selectedItem.getId())) {
                    sourceWalletCombo.setValue(item);
                    break;
                }
            }
        } else if (!sourceWalletCombo.getItems().isEmpty()) {
            sourceWalletCombo.setValue(sourceWalletCombo.getItems().get(0));
        }

        updateSelectedWalletHint();
    }

    private void updateSelectedWalletHint() {
        Wallet w = getSelectedWallet();
        if (w != null) {
            settledHintLabel.setText("Settled: " + Formatters.formatCurrency(w.getBalance(), w.getCurrency()));
        } else {
            settledHintLabel.setText("");
        }
    }

    private Wallet getSelectedWallet() {
        WalletItem item = sourceWalletCombo.getValue();
        return item != null ? item.wallet : null;
    }

    private void regenerateKey() {
        this.currentIdempotencyKey = UuidGenerator.generate();
        idempotencyKeyLabel.setText(currentIdempotencyKey);
    }

    private void handleReviewAndCommit() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        Wallet source = getSelectedWallet();
        if (source == null) {
            showError("Please select an active source wallet");
            return;
        }

        String recipientText = recipientIdField.getText();
        if (recipientText == null || recipientText.isBlank()) {
            showError("Enter a valid numeric recipient wallet ID");
            return;
        }

        Long recipientId;
        try {
            recipientId = Long.parseLong(recipientText.trim());
            if (recipientId <= 0) throw new IllegalArgumentException();
        } catch (Exception e) {
            showError("Enter a valid numeric recipient wallet ID");
            return;
        }

        if (recipientId.equals(source.getId())) {
            showError("Self-transfer is prohibited: Source and recipient wallets must be distinct.");
            return;
        }

        String amountText = amountField.getText();
        if (amountText == null || amountText.isBlank()) {
            showError("Transfer amount must be strictly greater than 0.0000");
            return;
        }

        BigDecimal amt;
        try {
            amt = new BigDecimal(amountText.trim());
            if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Transfer amount must be strictly greater than 0.0000");
                return;
            }
        } catch (Exception e) {
            showError("Transfer amount must be a valid number");
            return;
        }

        if (amountText.contains(".")) {
            String decimals = amountText.substring(amountText.indexOf('.') + 1);
            if (decimals.length() > 4) {
                showError("Transfer amount cannot exceed 4 decimal places of precision");
                return;
            }
        }

        if (source.getBalance() != null && amt.compareTo(source.getBalance()) > 0) {
            showError("Insufficient settled liquidity: Requested " +
                    Formatters.formatCurrency(amt, source.getCurrency()) +
                    " exceeds available balance of " +
                    Formatters.formatCurrency(source.getBalance(), source.getCurrency()));
            return;
        }

        // Open Confirmation Modal
        ConfirmTransferModal modal = new ConfirmTransferModal(
                source.getId(),
                recipientId,
                amt,
                source.getCurrency(),
                () -> executeTransfer(source.getId(), recipientId, amt, source.getCurrency()),
                () -> nav.closeModal()
        );
        nav.openModal(modal);
    }

    private void executeTransfer(Long senderId, Long recipientId, BigDecimal amount, String currency) {
        TransferService.getInstance().createTransfer(senderId, recipientId, amount, currency, currentIdempotencyKey)
                .thenAccept(completed -> Platform.runLater(() -> {
                    nav.closeModal();
                    showVerifiedReceipt(completed);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        nav.closeModal();
                        showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                    });
                    return null;
                });
    }

    private void showVerifiedReceipt(Transfer transfer) {
        formCard.setVisible(false);
        formCard.setManaged(false);

        receiptCard.getChildren().clear();

        StackPane checkCircle = new StackPane();
        checkCircle.setStyle("-fx-background-color: rgba(200, 241, 105, 0.14); -fx-border-color: rgba(200, 241, 105, 0.35); -fx-border-width: 1; -fx-border-radius: 32px; -fx-background-radius: 32px; -fx-min-width: 64px; -fx-min-height: 64px;");
        checkCircle.getChildren().add(Icons.checkCircle(Icons.LIME, 36));

        Label tag = new Label("DOUBLE-ENTRY COMMIT CONFIRMED");
        tag.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 10px; -fx-text-fill: #C8F169; -fx-font-weight: 700;");

        Label heading = new Label("Transfer Settled");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: #F5F1E8;");

        Label desc = new Label("Funds moved atomically across locked ledger partitions");
        desc.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #AAB5C6;");

        Label amountVal = new Label(Formatters.formatCurrency(transfer.getAmount(), transfer.getCurrency()));
        amountVal.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 38px; -fx-font-weight: 900; -fx-text-fill: #C8F169;");

        VBox detailsBox = new VBox(8);
        detailsBox.setStyle("-fx-background-color: #101A2B; -fx-border-color: rgba(170, 181, 198, 0.16); -fx-border-width: 1; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 16px;");

        detailsBox.getChildren().addAll(
                createReceiptRow("Ledger Transfer ID:", "#" + transfer.getId()),
                createReceiptRow("Source Account:", "Wallet #" + transfer.getSenderWalletId()),
                createReceiptRow("Recipient Account:", "Wallet #" + transfer.getRecipientWalletId()),
                createReceiptRow("Status:", transfer.getStatus()),
                createReceiptRow("Committed Timestamp:", Formatters.formatDateTime(transfer.getCreatedAt())),
                createReceiptRow("Cryptographic Key:", transfer.getIdempotencyKey().substring(0, Math.min(22, transfer.getIdempotencyKey().length())) + "...")
        );

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);

        Button btnSendAnother = new Button("Send Another");
        btnSendAnother.setGraphic(Icons.refresh(Icons.NAVY, 13));
        btnSendAnother.getStyleClass().addAll("btn", "btn-lime");
        btnSendAnother.setOnAction(e -> resetForm());

        Button btnInspectLedger = new Button("Inspect Ledger");
        btnInspectLedger.setGraphic(Icons.history(Icons.IVORY, 13));
        btnInspectLedger.getStyleClass().addAll("btn", "btn-secondary");
        btnInspectLedger.setOnAction(e -> nav.showView("ledger"));

        Button btnHome = new Button("Return Home");
        btnHome.getStyleClass().addAll("btn", "btn-ghost");
        btnHome.setOnAction(e -> nav.showView("dashboard"));

        actions.getChildren().addAll(btnSendAnother, btnInspectLedger, btnHome);

        receiptCard.getChildren().addAll(checkCircle, tag, heading, desc, amountVal, detailsBox, actions);
        receiptCard.setVisible(true);
        receiptCard.setManaged(true);
    }

    private HBox createReceiptRow(String label, String value) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #AAB5C6;");
        Region s = new Region();
        HBox.setHgrow(s, Priority.ALWAYS);
        Label v = new Label(value);
        v.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #F5F1E8;");
        row.getChildren().addAll(l, s, v);
        return row;
    }

    private void resetForm() {
        amountField.clear();
        recipientIdField.clear();
        regenerateKey();
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        receiptCard.setVisible(false);
        receiptCard.setManaged(false);
        formCard.setVisible(true);
        formCard.setManaged(true);
        loadData();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
