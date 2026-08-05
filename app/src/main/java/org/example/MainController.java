package org.example;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import org.example.components.Sidebar;
import org.example.navigation.NavigationService;

public class MainController {

    @FXML
    private Sidebar sidebar;

    @FXML
    private StackPane contentArea;

    @FXML
    private void initialize() {
        NavigationService navigation = new NavigationService(contentArea);
        for (Node child : contentArea.getChildren()) {
            navigation.register(String.valueOf(child.getUserData()), child);
        }
        sidebar.currentViewProperty().addListener((obs, oldView, newView) -> navigation.show(newView));
        navigation.show(sidebar.getCurrentView());
    }
}
