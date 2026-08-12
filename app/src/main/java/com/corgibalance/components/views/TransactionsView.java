package com.corgibalance.components.views;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import com.corgibalance.components.inputs.TransferDialog;
import com.corgibalance.components.table.Cells;
import com.corgibalance.components.table.ColumnSpec;
import com.corgibalance.components.table.CrudTable;
import com.corgibalance.components.table.FormSpec;
import com.corgibalance.models.Account;
import com.corgibalance.models.Currency;
import com.corgibalance.models.Tag;
import com.corgibalance.models.Transaction;
import com.corgibalance.models.TransactionType;
import com.corgibalance.repositories.AccountRepository;
import com.corgibalance.repositories.CurrencyRepository;
import com.corgibalance.repositories.ExchangeRateRepository;
import com.corgibalance.repositories.TagRepository;
import com.corgibalance.repositories.TransactionRepository;
import com.corgibalance.services.CurrencyFormatter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class TransactionsView extends View {

    @FXML
    private VBox content;

    private LocalDate lastUsedDate;
    private List<Long> accountIds;
    private Map<Long, String> accountLabels;
    private Map<Long, Long> accountCurrencyIds;
    private Map<Long, String> currencyCodes;
    private TransactionRepository transactionRepository;
    private ExchangeRateRepository exchangeRateRepository;
    private CurrencyFormatter currencyFormatter;

    public TransactionsView() {
        super("Transactions", "/fxml/views/transactions.fxml");
    }

    @FXML
    private void initialize() {
        transactionRepository = new TransactionRepository();
        exchangeRateRepository = new ExchangeRateRepository();
        currencyFormatter = new CurrencyFormatter();

        accountIds = new ArrayList<>();
        accountLabels = new HashMap<>();
        accountCurrencyIds = new HashMap<>();
        AccountRepository accountRepository = new AccountRepository();
        for (Account account : accountRepository.findAll()) {
            accountIds.add(account.getId());
            accountLabels.put(account.getId(), account.getName());
            if (account.getCurrencyId() != null) {
                accountCurrencyIds.put(account.getId(), account.getCurrencyId());
            }
        }

        currencyCodes = new HashMap<>();
        CurrencyRepository currencyRepository = new CurrencyRepository();
        for (Currency currency : currencyRepository.findAll()) {
            currencyCodes.put(currency.getId(), currency.getCode());
        }

        TagRepository tagRepository = new TagRepository();
        Map<Long, String> tagLabels = new HashMap<>();
        Map<Long, String> tagColors = new HashMap<>();
        List<Long> tagIds = new ArrayList<>();
        for (Tag tag : tagRepository.findAll()) {
            tagIds.add(tag.getId());
            tagLabels.put(tag.getId(), tag.getName());
            tagColors.put(tag.getId(), tag.getColor());
        }

        List<TransactionType> transactionTypes = List.of(TransactionType.INCOME, TransactionType.EXPENSE);

        ColumnSpec<Transaction> date = ColumnSpec.<Transaction>builder("Date")
                .width(120)
                .value(Transaction::getTransactionDate)
                .editable(Cells.dateEditable(),
                        (transaction, value) -> transaction.setTransactionDate((LocalDate) value))
                .form(FormSpec.date())
                .defaultValue(() -> lastUsedDate != null ? lastUsedDate : LocalDate.now())
                .required()
                .build();
        ColumnSpec<Transaction> type = ColumnSpec.<Transaction>builder("Type")
                .width(110)
                .value(Transaction::getTransactionType)
                .editable(Cells.enumEditable(transactionTypes),
                        (transaction, value) -> transaction.setTransactionType((TransactionType) value))
                .form(FormSpec.enumValue(transactionTypes))
                .required()
                .build();
        ColumnSpec<Transaction> account = ColumnSpec.<Transaction>builder("Account")
                .width(160)
                .value(Transaction::getAccountId)
                .editable(Cells.comboEditable(accountIds, accountLabels),
                        (transaction, value) -> transaction.setAccountId((Long) value))
                .form(FormSpec.combo(accountIds, accountLabels))
                .hint(accountId -> accountId instanceof Long id
                        ? "Balance: " + currencyFormatter.format(
                                accountRepository.currentBalance(id), accountCurrencyIds.get(id))
                        : "")
                .required()
                .build();
        ColumnSpec<Transaction> toAccount = ColumnSpec.<Transaction>builder("To account")
                .width(140)
                .value(Transaction::getToAccountId)
                .editable(Cells.comboEditable(accountIds, accountLabels),
                        (transaction, value) -> transaction.setToAccountId((Long) value))
                .build();
        ColumnSpec<Transaction> tag = ColumnSpec.<Transaction>builder("Tag")
                .width(140)
                .value(Transaction::getTagId)
                .editable(Cells.tagEditable(tagIds, tagLabels, tagColors),
                        (transaction, value) -> transaction.setTagId((Long) value))
                .form(FormSpec.combo(tagIds, tagLabels))
                .build();
        ColumnSpec<Transaction> description = ColumnSpec.<Transaction>builder("Description")
                .width(220)
                .value(Transaction::getDescription)
                .editable(Cells.editableText(),
                        (transaction, value) -> transaction.setDescription((String) value))
                .form(FormSpec.text())
                .build();
        ColumnSpec<Transaction> rate = ColumnSpec.<Transaction>builder("Rate")
                .width(90)
                .value(Transaction::getRate)
                .build();
        ColumnSpec<Transaction> amount = ColumnSpec.<Transaction>builder("Amount")
                .width(140)
                .value(Transaction::getAmount)
                .editable(signedAmountCell(currencyFormatter,
                                transaction -> accountCurrencyIds.get(transaction.getAccountId())),
                        (transaction, value) -> transaction.setAmount(currencyFormatter.toMinorUnits(
                                (BigDecimal) value, accountCurrencyIds.get(transaction.getAccountId()))))
                .form(FormSpec.decimal())
                .required()
                .build();

        CrudTable<Transaction> table = new CrudTable<>(
                "Transactions", transactionRepository, Transaction::new,
                List.of(date, type, account, toAccount, tag, description, rate, amount));
        table.setAfterCreate(transaction -> lastUsedDate = transaction.getTransactionDate());
        table.addToolbarButton("New Transfer", event -> showTransferDialog(table));
        VBox.setVgrow(table, Priority.ALWAYS);
        content.getChildren().add(table);
    }

    private Callback<TableColumn<Transaction, Object>, TableCell<Transaction, Object>> signedAmountCell(
            CurrencyFormatter formatter, Function<Transaction, Long> currencyOf) {
        return column -> new SignedAmountCell(formatter, currencyOf);
    }

    private static final class SignedAmountCell extends TableCell<Transaction, Object> {

        private static final Color INCOME_COLOR = Color.web("#72A276");
        private static final Color EXPENSE_COLOR = Color.web("#CA2E55");

        private final CurrencyFormatter formatter;
        private final Function<Transaction, Long> currencyOf;
        private final TextField textField = new TextField();

        SignedAmountCell(CurrencyFormatter formatter, Function<Transaction, Long> currencyOf) {
            this.formatter = formatter;
            this.currencyOf = currencyOf;
            textField.setOnAction(event -> commitAmount());
            textField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (wasFocused && !isFocused) {
                    commitAmount();
                }
            });
        }

        @Override
        protected void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else if (isEditing()) {
                setText(null);
                setGraphic(textField);
            } else {
                setText(displayText(item));
                setTextFill(textFill());
                setGraphic(null);
            }
        }

        @Override
        public void startEdit() {
            if (!isEditable()) {
                return;
            }
            super.startEdit();
            setText(null);
            setGraphic(textField);
            textField.setText(toPlain(getItem()));
            textField.requestFocus();
            textField.selectAll();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(displayText(getItem()));
            setTextFill(textFill());
            setGraphic(null);
        }

        private void commitAmount() {
            String text = textField.getText() == null ? "" : textField.getText().trim();
            if (text.isEmpty()) {
                cancelEdit();
                return;
            }
            try {
                BigDecimal value = formatter.parse(text);
                Long currencyId = currencyIdOf(row());
                formatter.toMinorUnits(value, currencyId);
                commitEdit(value);
            } catch (RuntimeException e) {
                cancelEdit();
            }
        }

        private String displayText(Object item) {
            String formatted = formatter.format(Math.abs(minorUnits(item)), currencyIdOf(row()));
            TransactionType type = typeOf(row());
            if (type == TransactionType.INCOME) {
                return "+" + formatted;
            }
            if (type == TransactionType.EXPENSE) {
                return "-" + formatted;
            }
            return formatted;
        }

        private String toPlain(Object item) {
            return formatter.toPlain(minorUnits(item), currencyIdOf(row()));
        }

        private Color textFill() {
            TransactionType type = typeOf(row());
            if (type == TransactionType.INCOME) {
                return INCOME_COLOR;
            }
            if (type == TransactionType.EXPENSE) {
                return EXPENSE_COLOR;
            }
            return null;
        }

        private Transaction row() {
            TableView<Transaction> tableView = getTableView();
            if (tableView == null) {
                return null;
            }
            List<Transaction> items = tableView.getItems();
            if (items == null) {
                return null;
            }
            int index = getIndex();
            return index >= 0 && index < items.size() ? items.get(index) : null;
        }

        private TransactionType typeOf(Transaction row) {
            return row == null ? null : row.getTransactionType();
        }

        private Long currencyIdOf(Transaction row) {
            return row == null ? null : currencyOf.apply(row);
        }

        private long minorUnits(Object item) {
            return item instanceof Number number ? number.longValue() : 0L;
        }
    }

    private void showTransferDialog(CrudTable<Transaction> table) {
        TransferDialog dialog = new TransferDialog(accountIds, accountLabels, accountCurrencyIds, currencyCodes);
        dialog.showAndWait().ifPresent(result -> {
            try {
                transactionRepository.createTransfer(result.fromAccountId(), result.toAccountId(),
                        result.amount(), result.description(), result.date(), result.rate());
                if (result.rate() != null && result.fromCurrencyId() != null
                        && result.toCurrencyId() != null && !result.fromCurrencyId().equals(result.toCurrencyId())) {
                    exchangeRateRepository.save(result.fromCurrencyId(), result.toCurrencyId(),
                            result.rate(), result.date());
                }
                table.refresh();
            } catch (RuntimeException e) {
                showError("Failed to create transfer", e.getMessage());
            }
        });
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
