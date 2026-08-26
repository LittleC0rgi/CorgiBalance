package com.corgibalance.components.dialogs;

import com.corgibalance.components.table.TransactionSuggestionSupport;
import com.corgibalance.models.Account;
import com.corgibalance.models.Currency;
import com.corgibalance.models.PlannedTransaction;
import com.corgibalance.models.Tag;
import com.corgibalance.models.Transaction;
import com.corgibalance.models.TransactionType;
import com.corgibalance.repositories.AccountRepository;
import com.corgibalance.repositories.PlannedTransactionRepository;
import com.corgibalance.repositories.TagRepository;
import com.corgibalance.repositories.TransactionRepository;
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
import javafx.geometry.Pos;
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

public class PlannedTransactionDialog extends Dialog<Void> {

    private final List<Account> accounts;
    private final List<Tag> tags;
    private final PlannedTransaction editing;
    @Getter
    private boolean created;

    @FXML
    private ComboBox<Account> accountCombo;
    @FXML
    private ComboBox<TransactionType> typeCombo;
    @FXML
    private ComboBox<Tag> tagCombo;
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
    private final Map<Long, Long> accountCurrencies = new HashMap<>();
    private final Map<Long, String> tagColors = new HashMap<>();
    private final TransactionSuggestionSupport suggestions;

    public PlannedTransactionDialog(List<Account> accounts, LocalDate initialDate) {
        this(accounts, null, initialDate);
    }

    public static PlannedTransactionDialog forEdit(PlannedTransaction existing) {
        return new PlannedTransactionDialog(new AccountRepository().findAll(), existing, existing.getPlannedDate());
    }

    private PlannedTransactionDialog(List<Account> accounts, PlannedTransaction editing, LocalDate initialDate) {
        this.accounts = accounts;
        this.tags = new TagRepository().findAll();
        this.editing = editing;
        setTitle(editing == null ? "Add planned transaction" : "Edit planned transaction");
        setDialogPane(loadPane());
        getDialogPane().getStylesheets().addAll(
                Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm(),
                Objects.requireNonNull(getClass().getResource("/css/overview.css")).toExternalForm());
        configureAccounts();
        configureType();
        configureTags();
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
        datePicker.setValue(initialDate == null ? LocalDate.now() : initialDate);
        if (editing != null) {
            prefill(editing);
        }
        configureButtons();
    }

    private void prefill(PlannedTransaction planned) {
        for (Account account : accounts) {
            if (account.getId().equals(planned.getAccountId())) {
                accountCombo.setValue(account);
                break;
            }
        }
        typeCombo.setValue(planned.getTransactionType());
        for (Tag tag : tags) {
            if (tag.getId().equals(planned.getTagId())) {
                tagCombo.setValue(tag);
                break;
            }
        }
        amountField.setText(formatter.toPlain(planned.getAmount(), accountCurrencies.get(planned.getAccountId())));
        descriptionField.setText(planned.getDescription());
    }

    private DialogPane loadPane() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/dialogs/PlannedTransaction.fxml"));
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
                    Label nameLabel = new Label(tag.getName());
                    nameLabel.setStyle("-fx-text-fill: #272932;");
                    box.getChildren().add(nameLabel);
                    setGraphic(box);
                    setText(null);
                }
            }
        };
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

    private String tagColorOf(Long tagId) {
        return tagId == null ? null : tagColors.get(tagId);
    }

    private Long currencyIdOf(Long accountId) {
        return accountId == null ? null : accountCurrencies.get(accountId);
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

    private String accountText(Account account) {
        Currency currency = formatter.currency(account.getCurrencyId());
        return currency == null ? account.getName() : account.getName() + " (" + currency.getCode() + ")";
    }
}