package com.wallet.desktop.ui.view;

import com.wallet.desktop.model.Transfer;
import com.wallet.desktop.model.Wallet;
import com.wallet.desktop.service.TransferService;
import com.wallet.desktop.service.WalletService;
import com.wallet.desktop.ui.NavigationManager;
import com.wallet.desktop.ui.component.Icons;
import com.wallet.desktop.ui.component.PaginationControl;
import com.wallet.desktop.ui.component.TransferAuditModal;
import com.wallet.desktop.util.Formatters;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LedgerView extends StackPane {

    private final NavigationManager nav;

    private List<Transfer> allTransfers = new ArrayList<>();
    private List<Wallet> userWallets = new ArrayList<>();

    private int currentPage = 0;
    private int totalPages = 0;
    private long totalElements = 0;
    private final int pageSize = 15;

    private enum FilterDirection { ALL, INCOMING, OUTGOING }
    private FilterDirection currentFilter = FilterDirection.ALL;

    private Label totalEntriesSubtitle;
    private Label errorLabel;
    private Button btnAll;
    private Button btnIncoming;
    private Button btnOutgoing;
    private Button btnSync;

    private VBox tableContent;
    private VBox emptyStateBox;
    private PaginationControl paginationControl;

    public LedgerView(NavigationManager nav) {
        this.nav = nav;
        initUI();
    }

    private void initUI() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");

        VBox content = new VBox(20);
        content.setMaxWidth(1100);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(24, 32, 48, 32));

        // Header & Filters
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox hText = new VBox(3);
        Label tag = new Label("DOUBLE-ENTRY AUDIT TRAIL");
        tag.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 10px; -fx-text-fill: #C8F169; -fx-font-weight: 700;");

        Label title = new Label("Transaction Ledger");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: #F5F1E8;");

        totalEntriesSubtitle = new Label("Immutable debit and credit transfer records (0 total entries)");
        totalEntriesSubtitle.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #AAB5C6;");

        hText.getChildren().addAll(tag, title, totalEntriesSubtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Filter pills box
        HBox filterBox = new HBox(2);
        filterBox.setStyle("-fx-background-color: #19273A; -fx-border-color: rgba(170, 181, 198, 0.16); -fx-border-width: 1; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 3;");

        btnAll = createFilterButton("ALL");
        btnIncoming = createFilterButton("+ INCOMING");
        btnOutgoing = createFilterButton("- OUTGOING");

        btnAll.setOnAction(e -> setFilter(FilterDirection.ALL));
        btnIncoming.setOnAction(e -> setFilter(FilterDirection.INCOMING));
        btnOutgoing.setOnAction(e -> setFilter(FilterDirection.OUTGOING));

        filterBox.getChildren().addAll(btnAll, btnIncoming, btnOutgoing);

        btnSync = new Button("Sync");
        btnSync.setGraphic(Icons.refresh(Icons.IVORY, 13));
        btnSync.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        btnSync.setOnAction(e -> loadData(currentPage));

        header.getChildren().addAll(hText, spacer, filterBox, btnSync);

        errorLabel = new Label();
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setWrapText(true);
        errorLabel.getStyleClass().addAll("alert-box", "alert-danger");

        // Surface Card for Table
        VBox tableCard = new VBox(0);
        tableCard.getStyleClass().add("surface");
        tableCard.setStyle("-fx-padding: 0;");

        tableContent = new VBox(0);

        emptyStateBox = new VBox(10);
        emptyStateBox.setAlignment(Pos.CENTER);
        emptyStateBox.setStyle("-fx-padding: 48px 20px;");
        Label esTitle = new Label("No Transfers Found");
        esTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #F5F1E8;");
        Label esDesc = new Label("Your wallets have not participated in any ledger transfers matching the active filter.");
        esDesc.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #AAB5C6;");
        emptyStateBox.getChildren().addAll(Icons.history(Icons.MUTED, 36), esTitle, esDesc);

        paginationControl = new PaginationControl(this::loadData);
        VBox.setMargin(paginationControl, new Insets(0, 20, 16, 20));

        tableCard.getChildren().addAll(tableContent, emptyStateBox, paginationControl);

        content.getChildren().addAll(header, errorLabel, tableCard);
        scrollPane.setContent(content);

        getChildren().add(scrollPane);
        updateFilterStyles();
    }

    private Button createFilterButton(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: 700; -fx-background-color: transparent; -fx-text-fill: #AAB5C6; -fx-background-radius: 4px; -fx-padding: 4 10; -fx-cursor: hand;");
        return b;
    }

    private void setFilter(FilterDirection filter) {
        this.currentFilter = filter;
        updateFilterStyles();
        renderTableRows();
    }

    private void updateFilterStyles() {
        btnAll.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: 700; -fx-background-radius: 4px; -fx-padding: 4 10; -fx-cursor: hand; -fx-background-color: " +
                (currentFilter == FilterDirection.ALL ? "#21324B; -fx-text-fill: #F5F1E8;" : "transparent; -fx-text-fill: #AAB5C6;"));

        btnIncoming.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: 700; -fx-background-radius: 4px; -fx-padding: 4 10; -fx-cursor: hand; -fx-background-color: " +
                (currentFilter == FilterDirection.INCOMING ? "rgba(200, 241, 105, 0.14); -fx-text-fill: #C8F169;" : "transparent; -fx-text-fill: #AAB5C6;"));

        btnOutgoing.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 10px; -fx-font-weight: 700; -fx-background-radius: 4px; -fx-padding: 4 10; -fx-cursor: hand; -fx-background-color: " +
                (currentFilter == FilterDirection.OUTGOING ? "rgba(242, 168, 138, 0.14); -fx-text-fill: #F2A88A;" : "transparent; -fx-text-fill: #AAB5C6;"));
    }

    public void loadData(int targetPage) {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        btnSync.setDisable(true);

        WalletService.getInstance().getWallets()
                .thenAccept(wList -> {
                    this.userWallets = (wList != null) ? wList : new ArrayList<>();
                    fetchTransfers(targetPage);
                })
                .exceptionally(ex -> {
                    fetchTransfers(targetPage);
                    return null;
                });
    }

    private void fetchTransfers(int targetPage) {
        TransferService.getInstance().getTransfers(targetPage, pageSize)
                .thenAccept(page -> Platform.runLater(() -> {
                    btnSync.setDisable(false);
                    if (page != null) {
                        this.allTransfers = (page.getContent() != null) ? page.getContent() : new ArrayList<>();
                        this.currentPage = page.getNumber();
                        this.totalPages = page.getTotalPages();
                        this.totalElements = page.getTotalElements();
                        totalEntriesSubtitle.setText(String.format("Immutable debit and credit transfer records (%d total entries)", totalElements));
                        paginationControl.update(currentPage, totalPages, false);
                    }
                    renderTableRows();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        btnSync.setDisable(false);
                        showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                    });
                    return null;
                });
    }

    private void renderTableRows() {
        List<Transfer> filtered = allTransfers.stream().filter(tx -> {
            boolean isSender = isUserWallet(tx.getSenderWalletId());
            if (currentFilter == FilterDirection.INCOMING) return !isSender;
            if (currentFilter == FilterDirection.OUTGOING) return isSender;
            return true;
        }).collect(Collectors.toList());

        if (filtered.isEmpty()) {
            tableContent.setVisible(false);
            tableContent.setManaged(false);
            emptyStateBox.setVisible(true);
            emptyStateBox.setManaged(true);
            return;
        }

        emptyStateBox.setVisible(false);
        emptyStateBox.setManaged(false);
        tableContent.setVisible(true);
        tableContent.setManaged(true);
        tableContent.getChildren().clear();

        // Table Header
        HBox th = new HBox();
        th.setStyle("-fx-background-color: rgba(16, 26, 43, 0.7); -fx-border-color: rgba(170, 181, 198, 0.16); -fx-border-width: 0 0 1 0; -fx-padding: 10 18;");
        th.getChildren().addAll(
                createColHeader("DIRECTION", 140),
                createColHeader("AMOUNT", 140),
                createColHeader("SOURCE WALLET", 160),
                createColHeader("RECIPIENT WALLET", 160),
                createColHeader("SETTLEMENT", 120),
                createColHeader("TIMESTAMP", 180),
                createColHeader("AUDIT", 60)
        );
        tableContent.getChildren().add(th);

        // Rows
        for (Transfer tx : filtered) {
            boolean isSender = isUserWallet(tx.getSenderWalletId());

            HBox row = new HBox();
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-border-color: rgba(170, 181, 198, 0.1); -fx-border-width: 0 0 1 0; -fx-padding: 12 18; -fx-cursor: hand;");
            row.setOnMouseClicked(e -> openAuditModal(tx, isSender));

            // Direction badge
            HBox flowCol = new HBox(4);
            flowCol.setAlignment(Pos.CENTER_LEFT);
            flowCol.setPrefWidth(140);
            Label badge = new Label(isSender ? "Debit / Sent" : "Credit / Recv");
            badge.setGraphic(isSender ? Icons.arrowUpRight(Icons.CORAL, 12) : Icons.arrowDownLeft(Icons.LIME, 12));
            badge.getStyleClass().addAll("flow-badge", isSender ? "flow-outgoing" : "flow-incoming");
            flowCol.getChildren().add(badge);

            // Amount
            HBox amtCol = new HBox();
            amtCol.setAlignment(Pos.CENTER_LEFT);
            amtCol.setPrefWidth(140);
            String sign = isSender ? "-" : "+";
            Label amtLbl = new Label(sign + Formatters.formatCurrency(tx.getAmount(), tx.getCurrency()));
            amtLbl.getStyleClass().add(isSender ? "amount-outgoing" : "amount-incoming");
            amtCol.getChildren().add(amtLbl);

            // Source
            HBox srcCol = new HBox(4);
            srcCol.setAlignment(Pos.CENTER_LEFT);
            srcCol.setPrefWidth(160);
            Label srcLbl = new Label("ID #" + tx.getSenderWalletId());
            srcLbl.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px; -fx-text-fill: #F5F1E8;");
            srcCol.getChildren().add(srcLbl);
            if (isSender) {
                Label youLbl = new Label("(You)");
                youLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #F2A88A;");
                srcCol.getChildren().add(youLbl);
            }

            // Recipient
            HBox recCol = new HBox(4);
            recCol.setAlignment(Pos.CENTER_LEFT);
            recCol.setPrefWidth(160);
            Label recLbl = new Label("ID #" + tx.getRecipientWalletId());
            recLbl.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 12px; -fx-text-fill: #F5F1E8;");
            recCol.getChildren().add(recLbl);
            if (!isSender) {
                Label youLbl = new Label("(You)");
                youLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #C8F169;");
                recCol.getChildren().add(youLbl);
            }

            // Settlement
            HBox stCol = new HBox();
            stCol.setAlignment(Pos.CENTER_LEFT);
            stCol.setPrefWidth(120);
            Label stLbl = new Label(tx.getStatus());
            stLbl.setGraphic(Icons.shieldCheck(Icons.LIME, 11));
            stLbl.getStyleClass().add("status-pill");
            stCol.getChildren().add(stLbl);

            // Timestamp
            HBox timeCol = new HBox();
            timeCol.setAlignment(Pos.CENTER_LEFT);
            timeCol.setPrefWidth(180);
            Label timeLbl = new Label(Formatters.formatDateTime(tx.getCreatedAt()));
            timeLbl.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px; -fx-text-fill: #AAB5C6;");
            timeCol.getChildren().add(timeLbl);

            // Audit Button
            HBox auditCol = new HBox();
            auditCol.setAlignment(Pos.CENTER);
            auditCol.setPrefWidth(60);
            Button auditBtn = new Button();
            auditBtn.setGraphic(Icons.externalLink(Icons.MUTED, 13));
            auditBtn.getStyleClass().addAll("btn", "btn-ghost", "btn-sm");
            auditBtn.setOnAction(e -> {
                e.consume();
                openAuditModal(tx, isSender);
            });
            auditCol.getChildren().add(auditBtn);

            row.getChildren().addAll(flowCol, amtCol, srcCol, recCol, stCol, timeCol, auditCol);
            tableContent.getChildren().add(row);
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
        return userWallets.stream().anyMatch(w -> w.getId().equals(walletId));
    }

    private void openAuditModal(Transfer tx, boolean isSender) {
        TransferAuditModal modal = new TransferAuditModal(tx, isSender, () -> nav.closeModal());
        nav.openModal(modal);
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
