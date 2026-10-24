package com.wallet.desktop.ui.view;

import com.wallet.desktop.model.User;
import com.wallet.desktop.model.Wallet;
import com.wallet.desktop.service.AuthService;
import com.wallet.desktop.service.SessionManager;
import com.wallet.desktop.service.WalletService;
import com.wallet.desktop.ui.NavigationManager;
import com.wallet.desktop.ui.component.DemoDepositModal;
import com.wallet.desktop.ui.component.Icons;
import com.wallet.desktop.util.Formatters;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AccountsView extends StackPane {

    private static final List<String> COMMON_CURRENCIES = Arrays.asList(
            "USD", "EUR", "GBP", "CAD", "AUD", "INR", "JPY", "SGD", "CHF"
    );

    private final NavigationManager nav;

    private List<Wallet> wallets = new ArrayList<>();
    private boolean useCustom = false;

    private Label errorLabel;
    private Label successLabel;

    // Identity Card Elements
    private Label avatarLabel;
    private Label userNameLabel;
    private Label peerNodeLabel;
    private Label emailValueLabel;
    private Label userIdValueLabel;
    private Label joinedValueLabel;

    // Currency Allocation Elements
    private ComboBox<String> currencyCombo;
    private TextField customCurrencyField;
    private Button toggleModeLink;
    private Button btnCreateWallet;

    // Wallets Table Elements
    private Label partitionCountLabel;
    private Button btnRefresh;
    private VBox tableContent;
    private VBox emptyStateBox;

    public AccountsView(NavigationManager nav) {
        this.nav = nav;
        initUI();
    }

    private void initUI() {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");

        VBox content = new VBox(22);
        content.setMaxWidth(1040);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(24, 28, 48, 28));

        // --- Header ---
        VBox header = new VBox(4);
        header.setAlignment(Pos.TOP_LEFT);

        Label badge = new Label("IDENTITY & PARTITIONS");
        badge.setGraphic(Icons.zap(Icons.LIME, 13));
        badge.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 10px; -fx-text-fill: #C8F169; -fx-font-weight: 700;");

        Label title = new Label("Accounts & Settings");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: #F5F1E8;");

        Label subtitle = new Label("Peer credentials and multi-currency ledger accounts");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #AAB5C6;");

        header.getChildren().addAll(badge, title, subtitle);

        // --- Alert Banners ---
        errorLabel = new Label();
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorLabel.setWrapText(true);
        errorLabel.getStyleClass().addAll("alert-box", "alert-danger");

        successLabel = new Label();
        successLabel.setVisible(false);
        successLabel.setManaged(false);
        successLabel.setWrapText(true);
        successLabel.getStyleClass().addAll("alert-box", "alert-success");

        // --- Split Top Grid: Identity Card & Create Wallet Card ---
        GridPane topGrid = new GridPane();
        topGrid.setHgap(20);
        topGrid.setVgap(16);

        VBox identityCard = createIdentityCard();
        VBox openAccountCard = createOpenAccountCard();

        GridPane.setHgrow(identityCard, Priority.ALWAYS);
        GridPane.setHgrow(openAccountCard, Priority.ALWAYS);
        topGrid.add(identityCard, 0, 0);
        topGrid.add(openAccountCard, 1, 0);

        // --- Allocated Partitions Surface ---
        VBox partitionsSurface = createPartitionsSurface();

        content.getChildren().addAll(header, errorLabel, successLabel, topGrid, partitionsSurface);
        scrollPane.setContent(content);

        getChildren().add(scrollPane);
    }

    private VBox createIdentityCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("surface");

        HBox cardHeader = new HBox(8);
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Peer Identity");
        title.getStyleClass().add("surface-title");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label verifiedPill = new Label("VERIFIED PEER");
        verifiedPill.getStyleClass().addAll("status-pill", "settled");
        cardHeader.getChildren().addAll(title, sp, verifiedPill);

        VBox cardBody = new VBox(14);

        // Avatar Row
        HBox avatarRow = new HBox(14);
        avatarRow.setAlignment(Pos.CENTER_LEFT);
        avatarRow.setStyle("-fx-background-color: #101A2B; -fx-border-color: rgba(33, 50, 75, 0.7); -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 12 16;");

        avatarLabel = new Label("P");
        avatarLabel.setStyle("-fx-background-color: #C8F169; -fx-text-fill: #101A2B; -fx-font-weight: 800; -fx-font-size: 18px; -fx-min-width: 44px; -fx-min-height: 44px; -fx-alignment: center; -fx-background-radius: 6px;");

        VBox nameBox = new VBox(2);
        userNameLabel = new Label("Account");
        userNameLabel.setStyle("-fx-font-weight: 800; -fx-font-size: 16px; -fx-text-fill: #F5F1E8;");
        peerNodeLabel = new Label("PEER NODE #0");
        peerNodeLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px; -fx-text-fill: #C8F169;");
        nameBox.getChildren().addAll(userNameLabel, peerNodeLabel);

        avatarRow.getChildren().addAll(avatarLabel, nameBox);

        // Info Rows
        HBox emailRow = createMetaRow(Icons.mail(Icons.MUTED, 15), "Email:", emailValueLabel = new Label("—"));
        emailValueLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-weight: 700; -fx-text-fill: #F5F1E8;");

        HBox idRow = createMetaRow(Icons.zap(Icons.MUTED, 15), "System User ID:", userIdValueLabel = new Label("—"));
        userIdValueLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-text-fill: #F5F1E8;");

        HBox joinedRow = createMetaRow(Icons.history(Icons.MUTED, 15), "Joined:", joinedValueLabel = new Label("—"));
        joinedValueLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-text-fill: #AAB5C6;");

        cardBody.getChildren().addAll(avatarRow, emailRow, idRow, joinedRow);
        card.getChildren().addAll(cardHeader, cardBody);
        return card;
    }

    private HBox createMetaRow(Node icon, String label, Label valueLabel) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #AAB5C6;");
        row.getChildren().addAll(icon, l, valueLabel);
        return row;
    }

    private VBox createOpenAccountCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("surface");

        HBox cardHeader = new HBox(8);
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        VBox headerTitles = new VBox(2);
        Label title = new Label("Open Currency Account");
        title.getStyleClass().add("surface-title");
        Label subtitle = new Label("Allocate an isolated ledger balance partition");
        subtitle.getStyleClass().add("surface-subtitle");
        headerTitles.getChildren().addAll(title, subtitle);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        cardHeader.getChildren().addAll(headerTitles, sp, Icons.wallet(Icons.LIME, 18));

        VBox form = new VBox(12);

        Label label = new Label("Currency Code (ISO-4217)");
        label.getStyleClass().add("form-label");

        currencyCombo = new ComboBox<>();
        currencyCombo.getStyleClass().add("form-input");
        currencyCombo.setMaxWidth(Double.MAX_VALUE);
        currencyCombo.setStyle("-fx-font-family: 'Consolas', monospace;");

        customCurrencyField = new TextField();
        customCurrencyField.getStyleClass().add("form-input");
        customCurrencyField.setPromptText("e.g. CHF, JPY");
        customCurrencyField.setMaxWidth(Double.MAX_VALUE);
        customCurrencyField.setVisible(false);
        customCurrencyField.setManaged(false);
        customCurrencyField.setStyle("-fx-font-family: 'Consolas', monospace;");
        customCurrencyField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                String upper = newV.toUpperCase();
                if (upper.length() > 3) upper = upper.substring(0, 3);
                if (!upper.equals(newV)) customCurrencyField.setText(upper);
            }
        });

        toggleModeLink = new Button("→ Enter custom 3-character code");
        toggleModeLink.setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-text-fill: #C8F169; -fx-cursor: hand; -fx-font-family: 'Consolas', monospace; -fx-font-size: 11px; -fx-underline: true;");
        toggleModeLink.setOnAction(e -> toggleCurrencyInputMode());

        btnCreateWallet = new Button("Initialize Wallet Partition");
        btnCreateWallet.setGraphic(Icons.plus(Icons.NAVY, 15));
        btnCreateWallet.getStyleClass().addAll("btn", "btn-lime", "btn-full");
        btnCreateWallet.setOnAction(e -> handleCreateWallet());

        form.getChildren().addAll(label, currencyCombo, customCurrencyField, toggleModeLink, btnCreateWallet);
        card.getChildren().addAll(cardHeader, form);
        return card;
    }

    private void toggleCurrencyInputMode() {
        useCustom = !useCustom;
        if (useCustom) {
            currencyCombo.setVisible(false);
            currencyCombo.setManaged(false);
            customCurrencyField.setVisible(true);
            customCurrencyField.setManaged(true);
            toggleModeLink.setText("← Pick from common ISO currencies");
        } else {
            currencyCombo.setVisible(true);
            currencyCombo.setManaged(true);
            customCurrencyField.setVisible(false);
            customCurrencyField.setManaged(false);
            toggleModeLink.setText("→ Enter custom 3-character code");
        }
    }

    private VBox createPartitionsSurface() {
        VBox surface = new VBox(0);
        surface.getStyleClass().add("surface");
        surface.setStyle("-fx-padding: 0; -fx-background-radius: 8px;");

        // Surface Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 20, 14, 20));

        VBox titleBox = new VBox(2);
        partitionCountLabel = new Label("Allocated Wallet Partitions (0)");
        partitionCountLabel.getStyleClass().add("surface-title");
        Label subtitle = new Label("Share your numeric Wallet ID with counterparties to receive peer transfers");
        subtitle.getStyleClass().add("surface-subtitle");
        titleBox.getChildren().addAll(partitionCountLabel, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnRefresh = new Button();
        btnRefresh.setGraphic(Icons.refresh(Icons.MUTED, 14));
        btnRefresh.getStyleClass().addAll("btn", "btn-ghost", "btn-sm");
        btnRefresh.setOnAction(e -> loadData());

        header.getChildren().addAll(titleBox, spacer, btnRefresh);

        // Table Header
        HBox thead = new HBox(12);
        thead.getStyleClass().add("table-header-row");
        thead.setPadding(new Insets(10, 20, 10, 20));
        thead.setAlignment(Pos.CENTER_LEFT);

        Label colId = new Label("PARTITION ID");
        colId.setPrefWidth(160);
        colId.getStyleClass().add("table-header-cell");

        Label colCurr = new Label("CURRENCY");
        colCurr.setPrefWidth(100);
        colCurr.getStyleClass().add("table-header-cell");

        Label colBal = new Label("AUTHORITATIVE BALANCE");
        colBal.setPrefWidth(220);
        colBal.getStyleClass().add("table-header-cell");

        Label colDate = new Label("ALLOCATED DATE");
        HBox.setHgrow(colDate, Priority.ALWAYS);
        colDate.getStyleClass().add("table-header-cell");

        Label colAction = new Label("ACTION");
        colAction.setPrefWidth(240);
        colAction.setAlignment(Pos.CENTER_RIGHT);
        colAction.getStyleClass().add("table-header-cell");

        thead.getChildren().addAll(colId, colCurr, colBal, colDate, colAction);

        // Rows container
        tableContent = new VBox(0);

        // Empty state
        emptyStateBox = new VBox(12);
        emptyStateBox.setAlignment(Pos.CENTER);
        emptyStateBox.setPadding(new Insets(48, 24, 48, 24));
        Label emptyText = new Label("No wallet accounts active. Use the allocation form above to open your primary account.");
        emptyText.setStyle("-fx-text-fill: #AAB5C6; -fx-font-size: 13px;");
        emptyStateBox.getChildren().add(emptyText);

        surface.getChildren().addAll(header, thead, tableContent, emptyStateBox);
        return surface;
    }

    public void loadData() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
        btnRefresh.setDisable(true);

        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            String name = user.getName() != null ? user.getName() : "Account";
            userNameLabel.setText(name);
            avatarLabel.setText(name.substring(0, 1).toUpperCase());
            peerNodeLabel.setText("PEER NODE #" + user.getId());
            emailValueLabel.setText(user.getEmail() != null ? user.getEmail() : "—");
            userIdValueLabel.setText("#" + user.getId());
            joinedValueLabel.setText(Formatters.formatDateTime(user.getCreatedAt()));
        }

        AuthService.getInstance().getProfile()
                .thenAccept(u -> Platform.runLater(() -> {
                    if (u != null) {
                        String name = u.getName() != null ? u.getName() : "Account";
                        userNameLabel.setText(name);
                        avatarLabel.setText(name.substring(0, 1).toUpperCase());
                        peerNodeLabel.setText("PEER NODE #" + u.getId());
                        emailValueLabel.setText(u.getEmail() != null ? u.getEmail() : "—");
                        userIdValueLabel.setText("#" + u.getId());
                        joinedValueLabel.setText(Formatters.formatDateTime(u.getCreatedAt()));
                    }
                }))
                .exceptionally(ex -> null);

        WalletService.getInstance().getWallets()
                .thenAccept(wList -> {
                    this.wallets = (wList != null) ? wList : new ArrayList<>();
                    Platform.runLater(() -> {
                        populateCurrencyCombo();
                        renderTableRows();
                        btnRefresh.setDisable(false);
                    });
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showError(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                        btnRefresh.setDisable(false);
                    });
                    return null;
                });
    }

    private void populateCurrencyCombo() {
        currencyCombo.getItems().clear();
        String firstAvailable = null;

        for (String curr : COMMON_CURRENCIES) {
            boolean exists = wallets.stream().anyMatch(w -> curr.equalsIgnoreCase(w.getCurrency()));
            String display = curr + (exists ? " (Already Allocated)" : "");
            currencyCombo.getItems().add(display);
            if (!exists && firstAvailable == null) {
                firstAvailable = display;
            }
        }

        if (firstAvailable != null) {
            currencyCombo.setValue(firstAvailable);
        } else if (!currencyCombo.getItems().isEmpty()) {
            currencyCombo.setValue(currencyCombo.getItems().get(0));
        }
    }

    private void renderTableRows() {
        tableContent.getChildren().clear();
        partitionCountLabel.setText("Allocated Wallet Partitions (" + wallets.size() + ")");

        if (wallets.isEmpty()) {
            emptyStateBox.setVisible(true);
            emptyStateBox.setManaged(true);
            return;
        }

        emptyStateBox.setVisible(false);
        emptyStateBox.setManaged(false);

        for (int i = 0; i < wallets.size(); i++) {
            Wallet wallet = wallets.get(i);
            HBox row = createWalletRow(wallet, i);
            tableContent.getChildren().add(row);
        }
    }

    private HBox createWalletRow(Wallet wallet, int index) {
        HBox row = new HBox(12);
        row.getStyleClass().add("table-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 20, 12, 20));
        if (index % 2 == 1) {
            row.setStyle("-fx-background-color: rgba(25, 39, 58, 0.45);");
        }

        // Partition ID
        Label idLabel = new Label("WALLET #" + wallet.getId());
        idLabel.setPrefWidth(160);
        idLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-weight: 700; -fx-text-fill: #F5F1E8;");

        // Currency Pill
        Label currLabel = new Label(wallet.getCurrency());
        currLabel.getStyleClass().add("currency-pill");
        HBox currBox = new HBox(currLabel);
        currBox.setPrefWidth(100);
        currBox.setAlignment(Pos.CENTER_LEFT);

        // Balance
        Label balLabel = new Label(Formatters.formatCurrency(wallet.getBalance(), wallet.getCurrency()));
        balLabel.setPrefWidth(220);
        balLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-weight: 700; -fx-text-fill: #C8F169; -fx-font-size: 13.5px;");

        // Created Date
        Label dateLabel = new Label(Formatters.formatDateTime(wallet.getCreatedAt()));
        HBox.setHgrow(dateLabel, Priority.ALWAYS);
        dateLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px; -fx-text-fill: #AAB5C6;");

        // Action Buttons Box
        HBox actionsBox = new HBox(8);
        actionsBox.setPrefWidth(240);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnDemo = new Button("Add Demo Funds");
        btnDemo.setGraphic(Icons.coins(Icons.NAVY, 13));
        btnDemo.getStyleClass().addAll("btn", "btn-lime", "btn-sm");
        btnDemo.setOnAction(e -> openDemoDepositModal(wallet));

        Button btnCopy = new Button("Copy ID");
        btnCopy.setGraphic(Icons.copy(Icons.MUTED, 13));
        btnCopy.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        btnCopy.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(wallet.getId()));
            clipboard.setContent(content);

            btnCopy.setText("Copied");
            btnCopy.setGraphic(Icons.check(Icons.LIME, 13));
            btnCopy.setStyle("-fx-text-fill: #C8F169;");

            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(ev -> {
                btnCopy.setText("Copy ID");
                btnCopy.setGraphic(Icons.copy(Icons.MUTED, 13));
                btnCopy.setStyle("");
            });
            pause.play();
        });

        actionsBox.getChildren().addAll(btnDemo, btnCopy);

        row.getChildren().addAll(idLabel, currBox, balLabel, dateLabel, actionsBox);
        return row;
    }

    private void openDemoDepositModal(Wallet wallet) {
        DemoDepositModal modal = new DemoDepositModal(
                wallet,
                () -> nav.closeModal(),
                updatedWallet -> {
                    nav.closeModal();
                    showSuccess("Successfully credited demo funds to " + updatedWallet.getCurrency() + " Wallet #" + updatedWallet.getId() + "!");
                    loadData();
                }
        );
        nav.openModal(modal);
    }

    private void handleCreateWallet() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        successLabel.setVisible(false);
        successLabel.setManaged(false);

        String targetCurrency;
        if (useCustom) {
            targetCurrency = customCurrencyField.getText() != null ? customCurrencyField.getText().trim().toUpperCase() : "";
        } else {
            String selected = currencyCombo.getValue();
            if (selected == null || selected.isBlank()) {
                showError("Please choose a currency code");
                return;
            }
            targetCurrency = selected.split(" ")[0].trim().toUpperCase();
        }

        if (targetCurrency.length() != 3) {
            showError("Currency code must be exactly 3 alphabetic letters (e.g. USD, EUR, GBP)");
            return;
        }

        for (Wallet w : wallets) {
            if (targetCurrency.equalsIgnoreCase(w.getCurrency())) {
                showError("An active " + targetCurrency + " wallet node is already allocated to your account.");
                return;
            }
        }

        btnCreateWallet.setDisable(true);
        btnCreateWallet.setText("Allocating...");

        WalletService.getInstance().createWallet(targetCurrency)
                .thenAccept(created -> Platform.runLater(() -> {
                    btnCreateWallet.setDisable(false);
                    btnCreateWallet.setText("Initialize Wallet Partition");
                    customCurrencyField.clear();
                    showSuccess("Activated new " + created.getCurrency() + " Wallet Partition (ID #" + created.getId() + ")");
                    loadData();
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        btnCreateWallet.setDisable(false);
                        btnCreateWallet.setText("Initialize Wallet Partition");
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

    private void showSuccess(String msg) {
        successLabel.setText(msg);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
    }
}
