package com.corgibalance.components.table;

import com.corgibalance.models.BaseModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.List;
import java.util.function.Function;

public class SelectTableCell<T extends BaseModel, V> extends TableCell<T, V> {

    private static final String PLACEHOLDER_STYLE_CLASS = "table__placeholder";

    private final ComboBox<V> comboBox = new ComboBox<>();
    private final Function<V, String> labelFor;
    private final Function<V, String> colorFor;
    private final String noneLabel;

    public SelectTableCell(List<V> values, Function<V, String> labelFor) {
        this(values, labelFor, null);
    }

    public SelectTableCell(List<V> values, Function<V, String> labelFor, Function<V, String> colorFor) {
        this(values, labelFor, colorFor, null);
    }

    public SelectTableCell(List<V> values, Function<V, String> labelFor, Function<V, String> colorFor,
                           String noneLabel) {
        this.labelFor = labelFor;
        this.colorFor = colorFor;
        this.noneLabel = noneLabel;
        ObservableList<V> items = FXCollections.observableArrayList(values);
        if (noneLabel != null) {
            items.add(0, null);
        }
        comboBox.setItems(items);
        comboBox.getStyleClass().add("selector");
        comboBox.setCellFactory(_ -> selectListCell());
        comboBox.setButtonCell(selectListCell());
        comboBox.setOnAction(_ -> commitSelection());
        comboBox.focusedProperty().addListener((_, _, isFocused) -> {
            if (!isFocused && !comboBox.isShowing() && isEditing()) {
                cancelEdit();
            }
        });
    }

    private ListCell<V> selectListCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(V value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else if (value == null) {
                    setText(noneLabel);
                    setGraphic(null);
                } else {
                    setText(labelFor.apply(value));
                    setGraphic(circleFor(value));
                }
            }
        };
    }

    private Circle circleFor(V value) {
        if (colorFor == null || value == null) {
            return null;
        }
        String color = colorFor.apply(value);
        if (color == null) {
            return null;
        }
        try {
            return new Circle(5, Color.web(color));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private T currentItem() {
        return getTableRow() == null ? null : getTableRow().getItem();
    }

    @Override
    protected void updateItem(V value, boolean empty) {
        super.updateItem(value, empty);
        T item = currentItem();
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            getStyleClass().remove(PLACEHOLDER_STYLE_CLASS);
        } else {
            if (value == null && noneLabel != null) {
                setText(noneLabel);
                setGraphic(null);
            } else {
                setText(labelFor.apply(value));
                setGraphic(circleFor(value));
            }
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
    public void commitEdit(V newValue) {
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
        V selected = comboBox.getValue();
        if (selected == null && noneLabel == null) {
            cancelEdit();
        } else {
            commitEdit(selected);
        }
    }
}
