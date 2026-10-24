package com.wallet.desktop.ui.component;

import com.wallet.desktop.model.Wallet;
import com.wallet.desktop.util.Formatters;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.util.function.Consumer;

public class ArchitecturalWalletCard extends StackPane {

    private final Wallet wallet;
    private final Consumer<Wallet> onSendHandler;
    private final Consumer<Wallet> onDepositHandler;

    public ArchitecturalWalletCard(Wallet wallet, Consumer<Wallet> onSendHandler, Consumer<Wallet> onDepositHandler) {
        this.wallet = wallet;
        this.onSendHandler = onSendHandler;
        this.onDepositHandler = onDepositHandler;

        initUI();
    }

    private void initUI() {
        getStyleClass().add("architectural-wallet-card");
        setPrefWidth(320);
        setMinHeight(185);

        // Watermark SVG path
        SVGPath watermark = new SVGPath();
        watermark.setContent("M10 110 C 40 10, 90 110, 130 10");
        watermark.setStroke(Icons.MUTED);
        watermark.setOpacity(0.08);
        watermark.setStrokeWidth(3);
        watermark.setFill(null);
        StackPane.setAlignment(watermark, Pos.TOP_RIGHT);
        StackPane.setMargin(watermark, new javafx.geometry.Insets(10, 10, 0, 0));

        VBox content = new VBox();
        content.setAlignment(Pos.TOP_LEFT);

        // Header: Currency Pill & Lock spec
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);

        Label currencyPill = new Label(wallet.getCurrency() + " / LEDGER");
        currencyPill.getStyleClass().add("currency-pill");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label lockSpec = new Label("ACTIVE ROW-LOCK");
        lockSpec.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 9.5px; -fx-text-fill: #AAB5C6; -fx-font-weight: 700;");

        header.getChildren().addAll(currencyPill, headerSpacer, lockSpec);

        // Body: Settled Balance
        VBox body = new VBox(4);
        body.setStyle("-fx-padding: 14 0 16 0;");

        Label balanceLabel = new Label("SETTLED BALANCE");
        balanceLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 10px; -fx-text-fill: #AAB5C6; -fx-font-weight: 700;");

        Label balanceValue = new Label(Formatters.formatCurrency(wallet.getBalance(), wallet.getCurrency()));
        balanceValue.getStyleClass().add("wallet-balance-num");

        body.getChildren().addAll(balanceLabel, balanceValue);

        // Footer: Wallet ID & Action buttons
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-border-color: rgba(170, 181, 198, 0.16); -fx-border-width: 1 0 0 0; -fx-padding: 12 0 0 0;");

        HBox idChip = new HBox(4);
        idChip.setAlignment(Pos.CENTER_LEFT);
        Label idLabel = new Label("WALLET #" + wallet.getId());
        idLabel.getStyleClass().add("wallet-id-chip");

        Button copyBtn = new Button();
        copyBtn.setGraphic(Icons.copy(Icons.MUTED, 12));
        copyBtn.getStyleClass().addAll("btn", "btn-ghost", "btn-sm");
        copyBtn.setStyle("-fx-padding: 2 4;");
        copyBtn.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent cc = new ClipboardContent();
            cc.putString(String.valueOf(wallet.getId()));
            clipboard.setContent(cc);

            copyBtn.setGraphic(Icons.check(Icons.LIME, 12));
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(ev -> copyBtn.setGraphic(Icons.copy(Icons.MUTED, 12)));
            pause.play();
        });

        idChip.getChildren().addAll(idLabel, copyBtn);

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        HBox actions = new HBox(6);
        actions.setAlignment(Pos.CENTER_RIGHT);

        if (onDepositHandler != null) {
            Button depositBtn = new Button("Add Funds");
            depositBtn.setGraphic(Icons.coins(Icons.IVORY, 13));
            depositBtn.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
            depositBtn.setOnAction(e -> onDepositHandler.accept(wallet));
            actions.getChildren().add(depositBtn);
        }

        Button sendBtn = new Button("Send");
        sendBtn.setGraphic(Icons.send(Icons.NAVY, 13));
        sendBtn.getStyleClass().addAll("btn", "btn-lime", "btn-sm");
        sendBtn.setOnAction(e -> {
            if (onSendHandler != null) onSendHandler.accept(wallet);
        });
        actions.getChildren().add(sendBtn);

        footer.getChildren().addAll(idChip, footerSpacer, actions);

        content.getChildren().addAll(header, body, footer);
        getChildren().addAll(watermark, content);
    }
}
