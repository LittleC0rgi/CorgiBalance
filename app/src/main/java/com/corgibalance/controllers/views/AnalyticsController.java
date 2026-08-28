package com.corgibalance.controllers.views;

import com.corgibalance.components.ProfitLossReport;
import com.corgibalance.models.Currency;
import com.corgibalance.repositories.SettingsRepository;
import com.corgibalance.repositories.TagRepository;
import com.corgibalance.repositories.TransactionRepository;
import com.corgibalance.services.CurrencyConverter;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
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

        ProfitLossReport.Data data = ProfitLossReport.compute(
                transactionRepository, tagRepository, converter, baseCurrencyId, year, month);
        ProfitLossReport.populate(reportGrid, data, converter, baseCurrencyId);
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