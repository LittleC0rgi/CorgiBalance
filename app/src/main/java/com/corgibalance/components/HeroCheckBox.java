package com.corgibalance.components;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.layout.StackPane;

public class HeroCheckBox extends StackPane {

    private static final String STYLE_CLASS = "hero-checkbox";
    private static final String CHECKED_CLASS = "hero-checkbox--checked";

    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final HeroIcon check = new HeroIcon(HeroIcon.Icon.CHECK);

    public HeroCheckBox() {
        getStyleClass().setAll(STYLE_CLASS);
        check.getStyleClass().add("hero-checkbox__check");
        check.setVisible(false);
        check.setManaged(false);
        getChildren().add(check);

        setOnMouseClicked(_ -> setSelected(!isSelected()));
        selected.addListener((_, _, on) -> {
            check.setVisible(on);
            check.setManaged(on);
            if (on) {
                if (!getStyleClass().contains(CHECKED_CLASS)) {
                    getStyleClass().add(CHECKED_CLASS);
                }
            } else {
                getStyleClass().remove(CHECKED_CLASS);
            }
        });
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean value) {
        selected.set(value);
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }
}