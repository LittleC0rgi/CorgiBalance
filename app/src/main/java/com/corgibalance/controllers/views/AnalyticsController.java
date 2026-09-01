package com.corgibalance.controllers.views;

import com.corgibalance.components.MonthYearPicker;
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

import java.util.ArrayList;
import java.util.List;

public class AnalyticsController implements Refreshable {

    @FXML
    private ComboBox<Integer> monthCombo;
    @FXML
    private ComboBox<Integer> yearCombo;
    @FXML
    private ComboBox<Account> accountCombo;
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
    private MonthYearPicker period;
    private Long baseCurrencyId;

    @FXML
    private void initialize() {
        period = new MonthYearPicker(monthCombo, yearCombo, true);
        period.initialize();

        accountCombo.setCellFactory(accountCellFactory());
        accountCombo.setButtonCell(accountCellFactory().call(null));
        accountCombo.valueProperty().addListener((obs, old, val) -> {
            updateCurrencySelector();
            refresh();
        });

        currencyCombo.setCellFactory(currencyCellFactory());
        currencyCombo.setButtonCell(currencyCellFactory().call(null));
        currencyCombo.valueProperty().addListener((obs, old, val) -> refresh());

        baseCurrencyId = converter.baseCurrencyId(settingsRepository);
        loadAccounts();
        loadPeriod();
        period.setOnChange(this::refresh);
        refresh();
    }

    @Override
    public void onShow() {
        converter.reload();
        baseCurrencyId = converter.baseCurrencyId(settingsRepository);
        loadAccounts();
        loadPeriod();
        refresh();
    }

    private void loadAccounts() {
        List<Account> items = new ArrayList<>();
        items.add(null);
        for (Account account : accountRepository.findAll()) {
            if (!account.isHidden()) {
                items.add(account);
            }
        }
        Account selected = accountCombo.getValue();
        accountCombo.getItems().setAll(items);
        accountCombo.setValue(selected != null && items.contains(selected) ? selected : null);
    }

    private void loadPeriod() {
        period.load(transactionRepository.availableYears(), transactionRepository.latestYearMonth(), false);
    }

    private void updateCurrencySelector() {
        Account selected = accountCombo.getValue();
        if (selected != null) {
            Long accountCurrencyId = selected.getCurrencyId();
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
            baseCurrencyId = converter.baseCurrencyId(settingsRepository);
        }
        int year = period.year();
        int month = period.month();

        Long accountId = null;
        Long displayCurrencyId = baseCurrencyId;

        Account selected = accountCombo.getValue();
        if (selected != null) {
            accountId = selected.getId();
            if (currencyCombo.isVisible() && currencyCombo.getValue() != null) {
                displayCurrencyId = currencyCombo.getValue().getId();
            }
        }

        ProfitLossReport.Data data = ProfitLossReport.compute(
                transactionRepository, tagRepository, converter, displayCurrencyId, accountId, year, month);
        ProfitLossReport.populate(reportGrid, data, converter, displayCurrencyId, true);
    }

    private Callback<ListView<Account>, ListCell<Account>> accountCellFactory() {
        return list -> new ListCell<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "All" : item.getName());
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
