package com.corgibalance.components;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class Sidebar extends VBox {

    @FXML
    private ToggleGroup navGroup;

    private ToggleButton lastSelected;

    private final SimpleObjectProperty<String> currentView =
            new SimpleObjectProperty<>(this, "currentView", "Overview");

    public Sidebar() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/Sidebar.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load sidebar component", e);
        }

        navGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                navGroup.selectToggle(lastSelected);
            } else if (newToggle instanceof ToggleButton button) {
                lastSelected = button;
                currentView.set(button.getText());
            }
        });
    }

    public ReadOnlyObjectProperty<String> currentViewProperty() {
        return currentView;
    }

    public String getCurrentView() {
        return currentView.get();
    }

    public void selectView(String name) {
        for (Toggle toggle : navGroup.getToggles()) {
            if (toggle instanceof ToggleButton button && name.equals(button.getText())) {
                navGroup.selectToggle(button);
                return;
            }
        }
    }
}
