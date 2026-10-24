package com.wallet.desktop.ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.function.Consumer;

public class PaginationControl extends HBox {

    private final Consumer<Integer> pageChangeHandler;
    private int currentPage = 0;
    private int totalPages = 0;

    private Label pageLabel;
    private Button btnPrev;
    private Button btnNext;

    public PaginationControl(Consumer<Integer> pageChangeHandler) {
        this.pageChangeHandler = pageChangeHandler;
        initUI();
    }

    private void initUI() {
        setAlignment(Pos.CENTER_LEFT);
        setStyle("-fx-border-color: rgba(170, 181, 198, 0.16); -fx-border-width: 1 0 0 0; -fx-padding: 14 0 4 0;");

        HBox labelBox = new HBox(4);
        labelBox.setAlignment(Pos.CENTER_LEFT);
        Label pageWord = new Label("PAGE");
        pageWord.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px; -fx-text-fill: #AAB5C6;");

        pageLabel = new Label("1 / 1");
        pageLabel.setStyle("-fx-font-family: 'Consolas', monospace; -fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #C8F169;");
        labelBox.getChildren().addAll(pageWord, pageLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox btnBox = new HBox(8);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        btnPrev = new Button("Prev");
        btnPrev.setGraphic(Icons.chevronLeft(Icons.IVORY, 13));
        btnPrev.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        btnPrev.setOnAction(e -> {
            if (currentPage > 0) {
                pageChangeHandler.accept(currentPage - 1);
            }
        });

        btnNext = new Button("Next");
        btnNext.setGraphic(Icons.chevronRight(Icons.IVORY, 13));
        btnNext.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        btnNext.getStyleClass().addAll("btn", "btn-secondary", "btn-sm");
        btnNext.setOnAction(e -> {
            if (currentPage < totalPages - 1) {
                pageChangeHandler.accept(currentPage + 1);
            }
        });

        btnBox.getChildren().addAll(btnPrev, btnNext);
        getChildren().addAll(labelBox, spacer, btnBox);

        update(0, 0, false);
    }

    public void update(int page, int total, boolean isLoading) {
        this.currentPage = page;
        this.totalPages = total;

        boolean show = totalPages > 1;
        setVisible(show);
        setManaged(show);

        if (show) {
            pageLabel.setText((currentPage + 1) + " / " + totalPages);
            btnPrev.setDisable(currentPage <= 0 || isLoading);
            btnNext.setDisable(currentPage >= totalPages - 1 || isLoading);
        }
    }
}
