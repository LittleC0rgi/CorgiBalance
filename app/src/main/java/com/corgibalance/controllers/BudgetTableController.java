package com.corgibalance.controllers;

import com.corgibalance.components.table.AmountTableCell;
import com.corgibalance.components.table.DateTableCell;
import com.corgibalance.components.table.TextTableCell;
import com.corgibalance.components.table.SelectTableCell;
import com.corgibalance.models.Budget;
import com.corgibalance.models.Currency;
import com.corgibalance.models.Tag;
import com.corgibalance.repositories.BudgetRepository;
import com.corgibalance.repositories.SettingsRepository;
import com.corgibalance.repositories.TagRepository;
import com.corgibalance.services.CurrencyFormatter;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class BudgetTableController extends BaseTableController<Budget, BudgetRepository> {

    private static final String BASE_CURRENCY_KEY = "overview.baseCurrencyId";

    private final CurrencyFormatter currencyFormatter = new CurrencyFormatter();
    private final List<Tag> tags;
    private final Long currencyId;

    @FXML
    private TableColumn<Budget, String> name;
    @FXML
    private TableColumn<Budget, Long> tag;
    @FXML
    private TableColumn<Budget, Long> plannedAmount;
    @FXML
    private TableColumn<Budget, LocalDate> startDate;
    @FXML
    private TableColumn<Budget, LocalDate> endDate;

    public BudgetTableController() {
        super(new BudgetRepository());
        this.tags = new TagRepository().findAll();
        this.currencyId = defaultCurrencyId();
    }

    @Override
    protected void configureColumns() {
        name.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        name.setCellFactory(_ -> new TextTableCell<>(Budget::getName, "+ Add budget"));
        name.setOnEditCommit(this::onNameCommitted);

        tag.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getTagId()));
        tag.setCellFactory(_ -> new SelectTableCell<>(tagIds(), this::tagName));
        tag.setOnEditCommit(this::onTagCommitted);

        plannedAmount.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getPlannedAmount()));
        plannedAmount.setCellFactory(_ -> new AmountTableCell<>(_ -> currencyId));
        plannedAmount.setOnEditCommit(this::onAmountCommitted);

        startDate.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getStartDate()));
        startDate.setCellFactory(_ -> new DateTableCell<>());
        startDate.setOnEditCommit(this::onStartDateCommitted);

        endDate.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getEndDate()));
        endDate.setCellFactory(_ -> new DateTableCell<>());
        endDate.setOnEditCommit(this::onEndDateCommitted);
    }

    private void onNameCommitted(TableColumn.CellEditEvent<Budget, String> event) {
        String newName = event.getNewValue() == null ? "" : event.getNewValue().trim();
        if (newName.isEmpty()) {
            refresh();
            return;
        }
        commit(event.getRowValue(), budget -> budget.setName(newName), true);
    }

    private void onTagCommitted(TableColumn.CellEditEvent<Budget, Long> event) {
        Budget budget = event.getRowValue();
        budget.setTagId(event.getNewValue());
        if (!isPlaceholder(budget)) {
            repository.update(budget);
            refresh();
        }
    }

    private void onAmountCommitted(TableColumn.CellEditEvent<Budget, Long> event) {
        Budget budget = event.getRowValue();
        if (isPlaceholder(budget)) {
            refresh();
            return;
        }
        commit(budget, b -> b.setPlannedAmount(event.getNewValue()), false);
    }

    private void onStartDateCommitted(TableColumn.CellEditEvent<Budget, LocalDate> event) {
        Budget budget = event.getRowValue();
        if (isPlaceholder(budget)) {
            refresh();
            return;
        }
        commit(budget, b -> b.setStartDate(event.getNewValue()), false);
    }

    private void onEndDateCommitted(TableColumn.CellEditEvent<Budget, LocalDate> event) {
        Budget budget = event.getRowValue();
        if (isPlaceholder(budget)) {
            refresh();
            return;
        }
        commit(budget, b -> b.setEndDate(event.getNewValue()), false);
    }

    private List<Long> tagIds() {
        return tags.stream().map(Tag::getId).toList();
    }

    private String tagName(Long tagId) {
        if (tagId == null) {
            return "";
        }
        for (Tag tag : tags) {
            if (tag.getId().equals(tagId)) {
                return tag.getName();
            }
        }
        return String.valueOf(tagId);
    }

    private Long defaultCurrencyId() {
        Optional<Long> saved = new SettingsRepository().getLong(BASE_CURRENCY_KEY);
        if (saved.isPresent() && currencyFormatter.currency(saved.get()) != null) {
            return saved.get();
        }
        List<Currency> currencies = currencyFormatter.currencies();
        return currencies.isEmpty() ? null : currencies.getFirst().getId();
    }

    @Override
    protected Budget newPlaceholder() {
        Budget budget = new Budget();
        if (!tags.isEmpty()) {
            budget.setTagId(tags.getFirst().getId());
        }
        LocalDate now = LocalDate.now();
        budget.setStartDate(now.withDayOfMonth(1));
        budget.setEndDate(now.withDayOfMonth(now.lengthOfMonth()));
        return budget;
    }

    @Override
    protected String deleteConfirmationText(Budget budget) {
        return "Delete budget \"" + budget.getName() + "\"?";
    }
}