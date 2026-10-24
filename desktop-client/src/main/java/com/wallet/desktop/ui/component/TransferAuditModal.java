package com.wallet.desktop.ui.component;

import com.wallet.desktop.model.Transfer;
import com.wallet.desktop.util.Formatters;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class TransferAuditModal extends StackPane {

    private final Transfer transfer;
    private final boolean isSender;
    private final Runnable onClose;

    public TransferAuditModal(Transfer transfer, boolean isSender, Runnable onClose) {
        this.transfer = transfer;
        this.isSender = isSender;
        this.onClose = onClose;

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
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.Node shieldIcon = Icons.shieldCheck(Icons.LIME, 20);

        Label title = new Label("Ledger Entry #" + transfer.getId());
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: 800; -fx-text-fill: #F5F1E8;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button();
        closeBtn.setGraphic(Icons.x(Icons.MUTED, 15));
        closeBtn.getStyleClass().addAll("btn", "btn-ghost", "btn-sm");
        closeBtn.setOnAction(e -> onClose.run());

        header.getChildren().addAll(shieldIcon, title, spacer, closeBtn);

        // Body Box
        VBox bodyBox = new VBox(12);
        bodyBox.setStyle("-fx-background-color: #101A2B; -fx-border-color: rgba(170, 181, 198, 0.16); -fx-border-width: 1; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 18px;");

        VBox amountBox = new VBox(4);
        amountBox.setAlignment(Pos.CENTER);
        Label amountLabel = new Label("COMMITTED AMOUNT");
        amountLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 9.5px; -fx-text-fill: #AAB5C6;");

        String amountText = (isSender ? "-" : "+") + Formatters.formatCurrency(transfer.getAmount(), transfer.getCurrency());
        Label amountVal = new Label(amountText);
        amountVal.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: " +
                (isSender ? "#F2A88A;" : "#C8F169;"));

        amountBox.getChildren().addAll(amountLabel, amountVal);

        VBox rows = new VBox(8);
        rows.getChildren().addAll(
                createDetailRow("Source Wallet:", "ID #" + transfer.getSenderWalletId() + (isSender ? " (You)" : "")),
                createDetailRow("Recipient Wallet:", "ID #" + transfer.getRecipientWalletId() + (!isSender ? " (You)" : "")),
                createDetailRow("Currency:", transfer.getCurrency()),
                createDetailRow("Settlement State:", transfer.getStatus()),
                createDetailRow("Committed Timestamp:", Formatters.formatDateTime(transfer.getCreatedAt()))
        );

        VBox keyBox = new VBox(4);
        keyBox.setStyle("-fx-border-color: rgba(170, 181, 198, 0.14); -fx-border-width: 1 0 0 0; -fx-padding: 8 0 0 0;");
        Label keyLabel = new Label("CLIENT IDEMPOTENCY KEY:");
        keyLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 9px; -fx-text-fill: #AAB5C6;");
        Label keyVal = new Label(transfer.getIdempotencyKey());
        keyVal.setWrapText(true);
        keyVal.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 10px; -fx-text-fill: #6B7C96;");
        keyBox.getChildren().addAll(keyLabel, keyVal);

        bodyBox.getChildren().addAll(amountBox, rows, keyBox);

        // Footer Actions
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        Button btnClose = new Button("Close Audit View");
        btnClose.getStyleClass().addAll("btn", "btn-secondary");
        btnClose.setOnAction(e -> onClose.run());
        footer.getChildren().add(btnClose);

        card.getChildren().addAll(header, bodyBox, footer);
        getChildren().add(card);
    }

    private HBox createDetailRow(String labelText, String valueText) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #AAB5C6;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label val = new Label(valueText);
        val.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px; -fx-text-fill: #F5F1E8; -fx-font-weight: 700;");
        row.getChildren().addAll(lbl, sp, val);
        return row;
    }
}
