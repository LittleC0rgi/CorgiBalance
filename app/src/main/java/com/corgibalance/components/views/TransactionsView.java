package com.corgibalance.components.views;

import com.corgibalance.components.dialogs.TransferDialog;
import com.corgibalance.controllers.TransactionTableController;
import com.corgibalance.repositories.AccountRepository;
import com.corgibalance.services.CurrencyConverter;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class TransactionsView extends View implements Refreshable {

    @FXML
    private TransactionTableController TransactionTableController;

    @FXML
    private Button transferButton;

    public TransactionsView() {
        super("Transactions", "/fxml/views/transactions.fxml");
    }

    @Override
    public void onShow() {
        TransactionTableController.reload();
        transferButton.setDisable(new AccountRepository().findAll().size() < 2);
    }

    @FXML
    private void openTransferDialog() {
        TransferDialog dialog = new TransferDialog(new AccountRepository().findAll(), new CurrencyConverter());
        dialog.showAndWait();
        if (dialog.isCreated()) {
            TransactionTableController.reload();
        }
    }
}