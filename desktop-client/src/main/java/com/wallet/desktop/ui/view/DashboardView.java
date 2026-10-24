package com.wallet.desktop.ui.view;

import com.wallet.desktop.model.HealthResponse;
import com.wallet.desktop.model.Transfer;
import com.wallet.desktop.model.User;
import com.wallet.desktop.model.Wallet;
import com.wallet.desktop.service.AuthService;
import com.wallet.desktop.service.SessionManager;
import com.wallet.desktop.service.TransferService;
import com.wallet.desktop.service.WalletService;
import com.wallet.desktop.ui.NavigationManager;
import com.wallet.desktop.ui.component.ArchitecturalWalletCard;
import com.wallet.desktop.ui.component.DemoDepositModal;
import com.wallet.desktop.ui.component.Icons;
import com.wallet.desktop.util.Formatters;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;

import java.util.ArrayList;
import java.util.List;

public class DashboardView extends StackPane {

    private final NavigationManager nav;

    private List<Wallet> wallets = new ArrayList<>();
    private List<Transfer> recentTransfers = new ArrayList<>();
    private HealthResponse health;
    private String selectedCurrency = "USD";

    private Circle nodeStatusDot;
    private Label nodeStatusText;
    private Label peerIdLabel;
    private Label errorLabel;
    private Button btnSync;

    // Hero components
    private Label oversizedBalanceLabel;
    private FlowPane currencyChipsPane;
    private Button btnAddDemoFunds;

    // Wallets Container
    private FlowPane walletGridPane;
    private VBox emptyWalletsBox;

    // Recent Transactions Table Box
    private VBox recentTransactionsTable;
    private VBox emptyTransactionsBox;

    public DashboardView(NavigationManager nav) {
        this.nav = nav;
        initUI();
    }

    private void initUI() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");

