package com.corgibalance.components.views;

import com.corgibalance.controllers.RecentTransactionsTableController;
import com.corgibalance.models.Account;
import com.corgibalance.models.Budget;
import com.corgibalance.models.Currency;
import com.corgibalance.models.TransactionType;
import com.corgibalance.repositories.AccountRepository;
import com.corgibalance.repositories.BudgetRepository;
import com.corgibalance.repositories.SettingsRepository;
import com.corgibalance.repositories.TransactionRepository;
import com.corgibalance.services.CurrencyConverter;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class OverviewView extends View implements Refreshable {

    private static final String BASE_CURRENCY_KEY = "overview.baseCurrencyId";

    @FXML
    private Label balanceValue;
    @FXML
    private Label incomeValue;
    @FXML
    private Label expenseValue;
    @FXML
    private VBox accountList;
    @FXML
    private VBox budgetList;
    @FXML
    private Hyperlink allAccountsLink;
    @FXML
    private Hyperlink allBudgetsLink;
    @FXML
    private ComboBox<Long> baseCurrencyCombo;
    @FXML
    private ComboBox<Integer> monthCombo;
    @FXML
    private ComboBox<Integer> yearCombo;
    @FXML
    private RecentTransactionsTableController RecentTransactionsTableController;

    private CurrencyConverter converter;
    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private BudgetRepository budgetRepository;
    private SettingsRepository settingsRepository;
    @Setter
    private Consumer<String> navigationHandler;

    public OverviewView() {
        super("Overview", "/fxml/views/overview.fxml");
    }

    @FXML
    private void initialize() {
        converter = new CurrencyConverter();
        accountRepository = new AccountRepository();
        transactionRepository = new TransactionRepository();
        budgetRepository = new BudgetRepository();
        settingsRepository = new SettingsRepository();

        monthCombo.getItems().setAll(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        monthCombo.setCellFactory(monthCellFactory());
        monthCombo.setButtonCell(monthCellFactory().call(null));

        baseCurrencyCombo.setCellFactory(currencyCellFactory());
        baseCurrencyCombo.setButtonCell(currencyCellFactory().call(null));

        loadCurrencies();
        selectSavedBaseCurrency();
        loadPeriod(true);

        allAccountsLink.setOnAction(event -> onAllAccounts());
        allBudgetsLink.setOnAction(event -> onAllBudgets());

        baseCurrencyCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                settingsRepository.setLong(BASE_CURRENCY_KEY, newValue);
            }
            refresh();
        });
        monthCombo.valueProperty().addListener((obs, oldValue, newValue) -> refresh());
        yearCombo.valueProperty().addListener((obs, oldValue, newValue) -> refresh());

        refresh();
    }

    @Override
    public void onShow() {
        loadCurrencies();
        loadPeriod(false);
        RecentTransactionsTableController.reload();
        refresh();
    }

    @FXML
    private void onAllAccounts() {
        if (navigationHandler != null) {
            navigationHandler.accept("Accounts");
        }
    }

    @FXML
    private void onAllBudgets() {
        if (navigationHandler != null) {
            navigationHandler.accept("Budgets");
        }
    }

    private void loadCurrencies() {
        converter.reload();
        Long selected = baseCurrencyCombo.getValue();
        baseCurrencyCombo.getItems().setAll(converter.currencies().stream().map(Currency::getId).toList());
        if (selected != null && converter.currency(selected) != null) {
            baseCurrencyCombo.setValue(selected);
        }
    }

    private void selectSavedBaseCurrency() {
        Optional<Long> saved = settingsRepository.getLong(BASE_CURRENCY_KEY);
        if (saved.isPresent() && converter.currency(saved.get()) != null) {
            baseCurrencyCombo.setValue(saved.get());
        } else if (!baseCurrencyCombo.getItems().isEmpty()) {
            baseCurrencyCombo.setValue(baseCurrencyCombo.getItems().getFirst());
        }
    }

    private void loadPeriod(boolean applyDefaults) {
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
            yearCombo.setValue(years.isEmpty() ? LocalDate.now().getYear() : years.get(0));
        }
        if (applyDefaults) {
            monthCombo.setValue(defaultMonth);
        } else if (monthCombo.getValue() == null) {
            monthCombo.setValue(defaultMonth);
        }
    }

    private void refresh() {
        Long baseCurrencyId = baseCurrencyCombo.getValue();

        long totalBalance = 0;
        for (Account account : accountRepository.findAll()) {
            long balance = accountRepository.currentBalance(account.getId());
            totalBalance += converter.convert(balance, account.getCurrencyId(), baseCurrencyId);
        }
        balanceValue.setText(converter.format(totalBalance, baseCurrencyId));

        int year = yearCombo.getValue() == null ? LocalDate.now().getYear() : yearCombo.getValue();
        int month = monthCombo.getValue() == null ? LocalDate.now().getMonthValue() : monthCombo.getValue();

        long income = sumForPeriod(TransactionType.INCOME, year, month, baseCurrencyId);
        long expense = sumForPeriod(TransactionType.EXPENSE, year, month, baseCurrencyId);

        incomeValue.setText((income != 0 ? "+" : "") + converter.format(income, baseCurrencyId));
        expenseValue.setText((expense != 0 ? "-" : "") + converter.format(expense, baseCurrencyId));

        toggleColor(incomeValue, "card__value--income", income == 0);
        toggleColor(expenseValue, "card__value--expense", expense == 0);

        accountList.getChildren().clear();
        for (Account account : accountRepository.findAll()) {
            long balance = accountRepository.currentBalance(account.getId());
            Label name = new Label(account.getName());
            name.getStyleClass().add("card__text");
            Label amount = new Label(converter.format(balance, account.getCurrencyId()));
            amount.getStyleClass().add("card__text");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox row = new HBox(name, spacer, amount);
            accountList.getChildren().add(row);
        }

        budgetList.getChildren().clear();
        for (Budget budget : budgetRepository.findAll()) {
            budgetList.getChildren().add(budgetRow(budget, baseCurrencyId));
        }
    }

    private VBox budgetRow(Budget budget, Long baseCurrencyId) {
        Map<Long, Long> totals = transactionRepository.sumByCurrency(
                TransactionType.EXPENSE, budget.getTagId(), budget.getStartDate(), budget.getEndDate());
        long spent = 0;
        for (Map.Entry<Long, Long> entry : totals.entrySet()) {
            spent += converter.convert(Math.abs(entry.getValue()), entry.getKey(), baseCurrencyId);
        }
        long planned = budget.getPlannedAmount();
        double ratio = planned <= 0 ? 0 : Math.min(1.0, (double) spent / planned);
        boolean over = planned > 0 && spent >= planned;
        int percent = (int) Math.round((planned <= 0 ? 0 : (double) spent / planned) * 100);

        Label name = new Label(budget.getName());
        name.getStyleClass().add("budget__name");
        Label amount = new Label(converter.format(spent, baseCurrencyId)
                + " / " + converter.format(planned, baseCurrencyId));
        amount.getStyleClass().add("budget__amount");
        Label percentLabel = new Label(percent + "%");
        percentLabel.getStyleClass().add(over ? "budget__percent--over" : "budget__percent");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(name, spacer, amount, percentLabel);
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

    private void toggleColor(Label label, String styleClass, boolean valueIsZero) {
        if (valueIsZero) {
            label.getStyleClass().remove(styleClass);
        } else if (!label.getStyleClass().contains(styleClass)) {
            label.getStyleClass().add(styleClass);
        }
    }

    private long sumForPeriod(TransactionType type, int year, int month, Long baseCurrencyId) {
        Map<Long, Long> totals = transactionRepository.sumByCurrency(type, year, month);
        long result = 0;
        for (Map.Entry<Long, Long> entry : totals.entrySet()) {
            result += converter.convert(Math.abs(entry.getValue()), entry.getKey(), baseCurrencyId);
        }
        return result;
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

    private Callback<ListView<Long>, ListCell<Long>> currencyCellFactory() {
        return list -> new ListCell<>() {
            @Override
            protected void updateItem(Long id, boolean empty) {
                super.updateItem(id, empty);
                Currency currency = empty || id == null ? null : converter.currency(id);
                setText(currency == null ? "" : currency.getCode());
            }
        };
    }
}
