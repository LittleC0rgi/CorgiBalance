package com.corgibalance.controllers.views;

import com.corgibalance.models.Currency;
import com.corgibalance.models.Tag;
import com.corgibalance.models.TransactionType;
import com.corgibalance.repositories.SettingsRepository;
import com.corgibalance.repositories.TagRepository;
import com.corgibalance.repositories.TransactionRepository;
import com.corgibalance.services.CurrencyConverter;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Callback;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class AnalyticsController implements Refreshable {

    private static final String BASE_CURRENCY_KEY = "overview.baseCurrencyId";

    @FXML
    private ComboBox<Integer> monthCombo;
    @FXML
    private ComboBox<Integer> yearCombo;
    @FXML
    private GridPane reportGrid;

    private final CurrencyConverter converter = new CurrencyConverter();
    private final TransactionRepository transactionRepository = new TransactionRepository();
    private final TagRepository tagRepository = new TagRepository();
    private final SettingsRepository settingsRepository = new SettingsRepository();
    private Long baseCurrencyId;

    @FXML
    private void initialize() {
        monthCombo.getItems().setAll(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        monthCombo.setCellFactory(monthCellFactory());
        monthCombo.setButtonCell(monthCellFactory().call(null));

        baseCurrencyId = defaultBaseCurrencyId();
        loadPeriod();
        monthCombo.valueProperty().addListener((obs, oldValue, newValue) -> refresh());
        yearCombo.valueProperty().addListener((obs, oldValue, newValue) -> refresh());
        refresh();
    }

    @Override
    public void onShow() {
        converter.reload();
        baseCurrencyId = defaultBaseCurrencyId();
        loadPeriod();
        refresh();
    }

    private void loadPeriod() {
        List<Integer> years = transactionRepository.availableYears();
        String latest = transactionRepository.latestYearMonth();
        int defaultYear = latest == null ? LocalDate.now().getYear() : Integer.parseInt(latest.substring(0, 4));
        int defaultMonth = latest == null ? LocalDate.now().getMonthValue() : Integer.parseInt(latest.substring(5, 7));

        boolean multiYear = years.size() > 1;
        yearCombo.setVisible(multiYear);
        yearCombo.setManaged(multiYear);
        if (multiYear) {
            Integer selected = yearCombo.getValue();
            yearCombo.getItems().setAll(years);
            yearCombo.setValue(selected != null && years.contains(selected) ? selected : defaultYear);
        } else {
            yearCombo.setValue(years.isEmpty() ? LocalDate.now().getYear() : years.getFirst());
        }
        if (monthCombo.getValue() == null) {
            monthCombo.setValue(defaultMonth);
        }
    }

    private void refresh() {
        if (baseCurrencyId == null) {
            baseCurrencyId = defaultBaseCurrencyId();
        }
        int year = yearCombo.getValue() == null ? LocalDate.now().getYear() : yearCombo.getValue();
        int month = monthCombo.getValue() == null ? LocalDate.now().getMonthValue() : monthCombo.getValue();

        long totalIncome = sumForPeriod(TransactionType.INCOME, year, month);
        long totalExpense = sumForPeriod(TransactionType.EXPENSE, year, month);

        Map<Long, long[]> tagTotals = mergeTagTotals(
                transactionRepository.sumByTag(TransactionType.INCOME, year, month),
                transactionRepository.sumByTag(TransactionType.EXPENSE, year, month));
        long taggedIncome = tagTotals.values().stream().mapToLong(t -> t[0]).sum();
        long taggedExpense = tagTotals.values().stream().mapToLong(t -> t[1]).sum();

        reportGrid.getChildren().removeIf(node -> {
            Integer row = GridPane.getRowIndex(node);
            return row != null && row > 0;
        });

        Map<Long, String> names = new HashMap<>();
        for (Tag tag : tagRepository.findAll()) {
            names.put(tag.getId(), tag.getName());
        }
        List<Long> tagIds = new ArrayList<>(tagTotals.keySet());
        tagIds.sort(Comparator.comparing(id -> names.getOrDefault(id, String.valueOf(id))));
        int row = 1;
        for (Long tagId : tagIds) {
            long[] totals = tagTotals.get(tagId);
            row = addRow(row, names.getOrDefault(tagId, String.valueOf(tagId)), totals[0], totals[1]);
        }
        long uncategorizedIncome = totalIncome - taggedIncome;
        long uncategorizedExpense = totalExpense - taggedExpense;
        if (uncategorizedIncome != 0 || uncategorizedExpense != 0) {
            row = addRow(row, "Uncategorized", uncategorizedIncome, uncategorizedExpense);
        }
        row = addSeparator(row);
        row = addTotalRow(row, totalIncome, totalExpense);
        addProfitRow(row, totalIncome - totalExpense);
    }

    private int addRow(int row, String name, long income, long expense) {
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().addAll("report__cell", "report__name");
        reportGrid.add(nameLabel, 0, row);
        reportGrid.add(valueCell(income, "report__income"), 1, row);
        reportGrid.add(valueCell(expense, "report__expense"), 2, row);
        return row + 1;
    }

    private int addSeparator(int row) {
        Region separator = new Region();
        separator.getStyleClass().add("report__separator");
        separator.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(separator, Priority.ALWAYS);
        GridPane.setColumnSpan(separator, 3);
        reportGrid.add(separator, 0, row);
        return row + 1;
    }

    private int addTotalRow(int row, long income, long expense) {
        Label nameLabel = new Label("Total");
        nameLabel.getStyleClass().addAll("report__cell", "report__name", "report__total");
        reportGrid.add(nameLabel, 0, row);
        reportGrid.add(totalCell(income, "report__income"), 1, row);
        reportGrid.add(totalCell(expense, "report__expense"), 2, row);
        return row + 1;
    }

    private void addProfitRow(int row, long profit) {
        Label nameLabel = new Label("Profit / Loss");
        nameLabel.getStyleClass().addAll("report__cell", "report__name", "report__profit-name");
        reportGrid.add(nameLabel, 0, row);
        String text = profit > 0 ? "+" + converter.format(profit, baseCurrencyId)
                : converter.format(profit, baseCurrencyId);
        Label valueLabel = new Label(text);
        valueLabel.getStyleClass().addAll("report__cell", profit >= 0 ? "report__profit" : "report__loss");
        valueLabel.setMaxWidth(Double.MAX_VALUE);
        valueLabel.setAlignment(Pos.CENTER_RIGHT);
        reportGrid.add(valueLabel, 1, row);
    }

    private Label valueCell(long amount, String toneClass) {
        Label label = new Label(converter.format(amount, baseCurrencyId));
        label.getStyleClass().addAll("report__cell", toneClass);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER_RIGHT);
        return label;
    }

    private Label totalCell(long amount, String toneClass) {
        Label label = valueCell(amount, toneClass);
        label.getStyleClass().add("report__total");
        return label;
    }

    private long sumForPeriod(TransactionType type, int year, int month) {
        Map<Long, Long> totals = transactionRepository.sumByCurrency(type, year, month);
        long result = 0;
        for (Map.Entry<Long, Long> entry : totals.entrySet()) {
            result += converter.convert(Math.abs(entry.getValue()), entry.getKey(), baseCurrencyId);
        }
        return result;
    }

    private Map<Long, long[]> mergeTagTotals(Map<Long, Map<Long, Long>> incomeByTag,
                                             Map<Long, Map<Long, Long>> expenseByTag) {
        Map<Long, long[]> totals = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Long>> entry : incomeByTag.entrySet()) {
            long income = sum(entry.getValue());
            if (income != 0) {
                totals.computeIfAbsent(entry.getKey(), _ -> new long[2])[0] = income;
            }
        }
        for (Map.Entry<Long, Map<Long, Long>> entry : expenseByTag.entrySet()) {
            long expense = sum(entry.getValue());
            if (expense != 0) {
                totals.computeIfAbsent(entry.getKey(), _ -> new long[2])[1] = expense;
            }
        }
        return totals;
    }

    private long sum(Map<Long, Long> currencyTotals) {
        long result = 0;
        for (Map.Entry<Long, Long> entry : currencyTotals.entrySet()) {
            result += converter.convert(Math.abs(entry.getValue()), entry.getKey(), baseCurrencyId);
        }
        return result;
    }

    private Long defaultBaseCurrencyId() {
        Optional<Long> saved = settingsRepository.getLong(BASE_CURRENCY_KEY);
        if (saved.isPresent() && converter.currency(saved.get()) != null) {
            return saved.get();
        }
        List<Currency> currencies = converter.currencies();
        return currencies.isEmpty() ? null : currencies.getFirst().getId();
    }

    private Callback<ListView<Integer>, ListCell<Integer>> monthCellFactory() {
        return list -> new ListCell<>() {
            @Override
            protected void updateItem(Integer month, boolean empty) {
                super.updateItem(month, empty);
                setText(empty || month == null ? "" : Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH));
            }
        };
    }
}