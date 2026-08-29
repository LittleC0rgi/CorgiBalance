package com.corgibalance.components;

import com.corgibalance.models.Budget;
import com.corgibalance.services.CurrencyConverter;
import com.corgibalance.services.OverviewService;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;

public class BudgetListComponent {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final OverviewService service;
    private final CurrencyConverter converter;

    public BudgetListComponent(OverviewService service, CurrencyConverter converter) {
        this.service = service;
        this.converter = converter;
    }

    public void render(VBox budgetList, int year, int month, Long baseCurrencyId) {
        budgetList.getChildren().clear();
        for (Budget budget : service.budgets(year, month)) {
            budgetList.getChildren().add(budgetRow(budget, baseCurrencyId));
        }
    }

    private VBox budgetRow(Budget budget, Long baseCurrencyId) {
        long spent = service.budgetSpent(budget, baseCurrencyId);
        long planned = budget.getPlannedAmount();
        double ratio = planned <= 0 ? 0 : Math.min(1.0, (double) spent / planned);
        boolean over = planned > 0 && spent >= planned;
        int percent = (int) Math.round((planned <= 0 ? 0 : (double) spent / planned) * 100);

        Label name = new Label(budget.getName());
        name.getStyleClass().add("budget__name");
        Label dates = new Label(budget.getStartDate().format(DATE_FORMAT) + " — " + budget.getEndDate().format(DATE_FORMAT));
        dates.getStyleClass().add("budget__dates");
        Label amount = new Label(converter.format(spent, baseCurrencyId)
                + " / " + converter.format(planned, baseCurrencyId));
        amount.getStyleClass().add("budget__amount");
        Label percentLabel = new Label(percent + "%");
        percentLabel.getStyleClass().add(over ? "budget__percent--over" : "budget__percent");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(name, dates, spacer, amount, percentLabel);
        header.setSpacing(6.0);

        Region fill = new Region();
        fill.getStyleClass().add(over ? "budget__fill--over" : "budget__fill");
        HBox track = new HBox(fill);
        track.getStyleClass().add("budget__track");
        fill.prefWidthProperty().bind(track.widthProperty().multiply(ratio));

        VBox row = new VBox(header, track);
        row.setSpacing(4.0);
        row.getStyleClass().add("budget__row");
        return row;
    }
}
