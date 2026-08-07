package org.example.components.inputs;

import javafx.scene.control.ComboBox;

public class CrudComboBox<T> extends ComboBox<T> {

    public CrudComboBox() {
        getStyleClass().add("crud-input");
        getStyleClass().add("crud-combo-box");
        setMaxWidth(Double.MAX_VALUE);
    }
}
