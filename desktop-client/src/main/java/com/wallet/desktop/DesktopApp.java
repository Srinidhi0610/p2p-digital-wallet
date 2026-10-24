package com.wallet.desktop;

import com.wallet.desktop.ui.MainLayout;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class DesktopApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainLayout rootLayout = new MainLayout();

        Scene scene = new Scene(rootLayout, 1260, 840);

        URL cssUrl = getClass().getResource("/com/wallet/desktop/css/styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("Warning: styles.css could not be located on classpath!");
        }

        primaryStage.setTitle("pocket // peer — Money in Motion");
        primaryStage.setMinWidth(980);
        primaryStage.setMinHeight(680);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
