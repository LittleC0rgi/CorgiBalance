package com.corgibalance.controllers.views;

import com.corgibalance.components.HeroIcon;
import com.corgibalance.components.ProfitLossReport;
import com.corgibalance.controllers.tables.RecentTransactionsTableController;
import com.corgibalance.models.*;
import com.corgibalance.models.Currency;
import com.corgibalance.repositories.*;
import com.corgibalance.services.CurrencyConverter;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Callback;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Consumer;

public class OverviewController implements Refreshable {

    private static final String BASE_CURRENCY_KEY = "overview.baseCurrencyId";
    private static final String SHOW_EXPENSES_BY_TAG_KEY = "overview.showExpensesByTag";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final int NEAREST_LIMIT = 5;
    private static final DataFormat ACCOUNT_ID_DATA = new DataFormat("application/x-corgibalance-account-id");
    private static final DataFormat FOLDER_ID_DATA = new DataFormat("application/x-corgibalance-folder-id");
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
    private AccountRepository accountRepository;
    private AccountFolderRepository accountFolderRepository;
    private TransactionRepository transactionRepository;
    private BudgetRepository budgetRepository;
    private SettingsRepository settingsRepository;
    private HBox unassignedHeader;
    @Setter
    private Consumer<String> navigationHandler;

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
        accountFolderRepository = new AccountFolderRepository();
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

        unassignedHeader = createUnassignedHeader();
        accountList.getChildren().add(unassignedHeader);

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

