package com.corgibalance.controllers;

import com.corgibalance.models.Account;
import com.corgibalance.repositories.AccountRepository;
import com.corgibalance.services.CurrencyFormatter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class AccountTableController {
    @FXML
    private TableView<Account> table;
    @FXML
    private TableColumn<Account, String> name;
    @FXML
    private TableColumn<Account, String> initialBalance;

    @FXML
    public void initialize() {
        configureTable();
        configureColumns();
        loadData();
    }

    private void configureTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
    }

    private void configureColumns() {
        CurrencyFormatter formatter = new CurrencyFormatter();
        
        name.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        initialBalance.setCellValueFactory(cell -> new SimpleStringProperty(
                formatter.format(cell.getValue().getInitialBalance(), cell.getValue().getCurrencyId())));
    }

    private void loadData() {
        AccountRepository accountRepository = new AccountRepository();
        var data = accountRepository.findAll();
        setItems(FXCollections.observableArrayList(data));
    }

    public void setItems(ObservableList<Account> items) {
        table.setItems(items);
    }
}
