package com.corgibalance.controllers.views;

import com.corgibalance.controllers.tables.AccountTableController;
import javafx.fxml.FXML;

public class AccountsController implements Refreshable {

    @FXML
    private AccountTableController AccountTableController;

    @Override
    public void onShow() {
        AccountTableController.reload();
    }
}
