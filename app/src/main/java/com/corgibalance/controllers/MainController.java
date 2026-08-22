package com.corgibalance.controllers;

import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import com.corgibalance.components.Sidebar;
import com.corgibalance.controllers.views.AccountsController;
import com.corgibalance.controllers.views.AnalyticsController;
import com.corgibalance.controllers.views.BudgetsController;
import com.corgibalance.controllers.views.CalendarController;
import com.corgibalance.controllers.views.OverviewController;
import com.corgibalance.controllers.views.SettingsController;
import com.corgibalance.controllers.views.TransactionsController;
import com.corgibalance.services.NavigationService;

public class MainController {

    @FXML
    private Sidebar sidebar;

    @FXML
    private StackPane contentArea;

    @FXML
    private VBox overview;
    @FXML
    private VBox transactions;
    @FXML
    private VBox calendar;
    @FXML
    private VBox accounts;
    @FXML
    private VBox budgets;
    @FXML
    private VBox analytics;
    @FXML
    private VBox settings;

    @FXML
    private OverviewController overviewController;
    @FXML
    private TransactionsController transactionsController;
    @FXML
    private CalendarController calendarController;
    @FXML
    private AccountsController accountsController;
    @FXML
    private BudgetsController budgetsController;
    @FXML
    private AnalyticsController analyticsController;
    @FXML
    private SettingsController settingsController;

    @FXML
    private void initialize() {
        NavigationService navigation = new NavigationService(contentArea);
        navigation.register("Overview", overview, overviewController);
        navigation.register("Transactions", transactions, transactionsController);
        navigation.register("Calendar", calendar, calendarController);
        navigation.register("Accounts", accounts, null);
        navigation.register("Budgets", budgets, budgetsController);
        navigation.register("Analytics", analytics, null);
        navigation.register("Settings", settings, null);

        overviewController.setNavigationHandler(sidebar::selectView);

        sidebar.currentViewProperty().addListener((obs, oldView, newView) -> navigation.show(newView));
        navigation.show(sidebar.getCurrentView());
    }
}
