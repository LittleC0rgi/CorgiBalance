package com.corgibalance.components.views;

import com.corgibalance.components.HeroIcon;
import com.corgibalance.controllers.tables.RecentTransactionsTableController;
import com.corgibalance.models.Account;
import com.corgibalance.models.Budget;
import com.corgibalance.models.Currency;
import com.corgibalance.models.PlannedTransaction;
import com.corgibalance.models.RecurringTransaction;
import com.corgibalance.models.Tag;
import com.corgibalance.models.Transaction;
import com.corgibalance.models.TransactionType;
import com.corgibalance.repositories.AccountRepository;
import com.corgibalance.repositories.BudgetRepository;
import com.corgibalance.repositories.PlannedTransactionRepository;
import com.corgibalance.repositories.RecurringTransactionRepository;
import com.corgibalance.repositories.SettingsRepository;
import com.corgibalance.repositories.TagRepository;
import com.corgibalance.repositories.TransactionRepository;
import com.corgibalance.services.CurrencyConverter;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Callback;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class OverviewView extends View implements Refreshable {

    private static final String BASE_CURRENCY_KEY = "overview.baseCurrencyId";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final int NEAREST_LIMIT = 5;
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
    private VBox nearestList;
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
        super("Overview", "/fxml/views/Overview.fxml");
    }

    public static List<NearestPayment> nearestPayments(List<PlannedTransaction> planned, List<RecurringTransaction> recurring,
                                                       LocalDate today, int limit) {
        List<NearestPayment> overdue = new ArrayList<>();
        List<NearestPayment> upcoming = new ArrayList<>();
        for (PlannedTransaction p : planned) {
            (p.getPlannedDate().isBefore(today) ? overdue : upcoming).add(NearestPayment.of(p));
        }
        for (RecurringTransaction r : recurring) {
            (r.getNextDate().isBefore(today) ? overdue : upcoming).add(NearestPayment.of(r));
        }
        overdue.sort(Comparator.comparing(NearestPayment::date));
        upcoming.sort(Comparator.comparing(NearestPayment::date));
        overdue.addAll(upcoming.subList(0, Math.min(limit, upcoming.size())));
        return overdue;
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
            yearCombo.setValue(years.isEmpty() ? LocalDate.now().getYear() : years.getFirst());
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
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        for (Budget budget : budgetRepository.findAll()) {
            if (budget.getStartDate().isAfter(to) || budget.getEndDate().isBefore(from)) {
                continue;
            }
            budgetList.getChildren().add(budgetRow(budget, baseCurrencyId));
        }

        refreshNearestPayments();
    }

    private void refreshNearestPayments() {
        Map<Long, Long> accountCurrencies = new HashMap<>();
        Map<Long, String> tagColors = new HashMap<>();
        for (Account account : accountRepository.findAll()) {
            accountCurrencies.put(account.getId(), account.getCurrencyId());
        }
        for (Tag tag : new TagRepository().findAll()) {
            tagColors.put(tag.getId(), tag.getColor());
        }
        List<NearestPayment> payments = nearestPayments(
                new PlannedTransactionRepository().findAll(),
                new RecurringTransactionRepository().findActiveUpcoming(),
                LocalDate.now(), NEAREST_LIMIT);

        nearestList.getChildren().clear();
        LocalDate today = LocalDate.now();
        for (NearestPayment payment : payments) {
            nearestList.getChildren().add(paymentRow(payment, today, accountCurrencies, tagColors));
        }
    }

    private HBox paymentRow(NearestPayment payment, LocalDate today, Map<Long, Long> accountCurrencies,
                            Map<Long, String> tagColors) {
        boolean overdue = payment.date().isBefore(today);

        Button confirm = new Button();
        confirm.setGraphic(new HeroIcon(HeroIcon.Icon.CHECK));
        confirm.getStyleClass().addAll("btn", "btn--transparent");
        confirm.setTooltip(new Tooltip("Confirm"));
        confirm.setOnAction(_ -> confirmPayment(payment));

        Label description = new Label(paymentText(payment));
        description.getStyleClass().add("nearest__desc");
        description.setMaxWidth(Double.MAX_VALUE);

        Label date = new Label(payment.date().format(DATE_FORMAT));
        date.getStyleClass().add("nearest__date");

        Label amount = new Label(converter.format(payment.amount(), accountCurrencies.get(payment.accountId())));
        amount.getStyleClass().add("nearest__amount");
        amount.getStyleClass().add(payment.type() == TransactionType.EXPENSE ? "nearest__amount--expense" : "nearest__amount--income");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(date, tagDot(payment.tagId(), tagColors), description, spacer, amount, confirm, deleteButton(payment));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setSpacing(6);
        row.getStyleClass().add("nearest__row");
        if (overdue) {
            row.getStyleClass().add("nearest__row--overdue");
        }
        return row;
    }

    private Button deleteButton(NearestPayment payment) {
        Button delete = new Button();
        delete.setGraphic(new HeroIcon(HeroIcon.Icon.X_MARK));
        delete.getStyleClass().addAll("btn", "btn--danger-transparent");
        delete.setTooltip(new Tooltip("Delete"));
        delete.setOnAction(_ -> deletePayment(payment));
        return delete;
    }

    private Circle tagDot(Long tagId, Map<Long, String> tagColors) {
        String color = tagId == null ? null : tagColors.get(tagId);
        if (color == null) {
            return null;
        }
        try {
            return new Circle(4, Color.web(color));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void confirmPayment(NearestPayment payment) {
        try {
            if (payment.planned != null) {
                createTransaction(payment.planned.getAccountId(), payment.planned.getTagId(), payment.planned.getAmount(),
                        payment.planned.getDescription(), payment.planned.getTransactionType(), payment.planned.getPlannedDate());
                new PlannedTransactionRepository().delete(payment.planned);
            } else {
                RecurringTransaction recurring = payment.recurring;
                createTransaction(recurring.getAccountId(), recurring.getTagId(), recurring.getAmount(),
                        recurring.getDescription(), recurring.getTransactionType(), recurring.getNextDate());
                LocalDate next = CalendarView.nextOccurrence(recurring.getNextDate(), recurring.getInterval());
                if (recurring.getEndDate() != null && next.isAfter(recurring.getEndDate())) {
                    recurring.setActive(false);
                } else {
                    recurring.setNextDate(next);
                }
                new RecurringTransactionRepository().update(recurring);
            }
            refresh();
        } catch (RuntimeException e) {
            showError(e);
        }
    }

    private void deletePayment(NearestPayment payment) {
        try {
            if (payment.planned != null) {
                new PlannedTransactionRepository().delete(payment.planned);
            } else {
                new RecurringTransactionRepository().delete(payment.recurring);
            }
            refresh();
        } catch (RuntimeException e) {
            showError(e);
        }
    }

    private void createTransaction(Long accountId, Long tagId, long amount, String description,
                                   TransactionType type, LocalDate date) {
        Transaction transaction = new Transaction();
        transaction.setAccountId(accountId);
        transaction.setTagId(tagId);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setTransactionType(type);
        transaction.setTransactionDate(date);
        new TransactionRepository().create(transaction);
    }

    private void showError(RuntimeException e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }

    private String paymentText(NearestPayment payment) {
        if (payment.description() != null && !payment.description().isBlank()) {
            return payment.description();
        }
        return payment.type() == TransactionType.EXPENSE ? "Planned expense" : "Planned income";
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

    public record NearestPayment(PlannedTransaction planned, RecurringTransaction recurring) {

        public static NearestPayment of(PlannedTransaction p) {
            return new NearestPayment(p, null);
        }

        public static NearestPayment of(RecurringTransaction r) {
            return new NearestPayment(null, r);
        }

        public LocalDate date() {
            return planned != null ? planned.getPlannedDate() : recurring.getNextDate();
        }

        public long amount() {
            return planned != null ? planned.getAmount() : recurring.getAmount();
        }

        public Long accountId() {
            return planned != null ? planned.getAccountId() : recurring.getAccountId();
        }

        public Long tagId() {
            return planned != null ? planned.getTagId() : recurring.getTagId();
        }

        public TransactionType type() {
            return planned != null ? planned.getTransactionType() : recurring.getTransactionType();
        }

        public String description() {
            return planned != null ? planned.getDescription() : recurring.getDescription();
        }

        public boolean isRecurring() {
            return recurring != null;
        }
    }
}
