package com.wallet.desktop.ui.component;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class ToastNotification {

    public enum Type {
        SUCCESS, ERROR, INFO
    }

    public static void show(StackPane rootPane, String title, String message, Type type) {
        Platform.runLater(() -> {
            HBox toastCard = new HBox(12);
            toastCard.setAlignment(Pos.CENTER_LEFT);
            toastCard.getStyleClass().add("toast-container");
            toastCard.setMaxWidth(420);
            toastCard.setPrefWidth(380);

            Node icon;
            if (type == Type.SUCCESS) {
                toastCard.getStyleClass().add("toast-success");
                icon = Icons.checkCircle(Icons.LIME, 18);
            } else if (type == Type.ERROR) {
                toastCard.getStyleClass().add("toast-error");
                icon = Icons.alertCircle(Icons.CORAL, 18);
            } else {
                icon = Icons.alertCircle(Icons.MUTED, 18);
            }

            VBox textContainer = new VBox(2);
            HBox.setHgrow(textContainer, Priority.ALWAYS);

            if (title != null && !title.isBlank()) {
                Label titleLabel = new Label(title);
                titleLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: #F5F1E8;");
                textContainer.getChildren().add(titleLabel);
            }

            Label msgLabel = new Label(message);
            msgLabel.setWrapText(true);
            msgLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #AAB5C6;");
            textContainer.getChildren().add(msgLabel);

            Button closeBtn = new Button();
            closeBtn.setGraphic(Icons.x(Icons.MUTED, 14));
            closeBtn.getStyleClass().add("btn-ghost");
            closeBtn.setStyle("-fx-padding: 3; -fx-cursor: hand;");

            toastCard.getChildren().addAll(icon, textContainer, closeBtn);

            StackPane.setAlignment(toastCard, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(toastCard, new javafx.geometry.Insets(0, 24, 24, 0));

            toastCard.setOpacity(0);
            rootPane.getChildren().add(toastCard);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toastCard);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            Runnable dismiss = () -> {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(200), toastCard);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> rootPane.getChildren().remove(toastCard));
                fadeOut.play();
            };

            closeBtn.setOnAction(e -> dismiss.run());

            PauseTransition delay = new PauseTransition(Duration.seconds(5));
            delay.setOnFinished(e -> dismiss.run());
            delay.play();
        });
    }
}
