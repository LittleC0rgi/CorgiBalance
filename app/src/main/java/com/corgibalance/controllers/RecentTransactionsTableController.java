package com.corgibalance.controllers;

import com.corgibalance.models.Transaction;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class RecentTransactionsTableController extends TransactionTableController {

    @Override
    protected void loadData() {
        ObservableList<Transaction> items = FXCollections.observableArrayList(repository.findLatest(10));
        items.add(newPlaceholder());
        setItems(items);
    }

    @Override
    public void reload() {
        super.reload();
    }
}