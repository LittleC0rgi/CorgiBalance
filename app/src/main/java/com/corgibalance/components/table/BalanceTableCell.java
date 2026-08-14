package com.corgibalance.components.table;

import com.corgibalance.models.Account;
import com.corgibalance.services.CurrencyFormatter;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;

public class BalanceTableCell extends TableCell<Account, Long> {

    private final CurrencyFormatter formatter = new CurrencyFormatter();
    private final TextField textField = new TextField();

    public BalanceTableCell() {
        textField.setOnAction(_ -> commitFromTextField());
        textField.focusedProperty().addListener((_, _, isFocused) -> {
            if (!isFocused && isEditing()) {
                cancelEdit();
            }
        });
    }

    private Account currentAccount() {
        return getTableRow() == null ? null : getTableRow().getItem();
    }

    @Override
    protected void updateItem(Long value, boolean empty) {
        super.updateItem(value, empty);
        Account account = currentAccount();
        if (empty || account == null) {
            setText(null);
            setGraphic(null);
        } else {
            setText(formatter.format(value, account.getCurrencyId()));
            setGraphic(null);
        }
    }

    @Override
    public void startEdit() {
        if (!isEditable()) {
            return;
        }
        super.startEdit();
        Account account = currentAccount();
        textField.setText(formatter.toPlain(getItem(), account == null ? null : account.getCurrencyId()));
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
        Account account = currentAccount();
        try {
            assert account != null;
            long minorUnits = formatter.toMinorUnits(formatter.parse(textField.getText()), account.getCurrencyId());
            commitEdit(minorUnits);
        } catch (NumberFormatException e) {
            cancelEdit();
        }
    }
}
