package com.corgibalance.components.dialogs;

import com.corgibalance.components.table.TransactionSuggestionSupport;
import com.corgibalance.models.Account;
import com.corgibalance.models.Currency;
import com.corgibalance.models.RecurrenceInterval;
import com.corgibalance.models.RecurringTransaction;
import com.corgibalance.models.Tag;
import com.corgibalance.models.Transaction;
import com.corgibalance.models.TransactionType;
import com.corgibalance.repositories.AccountRepository;
import com.corgibalance.repositories.RecurringTransactionRepository;
import com.corgibalance.repositories.TagRepository;
import com.corgibalance.repositories.TransactionRepository;
import com.corgibalance.services.CurrencyFormatter;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import lombok.Getter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RecurringTransactionDialog extends Dialog<Void> {

    private final List<Account> accounts;
    private final List<Tag> tags;
    private final RecurringTransaction editing;
    @Getter
    private boolean created;

    @FXML
    private ComboBox<Account> accountCombo;
    @FXML
    private ComboBox<TransactionType> typeCombo;
    @FXML
    private ComboBox<Tag> tagCombo;
    @FXML
    private ComboBox<RecurrenceInterval> intervalCombo;
    @FXML
    private TextField amountField;
    @FXML
    private TextField descriptionField;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private VBox errorBox;
    @FXML
    private Label errorLabel;

    private final CurrencyFormatter formatter = new CurrencyFormatter();
    private final Map<Long, Long> accountCurrencies = new HashMap<>();
    private final Map<Long, String> tagColors = new HashMap<>();
    private final TransactionSuggestionSupport suggestions;

    public RecurringTransactionDialog(List<Account> accounts, LocalDate initialDate) {
        this(accounts, null, initialDate);
    }

    public static RecurringTransactionDialog forEdit(RecurringTransaction existing) {
        return new RecurringTransactionDialog(new AccountRepository().findAll(), existing, existing.getNextDate());
    }

    private RecurringTransactionDialog(List<Account> accounts, RecurringTransaction editing, LocalDate initialDate) {
        this.accounts = accounts;
        this.tags = new TagRepository().findAll();
        this.editing = editing;
        setTitle(editing == null ? "Add recurring transaction" : "Edit recurring transaction");
        setDialogPane(loadPane());
        getDialogPane().getStylesheets().addAll(
                Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm(),
                Objects.requireNonNull(getClass().getResource("/css/overview.css")).toExternalForm());
        configureAccounts();
        configureType();
        configureTags();
        configureInterval();
        suggestions = new TransactionSuggestionSupport(
                q -> new TransactionRepository().findByDescriptionLike(q, 5),
                this::tagColorOf,
                this::currencyIdOf,
                id -> accounts.stream().filter(a -> a.getId().equals(id)).map(Account::getName).findFirst().orElse(null)) {
            @Override
            protected void apply(Transaction template) {
                applySuggestion(template);
            }
        };
        for (Account account : accounts) {
            accountCurrencies.put(account.getId(), account.getCurrencyId());
        }
        for (Tag tag : tags) {
            tagColors.put(tag.getId(), tag.getColor());
        }
        suggestions.bind(descriptionField);
        startDatePicker.setValue(initialDate == null ? LocalDate.now() : initialDate);
        if (editing != null) {
            prefill(editing);
        }
        configureButtons();
    }

    private void prefill(RecurringTransaction recurring) {
        for (Account account : accounts) {
            if (account.getId().equals(recurring.getAccountId())) {
                accountCombo.setValue(account);
                break;
            }
        }
        typeCombo.setValue(recurring.getTransactionType());
        for (Tag tag : tags) {
            if (tag.getId().equals(recurring.getTagId())) {
                tagCombo.setValue(tag);
                break;
            }
        }
        amountField.setText(formatter.toPlain(recurring.getAmount(), accountCurrencies.get(recurring.getAccountId())));
        descriptionField.setText(recurring.getDescription());
        intervalCombo.setValue(recurring.getInterval());
        startDatePicker.setValue(recurring.getStartDate());
        endDatePicker.setValue(recurring.getEndDate());
    }

    private DialogPane loadPane() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/dialogs/RecurringTransaction.fxml"));
        loader.setController(this);
        loader.setRoot(new DialogPane());
        try {
            return loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load recurring transaction dialog", e);
        }
    }

    private void configureAccounts() {
        accountCombo.getItems().setAll(accounts);
        accountCombo.setCellFactory(_ -> accountCell());
        accountCombo.setButtonCell(accountCell());
        if (!accounts.isEmpty()) {
            accountCombo.setValue(accounts.getFirst());
        }
    }

    private ListCell<Account> accountCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Account account, boolean empty) {
                super.updateItem(account, empty);
                setText(empty || account == null ? null : accountText(account));
            }
        };
    }

    private void configureType() {
        typeCombo.getItems().setAll(TransactionType.INCOME, TransactionType.EXPENSE);
        typeCombo.setValue(TransactionType.EXPENSE);
    }

    private void configureTags() {
        tagCombo.getItems().add(null);
        tagCombo.getItems().addAll(tags);
        tagCombo.setCellFactory(_ -> tagCell());
        tagCombo.setButtonCell(tagCell());
        tagCombo.setValue(null);
    }

    private ListCell<Tag> tagCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Tag tag, boolean empty) {
                super.updateItem(tag, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else if (tag == null) {
                    setGraphic(null);
                    setText("No tag");
                } else {
                    HBox box = new HBox(6);
                    box.setAlignment(Pos.CENTER_LEFT);
                    Circle dot = tagDot(tag.getColor());
                    if (dot != null) {
                        box.getChildren().add(dot);
                    }
                    box.getChildren().add(new Label(tag.getName()));
                    setGraphic(box);
                    setText(null);
                }
            }
        };
    }

    private void configureInterval() {
        intervalCombo.getItems().setAll(RecurrenceInterval.values());
        intervalCombo.setValue(RecurrenceInterval.MONTHLY);
    }

    private Circle tagDot(String color) {
        if (color == null) {
            return null;
        }
        try {
            return new Circle(5, Color.web(color));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void applySuggestion(Transaction template) {
        descriptionField.setText(template.getDescription());
        for (Account account : accounts) {
            if (account.getId().equals(template.getAccountId())) {
                accountCombo.setValue(account);
                break;
            }
        }
        if (template.getTransactionType() == TransactionType.INCOME
                || template.getTransactionType() == TransactionType.EXPENSE) {
            typeCombo.setValue(template.getTransactionType());
        }
        for (Tag tag : tags) {
            if (tag.getId().equals(template.getTagId())) {
                tagCombo.setValue(tag);
                break;
            }
        }
        amountField.setText(formatter.toPlain(template.getAmount(), accountCurrencies.get(template.getAccountId())));
    }

    private String tagColorOf(Long tagId) {
        return tagId == null ? null : tagColors.get(tagId);
    }

    private Long currencyIdOf(Long accountId) {
        return accountId == null ? null : accountCurrencies.get(accountId);
    }

    private void configureButtons() {
        Button createButton = (Button) getDialogPane().lookupButton(getDialogPane().getButtonTypes().get(0));
        createButton.getStyleClass().addAll("btn", "btn--primary");
        if (editing != null) {
            createButton.setText("Save");
        }
        Button cancelButton = (Button) getDialogPane().lookupButton(getDialogPane().getButtonTypes().get(1));
        cancelButton.getStyleClass().add("btn");
        createButton.addEventFilter(ActionEvent.ACTION, this::onCreate);
    }

    private void onCreate(ActionEvent event) {
        errorBox.setVisible(false);
        errorBox.setManaged(false);
        try {
            saveRecurringTransaction();
            created = true;
        } catch (RuntimeException e) {
            event.consume();
            errorLabel.setText(e.getMessage());
            errorBox.setVisible(true);
            errorBox.setManaged(true);
        }
    }

    private void saveRecurringTransaction() {
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
            new RecurringTransactionRepository().update(recurring);
        }
    }

    private String accountText(Account account) {
        Currency currency = formatter.currency(account.getCurrencyId());
        return currency == null ? account.getName() : account.getName() + " (" + currency.getCode() + ")";
    }
}