        VBox content = new VBox(24);
        content.setMaxWidth(1100);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(24, 32, 48, 32));

        // --- Top Ledger Status & Node Identity ---
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        HBox nodePill = new HBox(6);
        nodePill.setAlignment(Pos.CENTER_LEFT);
        nodePill.setStyle("-fx-background-color: #19273A; -fx-border-color: rgba(170, 181, 198, 0.16); -fx-border-width: 1; -fx-border-radius: 16px; -fx-background-radius: 16px; -fx-padding: 4 10;");

        nodeStatusDot = new Circle(3.5);
        nodeStatusDot.setFill(Icons.LIME);

        nodeStatusText = new Label("SETTLEMENT NODE: ONLINE");
        nodeStatusText.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 10px; -fx-text-fill: #F5F1E8; -fx-font-weight: 700;");
        nodePill.getChildren().addAll(nodeStatusDot, nodeStatusText);

        peerIdLabel = new Label("PEER #0");
        peerIdLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px; -fx-text-fill: #AAB5C6;");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        btnSync = new Button("Sync State");
        btnSync.setGraphic(Icons.refresh(Icons.IVORY, 13));
        btnSync.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        btnSync.setOnAction(e -> loadData());

        topBar.getChildren().addAll(nodePill, peerIdLabel, topSpacer, btnSync);

        // Error banner
        errorLabel = new Label();
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setWrapText(true);
        errorLabel.getStyleClass().addAll("alert-box", "alert-danger");

        // --- Hero: Motion Banner ---
        StackPane heroBanner = new StackPane();
        heroBanner.getStyleClass().add("motion-banner");

        // Abstract decorative curve watermark
        SVGPath heroWatermark = new SVGPath();
        heroWatermark.setContent("M20 120 C 100 20, 200 160, 320 20");
        heroWatermark.setStroke(Icons.LIME);
        heroWatermark.setStrokeWidth(3);
        heroWatermark.setOpacity(0.18);
        heroWatermark.setFill(null);
        StackPane.setAlignment(heroWatermark, Pos.TOP_RIGHT);
        StackPane.setMargin(heroWatermark, new Insets(10, 10, 0, 0));

        VBox heroContent = new VBox(14);
        heroContent.setAlignment(Pos.TOP_LEFT);

        HBox balanceLabelBox = new HBox(6);
        balanceLabelBox.setAlignment(Pos.CENTER_LEFT);
        balanceLabelBox.getChildren().addAll(
                Icons.zap(Icons.LIME, 14),
                new Label("AUTHORITATIVE SETTLED LIQUIDITY") {{
                    getStyleClass().add("oversized-balance-label");
                }}
        );

        oversizedBalanceLabel = new Label("$0.00");
        oversizedBalanceLabel.getStyleClass().add("oversized-balance-value");

        currencyChipsPane = new FlowPane(6, 6);

        HBox heroActions = new HBox(12);
        heroActions.setAlignment(Pos.CENTER_LEFT);

        Button btnSend = new Button("Send Money");
        btnSend.setGraphic(Icons.send(Icons.NAVY, 15));
        btnSend.getStyleClass().addAll("btn", "btn-lime", "btn-lg");
        btnSend.setOnAction(e -> nav.showView("send"));

        btnAddDemoFunds = new Button("Add Demo Funds");
        btnAddDemoFunds.setGraphic(Icons.coins(Icons.LIME, 15));
        btnAddDemoFunds.getStyleClass().addAll("btn", "btn-secondary", "btn-lg");
        btnAddDemoFunds.setOnAction(e -> {
            Wallet target = getActiveWallet();
            if (target != null) {
                openDemoDepositModal(target);
            }
        });

        Button btnOpenAccount = new Button("Open Currency Account");
        btnOpenAccount.setGraphic(Icons.plus(Icons.IVORY, 14));
        btnOpenAccount.getStyleClass().addAll("btn", "btn-secondary");
        btnOpenAccount.setOnAction(e -> nav.showView("accounts"));

        Button btnLedger = new Button("View Ledger");
        btnLedger.setGraphic(Icons.history(Icons.MUTED, 14));
        btnLedger.getStyleClass().addAll("btn", "btn-ghost");
        btnLedger.setOnAction(e -> nav.showView("ledger"));

        heroActions.getChildren().addAll(btnSend, btnAddDemoFunds, btnOpenAccount, btnLedger);

        heroContent.getChildren().addAll(balanceLabelBox, oversizedBalanceLabel, currencyChipsPane, heroActions);
        heroBanner.getChildren().addAll(heroWatermark, heroContent);

        // --- Wallets Section ---
        VBox walletsSection = new VBox(14);
        HBox walletsHeader = new HBox();
        walletsHeader.setAlignment(Pos.CENTER_LEFT);

        VBox whText = new VBox(2);
        Label whTitle = new Label("Wallet Nodes");
        whTitle.getStyleClass().add("surface-title");
        Label whSubtitle = new Label("Pessimistic row-locked ledger balances by currency");
        whSubtitle.getStyleClass().add("surface-subtitle");
        whText.getChildren().addAll(whTitle, whSubtitle);

        Region whSpacer = new Region();
        HBox.setHgrow(whSpacer, Priority.ALWAYS);

        Button btnManageAll = new Button("Manage All");
        btnManageAll.getStyleClass().addAll("btn", "btn-ghost", "btn-sm");
        btnManageAll.setOnAction(e -> nav.showView("accounts"));

        walletsHeader.getChildren().addAll(whText, whSpacer, btnManageAll);

        walletGridPane = new FlowPane(16, 16);

        emptyWalletsBox = new VBox(12);
        emptyWalletsBox.setAlignment(Pos.CENTER);
        emptyWalletsBox.getStyleClass().add("surface");
        emptyWalletsBox.setStyle("-fx-border-style: dashed; -fx-padding: 36px 20px;");

        Label ewTitle = new Label("No Active Ledger Accounts");
        ewTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #F5F1E8;");

        Label ewDesc = new Label("Your node currently has zero currency wallets. Open a primary USD wallet to begin peer-to-peer liquidity transfers.");
        ewDesc.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #AAB5C6;");

        Button btnCreatePrimary = new Button("Open Primary USD Wallet");
        btnCreatePrimary.setGraphic(Icons.plus(Icons.NAVY, 14));
        btnCreatePrimary.getStyleClass().addAll("btn", "btn-lime");
        btnCreatePrimary.setOnAction(e -> createPrimaryWallet("USD"));

        emptyWalletsBox.getChildren().addAll(ewTitle, ewDesc, btnCreatePrimary);

        walletsSection.getChildren().addAll(walletsHeader, walletGridPane, emptyWalletsBox);

        // --- Recent Transactions Section ---
        VBox txSection = new VBox(0);
        txSection.getStyleClass().add("surface");
        txSection.setStyle("-fx-padding: 0;");

        HBox txHeader = new HBox();
        txHeader.setAlignment(Pos.CENTER_LEFT);
        txHeader.setStyle("-fx-padding: 16px 20px 12px 20px;");

        VBox txhText = new VBox(2);
        Label txhTitle = new Label("Recent Ledger Entries");
        txhTitle.getStyleClass().add("surface-title");
        Label txhSubtitle = new Label("Guaranteed double-entry debits and credits");
        txhSubtitle.getStyleClass().add("surface-subtitle");
        txhText.getChildren().addAll(txhTitle, txhSubtitle);

        Region txhSpacer = new Region();
        HBox.setHgrow(txhSpacer, Priority.ALWAYS);

        Button btnFullLedger = new Button("Full Ledger");
        btnFullLedger.getStyleClass().addAll("btn", "btn-ghost", "btn-sm");
        btnFullLedger.setOnAction(e -> nav.showView("ledger"));

        txHeader.getChildren().addAll(txhText, txhSpacer, btnFullLedger);

        recentTransactionsTable = new VBox(0);

        emptyTransactionsBox = new VBox(8);
        emptyTransactionsBox.setAlignment(Pos.CENTER);
        emptyTransactionsBox.setStyle("-fx-padding: 36px 20px;");
        Label etMsg = new Label("No ledger transfers recorded yet.");
        etMsg.setStyle("-fx-text-fill: #AAB5C6; -fx-font-size: 13px;");
        emptyTransactionsBox.getChildren().addAll(Icons.history(Icons.MUTED, 28), etMsg);

        txSection.getChildren().addAll(txHeader, recentTransactionsTable, emptyTransactionsBox);

        content.getChildren().addAll(topBar, errorLabel, heroBanner, walletsSection, txSection);
        scrollPane.setContent(content);

        getChildren().add(scrollPane);
    }

    public void loadData() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        btnSync.setDisable(true);

        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            peerIdLabel.setText("PEER #" + currentUser.getId());
        }

        WalletService.getInstance().getWallets()
                .thenAccept(wList -> {
                    this.wallets = (wList != null) ? wList : new ArrayList<>();
                    Platform.runLater(this::renderWallets);
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
                    return null;
                });

        TransferService.getInstance().getTransfers(0, 6)
                .thenAccept(page -> {
                    this.recentTransfers = (page != null && page.getContent() != null) ? page.getContent() : new ArrayList<>();
                    Platform.runLater(this::renderRecentTransfers);
                })
                .exceptionally(ex -> null);

        AuthService.getInstance().checkHealth()
                .thenAccept(h -> {
                    this.health = h;
                    Platform.runLater(this::renderHealth);
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        nodeStatusDot.setFill(Icons.CORAL);
                        nodeStatusText.setText("SETTLEMENT NODE: CONNECTING");
                    });
                    return null;
                });

        Platform.runLater(() -> btnSync.setDisable(false));
    }

    private void renderHealth() {
        boolean isUp = health != null && "UP".equalsIgnoreCase(health.getStatus());
        nodeStatusDot.setFill(isUp ? Icons.LIME : Icons.CORAL);
        nodeStatusText.setText("SETTLEMENT NODE: " + (isUp ? "ONLINE" : "CONNECTING"));
    }

    private void renderWallets() {
        if (wallets.isEmpty()) {
            walletGridPane.setVisible(false);
            walletGridPane.setManaged(false);
            emptyWalletsBox.setVisible(true);
            emptyWalletsBox.setManaged(true);

            oversizedBalanceLabel.setText("$0.00");
            currencyChipsPane.getChildren().clear();
            btnAddDemoFunds.setVisible(false);
            btnAddDemoFunds.setManaged(false);
            return;
        }

        emptyWalletsBox.setVisible(false);
        emptyWalletsBox.setManaged(false);
        walletGridPane.setVisible(true);
        walletGridPane.setManaged(true);
        walletGridPane.getChildren().clear();

        boolean hasSelected = wallets.stream().anyMatch(w -> w.getCurrency().equalsIgnoreCase(selectedCurrency));
        if (!hasSelected) {
            selectedCurrency = wallets.get(0).getCurrency();
        }

        Wallet active = getActiveWallet();
        if (active != null) {
            oversizedBalanceLabel.setText(Formatters.formatCurrency(active.getBalance(), active.getCurrency()));
            btnAddDemoFunds.setVisible(true);
            btnAddDemoFunds.setManaged(true);
        }

        // Render Currency Chips
        currencyChipsPane.getChildren().clear();
        if (wallets.size() > 1) {
            for (Wallet w : wallets) {
                Button chip = new Button(w.getCurrency() + " (" + Formatters.formatCurrency(w.getBalance(), w.getCurrency()) + ")");
                chip.getStyleClass().add("currency-chip");
                if (w.getCurrency().equalsIgnoreCase(selectedCurrency)) {
                    chip.getStyleClass().add("currency-chip-active");
                }
                chip.setOnAction(e -> {
                    selectedCurrency = w.getCurrency();
                    renderWallets();
                });
                currencyChipsPane.getChildren().add(chip);
            }
        }

        // Render Wallet Cards
        for (Wallet w : wallets) {
            ArchitecturalWalletCard card = new ArchitecturalWalletCard(
                    w,
                    walletToSend -> nav.showSendWithWallet(walletToSend.getId(), walletToSend.getCurrency()),
                    this::openDemoDepositModal
            );
            walletGridPane.getChildren().add(card);
        }
    }

    private void renderRecentTransfers() {
        if (recentTransfers.isEmpty()) {
            recentTransactionsTable.setVisible(false);
            recentTransactionsTable.setManaged(false);
            emptyTransactionsBox.setVisible(true);
            emptyTransactionsBox.setManaged(true);
            return;
        }

        emptyTransactionsBox.setVisible(false);
        emptyTransactionsBox.setManaged(false);
        recentTransactionsTable.setVisible(true);
        recentTransactionsTable.setManaged(true);
        recentTransactionsTable.getChildren().clear();

        // Table Header Row
        HBox th = new HBox();
        th.setStyle("-fx-background-color: rgba(16, 26, 43, 0.7); -fx-border-color: rgba(170, 181, 198, 0.16); -fx-border-width: 0 0 1 0; -fx-padding: 8 16;");
        th.getChildren().addAll(
                createColHeader("FLOW", 130),
                createColHeader("AMOUNT", 140),
                createColHeader("COUNTERPARTY", 200),
                createColHeader("SETTLEMENT", 120),
                createColHeader("TIMESTAMP", 180)
        );
        recentTransactionsTable.getChildren().add(th);

        // Data Rows
        for (Transfer tx : recentTransfers) {
            boolean isSender = isUserWallet(tx.getSenderWalletId());

            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-border-color: rgba(170, 181, 198, 0.1); -fx-border-width: 0 0 1 0; -fx-padding: 10 16;");

            // Flow Badge
            HBox flowCol = new HBox(4);
            flowCol.setAlignment(Pos.CENTER_LEFT);
            flowCol.setPrefWidth(130);
            Label badge = new Label(isSender ? "Debit / Sent" : "Credit / Recv");
            badge.setGraphic(isSender ? Icons.arrowUpRight(Icons.CORAL, 12) : Icons.arrowDownLeft(Icons.LIME, 12));
            badge.getStyleClass().addAll("flow-badge", isSender ? "flow-outgoing" : "flow-incoming");
            flowCol.getChildren().add(badge);

            // Amount
            HBox amountCol = new HBox();
            amountCol.setAlignment(Pos.CENTER_LEFT);
            amountCol.setPrefWidth(140);
            String sign = isSender ? "-" : "+";
            Label amtLabel = new Label(sign + Formatters.formatCurrency(tx.getAmount(), tx.getCurrency()));
            amtLabel.getStyleClass().add(isSender ? "amount-outgoing" : "amount-incoming");
            amountCol.getChildren().add(amtLabel);

            // Counterparty
            HBox cpCol = new HBox();
            cpCol.setAlignment(Pos.CENTER_LEFT);
            cpCol.setPrefWidth(200);
            String cpText = isSender ? "To Wallet #" + tx.getRecipientWalletId() : "From Wallet #" + tx.getSenderWalletId();
            Label cpLabel = new Label(cpText);
            cpLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px; -fx-text-fill: #F5F1E8;");
            cpCol.getChildren().add(cpLabel);

            // Settlement
            HBox statusCol = new HBox();
            statusCol.setAlignment(Pos.CENTER_LEFT);
            statusCol.setPrefWidth(120);
            Label stLabel = new Label(tx.getStatus());
            stLabel.setGraphic(Icons.shieldCheck(Icons.LIME, 11));
            stLabel.getStyleClass().add("status-pill");
            statusCol.getChildren().add(stLabel);

            // Timestamp
            HBox timeCol = new HBox();
            timeCol.setAlignment(Pos.CENTER_LEFT);
            timeCol.setPrefWidth(180);
            Label timeLabel = new Label(Formatters.formatDateTime(tx.getCreatedAt()));
            timeLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px; -fx-text-fill: #AAB5C6;");
            timeCol.getChildren().add(timeLabel);

            row.getChildren().addAll(flowCol, amountCol, cpCol, statusCol, timeCol);
            recentTransactionsTable.getChildren().add(row);
        }
    }

    private Label createColHeader(String text, double width) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #AAB5C6;");
        lbl.setPrefWidth(width);
        return lbl;
    }

    private boolean isUserWallet(Long walletId) {
        if (walletId == null) return false;
        return wallets.stream().anyMatch(w -> w.getId().equals(walletId));
    }

    private Wallet getActiveWallet() {
        if (wallets.isEmpty()) return null;
        for (Wallet w : wallets) {
            if (w.getCurrency().equalsIgnoreCase(selectedCurrency)) return w;
        }
        return wallets.get(0);
    }

    private void createPrimaryWallet(String currency) {
        WalletService.getInstance().createWallet(currency)
                .thenAccept(w -> Platform.runLater(this::loadData))
                .exceptionally(ex -> {
                    Platform.runLater(() -> showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
                    return null;
                });
    }

    private void openDemoDepositModal(Wallet wallet) {
        DemoDepositModal modal = new DemoDepositModal(
                wallet,
                () -> nav.closeModal(),
                updatedWallet -> loadData()
        );
        nav.openModal(modal);
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
