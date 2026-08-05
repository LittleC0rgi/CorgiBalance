package org.example.services;

import javafx.scene.Node;
import javafx.scene.layout.Pane;

import java.util.LinkedHashMap;
import java.util.Map;

public class NavigationService {

    private final Pane container;
    private final Map<String, Node> views = new LinkedHashMap<>();
    private Node currentView;

    public NavigationService(Pane container) {
        this.container = container;
    }

    public void register(String name, Node view) {
        views.put(name, view);
        if (!container.getChildren().contains(view)) {
            container.getChildren().add(view);
        }
        view.setVisible(false);
        view.setManaged(false);
    }

    public void show(String name) {
        Node target = views.get(name);
        if (target == null) {
            throw new IllegalArgumentException("Unknown view: " + name);
        }
        if (target == currentView) {
            return;
        }
        if (currentView != null) {
            currentView.setVisible(false);
            currentView.setManaged(false);
        }
        target.setVisible(true);
        target.setManaged(true);
        currentView = target;
    }
}
