package com.corgibalance.components.views;

import com.corgibalance.components.HeroIcon;
import com.corgibalance.components.dialogs.PlannedTransactionDialog;
import com.corgibalance.components.dialogs.RecurringTransactionDialog;
import com.corgibalance.models.Account;
import com.corgibalance.models.PlannedTransaction;
import com.corgibalance.models.RecurrenceInterval;
import com.corgibalance.models.RecurringTransaction;
import com.corgibalance.models.Tag;
import com.corgibalance.models.Transaction;
import com.corgibalance.models.TransactionType;
import com.corgibalance.repositories.AccountRepository;
import com.corgibalance.repositories.PlannedTransactionRepository;
import com.corgibalance.repositories.RecurringTransactionRepository;
import com.corgibalance.repositories.TagRepository;
import com.corgibalance.repositories.TransactionRepository;
import com.corgibalance.services.CurrencyFormatter;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CalendarView extends View implements Refreshable {

    private static final DateTimeFormatter MONTH_FORMAT =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @FXML
    private Label monthLabel;
    @FXML
    private GridPane grid;
    @FXML
    private VBox overdueBox;

    private YearMonth currentMonth;
    private CurrencyFormatter formatter;
    private Map<Long, Long> accountCurrencies;
    private Map<Long, String> tagColors;
    private Map<LocalDate, List<PlannedTransaction>> plannedByDate;
    private Map<LocalDate, List<RecurringTransaction>> recurringByDate;
    private List<PlannedTransaction> overduePlanned;
    private List<RecurringTransaction> overdueRecurring;

    public CalendarView() {
        super("Calendar", "/fxml/views/Calendar.fxml");
    }

    public static int firstColumn(YearMonth month) {
        return month.atDay(1).getDayOfWeek().getValue() - 1;
    }

    public static LocalDate nextOccurrence(LocalDate date, RecurrenceInterval interval) {
        return switch (interval) {
            case DAILY -> date.plusDays(1);
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
            case YEARLY -> date.plusYears(1);
        };
    }

    @FXML
    private void initialize() {
        formatter = new CurrencyFormatter();
        accountCurrencies = new HashMap<>();
        tagColors = new HashMap<>();
        plannedByDate = new HashMap<>();
        recurringByDate = new HashMap<>();
        overduePlanned = new ArrayList<>();
        overdueRecurring = new ArrayList<>();
        currentMonth = YearMonth.now();
        reloadData();
    }

    @Override
    public void onShow() {
        reloadData();
    }

    @FXML
    private void prevMonth() {
        currentMonth = currentMonth.minusMonths(1);
        render();
    }

    @FXML
    private void nextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        render();
    }

    @FXML
    private void openAddPlannedDialog() {
        openAddPlannedDialog(LocalDate.now());
    }

    private void openAddPlannedDialog(LocalDate date) {
        List<Account> accounts = new AccountRepository().findAll();
        if (accounts.isEmpty()) {
            return;
        }
        PlannedTransactionDialog dialog = new PlannedTransactionDialog(accounts, date);
        dialog.showAndWait();
        if (dialog.isCreated()) {
            reloadData();
        }
    }

    @FXML
    private void openAddRecurringDialog() {
        List<Account> accounts = new AccountRepository().findAll();
        if (accounts.isEmpty()) {
            return;
        }
        RecurringTransactionDialog dialog = new RecurringTransactionDialog(accounts, LocalDate.now());
        dialog.showAndWait();
        if (dialog.isCreated()) {
            reloadData();
        }
    }

    private void reloadData() {
        accountCurrencies.clear();
        for (Account account : new AccountRepository().findAll()) {
            accountCurrencies.put(account.getId(), account.getCurrencyId());
        }
        tagColors.clear();
        for (Tag tag : new TagRepository().findAll()) {
            tagColors.put(tag.getId(), tag.getColor());
        }
        plannedByDate.clear();
        overduePlanned.clear();
        LocalDate today = LocalDate.now();
        for (PlannedTransaction planned : new PlannedTransactionRepository().findAll()) {
            if (planned.getPlannedDate().isBefore(today)) {
                overduePlanned.add(planned);
            } else {
                plannedByDate.computeIfAbsent(planned.getPlannedDate(), _ -> new ArrayList<>()).add(planned);
            }
        }
        recurringByDate.clear();
        overdueRecurring.clear();
        for (RecurringTransaction recurring : new RecurringTransactionRepository().findActiveUpcoming()) {
            if (recurring.getNextDate().isBefore(today)) {
                overdueRecurring.add(recurring);
            } else {
                recurringByDate.computeIfAbsent(recurring.getNextDate(), _ -> new ArrayList<>()).add(recurring);
            }
        }
        render();
    }

    private void render() {
        monthLabel.setText(currentMonth.format(MONTH_FORMAT));
        grid.getChildren().clear();
        LocalDate today = LocalDate.now();
        int offset = firstColumn(currentMonth);
        int days = currentMonth.lengthOfMonth();
        for (int day = 1; day <= days; day++) {
            int position = offset + day - 1;
            VBox cell = dayCell(currentMonth.atDay(day), today);
            grid.add(cell, position % 7, position / 7);
            GridPane.setHgrow(cell, Priority.ALWAYS);
            GridPane.setVgrow(cell, Priority.ALWAYS);
        }
        renderOverdue();
    }

    private void renderOverdue() {
        overdueBox.getChildren().clear();
        boolean hasOverdue = !overduePlanned.isEmpty() || !overdueRecurring.isEmpty();
        overdueBox.setVisible(hasOverdue);
        overdueBox.setManaged(hasOverdue);
        if (!hasOverdue) {
            return;
        }
        Label title = new Label("Overdue");
        title.getStyleClass().add("calendar__overdue__title");
        overdueBox.getChildren().add(title);
        for (PlannedTransaction planned : overduePlanned) {
            overdueBox.getChildren().add(plannedRow(planned, true));
        }
        for (RecurringTransaction recurring : overdueRecurring) {
            overdueBox.getChildren().add(recurringRow(recurring, true));
        }
    }

    private VBox dayCell(LocalDate date, LocalDate today) {
        VBox cell = new VBox(2);
        cell.getStyleClass().add("calendar__day");
        if (date.equals(today)) {
            cell.getStyleClass().add("calendar__day--today");
        }
        cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        cell.setOnMouseClicked(_ -> openAddPlannedDialog(date));

        Label number = new Label(String.valueOf(date.getDayOfMonth()));
        number.getStyleClass().add("calendar__day__number");

        VBox plans = new VBox(2);
        plans.getStyleClass().add("calendar__day__plans");
        VBox.setVgrow(plans, Priority.ALWAYS);
        plans.setMaxHeight(Double.MAX_VALUE);
        addPlannedRows(plans, plannedByDate.get(date));
        addRecurringRows(plans, recurringByDate.get(date));

        cell.getChildren().addAll(number, plans);
        return cell;
    }

    private void addPlannedRows(VBox plans, List<PlannedTransaction> dayPlans) {
        if (dayPlans == null) {
            return;
        }
        for (PlannedTransaction planned : dayPlans) {
            plans.getChildren().add(plannedRow(planned, false));
        }
    }

    private void addRecurringRows(VBox plans, List<RecurringTransaction> dayRecurring) {
        if (dayRecurring == null) {
            return;
        }
        for (RecurringTransaction recurring : dayRecurring) {
            plans.getChildren().add(recurringRow(recurring, false));
        }
    }

    private HBox plannedRow(PlannedTransaction planned, boolean showDate) {
        LocalDate date = planned.getPlannedDate();
        boolean overdue = date.isBefore(LocalDate.now());
        HBox row = baseRow(showDate, date, overdue);

        Button confirm = new Button();
        confirm.setGraphic(new HeroIcon(HeroIcon.Icon.CHECK));
        confirm.getStyleClass().add("calendar__cell-btn");
        confirm.setOnAction(_ -> confirmPlanned(planned));
        confirm.setOnMouseClicked(Event::consume);
        row.getChildren().add(confirm);

        row.getChildren().add(tagDot(planned.getTagId()));

        Label description = descriptionLabel(
                descriptionText(planned.getTransactionType(), planned.getDescription()),
                () -> editPlanned(planned),
                overdue);
        row.getChildren().add(description);

        row.getChildren().add(amountLabel(planned.getAmount(), planned.getAccountId(), planned.getTransactionType()));
        row.getChildren().add(deleteButton(() -> deletePlanned(planned)));
        return row;
    }

    private HBox recurringRow(RecurringTransaction recurring, boolean showDate) {
        LocalDate date = recurring.getNextDate();
        boolean overdue = date.isBefore(LocalDate.now());
        HBox row = baseRow(showDate, date, overdue);

        Button confirm = new Button();
        confirm.setGraphic(new HeroIcon(HeroIcon.Icon.CHECK));
        confirm.getStyleClass().add("calendar__cell-btn");
        confirm.setOnAction(_ -> confirmRecurring(recurring));
        confirm.setOnMouseClicked(Event::consume);
        row.getChildren().add(confirm);

        HeroIcon refresh = new HeroIcon(HeroIcon.Icon.REFRESH);
        refresh.getStyleClass().add("calendar__cell-icon");
        row.getChildren().add(refresh);

        row.getChildren().add(tagDot(recurring.getTagId()));

        Label description = descriptionLabel(
                descriptionText(recurring.getTransactionType(), recurring.getDescription()),
                () -> editRecurring(recurring),
                overdue);
        row.getChildren().add(description);

        row.getChildren().add(amountLabel(recurring.getAmount(), recurring.getAccountId(), recurring.getTransactionType()));
        row.getChildren().add(deleteButton(() -> deleteRecurring(recurring)));
        return row;
    }

    private HBox baseRow(boolean showDate, LocalDate date, boolean overdue) {
        HBox row = new HBox(4);
        row.setAlignment(Pos.CENTER_LEFT);
        if (overdue) {
            row.getStyleClass().add("calendar__row--overdue");
        }
        if (showDate) {
            Label dateLabel = new Label(date.format(DATE_FORMAT));
            dateLabel.getStyleClass().add("calendar__cell-text");
            if (overdue) {
                dateLabel.getStyleClass().add("calendar__cell-text--overdue");
            }
            row.getChildren().add(dateLabel);
        }
        return row;
    }

    private Label descriptionLabel(String text, Runnable onEdit, boolean overdue) {
        Label description = new Label(text);
        description.getStyleClass().add("calendar__cell-text");
        if (overdue) {
            description.getStyleClass().add("calendar__cell-text--overdue");
        }
        description.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(description, Priority.ALWAYS);
        description.setOnMouseClicked(e -> {
            e.consume();
            onEdit.run();
        });
        return description;
    }

    private Label amountLabel(long amount, Long accountId, TransactionType type) {
        Label amountLabel = new Label(formatter.format(amount, accountCurrencies.get(accountId)));
        amountLabel.getStyleClass().add("calendar__cell-text");
        amountLabel.getStyleClass().add(type == TransactionType.EXPENSE
                ? "calendar__cell-text--expense"
                : "calendar__cell-text--income");
        return amountLabel;
    }

    private Button deleteButton(Runnable action) {
        Button delete = new Button();
        delete.setGraphic(new HeroIcon(HeroIcon.Icon.X_MARK));
        delete.getStyleClass().add("calendar__cell-btn");
        delete.setOnAction(_ -> action.run());
        delete.setOnMouseClicked(Event::consume);
        return delete;
    }

    private Circle tagDot(Long tagId) {
        String color = tagId == null ? null : tagColors.get(tagId);
        if (color == null) {
            return null;
        }
        try {
            return new Circle(5, Color.web(color));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void editPlanned(PlannedTransaction planned) {
        PlannedTransactionDialog dialog = PlannedTransactionDialog.forEdit(planned);
        dialog.showAndWait();
        if (dialog.isCreated()) {
            reloadData();
        }
    }

    private void editRecurring(RecurringTransaction recurring) {
        RecurringTransactionDialog dialog = RecurringTransactionDialog.forEdit(recurring);
        dialog.showAndWait();
        if (dialog.isCreated()) {
            reloadData();
        }
    }

    private void confirmPlanned(PlannedTransaction planned) {
        try {
            createTransaction(planned.getAccountId(), planned.getTagId(), planned.getAmount(),
                    planned.getDescription(), planned.getTransactionType(), planned.getPlannedDate());
            new PlannedTransactionRepository().delete(planned);
            reloadData();
        } catch (RuntimeException e) {
            showError(e);
        }
    }

    private void confirmRecurring(RecurringTransaction recurring) {
        try {
            createTransaction(recurring.getAccountId(), recurring.getTagId(), recurring.getAmount(),
                    recurring.getDescription(), recurring.getTransactionType(), recurring.getNextDate());
            LocalDate next = nextOccurrence(recurring.getNextDate(), recurring.getInterval());
            if (recurring.getEndDate() != null && next.isAfter(recurring.getEndDate())) {
                recurring.setActive(false);
            } else {
                recurring.setNextDate(next);
            }
            new RecurringTransactionRepository().update(recurring);
            reloadData();
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

    private void deletePlanned(PlannedTransaction planned) {
        new PlannedTransactionRepository().delete(planned);
        reloadData();
    }

    private void deleteRecurring(RecurringTransaction recurring) {
        new RecurringTransactionRepository().delete(recurring);
        reloadData();
    }

    private void showError(RuntimeException e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
    }

    private String descriptionText(TransactionType type, String description) {
        if (description != null && !description.isBlank()) {
            return description;
        }
        return type == TransactionType.EXPENSE ? "Planned expense" : "Planned income";
    }
}