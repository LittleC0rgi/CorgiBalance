package com.corgibalance.components.dialogs;

import com.corgibalance.models.Account;
import com.corgibalance.models.Currency;
import com.corgibalance.repositories.AccountRepository;
import com.corgibalance.repositories.TransactionRepository;
import com.corgibalance.services.CurrencyConverter;
import com.corgibalance.services.CurrencyFormatter;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.Getter;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class TransferDialog extends Dialog<Void> {

    private static final int RATE_SCALE = 12;

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
    private TextField receivedField;
    @FXML
    private ToggleButton byRateToggle;
    @FXML
    private ToggleButton byAmountToggle;
    @FXML
    private ToggleGroup calcGroup;
    @FXML
    private VBox rateRow;
    @FXML
    private Label fromBalanceLabel;
    @FXML
    private Label toBalanceLabel;
    @FXML
    private VBox errorBox;
    @FXML
    private Label errorLabel;

    private final CurrencyFormatter formatter = new CurrencyFormatter();
    private final AccountRepository accountRepository = new AccountRepository();
    private boolean updating;

    public TransferDialog(List<Account> accounts, CurrencyConverter converter) {
        this.accounts = accounts;
        this.converter = converter;
        setTitle("Transfer between accounts");
        setDialogPane(loadPane());
        getDialogPane().getStylesheets().addAll(
                Objects.requireNonNull(getClass().getResource("/css/base.css")).toExternalForm(),
                Objects.requireNonNull(getClass().getResource("/css/overview.css")).toExternalForm());
        configureAccounts();
        configureRateRow();
        configureButtons();
    }

    private DialogPane loadPane() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/dialogs/Transfer.fxml"));
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
        fromCombo.valueProperty().addListener((_, _, _) -> updateBalance(fromCombo, fromBalanceLabel));
        toCombo.valueProperty().addListener((_, _, _) -> updateBalance(toCombo, toBalanceLabel));
        fromCombo.setValue(accounts.get(0));
        toCombo.setValue(accounts.get(1));
    }

    private void updateBalance(ComboBox<Account> combo, Label label) {
        Account account = combo.getValue();
        if (account == null) {
            label.setText("");
            return;
        }
        label.setText("Balance " + formatter.format(accountRepository.currentBalance(account.getId()), account.getCurrencyId()));
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
            showRow(rateRow, different);
            if (different && byRateToggle.isSelected()) {
                converter.rate(fromCombo.getValue().getCurrencyId(), toCombo.getValue().getCurrencyId())
                        .ifPresent(rate -> rateField.setText(rate.toPlainString()));
            }
        };
        fromCombo.valueProperty().addListener((_, _, _) -> updateRateRow.run());
        toCombo.valueProperty().addListener((_, _, _) -> updateRateRow.run());
        fromCombo.valueProperty().addListener((_, _, _) -> updateCalculated());
        toCombo.valueProperty().addListener((_, _, _) -> updateCalculated());
        amountField.textProperty().addListener((_, _, _) -> updateCalculated());
        rateField.textProperty().addListener((_, _, _) -> updateCalculated());
        receivedField.textProperty().addListener((_, _, _) -> updateCalculated());
        calcGroup.selectedToggleProperty().addListener((_, _, _) -> applyCalcMode());
        updateRateRow.run();
        applyCalcMode();
    }

    private void applyCalcMode() {
        boolean byRate = byRateToggle.isSelected();
        rateField.setEditable(byRate);
        receivedField.setEditable(!byRate);
        rateField.getStyleClass().remove("input--readonly");
        receivedField.getStyleClass().remove("input--readonly");
        (byRate ? receivedField : rateField).getStyleClass().add("input--readonly");
        updateCalculated();
    }

    private void updateCalculated() {
        if (updating) {
            return;
        }
        Account from = fromCombo.getValue();
        Account to = toCombo.getValue();
        updating = true;
        try {
            if (!differentCurrency(from, to)) {
                if (byRateToggle.isSelected()) {
                    receivedField.clear();
                } else {
                    rateField.clear();
                }
                return;
            }
            if (byRateToggle.isSelected()) {
                fillReceived(from, to);
            } else {
                fillRate(from, to);
            }
        } finally {
            updating = false;
        }
    }

    private void fillReceived(Account from, Account to) {
        try {
            BigDecimal amount = formatter.parse(amountField.getText());
            BigDecimal rate = formatter.parse(rateField.getText());
            if (amount.signum() > 0 && rate.signum() > 0) {
                long sourceMinor = formatter.toMinorUnits(amount, from.getCurrencyId());
                long targetMinor = rate.multiply(BigDecimal.valueOf(sourceMinor))
                        .setScale(0, RoundingMode.HALF_UP)
                        .longValueExact();
                receivedField.setText(formatter.toPlain(targetMinor, to.getCurrencyId()));
                return;
            }
        } catch (NumberFormatException | ArithmeticException ignored) {
        }
        receivedField.clear();
    }

    private void fillRate(Account from, Account to) {
        try {
            BigDecimal amount = formatter.parse(amountField.getText());
            BigDecimal received = formatter.parse(receivedField.getText());
            if (amount.signum() > 0 && received.signum() > 0) {
                long sourceMinor = formatter.toMinorUnits(amount, from.getCurrencyId());
                if (sourceMinor == 0) {
                    return;
                }
                long targetMinor = formatter.toMinorUnits(received, to.getCurrencyId());
                BigDecimal rate = BigDecimal.valueOf(targetMinor)
                        .divide(BigDecimal.valueOf(sourceMinor), RATE_SCALE, RoundingMode.HALF_UP);
                rateField.setText(rate.toPlainString());
                return;
            }
        } catch (NumberFormatException | ArithmeticException ignored) {
        }
        rateField.clear();
    }

    private void configureButtons() {
        Button createButton = (Button) getDialogPane().lookupButton(getDialogPane().getButtonTypes().get(0));
        createButton.getStyleClass().addAll("btn", "btn--primary");
        Button cancelButton = (Button) getDialogPane().lookupButton(getDialogPane().getButtonTypes().get(1));
        cancelButton.getStyleClass().add("btn");
        createButton.addEventFilter(ActionEvent.ACTION, this::onTransfer);
    }

    private void onTransfer(ActionEvent event) {
        showRow(errorBox, false);
        try {
            createTransfer();
            created = true;
        } catch (RuntimeException e) {
            event.consume();
            errorLabel.setText(e.getMessage());
            showRow(errorBox, true);
        }
    }

    private void showRow(VBox row, boolean show) {
        row.setVisible(show);
        row.setManaged(show);
        javafx.stage.Window window = getDialogPane().getScene() == null
                ? null
                : getDialogPane().getScene().getWindow();
        if (window instanceof Stage stage) {
            stage.sizeToScene();
        }
    }

    private void createTransfer() {
        Account from = fromCombo.getValue();
        Account to = toCombo.getValue();
        if (from == null || to == null || from.getId().equals(to.getId())) {
            throw new IllegalArgumentException("Accounts must be different.");
        }
        BigDecimal amount = formatter.parse(amountField.getText());
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
        BigDecimal rate = null;
        if (differentCurrency(from, to)) {
            if (byRateToggle.isSelected()) {
                rate = formatter.parse(rateField.getText());
                if (rate.signum() <= 0) {
                    throw new IllegalArgumentException("Rate must be positive.");
                }
            } else {
                BigDecimal received = formatter.parse(receivedField.getText());
                if (received.signum() <= 0) {
                    throw new IllegalArgumentException("Amount received must be positive.");
                }
                long sourceMinor = formatter.toMinorUnits(amount, from.getCurrencyId());
                if (sourceMinor == 0) {
                    throw new IllegalArgumentException("Rate cannot be calculated.");
                }
                long targetMinor = formatter.toMinorUnits(received, to.getCurrencyId());
                rate = BigDecimal.valueOf(targetMinor)
                        .divide(BigDecimal.valueOf(sourceMinor), RATE_SCALE, RoundingMode.HALF_UP);
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
