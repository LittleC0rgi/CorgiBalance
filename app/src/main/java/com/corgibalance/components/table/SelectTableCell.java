package com.corgibalance.components.table;

import com.corgibalance.models.BaseModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;

import java.util.List;
import java.util.function.Function;

public class SelectTableCell<T extends BaseModel> extends TableCell<T, Long> {

    private static final String PLACEHOLDER_STYLE_CLASS = "table__placeholder";

    private final ComboBox<Long> comboBox = new ComboBox<>();
    private final Function<Long, String> labelFor;

    public SelectTableCell(List<Long> ids, Function<Long, String> labelFor) {
        this.labelFor = labelFor;
        ObservableList<Long> items = FXCollections.observableArrayList(ids);
        comboBox.setItems(items);
        comboBox.setCellFactory(_ -> selectListCell());
        comboBox.setButtonCell(selectListCell());
        comboBox.setOnAction(_ -> commitSelection());
        comboBox.focusedProperty().addListener((_, _, isFocused) -> {
            if (!isFocused && !comboBox.isShowing() && isEditing()) {
                cancelEdit();
            }
        });
    }

    private ListCell<Long> selectListCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Long id, boolean empty) {
                super.updateItem(id, empty);
                setText(empty || id == null ? null : labelFor.apply(id));
            }
        };
    }

    private T currentItem() {
        return getTableRow() == null ? null : getTableRow().getItem();
    }

    @Override
    protected void updateItem(Long id, boolean empty) {
        super.updateItem(id, empty);
        T item = currentItem();
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            getStyleClass().remove(PLACEHOLDER_STYLE_CLASS);
        } else {
            setText(labelFor.apply(id));
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
        comboBox.setValue(getItem());
        super.startEdit();
        setText(null);
        setGraphic(comboBox);
        comboBox.show();
    }

    @Override
    public void commitEdit(Long newValue) {
        super.commitEdit(newValue);
        setGraphic(null);
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setGraphic(null);
        updateItem(getItem(), isEmpty());
    }

    private void commitSelection() {
        if (!isEditing()) {
            return;
        }
        Long selected = comboBox.getValue();
        if (selected == null) {
            cancelEdit();
        } else {
            commitEdit(selected);
        }
    }
}