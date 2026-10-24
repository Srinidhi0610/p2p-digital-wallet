package com.wallet.desktop.ui.component;

import com.wallet.desktop.util.Formatters;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;

public class ConfirmTransferModal extends StackPane {

    private final Long senderWalletId;
    private final Long recipientWalletId;
    private final BigDecimal amount;
    private final String currency;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    private Button btnConfirm;
    private Button btnCancel;

    public ConfirmTransferModal(Long senderWalletId, Long recipientWalletId, BigDecimal amount, String currency,
                                Runnable onConfirm, Runnable onCancel) {
        this.senderWalletId = senderWalletId;
        this.recipientWalletId = recipientWalletId;
        this.amount = amount;
        this.currency = currency;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;

        initUI();
    }

    private void initUI() {
        getStyleClass().add("modal-backdrop");
        setAlignment(Pos.CENTER);
        setOnMouseClicked(e -> {
            if (e.getTarget() == this) onCancel.run();
        });

        VBox card = new VBox(18);
        card.getStyleClass().add("modal-card");
        card.setMaxWidth(480);
        card.setPrefWidth(460);
        card.setOnMouseClicked(javafx.scene.input.MouseEvent::consume);

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane shieldBox = new StackPane();
        shieldBox.setStyle("-fx-background-color: rgba(200, 241, 105, 0.14); -fx-border-color: rgba(200, 241, 105, 0.35); -fx-border-width: 1; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-min-width: 34px; -fx-min-height: 34px;");
        shieldBox.getChildren().add(Icons.shieldCheck(Icons.LIME, 18));

        VBox titleBox = new VBox(1);
        Label title = new Label("Review & Settle");
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: 800; -fx-text-fill: #F5F1E8;");
        Label subtitle = new Label("P2P DOUBLE-ENTRY COMMIT");
        subtitle.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 9px; -fx-text-fill: #AAB5C6; -fx-font-weight: 700;");
        titleBox.getChildren().addAll(title, subtitle);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button closeBtn = new Button();
        closeBtn.setGraphic(Icons.x(Icons.MUTED, 15));
        closeBtn.getStyleClass().addAll("btn", "btn-ghost", "btn-sm");
        closeBtn.setOnAction(e -> onCancel.run());

        header.getChildren().addAll(shieldBox, titleBox, headerSpacer, closeBtn);

        // Flow box
        VBox flowBox = new VBox(12);
        flowBox.setStyle("-fx-background-color: #101A2B; -fx-border-color: rgba(170, 181, 198, 0.16); -fx-border-width: 1; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 16px;");

        HBox endpoints = new HBox();
        endpoints.setAlignment(Pos.CENTER);

        VBox sourceCol = new VBox(2);
        sourceCol.setAlignment(Pos.CENTER_LEFT);
        Label srcLabel = new Label("SOURCE WALLET");
        srcLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 9px; -fx-text-fill: #AAB5C6;");
        Label srcVal = new Label("ID #" + senderWalletId);
        srcVal.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #F5F1E8;");
        sourceCol.getChildren().addAll(srcLabel, srcVal);

        Region epSpacer1 = new Region();
        HBox.setHgrow(epSpacer1, Priority.ALWAYS);

        javafx.scene.Node arrow = Icons.arrowRight(Icons.LIME, 16);

        Region epSpacer2 = new Region();
        HBox.setHgrow(epSpacer2, Priority.ALWAYS);

        VBox targetCol = new VBox(2);
        targetCol.setAlignment(Pos.CENTER_RIGHT);
        Label tgtLabel = new Label("RECIPIENT WALLET");
        tgtLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 9px; -fx-text-fill: #AAB5C6;");
        Label tgtVal = new Label("ID #" + recipientWalletId);
        tgtVal.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #C8F169;");
        targetCol.getChildren().addAll(tgtLabel, tgtVal);

        endpoints.getChildren().addAll(sourceCol, epSpacer1, arrow, epSpacer2, targetCol);

        VBox amountBox = new VBox(3);
        amountBox.setAlignment(Pos.CENTER);
        amountBox.setStyle("-fx-border-color: rgba(170, 181, 198, 0.14); -fx-border-width: 1 0 0 0; -fx-padding: 10 0 0 0;");

        Label totalLabel = new Label("TOTAL SETTLEMENT AMOUNT");
        totalLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 9.5px; -fx-text-fill: #AAB5C6;");

        Label amountVal = new Label(Formatters.formatCurrency(amount, currency));
        amountVal.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: #F5F1E8;");

        amountBox.getChildren().addAll(totalLabel, amountVal);

        flowBox.getChildren().addAll(endpoints, amountBox);

        // Immutable notice
        Label notice = new Label("Immutable Ledger Notice: This operation applies pessimistic locks across both accounts and appends matching DEBIT and CREDIT records. Balance updates cannot be reversed.");
        notice.setWrapText(true);
        notice.setStyle("-fx-background-color: rgba(242, 168, 138, 0.1); -fx-border-color: rgba(242, 168, 138, 0.35); -fx-border-width: 1; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 10 12; -fx-font-size: 11.5px; -fx-text-fill: #F2A88A; -fx-line-spacing: 2px;");

        // Action buttons
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        btnCancel = new Button("Cancel");
        btnCancel.getStyleClass().addAll("btn", "btn-secondary");
        btnCancel.setOnAction(e -> onCancel.run());

        btnConfirm = new Button("Commit & Transfer");
        btnConfirm.getStyleClass().addAll("btn", "btn-lime");
        btnConfirm.setOnAction(e -> {
            btnConfirm.setDisable(true);
            btnCancel.setDisable(true);
            btnConfirm.setText("Settling Ledger...");
            onConfirm.run();
        });

        actions.getChildren().addAll(btnCancel, btnConfirm);

        card.getChildren().addAll(header, flowBox, notice, actions);
        getChildren().add(card);
    }
}
