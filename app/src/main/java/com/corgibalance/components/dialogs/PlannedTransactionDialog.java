package com.corgibalance.components.dialogs;

import com.corgibalance.models.Account;
import com.corgibalance.models.Currency;
import com.corgibalance.models.PlannedTransaction;
import com.corgibalance.models.TransactionType;
import com.corgibalance.repositories.AccountRepository;
import com.corgibalance.repositories.PlannedTransactionRepository;
import com.corgibalance.services.CurrencyFormatter;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import lombok.Getter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class PlannedTransactionDialog extends Dialog<Void> {

    private final List<Account> accounts;
    @Getter
    private boolean created;

    @FXML
    private ComboBox<Account> accountCombo;
    @FXML
    private ComboBox<TransactionType> typeCombo;
    @FXML
    private TextField amountField;
    @FXML
    private TextField descriptionField;
    @FXML
    private DatePicker datePicker;
    @FXML
    private VBox errorBox;
    @FXML
    private Label errorLabel;

    private final CurrencyFormatter formatter = new CurrencyFormatter();

    public PlannedTransactionDialog(List<Account> accounts, LocalDate initialDate) {
        this.accounts = accounts;
        setTitle("Add planned transaction");
        setDialogPane(loadPane());
        getDialogPane().getStylesheets().addAll(
                Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm(),
                Objects.requireNonNull(getClass().getResource("/css/overview.css")).toExternalForm());
        configureAccounts();
        configureType();
        datePicker.setValue(initialDate == null ? LocalDate.now() : initialDate);
        configureButtons();
    }

    private DialogPane loadPane() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/dialogs/planned_transaction.fxml"));
        loader.setController(this);
        loader.setRoot(new DialogPane());
        try {
            return loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load planned transaction dialog", e);
        }
    }

    private void configureAccounts() {
        accountCombo.getItems().setAll(accounts);
        accountCombo.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(Account account, boolean empty) {
                super.updateItem(account, empty);
                setText(empty || account == null ? null : accountText(account));
            }
        });
        accountCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Account account, boolean empty) {
                super.updateItem(account, empty);
                setText(empty || account == null ? null : accountText(account));
            }
        });
        if (!accounts.isEmpty()) {
            accountCombo.setValue(accounts.getFirst());
        }
    }

    private void configureType() {
        typeCombo.getItems().setAll(TransactionType.INCOME, TransactionType.EXPENSE);
        typeCombo.setValue(TransactionType.EXPENSE);
    }

    private void configureButtons() {
        Button createButton = (Button) getDialogPane().lookupButton(getDialogPane().getButtonTypes().get(0));
        createButton.getStyleClass().addAll("btn", "btn--primary");
        Button cancelButton = (Button) getDialogPane().lookupButton(getDialogPane().getButtonTypes().get(1));
        cancelButton.getStyleClass().add("btn");
        createButton.addEventFilter(ActionEvent.ACTION, this::onCreate);
    }

    private void onCreate(ActionEvent event) {
        errorBox.setVisible(false);
        errorBox.setManaged(false);
        try {
            createPlannedTransaction();
            created = true;
        } catch (RuntimeException e) {
            event.consume();
            errorLabel.setText(e.getMessage());
            errorBox.setVisible(true);
            errorBox.setManaged(true);
        }
    }

    private void createPlannedTransaction() {
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

        PlannedTransaction planned = new PlannedTransaction();
        planned.setAccountId(account.getId());
        planned.setTransactionType(typeCombo.getValue());
        planned.setAmount(formatter.toMinorUnits(amount, account.getCurrencyId()));
        planned.setDescription(descriptionField.getText() == null ? null : descriptionField.getText().trim());
        planned.setPlannedDate(date);
        new PlannedTransactionRepository().create(planned);
    }

    private String accountText(Account account) {
        Currency currency = formatter.currency(account.getCurrencyId());
        return currency == null ? account.getName() : account.getName() + " (" + currency.getCode() + ")";
    }
}