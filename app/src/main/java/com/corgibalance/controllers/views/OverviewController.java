package com.corgibalance.controllers.views;

import com.corgibalance.components.AccountListComponent;
import com.corgibalance.components.BudgetListComponent;
import com.corgibalance.components.NearestPaymentsComponent;
import com.corgibalance.components.ProfitLossReport;
import com.corgibalance.controllers.tables.RecentTransactionsTableController;
import com.corgibalance.models.Currency;
import com.corgibalance.repositories.SettingsRepository;
import com.corgibalance.repositories.TagRepository;
import com.corgibalance.repositories.TransactionRepository;
import com.corgibalance.services.AccountFolderService;
import com.corgibalance.services.CurrencyConverter;
import com.corgibalance.services.NearestPaymentService;
import com.corgibalance.services.OverviewService;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

public class OverviewController implements Refreshable {

    private static final String BASE_CURRENCY_KEY = "overview.baseCurrencyId";
    private static final String SHOW_EXPENSES_BY_TAG_KEY = "overview.showExpensesByTag";

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
    private GridPane profitLossReport;
    @FXML
    private VBox tagExpenseCard;
    @FXML
    private GridPane grid;
    @FXML
    private RecentTransactionsTableController RecentTransactionsTableController;

    private CurrencyConverter converter;
    private SettingsRepository settingsRepository;
    private TransactionRepository transactionRepository;
    private TagRepository tagRepository;

    private AccountFolderService accountFolderService;
    private NearestPaymentService nearestPaymentService;
    private OverviewService overviewService;

    private AccountListComponent accountListComponent;
    private BudgetListComponent budgetListComponent;
    private NearestPaymentsComponent nearestPaymentsComponent;

    @Setter
    private Consumer<String> navigationHandler;

    @FXML
    private void initialize() {
        converter = new CurrencyConverter();
        settingsRepository = new SettingsRepository();
        transactionRepository = new TransactionRepository();
        tagRepository = new TagRepository();

        accountFolderService = new AccountFolderService();
        nearestPaymentService = new NearestPaymentService();
        overviewService = new OverviewService(converter);
        accountListComponent = new AccountListComponent(accountFolderService, converter, this::refresh);
        budgetListComponent = new BudgetListComponent(overviewService, converter);
        nearestPaymentsComponent = new NearestPaymentsComponent(nearestPaymentService, converter, this::refresh);

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

    @FXML
    private void onAddFolder() {
        accountListComponent.addFolder();
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
        int year = yearCombo.getValue() == null ? LocalDate.now().getYear() : yearCombo.getValue();
        int month = monthCombo.getValue() == null ? LocalDate.now().getMonthValue() : monthCombo.getValue();

        balanceValue.setText(converter.format(overviewService.totalBalance(baseCurrencyId), baseCurrencyId));

        long income = overviewService.income(year, month, baseCurrencyId);
        long expense = overviewService.expense(year, month, baseCurrencyId);
        incomeValue.setText((income != 0 ? "+" : "") + converter.format(income, baseCurrencyId));
        expenseValue.setText((expense != 0 ? "-" : "") + converter.format(expense, baseCurrencyId));
        toggleColor(incomeValue, "card__value--income", income == 0);
        toggleColor(expenseValue, "card__value--expense", expense == 0);

        accountListComponent.render(accountList, baseCurrencyId);
        budgetListComponent.render(budgetList, year, month, baseCurrencyId);
        nearestPaymentsComponent.render(nearestList);
        renderTagReport(year, month, baseCurrencyId);
    }

    private void renderTagReport(int year, int month, Long baseCurrencyId) {
        boolean showByTag = settingsRepository.get(SHOW_EXPENSES_BY_TAG_KEY)
                .map(Boolean::parseBoolean)
                .orElse(true);
        tagExpenseCard.setVisible(showByTag);
        tagExpenseCard.setManaged(showByTag);
        ColumnConstraints col3 = grid.getColumnConstraints().get(3);
        if (showByTag) {
            for (int i = 0; i < 4; i++) {
                grid.getColumnConstraints().get(i).setPercentWidth(25);
                grid.getColumnConstraints().get(i).setHgrow(Priority.ALWAYS);
                grid.getColumnConstraints().get(i).setMaxWidth(Double.MAX_VALUE);
                grid.getColumnConstraints().get(i).setMinWidth(0);
            }
            ProfitLossReport.Data data = ProfitLossReport.compute(
                    transactionRepository, tagRepository, converter, baseCurrencyId, year, month);
            ProfitLossReport.populate(profitLossReport, data, converter, baseCurrencyId, false);
        } else {
            for (int i = 0; i < 3; i++) {
                grid.getColumnConstraints().get(i).setPercentWidth(33.33);
                grid.getColumnConstraints().get(i).setHgrow(Priority.ALWAYS);
                grid.getColumnConstraints().get(i).setMaxWidth(Double.MAX_VALUE);
                grid.getColumnConstraints().get(i).setMinWidth(0);
            }
            col3.setPercentWidth(0);
            col3.setHgrow(Priority.NEVER);
            col3.setMaxWidth(0);
            col3.setMinWidth(0);
        }
    }

    private void toggleColor(Label label, String styleClass, boolean valueIsZero) {
        if (valueIsZero) {
            label.getStyleClass().remove(styleClass);
        } else if (!label.getStyleClass().contains(styleClass)) {
            label.getStyleClass().add(styleClass);
        }
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
