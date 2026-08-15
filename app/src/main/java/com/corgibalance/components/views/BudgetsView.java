package com.corgibalance.components.views;

import com.corgibalance.controllers.BudgetTableController;
import javafx.fxml.FXML;

public class BudgetsView extends View implements Refreshable {

    @FXML
    private BudgetTableController BudgetTableController;

    public BudgetsView() {
        super("Budgets", "/fxml/views/budgets.fxml");
    }

    @Override
    public void onShow() {
        BudgetTableController.reload();
    }
}