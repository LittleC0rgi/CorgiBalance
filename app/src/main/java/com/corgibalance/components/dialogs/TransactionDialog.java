package com.corgibalance.components.dialogs;

import com.corgibalance.components.table.TransactionSuggestionSupport;
import com.corgibalance.models.Account;
import com.corgibalance.models.Currency;
import com.corgibalance.models.Tag;
import com.corgibalance.models.Transaction;
import com.corgibalance.models.TransactionType;
import com.corgibalance.repositories.TagRepository;
import com.corgibalance.repositories.TransactionRepository;
import com.corgibalance.services.CurrencyFormatter;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import lombok.Getter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class TransactionDialog extends Dialog<Void> {

    protected final List<Account> accounts;
    protected final List<Tag> tags;
    protected final boolean editing;
    @Getter
    private boolean created;

    @FXML
    protected ComboBox<Account> accountCombo;
    @FXML
    protected ComboBox<TransactionType> typeCombo;
    @FXML
    protected ComboBox<Tag> tagCombo;
    @FXML
    protected TextField amountField;
    @FXML
    protected TextField descriptionField;
    @FXML
    protected VBox errorBox;
    @FXML
    protected Label errorLabel;

    protected final CurrencyFormatter formatter = new CurrencyFormatter();
    protected final Map<Long, Long> accountCurrencies = new HashMap<>();
    protected final Map<Long, String> tagColors = new HashMap<>();
    private TransactionSuggestionSupport suggestions;

    protected TransactionDialog(List<Account> accounts, String title, String fxmlPath, boolean editing) {
        this.accounts = accounts;
        this.tags = new TagRepository().findAll();
        this.editing = editing;
        setTitle(title);
        setDialogPane(loadPane(fxmlPath));
        getDialogPane().getStylesheets().addAll(
                Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm(),
                Objects.requireNonNull(getClass().getResource("/css/overview.css")).toExternalForm());
        configureAccounts();
        configureType();
        configureTags();
        suggestions = new TransactionSuggestionSupport(
                formatter,
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
        configureButtons();
    }

    protected abstract void save();

    protected void prefillFields(Long accountId, TransactionType type, Long tagId, long amount, String description) {
        accounts.stream().filter(a -> a.getId().equals(accountId)).findFirst().ifPresent(accountCombo::setValue);
        typeCombo.setValue(type);
        tags.stream().filter(t -> t.getId().equals(tagId)).findFirst().ifPresent(tagCombo::setValue);
        amountField.setText(formatter.toPlain(amount, accountCurrencies.get(accountId)));
        descriptionField.setText(description);
    }

    private DialogPane loadPane(String fxmlPath) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        loader.setController(this);
        loader.setRoot(new DialogPane());
        try {
            return loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load dialog", e);
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

    private String accountText(Account account) {
        Currency currency = formatter.currency(account.getCurrencyId());
        return currency == null ? account.getName() : account.getName() + " (" + currency.getCode() + ")";
    }

    private void applySuggestion(Transaction template) {
        descriptionField.setText(template.getDescription());
        accounts.stream()
                .filter(a -> a.getId().equals(template.getAccountId()))
                .findFirst()
                .ifPresent(accountCombo::setValue);
        if (template.getTransactionType() == TransactionType.INCOME
                || template.getTransactionType() == TransactionType.EXPENSE) {
            typeCombo.setValue(template.getTransactionType());
        }
        tags.stream()
                .filter(t -> t.getId().equals(template.getTagId()))
                .findFirst()
                .ifPresent(tagCombo::setValue);
        amountField.setText(formatter.toPlain(template.getAmount(), accountCurrencies.get(template.getAccountId())));
    }

    private String tagColorOf(Long tagId) {
        return tagId == null ? null : tagColors.get(tagId);
    }

    private Long currencyIdOf(Long accountId) {
        return accountId == null ? null : accountCurrencies.get(accountId);
    }

    private void configureButtons() {
        DialogPane pane = getDialogPane();
        Button createButton = (Button) pane.getButtonTypes().stream()
                .filter(type -> type.getButtonData() == ButtonData.OK_DONE)
                .map(pane::lookupButton)
                .findFirst()
                .orElseThrow();
        createButton.getStyleClass().addAll("btn", "btn--primary");
        if (editing) {
            createButton.setText("Save");
        }
        Button cancelButton = (Button) pane.lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().add("btn");
        createButton.addEventFilter(ActionEvent.ACTION, this::onCreate);
    }

    private void onCreate(ActionEvent event) {
        errorBox.setVisible(false);
        errorBox.setManaged(false);
        try {
            save();
            created = true;
        } catch (RuntimeException e) {
            event.consume();
            errorLabel.setText(e.getMessage());
            errorBox.setVisible(true);
            errorBox.setManaged(true);
            javafx.stage.Window window = getDialogPane().getScene() == null
                    ? null
                    : getDialogPane().getScene().getWindow();
            if (window instanceof Stage stage) {
                stage.sizeToScene();
            }
        }
    }
}
