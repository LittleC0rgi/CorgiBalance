package com.corgibalance.components.views;

import com.corgibalance.components.dialogs.PlannedTransactionDialog;
import com.corgibalance.models.Account;
import com.corgibalance.models.PlannedTransaction;
import com.corgibalance.models.Transaction;
import com.corgibalance.models.TransactionType;
import com.corgibalance.repositories.AccountRepository;
import com.corgibalance.repositories.PlannedTransactionRepository;
import com.corgibalance.repositories.TransactionRepository;
import com.corgibalance.services.CurrencyFormatter;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class CalendarView extends View implements Refreshable {

    private static final DateTimeFormatter MONTH_FORMAT =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @FXML
    private Label monthLabel;
    @FXML
    private GridPane grid;
    @FXML
    private VBox plannedList;

    private YearMonth currentMonth;
    private CurrencyFormatter formatter;
    private Map<Long, Long> accountCurrencies;

    public CalendarView() {
        super("Calendar", "/fxml/views/calendar.fxml");
    }

    @FXML
    private void initialize() {
        formatter = new CurrencyFormatter();
        accountCurrencies = new HashMap<>();
        currentMonth = YearMonth.now();
        render();
        reloadPlanned();
    }

    @Override
    public void onShow() {
        render();
        reloadPlanned();
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
            reloadPlanned();
        }
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
    }

    private VBox dayCell(LocalDate date, LocalDate today) {
        VBox cell = new VBox(4);
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

        cell.getChildren().addAll(number, plans);
        return cell;
    }

    private void reloadPlanned() {
        accountCurrencies.clear();
        for (Account account : new AccountRepository().findAll()) {
            accountCurrencies.put(account.getId(), account.getCurrencyId());
        }
        plannedList.getChildren().clear();
        for (PlannedTransaction planned : new PlannedTransactionRepository().findAll()) {
            plannedList.getChildren().add(plannedRow(planned));
        }
    }

    private HBox plannedRow(PlannedTransaction planned) {
        Label date = new Label(planned.getPlannedDate().format(DATE_FORMAT));
        date.getStyleClass().add("calendar__row-text");
        Label description = new Label(descriptionText(planned));
        description.getStyleClass().add("calendar__row-text");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label amount = new Label(formatter.format(planned.getAmount(), accountCurrencies.get(planned.getAccountId())));
        amount.getStyleClass().add("calendar__row-text");
        if (planned.getTransactionType() == TransactionType.EXPENSE) {
            amount.getStyleClass().add("calendar__row-text--expense");
        } else {
            amount.getStyleClass().add("calendar__row-text--income");
        }

        HBox row = new HBox(8, date, description, spacer, amount);
        row.setAlignment(Pos.CENTER_LEFT);

        if (!planned.getPlannedDate().isAfter(LocalDate.now())) {
            Button confirm = new Button("Confirm");
            confirm.getStyleClass().addAll("btn", "btn--primary");
            confirm.setOnAction(_ -> confirmPlanned(planned));
            row.getChildren().add(confirm);
        }
        Button delete = new Button("Delete");
        delete.getStyleClass().add("btn");
        delete.setOnAction(_ -> deletePlanned(planned));
        row.getChildren().add(delete);

        return row;
    }

    private void confirmPlanned(PlannedTransaction planned) {
        try {
            Transaction transaction = new Transaction();
            transaction.setAccountId(planned.getAccountId());
            transaction.setTagId(planned.getTagId());
            transaction.setAmount(planned.getAmount());
            transaction.setDescription(planned.getDescription());
            transaction.setTransactionType(planned.getTransactionType());
            transaction.setTransactionDate(planned.getPlannedDate());
            new TransactionRepository().create(transaction);
            new PlannedTransactionRepository().delete(planned);
            reloadPlanned();
        } catch (RuntimeException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void deletePlanned(PlannedTransaction planned) {
        new PlannedTransactionRepository().delete(planned);
        reloadPlanned();
    }

    private String descriptionText(PlannedTransaction planned) {
        if (planned.getDescription() != null && !planned.getDescription().isBlank()) {
            return planned.getDescription();
        }
        return planned.getTransactionType() == TransactionType.EXPENSE
                ? "Planned expense" : "Planned income";
    }

    public static int firstColumn(YearMonth month) {
        return month.atDay(1).getDayOfWeek().getValue() - 1;
    }
}