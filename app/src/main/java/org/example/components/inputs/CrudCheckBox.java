package org.example.components.inputs;

import javafx.scene.control.CheckBox;

public class CrudCheckBox extends CheckBox {

    public CrudCheckBox() {
        getStyleClass().add("crud-input");
        getStyleClass().add("crud-check-box");
    }
}
