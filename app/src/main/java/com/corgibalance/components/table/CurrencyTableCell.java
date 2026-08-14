package com.corgibalance.components.table;

import com.corgibalance.models.Account;
import com.corgibalance.models.Currency;
import com.corgibalance.services.CurrencyFormatter;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;

public class CurrencyTableCell extends TableCell<Account, Long> {

    private static final String PLACEHOLDER_STYLE_CLASS = "table__placeholder";

    private final CurrencyFormatter formatter;
    private final ComboBox<Currency> comboBox = new ComboBox<>();

    public CurrencyTableCell(CurrencyFormatter formatter) {
        this.formatter = formatter;
        comboBox.setItems(FXCollections.observableArrayList(formatter.currencies()));
        comboBox.setCellFactory(_ -> currencyListCell());
        comboBox.setButtonCell(currencyListCell());
        comboBox.setOnAction(_ -> commitSelection());
        comboBox.focusedProperty().addListener((_, _, isFocused) -> {
            if (!isFocused && !comboBox.isShowing() && isEditing()) {
                cancelEdit();
            }
        });
    }

    private ListCell<Currency> currencyListCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Currency currency, boolean empty) {
                super.updateItem(currency, empty);
                setText(empty || currency == null ? null : currency.getName());
            }
        };
    }

    private Account currentAccount() {
        return getTableRow() == null ? null : getTableRow().getItem();
    }

    @Override
    protected void updateItem(Long currencyId, boolean empty) {
        super.updateItem(currencyId, empty);
        Account account = currentAccount();
        if (empty || account == null) {
            setText(null);
            setGraphic(null);
            getStyleClass().remove(PLACEHOLDER_STYLE_CLASS);
        } else {
            setText(currencyName(currencyId));
            setGraphic(null);
            if (account.getId() == null) {
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
        Long currencyId = getItem();
        comboBox.setValue(currencyId == null ? null : formatter.currency(currencyId));
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
        Currency selected = comboBox.getValue();
        if (selected == null) {
            cancelEdit();
        } else {
            commitEdit(selected.getId());
        }
    }

    private String currencyName(Long currencyId) {
        if (currencyId == null) {
            return "";
        }
        var currency = formatter.currency(currencyId);
        return currency == null ? String.valueOf(currencyId) : currency.getName();
    }
}
