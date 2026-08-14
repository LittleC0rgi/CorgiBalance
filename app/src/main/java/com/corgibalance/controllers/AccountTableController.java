package com.corgibalance.controllers;

import com.corgibalance.components.table.BalanceTableCell;
import com.corgibalance.models.Account;
import com.corgibalance.repositories.AccountRepository;
import com.corgibalance.services.CurrencyFormatter;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;

public class AccountTableController {

    private final CurrencyFormatter formatter = new CurrencyFormatter();
    private final AccountRepository accountRepository = new AccountRepository();

    @FXML
    private TableView<Account> table;
    @FXML
    private TableColumn<Account, String> name;
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
    }

    private void configureColumns() {
        name.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        name.setCellFactory(TextFieldTableCell.forTableColumn());
        name.setOnEditCommit(this::onNameCommitted);

        initialBalance.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getInitialBalance()));
        initialBalance.setCellFactory(column -> new BalanceTableCell(formatter));
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
        accountRepository.update(account);
    }

    private void onBalanceCommitted(TableColumn.CellEditEvent<Account, Long> event) {
        Account account = event.getRowValue();
        account.setInitialBalance(event.getNewValue());
        accountRepository.update(account);
    }

    private void loadData() {
        var data = accountRepository.findAll();
        setItems(FXCollections.observableArrayList(data));
    }

    public void setItems(ObservableList<Account> items) {
        table.setItems(items);
    }
}
