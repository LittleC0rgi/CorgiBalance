package org.example.components.views;

import javafx.fxml.FXML;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.example.components.table.Cells;
import org.example.components.table.ColumnSpec;
import org.example.components.table.CrudTable;
import org.example.components.table.FormSpec;
import org.example.models.Budget;
import org.example.models.Tag;
import org.example.repositories.BudgetRepository;
import org.example.repositories.SettingsRepository;
import org.example.repositories.TagRepository;
import org.example.services.CurrencyFormatter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetsView extends View {

    private static final String BASE_CURRENCY_KEY = "overview.baseCurrencyId";

    @FXML
    private VBox content;

    public BudgetsView() {
        super("Budgets", "/fxml/views/budgets.fxml");
    }

    @FXML
    private void initialize() {
        TagRepository tagRepository = new TagRepository();
        Map<Long, String> tagLabels = new HashMap<>();
        List<Long> tagIds = new ArrayList<>();
        for (Tag tag : tagRepository.findAll()) {
            tagIds.add(tag.getId());
            tagLabels.put(tag.getId(), tag.getName());
        }

        CurrencyFormatter currencyFormatter = new CurrencyFormatter();
        Long baseCurrencyId = new SettingsRepository().getLong(BASE_CURRENCY_KEY)
                .filter(id -> currencyFormatter.currency(id) != null)
                .orElseGet(() -> currencyFormatter.currencies().isEmpty()
                        ? null
                        : currencyFormatter.currencies().get(0).getId());

        ColumnSpec<Budget> name = ColumnSpec.<Budget>builder("Name")
                .width(220)
                .value(Budget::getName)
                .editable(Cells.editableText(), (budget, value) -> budget.setName((String) value))
                .form(FormSpec.text())
                .required()
                .build();
        ColumnSpec<Budget> tag = ColumnSpec.<Budget>builder("Tag")
                .width(160)
                .value(Budget::getTagId)
                .editable(Cells.comboEditable(tagIds, tagLabels),
                        (budget, value) -> budget.setTagId((Long) value))
                .form(FormSpec.combo(tagIds, tagLabels))
                .required()
                .build();
        ColumnSpec<Budget> plannedAmount = ColumnSpec.<Budget>builder("Planned amount")
                .width(160)
                .value(Budget::getPlannedAmount)
                .editable(Cells.amountEditable(currencyFormatter, budget -> baseCurrencyId),
                        (budget, value) -> budget.setPlannedAmount(
                                currencyFormatter.toMinorUnits((BigDecimal) value, baseCurrencyId)))
                .form(FormSpec.decimal())
                .required()
                .build();
        ColumnSpec<Budget> startDate = ColumnSpec.<Budget>builder("Start date")
                .width(130)
                .value(Budget::getStartDate)
                .editable(Cells.dateEditable(),
                        (budget, value) -> budget.setStartDate((LocalDate) value))
                .form(FormSpec.date())
                .required()
                .build();
        ColumnSpec<Budget> endDate = ColumnSpec.<Budget>builder("End date")
                .width(130)
                .value(Budget::getEndDate)
                .editable(Cells.dateEditable(),
                        (budget, value) -> budget.setEndDate((LocalDate) value))
                .form(FormSpec.date())
                .required()
                .build();

        CrudTable<Budget> table = new CrudTable<>(
                "Budgets", new BudgetRepository(), Budget::new,
                List.of(name, tag, plannedAmount, startDate, endDate));
        VBox.setVgrow(table, Priority.ALWAYS);
        content.getChildren().add(table);
    }
}
