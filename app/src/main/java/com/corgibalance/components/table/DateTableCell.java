package com.corgibalance.components.table;

import com.corgibalance.models.BaseModel;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;

import java.time.LocalDate;

public class DateTableCell<T extends BaseModel> extends TableCell<T, LocalDate> {

    private static final String PLACEHOLDER_STYLE_CLASS = "table__placeholder";

    private final DatePicker datePicker = new DatePicker();

    public DateTableCell() {
        datePicker.setOnAction(_ -> commitEdit(datePicker.getValue()));
        datePicker.focusedProperty().addListener((_, _, isFocused) -> {
            if (!isFocused && !datePicker.isShowing() && isEditing()) {
                cancelEdit();
            }
        });
    }

    private T currentItem() {
        return getTableRow() == null ? null : getTableRow().getItem();
    }

    @Override
    protected void updateItem(LocalDate value, boolean empty) {
        super.updateItem(value, empty);
        T item = currentItem();
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            getStyleClass().remove(PLACEHOLDER_STYLE_CLASS);
        } else {
            setText(value == null ? "" : value.toString());
            setGraphic(null);
            if (item.getId() == null) {
                if (!getStyleClass().contains(PLACEHOLDER_STYLE_CLASS)) {
                    getStyleClass().add(PLACEHOLDER_STYLE_CLASS);
                }
            } else {
                getStyleClass().remove(PLACEHOLDER_STYLE_CLASS);
            }
        }
    }

    @Override
    public void startEdit() {
        if (!isEditable()) {
            return;
        }
        super.startEdit();
        datePicker.setValue(getItem());
        setText(null);
        setGraphic(datePicker);
        datePicker.requestFocus();
    }

    @Override
    public void commitEdit(LocalDate newValue) {
        super.commitEdit(newValue);
        setGraphic(null);
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setGraphic(null);
        updateItem(getItem(), isEmpty());
    }
}