package com.corgibalance.components.views;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.Setter;
import com.corgibalance.components.table.Cells;
import com.corgibalance.components.table.ColumnSpec;
import com.corgibalance.components.table.CrudTable;
import com.corgibalance.components.table.FormSpec;
import com.corgibalance.models.Account;
import com.corgibalance.models.Budget;
import com.corgibalance.models.Currency;
import com.corgibalance.models.Tag;
import com.corgibalance.models.Transaction;
import com.corgibalance.models.TransactionType;
import com.corgibalance.repositories.AccountRepository;
import com.corgibalance.repositories.BudgetRepository;
import com.corgibalance.repositories.SettingsRepository;
import com.corgibalance.repositories.TagRepository;
import com.corgibalance.repositories.TransactionRepository;
import com.corgibalance.services.CurrencyConverter;
import com.corgibalance.services.CurrencyFormatter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
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
    private VBox transactionList;
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

        CrudTable<Transaction> table = buildTransactionsTable();
        table.setOnDataChanged(this::refresh);
        transactionList.getChildren().add(table);

        refresh();
    }

    @Override
    public void onShow() {
        loadCurrencies();
        loadPeriod(false);
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

    private void onAllTransactions() {
        if (navigationHandler != null) {
            navigationHandler.accept("Transactions");
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
            baseCurrencyCombo.setValue(baseCurrencyCombo.getItems().get(0));
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

        incomeValue.setText("+" + converter.format(income, baseCurrencyId));
        expenseValue.setText("-" + converter.format(expense, baseCurrencyId));

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

    private CrudTable<Transaction> buildTransactionsTable() {
        CurrencyFormatter currencyFormatter = new CurrencyFormatter();

        List<Long> accountIds = new ArrayList<>();
        Map<Long, String> accountLabels = new HashMap<>();
        Map<Long, Long> accountCurrencyIds = new HashMap<>();
        for (Account account : accountRepository.findAll()) {
            accountIds.add(account.getId());
            accountLabels.put(account.getId(), account.getName());
            if (account.getCurrencyId() != null) {
                accountCurrencyIds.put(account.getId(), account.getCurrencyId());
            }
        }

        Map<Long, String> tagLabels = new HashMap<>();
        Map<Long, String> tagColors = new HashMap<>();
        List<Long> tagIds = new ArrayList<>();
        TagRepository tagRepository = new TagRepository();
        for (Tag tag : tagRepository.findAll()) {
            tagIds.add(tag.getId());
            tagLabels.put(tag.getId(), tag.getName());
            tagColors.put(tag.getId(), tag.getColor());
        }

        List<TransactionType> transactionTypes = List.of(TransactionType.INCOME, TransactionType.EXPENSE);

        ColumnSpec<Transaction> date = ColumnSpec.<Transaction>builder("Date")
                .width(120)
                .value(Transaction::getTransactionDate)
                .editable(Cells.dateEditable(),
                        (transaction, value) -> transaction.setTransactionDate((LocalDate) value))
                .form(FormSpec.date())
                .required()
                .build();
        ColumnSpec<Transaction> type = ColumnSpec.<Transaction>builder("Type")
                .width(110)
                .value(Transaction::getTransactionType)
                .editable(Cells.enumEditable(transactionTypes),
                        (transaction, value) -> transaction.setTransactionType((TransactionType) value))
                .form(FormSpec.enumValue(transactionTypes))
                .required()
                .build();
        ColumnSpec<Transaction> account = ColumnSpec.<Transaction>builder("Account")
                .width(160)
                .value(Transaction::getAccountId)
                .editable(Cells.comboEditable(accountIds, accountLabels),
                        (transaction, value) -> transaction.setAccountId((Long) value))
                .form(FormSpec.combo(accountIds, accountLabels))
                .required()
                .build();
        ColumnSpec<Transaction> toAccount = ColumnSpec.<Transaction>builder("To account")
                .width(140)
                .value(Transaction::getToAccountId)
                .editable(Cells.comboEditable(accountIds, accountLabels),
                        (transaction, value) -> transaction.setToAccountId((Long) value))
                .build();
        ColumnSpec<Transaction> tag = ColumnSpec.<Transaction>builder("Tag")
                .width(140)
                .value(Transaction::getTagId)
                .editable(Cells.tagEditable(tagIds, tagLabels, tagColors),
                        (transaction, value) -> transaction.setTagId((Long) value))
                .form(FormSpec.combo(tagIds, tagLabels))
                .build();
        ColumnSpec<Transaction> description = ColumnSpec.<Transaction>builder("Description")
                .width(220)
                .value(Transaction::getDescription)
                .editable(Cells.editableText(),
                        (transaction, value) -> transaction.setDescription((String) value))
                .form(FormSpec.text())
                .build();
        ColumnSpec<Transaction> rate = ColumnSpec.<Transaction>builder("Rate")
                .width(90)
                .value(Transaction::getRate)
                .build();
        ColumnSpec<Transaction> amount = ColumnSpec.<Transaction>builder("Amount")
                .width(140)
                .value(Transaction::getAmount)
                .editable(Cells.amountEditable(currencyFormatter,
                                transaction -> accountCurrencyIds.get(transaction.getAccountId())),
                        (transaction, value) -> transaction.setAmount(currencyFormatter.toMinorUnits(
                                (BigDecimal) value, accountCurrencyIds.get(transaction.getAccountId()))))
                .form(FormSpec.decimal())
                .required()
                .build();

        CrudTable<Transaction> table = new CrudTable<>(
                "Recent transactions", transactionRepository,
                () -> transactionRepository.findLatest(10), Transaction::new,
                List.of(date, type, account, toAccount, tag, description, rate, amount));

        Hyperlink allLink = new Hyperlink("All");
        allLink.getStyleClass().add("card__link");
        allLink.setOnAction(event -> onAllTransactions());
        table.addToolbarNode(allLink);

        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
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
