package com.corgibalance.controllers;

import com.corgibalance.components.table.AmountTableCell;
import com.corgibalance.components.table.DateTableCell;
import com.corgibalance.components.table.TextTableCell;
import com.corgibalance.components.table.SelectTableCell;
import com.corgibalance.models.Account;
import com.corgibalance.models.Tag;
import com.corgibalance.models.Transaction;
import com.corgibalance.models.TransactionType;
import com.corgibalance.repositories.AccountRepository;
import com.corgibalance.repositories.TagRepository;
import com.corgibalance.repositories.TransactionRepository;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;

import java.time.LocalDate;
import java.util.List;

public class TransactionTableController extends BaseTableController<Transaction, TransactionRepository> {

    private List<Account> accounts;
    private List<Tag> tags;

    @FXML
    private TableColumn<Transaction, LocalDate> date;
    @FXML
    private TableColumn<Transaction, Long> account;
    @FXML
    private TableColumn<Transaction, Long> tag;
    @FXML
    private TableColumn<Transaction, TransactionType> type;
    @FXML
    private TableColumn<Transaction, String> description;
    @FXML
    private TableColumn<Transaction, Long> amount;

    public TransactionTableController() {
        super(new TransactionRepository());
        this.accounts = new AccountRepository().findAll();
        this.tags = new TagRepository().findAll();
    }

    public void reload() {
        this.accounts = new AccountRepository().findAll();
        this.tags = new TagRepository().findAll();
        table.refresh();
    }

    @Override
    protected void configureColumns() {
        date.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getTransactionDate()));
        date.setCellFactory(_ -> new DateTableCell<>());
        date.setOnEditCommit(this::onDateCommitted);

        account.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getAccountId()));
        account.setCellFactory(_ -> new SelectTableCell<>(accountIds(), this::accountName));
        account.setOnEditCommit(this::onAccountCommitted);

        tag.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getTagId()));
        tag.setCellFactory(_ -> new SelectTableCell<>(tagIds(), this::tagName));
        tag.setOnEditCommit(this::onTagCommitted);

        type.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getTransactionType()));
        type.setCellFactory(_ -> new SelectTableCell<>(List.of(TransactionType.INCOME, TransactionType.EXPENSE), this::typeName) {
            @Override
            public void startEdit() {
                Transaction transaction = getTableRow() == null ? null : getTableRow().getItem();
                if (transaction != null && transaction.getTransactionType() == TransactionType.TRANSFER) {
                    return;
                }
                super.startEdit();
            }
        });
        type.setOnEditCommit(this::onTypeCommitted);

        description.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescription()));
        description.setCellFactory(_ -> new TextTableCell<>(Transaction::getDescription, "+ Add transaction"));
        description.setOnEditCommit(this::onDescriptionCommitted);

        amount.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getAmount()));
        amount.setCellFactory(_ -> new AmountTableCell<>(this::currencyIdOf));
        amount.setOnEditCommit(this::onAmountCommitted);
    }

    private void onDateCommitted(TableColumn.CellEditEvent<Transaction, LocalDate> event) {
        commit(event.getRowValue(), t -> t.setTransactionDate(event.getNewValue()), false);
    }

    private void onAmountCommitted(TableColumn.CellEditEvent<Transaction, Long> event) {
        commit(event.getRowValue(), t -> t.setAmount(event.getNewValue()), false);
    }

    private void onAccountCommitted(TableColumn.CellEditEvent<Transaction, Long> event) {
        Transaction transaction = event.getRowValue();
        transaction.setAccountId(event.getNewValue());
        if (!isPlaceholder(transaction)) {
            repository.update(transaction);
            refresh();
        }
    }

    private void onTagCommitted(TableColumn.CellEditEvent<Transaction, Long> event) {
        Transaction transaction = event.getRowValue();
        transaction.setTagId(event.getNewValue());
        if (!isPlaceholder(transaction)) {
            repository.update(transaction);
            refresh();
        }
    }

    private void onTypeCommitted(TableColumn.CellEditEvent<Transaction, TransactionType> event) {
        Transaction transaction = event.getRowValue();
        transaction.setTransactionType(event.getNewValue());
        if (!isPlaceholder(transaction)) {
            repository.update(transaction);
            refresh();
        }
    }

    private void onDescriptionCommitted(TableColumn.CellEditEvent<Transaction, String> event) {
        String newDescription = event.getNewValue() == null ? "" : event.getNewValue().trim();
        if (newDescription.isEmpty()) {
            refresh();
            return;
        }
        commit(event.getRowValue(), t -> t.setDescription(newDescription), true);
    }

    private List<Long> accountIds() {
        return accounts.stream().map(Account::getId).toList();
    }

    private List<Long> tagIds() {
        return tags.stream().map(Tag::getId).toList();
    }

    private String accountName(Long accountId) {
        if (accountId == null) {
            return "";
        }
        for (Account account : accounts) {
            if (account.getId().equals(accountId)) {
                return account.getName();
            }
        }
        return "";
    }

    private String tagName(Long tagId) {
        if (tagId == null) {
            return "";
        }
        for (Tag tag : tags) {
            if (tag.getId().equals(tagId)) {
                return tag.getName();
            }
        }
        return "";
    }

    private String typeName(TransactionType type) {
        return type == null ? "" : type.toString();
    }

    private Long currencyIdOf(Transaction transaction) {
        if (transaction.getAccountId() == null) {
            return null;
        }
        for (Account account : accounts) {
            if (account.getId().equals(transaction.getAccountId())) {
                return account.getCurrencyId();
            }
        }
        return null;
    }

    @Override
    protected Transaction newPlaceholder() {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.EXPENSE);
        transaction.setTransactionDate(LocalDate.now());
        if (!accounts.isEmpty()) {
            transaction.setAccountId(accounts.getFirst().getId());
        }
        if (!tags.isEmpty()) {
            transaction.setTagId(tags.getFirst().getId());
        }
        return transaction;
    }

    @Override
    protected String deleteConfirmationText(Transaction transaction) {
        return "Delete this transaction?";
    }
}
