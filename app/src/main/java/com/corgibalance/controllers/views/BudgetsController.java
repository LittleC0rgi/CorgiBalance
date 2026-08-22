package com.corgibalance.controllers.views;

import com.corgibalance.controllers.tables.BudgetTableController;
import javafx.fxml.FXML;

public class BudgetsController implements Refreshable {

    @FXML
    private BudgetTableController BudgetTableController;

    @Override
    public void onShow() {
        BudgetTableController.reload();
    }
}
