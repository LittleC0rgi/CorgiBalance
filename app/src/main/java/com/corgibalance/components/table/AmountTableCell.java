package com.corgibalance.components.table;

import com.corgibalance.models.BaseModel;
import com.corgibalance.services.CurrencyFormatter;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;

import java.util.function.Function;

public class AmountTableCell<T extends BaseModel> extends TableCell<T, Long> {

    private static final String PLACEHOLDER_STYLE_CLASS = "table__placeholder";

    private final CurrencyFormatter formatter = new CurrencyFormatter();
    private final Function<T, Long> currencyIdOf;
    private final TextField textField = new TextField();

    public AmountTableCell(Function<T, Long> currencyIdOf) {
        this.currencyIdOf = currencyIdOf;
        textField.setOnAction(_ -> commitFromTextField());
        textField.focusedProperty().addListener((_, _, isFocused) -> {
            if (!isFocused && isEditing()) {
                cancelEdit();
            }
        });
    }

    private T currentItem() {
        return getTableRow() == null ? null : getTableRow().getItem();
    }

    private Long currencyId() {
        T item = currentItem();
        return item == null ? null : currencyIdOf.apply(item);
    }

    @Override
    protected void updateItem(Long value, boolean empty) {
        super.updateItem(value, empty);
        T item = currentItem();
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            getStyleClass().remove(PLACEHOLDER_STYLE_CLASS);
        } else {
            setText(formatter.format(value, currencyIdOf.apply(item)));
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
        textField.setText(formatter.toPlain(getItem(), currencyId()));
        setText(null);
        setGraphic(textField);
        textField.selectAll();
        textField.requestFocus();
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

    private void commitFromTextField() {
        if (!isEditing()) {
            return;
        }
        try {
            long minorUnits = formatter.toMinorUnits(formatter.parse(textField.getText()), currencyId());
            commitEdit(minorUnits);
        } catch (NumberFormatException e) {
            cancelEdit();
        }
    }
}