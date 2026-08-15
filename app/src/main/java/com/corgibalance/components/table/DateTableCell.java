package com.corgibalance.components.table;

import com.corgibalance.models.BaseModel;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableCell;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateTableCell<T extends BaseModel> extends TableCell<T, LocalDate> {

    private static final String PLACEHOLDER_STYLE_CLASS = "table__placeholder";
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final DatePicker datePicker = new DatePicker();

    public DateTableCell() {
        datePicker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate value) {
                return value == null ? "" : value.format(DISPLAY_FORMAT);
            }

            @Override
            public LocalDate fromString(String value) {
                return value == null || value.isBlank() ? null : LocalDate.parse(value, DISPLAY_FORMAT);
            }
        });
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
            setText(value == null ? "" : value.format(DISPLAY_FORMAT));
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