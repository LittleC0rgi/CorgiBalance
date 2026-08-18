package com.corgibalance.components.table;

import com.corgibalance.models.BaseModel;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;

import java.util.function.Function;

public class TextTableCell<T extends BaseModel> extends TableCell<T, String> {

    private static final String PLACEHOLDER_STYLE_CLASS = "table__placeholder";

    private final Function<T, String> valueOf;
    private final String placeholderText;
    private final TextField textField = new TextField();

    public TextTableCell(Function<T, String> valueOf, String placeholderText) {
        this.valueOf = valueOf;
        this.placeholderText = placeholderText;
        textField.setOnAction(_ -> commitEdit(textField.getText()));
        textField.getStyleClass().add("input");
        textField.focusedProperty().addListener((_, _, isFocused) -> {
            if (!isFocused && isEditing()) {
                cancelEdit();
            }
        });
    }

    private T currentItem() {
        return getTableRow() == null ? null : getTableRow().getItem();
    }

    @Override
    protected void updateItem(String value, boolean empty) {
        super.updateItem(value, empty);
        T item = currentItem();
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            getStyleClass().remove(PLACEHOLDER_STYLE_CLASS);
        } else if (item.getId() == null) {
            setText(placeholderText);
            setGraphic(null);
            if (!getStyleClass().contains(PLACEHOLDER_STYLE_CLASS)) {
                getStyleClass().add(PLACEHOLDER_STYLE_CLASS);
            }
        } else {
            setText(valueOf.apply(item));
            setGraphic(null);
            getStyleClass().remove(PLACEHOLDER_STYLE_CLASS);
        }
    }

    @Override
    public void startEdit() {
        if (!isEditable()) {
            return;
        }
        super.startEdit();
        T item = currentItem();
        String name = item == null ? "" : valueOf.apply(item);
        textField.setText(name == null ? "" : name);
        setText(null);
        setGraphic(textField);
        textField.selectAll();
        textField.requestFocus();
    }

    @Override
    public void commitEdit(String newValue) {
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
