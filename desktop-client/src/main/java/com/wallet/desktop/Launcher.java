package com.wallet.desktop;

import javafx.application.Application;

/**
 * Standard launcher entry point to bypass module path restrictions when launching JavaFX fat JARs or classes.
 */
public class Launcher {

    public static void main(String[] args) {
        Application.launch(DesktopApp.class, args);
    }
}
