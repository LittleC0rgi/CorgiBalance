package com.corgibalance.controllers.views;

import com.corgibalance.components.ProfitLossReport;
import com.corgibalance.models.Account;
import com.corgibalance.models.Currency;
import com.corgibalance.repositories.AccountRepository;
import com.corgibalance.repositories.SettingsRepository;
import com.corgibalance.repositories.TagRepository;
import com.corgibalance.repositories.TransactionRepository;
import com.corgibalance.services.CurrencyConverter;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
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
    private ComboBox<Object> accountCombo;
    @FXML
    private Label currencyLabel;
    @FXML
    private ComboBox<Currency> currencyCombo;
    @FXML
    private GridPane reportGrid;

    private final CurrencyConverter converter = new CurrencyConverter();
    private final TransactionRepository transactionRepository = new TransactionRepository();
    private final TagRepository tagRepository = new TagRepository();
    private final AccountRepository accountRepository = new AccountRepository();
    private final SettingsRepository settingsRepository = new SettingsRepository();
    private Long baseCurrencyId;

    @FXML
    private void initialize() {
        monthCombo.setCellFactory(monthCellFactory());
        monthCombo.setButtonCell(monthCellFactory().call(null));
        monthCombo.getItems().setAll(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);

        accountCombo.setCellFactory(accountCellFactory());
        accountCombo.setButtonCell(accountCellFactory().call(null));
        accountCombo.valueProperty().addListener((obs, old, val) -> {
            updateCurrencySelector();
            refresh();
        });

        currencyCombo.setCellFactory(currencyCellFactory());
        currencyCombo.setButtonCell(currencyCellFactory().call(null));
        currencyCombo.valueProperty().addListener((obs, old, val) -> refresh());

        baseCurrencyId = defaultBaseCurrencyId();
        loadAccounts();
        loadPeriod();
        monthCombo.valueProperty().addListener((obs, oldValue, newValue) -> refresh());
        yearCombo.valueProperty().addListener((obs, oldValue, newValue) -> refresh());
        refresh();
    }

    @Override
    public void onShow() {
        converter.reload();
        baseCurrencyId = defaultBaseCurrencyId();
        loadAccounts();
        loadPeriod();
        refresh();
    }

    private void loadAccounts() {
        List<Object> items = new ArrayList<>();
        items.add("All");
        for (Account account : accountRepository.findAll()) {
            if (!account.isHidden()) {
                items.add(account);
            }
        }
        Object selected = accountCombo.getValue();
        accountCombo.getItems().setAll(items);
        accountCombo.setValue(selected != null && items.contains(selected) ? selected : items.getFirst());
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

    private void updateCurrencySelector() {
        Object selected = accountCombo.getValue();
        if (selected instanceof Account account) {
            Long accountCurrencyId = account.getCurrencyId();
            if (accountCurrencyId != null && !accountCurrencyId.equals(baseCurrencyId)) {
                Currency accountCurrency = converter.currency(accountCurrencyId);
                Currency baseCurrency = converter.currency(baseCurrencyId);
                if (accountCurrency != null && baseCurrency != null) {
                    currencyCombo.getItems().setAll(accountCurrency, baseCurrency);
                    currencyCombo.setValue(accountCurrency);
                    showCurrencySelector(true);
                    return;
                }
            }
        }
        currencyCombo.getItems().clear();
        showCurrencySelector(false);
    }

    private void showCurrencySelector(boolean show) {
        currencyCombo.setVisible(show);
        currencyCombo.setManaged(show);
        currencyLabel.setVisible(show);
        currencyLabel.setManaged(show);
    }

    private void refresh() {
        if (baseCurrencyId == null) {
            baseCurrencyId = defaultBaseCurrencyId();
        }
        int year = yearCombo.getValue() == null ? LocalDate.now().getYear() : yearCombo.getValue();
        int month = monthCombo.getValue() == null ? LocalDate.now().getMonthValue() : monthCombo.getValue();

        Long accountId = null;
        Long displayCurrencyId = baseCurrencyId;

        Object selected = accountCombo.getValue();
        if (selected instanceof Account account) {
            accountId = account.getId();
            if (currencyCombo.isVisible() && currencyCombo.getValue() != null) {
                displayCurrencyId = currencyCombo.getValue().getId();
            }
        }

        ProfitLossReport.Data data = ProfitLossReport.compute(
                transactionRepository, tagRepository, converter, displayCurrencyId, accountId, year, month);
        ProfitLossReport.populate(reportGrid, data, converter, displayCurrencyId, true);
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
                setText(empty || month == null ? "" : month == 0 ? "All" : Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH));
            }
        };
    }

    private Callback<ListView<Object>, ListCell<Object>> accountCellFactory() {
        return list -> new ListCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item instanceof Account account) {
                    setText(account.getName());
                } else {
                    setText(String.valueOf(item));
                }
            }
        };
    }

    private Callback<ListView<Currency>, ListCell<Currency>> currencyCellFactory() {
        return list -> new ListCell<>() {
            @Override
            protected void updateItem(Currency currency, boolean empty) {
                super.updateItem(currency, empty);
                setText(empty || currency == null ? null : currency.getCode());
            }
        };
    }
}
