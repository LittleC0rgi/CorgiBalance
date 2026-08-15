package com.corgibalance.components.views;

import com.corgibalance.controllers.TransactionTableController;
import javafx.fxml.FXML;

public class TransactionsView extends View implements Refreshable {

    @FXML
    private TransactionTableController TransactionTableController;

    public TransactionsView() {
        super("Transactions", "/fxml/views/transactions.fxml");
    }

    @Override
    public void onShow() {
        TransactionTableController.reload();
    }
}