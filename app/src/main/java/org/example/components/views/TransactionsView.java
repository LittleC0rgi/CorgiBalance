package org.example.components.views;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.example.components.inputs.TransferDialog;
import org.example.components.table.Cells;
import org.example.components.table.ColumnSpec;
import org.example.components.table.CrudTable;
import org.example.components.table.FormSpec;
import org.example.models.Account;
import org.example.models.Currency;
import org.example.models.Tag;
import org.example.models.Transaction;
import org.example.models.TransactionType;
import org.example.repositories.AccountRepository;
import org.example.repositories.CurrencyRepository;
import org.example.repositories.ExchangeRateRepository;
import org.example.repositories.TagRepository;
import org.example.repositories.TransactionRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public TransactionsView() {
        super("Transactions", "/fxml/views/transactions.fxml");
    }

    @FXML
    private void initialize() {
        transactionRepository = new TransactionRepository();
        exchangeRateRepository = new ExchangeRateRepository();

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
        List<Long> tagIds = new ArrayList<>();
        for (Tag tag : tagRepository.findAll()) {
            tagIds.add(tag.getId());
            tagLabels.put(tag.getId(), tag.getName());
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
                .editable(Cells.comboEditable(tagIds, tagLabels),
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
                .value(transaction -> transaction.getAmount())
                .editable(Cells.longEditable(),
                        (transaction, value) -> transaction.setAmount(((Number) value).longValue()))
                .form(FormSpec.number())
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
