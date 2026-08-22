package com.corgibalance;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ViewsFxmlTest {

    private static final String[][] VIEWS = {
            {"Overview", "OverviewController"},
            {"Transactions", "TransactionsController"},
            {"Calendar", "CalendarController"},
            {"Accounts", "AccountsController"},
            {"Budgets", "BudgetsController"},
            {"Analytics", "AnalyticsController"},
            {"Settings", "SettingsController"},
    };

    @BeforeClass
    public static void startToolkit() {
        Platform.startup(() -> {});
    }

    @Test
    public void allViewFxmlsLoadWithTheirControllers() throws Exception {
        for (String[] view : VIEWS) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/" + view[0] + ".fxml"));
            Pane root = loader.load();
            assertNotNull("controller missing: " + view[0], loader.getController());
            assertEquals(view[0] + ": wrong controller",
                    "com.corgibalance.controllers.views." + view[1],
                    loader.getController().getClass().getName());
        }
    }

    @Test
    public void settingsRootIsInjectableVBox() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/Settings.fxml"));
        VBox root = loader.load();
        assertNotNull(root);
    }
}
