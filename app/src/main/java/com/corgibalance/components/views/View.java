package com.corgibalance.components.views;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

import java.io.IOException;

public abstract class View extends VBox {

    protected View(String title, String fxml) {
        setUserData(title);
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load view: " + fxml, e);
        }
    }
}
