package org.example.components.inputs;

import javafx.scene.control.DatePicker;

public class CrudDatePicker extends DatePicker {

    public CrudDatePicker() {
        getStyleClass().add("crud-input");
        getStyleClass().add("crud-date-picker");
        setMaxWidth(Double.MAX_VALUE);
    }
}
