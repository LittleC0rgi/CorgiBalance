package org.example.components.inputs;

import javafx.scene.control.TextField;

public class CrudTextField extends TextField {

    public CrudTextField() {
        this("");
    }

    public CrudTextField(String promptText) {
        setPromptText(promptText);
        getStyleClass().add("crud-input");
        getStyleClass().add("crud-text-field");
        setMaxWidth(Double.MAX_VALUE);
    }
}
