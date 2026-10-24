package com.wallet.desktop;

import com.wallet.desktop.service.SessionManager;
import com.wallet.desktop.ui.MainLayout;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainLayout nav = new MainLayout();

        Scene scene = new Scene(nav.getRootPane(), 1240, 800);

        // Load stylesheet
        URL cssResource = getClass().getResource("/com/wallet/desktop/css/styles.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        } else {
            System.err.println("Warning: Could not locate styles.css resource");
        }

        primaryStage.setTitle("pocket // peer — Money in Motion");
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(640);
        primaryStage.setScene(scene);

        // Initial view based on auth state
        if (SessionManager.getInstance().isAuthenticated()) {
            nav.showView("dashboard");
        } else {
            nav.showView("login");
        }

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
