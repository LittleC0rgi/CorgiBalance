package com.corgibalance.controllers;

import com.corgibalance.components.table.BalanceTableCell;
import com.corgibalance.components.table.CurrencyTableCell;
import com.corgibalance.components.table.NameTableCell;
import com.corgibalance.models.Account;
import com.corgibalance.models.Currency;
import com.corgibalance.repositories.AccountRepository;
import com.corgibalance.services.CurrencyFormatter;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;

import java.util.List;

public class AccountTableController {
    private final AccountRepository accountRepository = new AccountRepository();
    private final CurrencyFormatter currencyFormatter = new CurrencyFormatter();

    @FXML
    private TableView<Account> table;
    @FXML
    private TableColumn<Account, String> name;
    @FXML
    private TableColumn<Account, Long> currency;
    @FXML
    private TableColumn<Account, Long> initialBalance;

    @FXML
    public void initialize() {
        configureTable();
        configureColumns();
        loadData();
    }

    private void configureTable() {
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE) {
                deleteSelectedAccount();
            }
        });
    }

    private void configureColumns() {
        name.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        name.setCellFactory(_ -> new NameTableCell());
        name.setOnEditCommit(this::onNameCommitted);

        currency.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getCurrencyId()));
        currency.setCellFactory(_ -> new CurrencyTableCell(currencyFormatter));
        currency.setOnEditCommit(this::onCurrencyCommitted);

        initialBalance.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getInitialBalance()));
        initialBalance.setCellFactory(_ -> new BalanceTableCell());
        initialBalance.setOnEditCommit(this::onBalanceCommitted);
    }

    private void onNameCommitted(TableColumn.CellEditEvent<Account, String> event) {
        String newName = event.getNewValue() == null ? "" : event.getNewValue().trim();
        if (newName.isEmpty()) {
            table.refresh();
            return;
        }
        Account account = event.getRowValue();
        account.setName(newName);
        if (isPlaceholder(account)) {
            accountRepository.create(account);
            table.getItems().add(newPlaceholder());
            table.refresh();
        } else {
            accountRepository.update(account);
        }
    }

    private void onBalanceCommitted(TableColumn.CellEditEvent<Account, Long> event) {
        Account account = event.getRowValue();
        if (isPlaceholder(account)) {
            table.refresh();
            return;
        }
        account.setInitialBalance(event.getNewValue());
        accountRepository.update(account);
    }

    private void onCurrencyCommitted(TableColumn.CellEditEvent<Account, Long> event) {
        Account account = event.getRowValue();
        account.setCurrencyId(event.getNewValue());
        if (!isPlaceholder(account)) {
            accountRepository.update(account);
            table.refresh();
        }
    }

    private void deleteSelectedAccount() {
        Account selected = table.getSelectionModel().getSelectedItem();
        if (selected == null || isPlaceholder(selected)) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText(null);
        confirm.setContentText("Delete account \"" + selected.getName() + "\"?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            accountRepository.delete(selected);
            table.getItems().remove(selected);
        }
    }

    private void loadData() {
        var data = accountRepository.findAll();
        ObservableList<Account> items = FXCollections.observableArrayList(data);
        items.add(newPlaceholder());
        setItems(items);
    }

    private Account newPlaceholder() {
        Account account = new Account();
        account.setCurrencyId(defaultCurrencyId());
        return account;
    }

    private Long defaultCurrencyId() {
        List<Currency> currencies = currencyFormatter.currencies();
        return currencies.isEmpty() ? null : currencies.getFirst().getId();
    }

    private boolean isPlaceholder(Account account) {
        return account.getId() == null;
    }

    public void setItems(ObservableList<Account> items) {
        table.setItems(items);
    }
}
