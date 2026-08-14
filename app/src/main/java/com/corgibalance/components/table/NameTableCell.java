package com.corgibalance.components.table;

import com.corgibalance.models.Account;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;

public class NameTableCell extends TableCell<Account, String> {

    private static final String PLACEHOLDER_STYLE_CLASS = "table__placeholder";

    private final TextField textField = new TextField();

    public NameTableCell() {
        textField.setOnAction(_ -> commitEdit(textField.getText()));
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
    protected void updateItem(String value, boolean empty) {
        super.updateItem(value, empty);
        Account account = currentAccount();
        if (empty || account == null) {
            setText(null);
            setGraphic(null);
            getStyleClass().remove(PLACEHOLDER_STYLE_CLASS);
        } else if (account.getId() == null) {
            setText("+ Add account");
            setGraphic(null);
            if (!getStyleClass().contains(PLACEHOLDER_STYLE_CLASS)) {
                getStyleClass().add(PLACEHOLDER_STYLE_CLASS);
            }
        } else {
            setText(account.getName());
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
        Account account = currentAccount();
        textField.setText(account == null ? "" : account.getName());
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
