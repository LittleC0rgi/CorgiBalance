package com.corgibalance.services;

import com.corgibalance.controllers.views.Refreshable;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

import java.util.LinkedHashMap;
import java.util.Map;

public class NavigationService {

    private final Pane container;
    private final Map<String, Node> views = new LinkedHashMap<>();
    private final Map<String, Refreshable> refreshables = new LinkedHashMap<>();
    private Node currentView;

    public NavigationService(Pane container) {
        this.container = container;
    }

    public void register(String name, Node view, Refreshable refreshable) {
        views.put(name, view);
        if (refreshable != null) {
            refreshables.put(name, refreshable);
        }
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
        Refreshable refreshable = refreshables.get(name);
        if (refreshable != null) {
            refreshable.onShow();
        }
    }
}
