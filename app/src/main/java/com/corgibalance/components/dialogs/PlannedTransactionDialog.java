package com.corgibalance.components.dialogs;

import com.corgibalance.models.Account;
import com.corgibalance.models.PlannedTransaction;
import com.corgibalance.repositories.AccountRepository;
import com.corgibalance.repositories.PlannedTransactionRepository;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class PlannedTransactionDialog extends TransactionDialog {

    private final PlannedTransaction editing;

    @FXML
    private DatePicker datePicker;

    public PlannedTransactionDialog(List<Account> accounts, LocalDate initialDate) {
        this(accounts, null, initialDate);
    }

    public static PlannedTransactionDialog forEdit(PlannedTransaction existing) {
        return new PlannedTransactionDialog(new AccountRepository().findAll(), existing, existing.getPlannedDate());
    }

    private PlannedTransactionDialog(List<Account> accounts, PlannedTransaction editing, LocalDate initialDate) {
        super(accounts,
                editing == null ? "Add planned transaction" : "Edit planned transaction",
                "/fxml/components/dialogs/PlannedTransaction.fxml",
                editing != null);
        this.editing = editing;
        datePicker.setValue(initialDate == null ? LocalDate.now() : initialDate);
        if (editing != null) {
            prefillFields(editing.getAccountId(), editing.getTransactionType(), editing.getTagId(),
                    editing.getAmount(), editing.getDescription());
            datePicker.setValue(editing.getPlannedDate());
        }
    }

    @Override
    protected void save() {
        Account account = accountCombo.getValue();
        if (account == null) {
            throw new IllegalArgumentException("Choose an account.");
        }
        LocalDate date = datePicker.getValue();
        if (date == null) {
            throw new IllegalArgumentException("Choose a planned date.");
        }
        BigDecimal amount = formatter.parse(amountField.getText());
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }

        PlannedTransaction planned = editing == null ? new PlannedTransaction() : editing;
        planned.setAccountId(account.getId());
        planned.setTagId(tagCombo.getValue() == null ? null : tagCombo.getValue().getId());
        planned.setTransactionType(typeCombo.getValue());
        planned.setAmount(formatter.toMinorUnits(amount, account.getCurrencyId()));
        planned.setDescription(descriptionField.getText() == null ? null : descriptionField.getText().trim());
        planned.setPlannedDate(date);
        if (editing == null) {
            new PlannedTransactionRepository().create(planned);
        } else {
            new PlannedTransactionRepository().update(planned);
        }
    }
}
