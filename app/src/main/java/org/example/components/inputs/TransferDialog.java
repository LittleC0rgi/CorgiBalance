package org.example.components.inputs;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import javafx.util.Callback;
import org.example.components.table.CrudTable;
import org.example.repositories.AccountRepository;
import org.example.repositories.ExchangeRateRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TransferDialog extends Dialog<TransferDialog.Result> {

    private final List<Long> accountIds;
    private final Map<Long, String> accountLabels;
    private final Map<Long, Long> accountCurrencyIds;
    private final Map<Long, String> currencyCodes;
    private final ExchangeRateRepository exchangeRateRepository = new ExchangeRateRepository();
    private final AccountRepository accountRepository = new AccountRepository();
    private final CrudDatePicker datePicker = new CrudDatePicker();
    private final CrudComboBox<Long> fromCombo = new CrudComboBox<>();
    private final CrudComboBox<Long> toCombo = new CrudComboBox<>();
    private final Label fromBalanceLabel = balanceLabel();
    private final Label toBalanceLabel = balanceLabel();
    private final CrudTextField amountField = new CrudTextField();
    private final CrudTextField descriptionField = new CrudTextField();
    private final CrudTextField rateField = new CrudTextField();
    private Node rateLabel;
    private Long lastFromCurrencyId;
    private Long lastToCurrencyId;
    public TransferDialog(List<Long> accountIds, Map<Long, String> accountLabels,
                          Map<Long, Long> accountCurrencyIds, Map<Long, String> currencyCodes) {
        this.accountIds = accountIds;
        this.accountLabels = accountLabels;
        this.accountCurrencyIds = accountCurrencyIds;
        this.currencyCodes = currencyCodes;

        setTitle("New Transfer");

        getDialogPane().getStylesheets()
                .add(Objects.requireNonNull(
                        CrudTable.class.getResource("/css/table.css")).toExternalForm());
        getDialogPane().setContent(buildForm());
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.getStyleClass().add("crud-btn");
        Button cancelButton = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().add("crud-cancel-btn");
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            String error = validate();
            if (error != null) {
                event.consume();
                showError(error);
            }
        });

        setResultConverter(buttonType -> buttonType == ButtonType.OK ? collect() : null);
    }

    private GridPane buildForm() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setPrefWidth(360);

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setPrefWidth(120);
        ColumnConstraints controlColumn = new ColumnConstraints();
        controlColumn.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labelColumn, controlColumn);

        datePicker.setValue(LocalDate.now());

        fromCombo.getItems().setAll(accountIds);
        toCombo.getItems().setAll(accountIds);
        configureCombo(fromCombo);
        configureCombo(toCombo);
        fromCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateBalanceLabel(fromBalanceLabel, newValue);
            updateRateState();
        });
        toCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            updateBalanceLabel(toBalanceLabel, newValue);
            updateRateState();
        });

        int row = 0;
        grid.add(label("Date", true), 0, row);
        grid.add(datePicker, 1, row++);
        grid.add(label("From account", true), 0, row);
        grid.add(wrapCombo(fromCombo, fromBalanceLabel), 1, row++);
        grid.add(label("To account", true), 0, row);
        grid.add(wrapCombo(toCombo, toBalanceLabel), 1, row++);
        grid.add(label("Amount", true), 0, row);
        amountField.setTextFormatter(digitsOnlyFormatter());
        grid.add(amountField, 1, row++);
        grid.add(label("Description", false), 0, row);
        grid.add(descriptionField, 1, row++);
        rateLabel = label("Rate", false);
        grid.add(rateLabel, 0, row);
        grid.add(rateField, 1, row);
        setRateVisible(false);
        return grid;
    }

    private void updateRateState() {
        Long from = fromCombo.getValue();
        Long to = toCombo.getValue();
        Long fromCurrencyId = from == null ? null : accountCurrencyIds.get(from);
        Long toCurrencyId = to == null ? null : accountCurrencyIds.get(to);
        boolean differentCurrencies = fromCurrencyId != null && toCurrencyId != null
                && !fromCurrencyId.equals(toCurrencyId);
        if (!differentCurrencies) {
            lastFromCurrencyId = null;
            lastToCurrencyId = null;
            setRateVisible(false);
            return;
        }
        setRateVisible(true);
        ((Label) rateLabel).setText("Rate (1 " + code(fromCurrencyId) + " = ? " + code(toCurrencyId) + ")");
        if (!Objects.equals(fromCurrencyId, lastFromCurrencyId)
                || !Objects.equals(toCurrencyId, lastToCurrencyId)) {
            lastFromCurrencyId = fromCurrencyId;
            lastToCurrencyId = toCurrencyId;
            rateField.clear();
            exchangeRateRepository.findLatest(fromCurrencyId, toCurrencyId)
                    .ifPresent(exchangeRate -> rateField.setText(exchangeRate.getRate().toPlainString()));
        }
    }

    private void setRateVisible(boolean visible) {
        rateLabel.setVisible(visible);
        rateLabel.setManaged(visible);
        rateField.setVisible(visible);
        rateField.setManaged(visible);
    }

    private String code(Long currencyId) {
        return currencyCodes.getOrDefault(currencyId, String.valueOf(currencyId));
    }

    private Node label(String text, boolean required) {
        Label label = new Label(text);
        label.getStyleClass().add("crud-form-label");
        if (!required) {
            return label;
        }
        Label star = new Label("*");
        star.getStyleClass().add("crud-form-required");
        HBox box = new HBox(3, label, star);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void configureCombo(CrudComboBox<Long> combo) {
        Callback<ListView<Long>, ListCell<Long>> cellFactory =
                list -> new ListCell<>() {
                    @Override
                    protected void updateItem(Long id, boolean empty) {
                        super.updateItem(id, empty);
                        setText(empty || id == null ? "" : accountLabels.getOrDefault(id, String.valueOf(id)));
                    }
                };
        combo.setCellFactory(cellFactory);
        combo.setButtonCell(cellFactory.call(null));
    }

    private VBox wrapCombo(Node combo, Label balance) {
        VBox box = new VBox(3, combo, balance);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private Label balanceLabel() {
        Label label = new Label();
        label.getStyleClass().add("crud-balance-label");
        label.setVisible(false);
        label.setManaged(false);
        return label;
    }

    private void updateBalanceLabel(Label label, Long accountId) {
        if (accountId == null) {
            label.setText("");
            label.setVisible(false);
            label.setManaged(false);
            return;
        }
        long balance = accountRepository.currentBalance(accountId);
        String currencyCode = currencyCodes.getOrDefault(accountCurrencyIds.get(accountId), "");
        label.setText("Balance: " + balance + (currencyCode.isEmpty() ? "" : " " + currencyCode));
        label.setVisible(true);
        label.setManaged(true);
        resizeToContent();
    }

    private TextFormatter<TextFormatter.Change> digitsOnlyFormatter() {
        return new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d*") ? change : null);
    }

    private void resizeToContent() {
        Platform.runLater(() -> {
            if (getDialogPane().getScene() == null) {
                return;
            }
            Window window = getDialogPane().getScene().getWindow();
            if (window != null) {
                window.sizeToScene();
            }
        });
    }

    private String validate() {
        if (datePicker.getValue() == null) {
            return "Date is required";
        }
        if (fromCombo.getValue() == null) {
            return "From account is required";
        }
        if (toCombo.getValue() == null) {
            return "To account is required";
        }
        if (Objects.equals(fromCombo.getValue(), toCombo.getValue())) {
            return "Accounts must be different";
        }
        String amountText = amountField.getText() == null ? "" : amountField.getText().trim();
        if (amountText.isEmpty()) {
            return "Amount is required";
        }
        try {
            if (Long.parseLong(amountText) <= 0) {
                return "Amount must be positive";
            }
        } catch (NumberFormatException e) {
            return "Amount must be a valid number";
        }
        if (rateField.isVisible()) {
            String rateText = rateField.getText() == null ? "" : rateField.getText().trim();
            if (rateText.isEmpty()) {
                return "Exchange rate is required for a transfer between different currencies";
            }
            try {
                if (new BigDecimal(rateText).compareTo(BigDecimal.ZERO) <= 0) {
                    return "Exchange rate must be positive";
                }
            } catch (NumberFormatException e) {
                return "Exchange rate must be a valid number";
            }
        }
        return null;
    }

    private Result collect() {
        long amount = Long.parseLong(amountField.getText().trim());
        String description = descriptionField.getText() == null ? "" : descriptionField.getText().trim();
        BigDecimal rate = rateField.isVisible() ? new BigDecimal(rateField.getText().trim()) : null;
        Long fromCurrencyId = fromCombo.getValue() == null ? null : accountCurrencyIds.get(fromCombo.getValue());
        Long toCurrencyId = toCombo.getValue() == null ? null : accountCurrencyIds.get(toCombo.getValue());
        return new Result(fromCombo.getValue(), toCombo.getValue(), amount,
                description.isEmpty() ? null : description, datePicker.getValue(),
                rate, fromCurrencyId, toCurrencyId);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public record Result(long fromAccountId, long toAccountId, long amount, String description,
                         LocalDate date, BigDecimal rate, Long fromCurrencyId, Long toCurrencyId) {
    }
}
