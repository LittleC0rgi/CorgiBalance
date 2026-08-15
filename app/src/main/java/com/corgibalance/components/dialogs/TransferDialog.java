package com.corgibalance.components.dialogs;

import com.corgibalance.models.Account;
import com.corgibalance.models.Currency;
import com.corgibalance.repositories.TransactionRepository;
import com.corgibalance.services.CurrencyConverter;
import com.corgibalance.services.CurrencyFormatter;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import lombok.Getter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TransferDialog extends Dialog<Void> {

    private final CurrencyConverter converter;
    private final List<Account> accounts;
    @Getter
    private boolean created;

    @FXML
    private ComboBox<Account> fromCombo;
    @FXML
    private ComboBox<Account> toCombo;
    @FXML
    private TextField amountField;
    @FXML
    private TextField rateField;
    @FXML
    private GridPane rateRow;
    @FXML
    private Label errorLabel;

    public TransferDialog(List<Account> accounts, CurrencyConverter converter) {
        this.accounts = accounts;
        this.converter = converter;
        setTitle("Transfer between accounts");
        setDialogPane(loadPane());
        configureAccounts();
        configureRateRow();
        configureButtons();
    }

    private DialogPane loadPane() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dialogs/transfer.fxml"));
        loader.setController(this);
        loader.setRoot(new DialogPane());
        try {
            return loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load transfer dialog", e);
        }
    }

    private void configureAccounts() {
        configureCombo(fromCombo);
        configureCombo(toCombo);
        fromCombo.setValue(accounts.get(0));
        toCombo.setValue(accounts.get(1));
    }

    private void configureCombo(ComboBox<Account> combo) {
        combo.getItems().setAll(accounts);
        combo.setCellFactory(_ -> new ListCell<>() {
            @Override
            protected void updateItem(Account account, boolean empty) {
                super.updateItem(account, empty);
                setText(empty || account == null ? null : accountText(account));
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Account account, boolean empty) {
                super.updateItem(account, empty);
                setText(empty || account == null ? null : accountText(account));
            }
        });
    }

    private void configureRateRow() {
        Runnable updateRateRow = () -> {
            boolean different = differentCurrency(fromCombo.getValue(), toCombo.getValue());
            rateRow.setVisible(different);
            rateRow.setManaged(different);
            if (different) {
                converter.rate(fromCombo.getValue().getCurrencyId(), toCombo.getValue().getCurrencyId())
                        .ifPresent(rate -> rateField.setText(rate.toPlainString()));
            }
        };
        fromCombo.valueProperty().addListener((_, _, _) -> updateRateRow.run());
        toCombo.valueProperty().addListener((_, _, _) -> updateRateRow.run());
        updateRateRow.run();
    }

    private void configureButtons() {
        ButtonType createType = new ButtonType("Transfer", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(createType, ButtonType.CANCEL);
        Button createButton = (Button) getDialogPane().lookupButton(createType);
        createButton.addEventFilter(ActionEvent.ACTION, this::onTransfer);
    }

    private void onTransfer(ActionEvent event) {
        errorLabel.setText("");
        try {
            createTransfer();
            created = true;
        } catch (RuntimeException e) {
            event.consume();
            errorLabel.setText(e.getMessage());
        }
    }

    private void createTransfer() {
        Account from = fromCombo.getValue();
        Account to = toCombo.getValue();
        if (from == null || to == null || from.getId().equals(to.getId())) {
            throw new IllegalArgumentException("Accounts must be different.");
        }
        CurrencyFormatter formatter = new CurrencyFormatter();
        BigDecimal amount = formatter.parse(amountField.getText());
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
        BigDecimal rate = null;
        if (differentCurrency(from, to)) {
            rate = formatter.parse(rateField.getText());
            if (rate.signum() <= 0) {
                throw new IllegalArgumentException("Rate must be positive.");
            }
        }
        long amountMinor = formatter.toMinorUnits(amount, from.getCurrencyId());
        new TransactionRepository().createTransfer(from.getId(), to.getId(), amountMinor, null, LocalDate.now(), rate);
    }

    private String accountText(Account account) {
        Currency currency = converter.currency(account.getCurrencyId());
        return currency == null ? account.getName() : account.getName() + " (" + currency.getCode() + ")";
    }

    private boolean differentCurrency(Account from, Account to) {
        return from != null && to != null && !from.getCurrencyId().equals(to.getCurrencyId());
    }
}