        renderAccountList();

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
                    transactionRepository, new TagRepository(), converter, baseCurrencyId, year, month);
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

    private void renderAccountList() {
        accountList.getChildren().clear();
        List<Account> accounts = accountRepository.findAll();
        List<AccountFolder> folders = accountFolderRepository.findAll();
        Set<Long> folderIds = folders.stream().map(AccountFolder::getId).collect(java.util.stream.Collectors.toSet());

        Map<Long, List<AccountFolder>> childrenByParent = new HashMap<>();
        for (AccountFolder f : folders) {
            Long pid = f.getParentId();
            childrenByParent.computeIfAbsent(pid, _ -> new ArrayList<>()).add(f);
        }

        List<Account> unassigned = accounts.stream()
                .filter(account -> account.getFolderId() == null || !folderIds.contains(account.getFolderId()))
                .toList();
        for (Account account : unassigned) {
            accountList.getChildren().add(accountRow(account));
        }
        renderFolderTree(childrenByParent, accounts, null, 0);
        accountList.getChildren().add(unassignedHeader);
        unassignedHeader.setVisible(false);
        unassignedHeader.setManaged(false);
    }

    private void renderFolderTree(Map<Long, List<AccountFolder>> childrenByParent,
                                   List<Account> accounts, Long parentId, int depth) {
        List<AccountFolder> children = childrenByParent.getOrDefault(parentId, List.of());
        for (AccountFolder folder : children) {
            List<Account> inFolder = accounts.stream()
                    .filter(account -> folder.getId().equals(account.getFolderId()))
                    .toList();
            accountList.getChildren().add(folderHeader(folder, inFolder, depth));
            if (folder.isExpanded()) {
                for (Account account : inFolder) {
                    accountList.getChildren().add(accountRow(account, depth + 1));
                }
                renderFolderTree(childrenByParent, accounts, folder.getId(), depth + 1);
            }
        }
    }

    private Node folderHeader(AccountFolder folder, List<Account> accounts, int depth) {
        Long baseCurrencyId = baseCurrencyCombo.getValue();
        long total = 0;
        for (Account account : accounts) {
            long balance = accountRepository.currentBalance(account.getId());
            total += converter.convert(balance, account.getCurrencyId(), baseCurrencyId);
        }

        HeroIcon icon = new HeroIcon(folder.isExpanded() ? HeroIcon.Icon.FOLDER : HeroIcon.Icon.FOLDER_PLUS);
        icon.getStyleClass().add("account-folder__icon");
        Label name = new Label(folder.getName());
        name.getStyleClass().add("account-folder__name");
        Label amount = new Label(converter.format(total, baseCurrencyId));
        amount.getStyleClass().add("account-folder__amount");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(icon, name, spacer, amount);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(6);
        header.getStyleClass().add("account-folder");
        if (depth > 0) {
            header.setStyle("-fx-padding: 6 10 6 " + (12 + depth * 16) + ";");
        }
        header.setOnMouseClicked(_ -> toggleFolder(folder));
        dropTarget(header, folder.getId());
        folderDragSource(header, folder);

        MenuItem addSubfolder = new MenuItem("Add subfolder");
        addSubfolder.setOnAction(_ -> onAddSubfolder(folder));
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(_ -> deleteFolder(folder));
        ContextMenu menu = new ContextMenu(addSubfolder, deleteItem);
        header.setOnContextMenuRequested(e -> {
            menu.show(header, e.getScreenX(), e.getScreenY());
            e.consume();
        });

        return header;
    }

    private void toggleFolder(AccountFolder folder) {
        folder.setExpanded(!folder.isExpanded());
        try {
            accountFolderRepository.update(folder);
            refresh();
        } catch (RuntimeException e) {
            showError(e);
        }
    }

    private HBox createUnassignedHeader() {
        Label name = new Label("No folder");
        name.getStyleClass().add("account-folder__name");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(name, spacer);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(6);
        header.getStyleClass().addAll("account-folder", "account-folder--muted");
        dropTarget(header, null);
        return header;
    }

    private Node accountRow(Account account) {
        return accountRow(account, 0);
    }

    private Node accountRow(Account account, int depth) {
        long balance = accountRepository.currentBalance(account.getId());
        Label name = new Label(account.getName());
        name.getStyleClass().add("card__text");
        Label amount = new Label(converter.format(balance, account.getCurrencyId()));
        amount.getStyleClass().add("card__text");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(name, spacer, amount);
        row.getStyleClass().add("account-row");
        if (depth > 0) {
            row.setStyle("-fx-padding: 2 0 2 " + (12 + depth * 16) + ";");
        }
        dragSource(row, account);
        return row;
    }

    private void dragSource(Node node, Account account) {
        node.setOnDragDetected(event -> {
            Dragboard dragboard = node.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.put(ACCOUNT_ID_DATA, String.valueOf(account.getId()));
            dragboard.setContent(content);
            dragboard.setDragView(node.snapshot(null, null));
            unassignedHeader.setVisible(true);
            unassignedHeader.setManaged(true);
            event.consume();
        });
        node.setOnDragDone(event -> {
            unassignedHeader.setVisible(false);
            unassignedHeader.setManaged(false);
        });
    }

    private void folderDragSource(Node node, AccountFolder folder) {
        node.setOnDragDetected(event -> {
            Dragboard dragboard = node.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.put(FOLDER_ID_DATA, String.valueOf(folder.getId()));
            dragboard.setContent(content);
            dragboard.setDragView(node.snapshot(null, null));
            event.consume();
        });
    }

    private void dropTarget(Node node, Long folderId) {
        node.setOnDragOver(event -> {
            if (event.getGestureSource() != node
                    && (event.getDragboard().hasContent(ACCOUNT_ID_DATA) || event.getDragboard().hasContent(FOLDER_ID_DATA))) {
                event.acceptTransferModes(TransferMode.MOVE);
                node.getStyleClass().add("account-folder--drag-over");
            }
            event.consume();
        });
        node.setOnDragExited(event -> node.getStyleClass().remove("account-folder--drag-over"));
        node.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;
            if (dragboard.hasContent(ACCOUNT_ID_DATA)) {
                long accountId = Long.parseLong((String) dragboard.getContent(ACCOUNT_ID_DATA));
                moveAccountToFolder(accountId, folderId);
                success = true;
            } else if (dragboard.hasContent(FOLDER_ID_DATA)) {
                long draggedFolderId = Long.parseLong((String) dragboard.getContent(FOLDER_ID_DATA));
                if (folderId != null && draggedFolderId != folderId && !isDescendant(draggedFolderId, folderId)) {
                    moveFolderToFolder(draggedFolderId, folderId);
                    success = true;
                } else if (folderId == null && draggedFolderId != 0) {
                    moveFolderToRoot(draggedFolderId);
                    success = true;
                }
            }
            node.getStyleClass().remove("account-folder--drag-over");
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void moveAccountToFolder(long accountId, Long folderId) {
        try {
            for (Account account : accountRepository.findAll()) {
                if (account.getId() != null && account.getId() == accountId) {
                    account.setFolderId(folderId);
                    accountRepository.update(account);
                    refresh();
                    return;
                }
            }
        } catch (RuntimeException e) {
            showError(e);
        }
    }

    private void moveFolderToFolder(long childId, Long newParentId) {
        try {
            for (AccountFolder folder : accountFolderRepository.findAll()) {
                if (folder.getId() == childId) {
                    folder.setParentId(newParentId);
                    accountFolderRepository.update(folder);
                    refresh();
                    return;
                }
            }
        } catch (RuntimeException e) {
            showError(e);
        }
    }

    private void moveFolderToRoot(long folderId) {
        try {
            for (AccountFolder folder : accountFolderRepository.findAll()) {
                if (folder.getId() == folderId) {
                    folder.setParentId(null);
                    accountFolderRepository.update(folder);
                    refresh();
                    return;
                }
            }
        } catch (RuntimeException e) {
            showError(e);
        }
    }

    private boolean isDescendant(long ancestorId, long folderId) {
        List<AccountFolder> folders = accountFolderRepository.findAll();
        Map<Long, AccountFolder> byId = new HashMap<>();
        for (AccountFolder f : folders) {
            byId.put(f.getId(), f);
        }
        Long current = folderId;
        while (current != null) {
            if (current == ancestorId) {
                return true;
            }
            AccountFolder f = byId.get(current);
            current = f == null ? null : f.getParentId();
        }
        return false;
    }

    @FXML
    private void onAddFolder() {
        onAddSubfolder(null);
    }

    private void onAddSubfolder(AccountFolder parent) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(parent == null ? "New folder" : "New subfolder");
        dialog.setHeaderText(null);
        dialog.setContentText("Folder name:");
        dialog.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm());
        dialog.getDialogPane().lookupButton(ButtonType.OK).getStyleClass().addAll("btn", "btn--primary");
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).getStyleClass().add("btn");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }
        String name = result.get().trim();
        if (name.isEmpty()) {
            return;
        }
        AccountFolder folder = new AccountFolder();
        folder.setName(name);
        folder.setParentId(parent != null ? parent.getId() : null);
        try {
            accountFolderRepository.create(folder);
            refresh();
        } catch (RuntimeException e) {
            showError(e);
        }
    }

    private void deleteFolder(AccountFolder folder) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete folder");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete folder \"" + folder.getName() + "\"? Accounts and subfolders will become unassigned.");
        confirm.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm());
        confirm.getDialogPane().lookupButton(ButtonType.OK).getStyleClass().addAll("btn", "btn--danger");
        confirm.getDialogPane().lookupButton(ButtonType.CANCEL).getStyleClass().add("btn");
        if (confirm.showAndWait().filter(r -> r == ButtonType.OK).isEmpty()) {
            return;
        }
        try {
            accountFolderRepository.delete(folder);
            refresh();
        } catch (RuntimeException e) {
            showError(e);
        }
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
        confirm.getStyleClass().addAll("btn", "btn--transparent", "btn--mini", "nearest__btn");
        confirm.setTooltip(new Tooltip("Confirm"));
        confirm.setOnAction(_ -> confirmPayment(payment));

        Label description = new Label(paymentText(payment));
        description.getStyleClass().add("nearest__desc");
        description.setMaxWidth(Double.MAX_VALUE);

        Label date = new Label(String.valueOf(payment.date().getDayOfMonth()));
        date.getStyleClass().add("nearest__date");

        StackPane dateCard = new StackPane(date);
        dateCard.getStyleClass().add("nearest__date_card");
        Tooltip.install(dateCard, new Tooltip(payment.date().format(DATE_FORMAT)));

        Label amount = new Label(converter.format(payment.amount(), accountCurrencies.get(payment.accountId())));
        amount.getStyleClass().add("nearest__amount");
        amount.getStyleClass().add(payment.type() == TransactionType.EXPENSE ? "nearest__amount--expense" : "nearest__amount--income");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(dateCard, tagDot(payment.tagId(), tagColors), description, spacer, amount, confirm, deleteButton(payment));
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
        delete.getStyleClass().addAll("btn", "btn--danger-transparent", "btn--mini", "nearest__btn");
        delete.setTooltip(new Tooltip("Delete"));
        delete.setOnAction(_ -> deletePayment(payment));
        return delete;
    }

    private Circle tagDot(Long tagId, Map<Long, String> tagColors) {
        String color = tagId == null ? null : tagColors.get(tagId);
        if (color == null) {
            return new Circle(4, Color.TRANSPARENT);
        }
        try {
            return new Circle(4, Color.web(color));
        } catch (IllegalArgumentException e) {
            return new Circle(4, Color.TRANSPARENT);
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
                LocalDate next = CalendarController.nextOccurrence(recurring.getNextDate(), recurring.getInterval());
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
        alert.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm());
        alert.getDialogPane().lookupButton(ButtonType.OK).getStyleClass().addAll("btn", "btn--primary");
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
