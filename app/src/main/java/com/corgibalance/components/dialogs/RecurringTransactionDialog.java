package com.corgibalance.components.dialogs;

import com.corgibalance.models.Account;
import com.corgibalance.models.RecurrenceInterval;
import com.corgibalance.models.RecurringTransaction;
import com.corgibalance.repositories.AccountRepository;
import com.corgibalance.repositories.RecurringTransactionRepository;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class RecurringTransactionDialog extends TransactionDialog {

    private final RecurringTransaction editing;

    @FXML
    private ComboBox<RecurrenceInterval> intervalCombo;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;

    public RecurringTransactionDialog(List<Account> accounts, LocalDate initialDate) {
        this(accounts, null, initialDate);
    }

    public static RecurringTransactionDialog forEdit(RecurringTransaction existing) {
        return new RecurringTransactionDialog(new AccountRepository().findAll(), existing, existing.getNextDate());
    }

    private RecurringTransactionDialog(List<Account> accounts, RecurringTransaction editing, LocalDate initialDate) {
        super(accounts,
                editing == null ? "Add recurring transaction" : "Edit recurring transaction",
                "/fxml/components/dialogs/RecurringTransaction.fxml",
                editing != null);
        this.editing = editing;
        configureInterval();
        startDatePicker.setValue(initialDate == null ? LocalDate.now() : initialDate);
        if (editing != null) {
            prefill(editing);
        }
    }

    private void prefill(RecurringTransaction recurring) {
        prefillFields(recurring.getAccountId(), recurring.getTransactionType(), recurring.getTagId(),
                recurring.getAmount(), recurring.getDescription());
        intervalCombo.setValue(recurring.getInterval());
        startDatePicker.setValue(recurring.getStartDate());
        endDatePicker.setValue(recurring.getEndDate());
    }

    private void configureInterval() {
        intervalCombo.getItems().setAll(RecurrenceInterval.values());
        intervalCombo.setValue(RecurrenceInterval.MONTHLY);
    }

    @Override
    protected void save() {
        Account account = accountCombo.getValue();
        if (account == null) {
            throw new IllegalArgumentException("Choose an account.");
        }
        LocalDate startDate = startDatePicker.getValue();
        if (startDate == null) {
            throw new IllegalArgumentException("Choose a start date.");
        }
        LocalDate endDate = endDatePicker.getValue();
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must not be before start date.");
        }
        BigDecimal amount = formatter.parse(amountField.getText());
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }

        RecurringTransaction recurring = editing == null ? new RecurringTransaction() : editing;
        recurring.setAccountId(account.getId());
        recurring.setTagId(tagCombo.getValue() == null ? null : tagCombo.getValue().getId());
        recurring.setTransactionType(typeCombo.getValue());
        recurring.setAmount(formatter.toMinorUnits(amount, account.getCurrencyId()));
        recurring.setDescription(descriptionField.getText() == null ? null : descriptionField.getText().trim());
        recurring.setInterval(intervalCombo.getValue());
        recurring.setStartDate(startDate);
        recurring.setEndDate(endDate);
        if (editing == null) {
            recurring.setNextDate(startDate);
            recurring.setActive(true);
            new RecurringTransactionRepository().create(recurring);
        } else {
            if (recurring.getNextDate() == null || recurring.getNextDate().isBefore(startDate)) {
                recurring.setNextDate(startDate);
            }
            new RecurringTransactionRepository().update(recurring);
        }
    }
}
