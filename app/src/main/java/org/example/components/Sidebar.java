package org.example.components;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class Sidebar extends VBox {

    public Sidebar() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/sidebar.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load sidebar component", e);
        }
    }
}
