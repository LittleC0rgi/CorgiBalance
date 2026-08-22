package com.corgibalance.controllers.tables;

import com.corgibalance.components.table.AmountTableCell;
import com.corgibalance.components.table.DateTableCell;
import com.corgibalance.components.table.DescriptionTemplateTableCell;
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
import java.util.function.Consumer;

public class TransactionTableController extends BaseTableController<Transaction, TransactionRepository> {

    private List<Account> accounts;
    private List<Tag> tags;
    private LocalDate lastEnteredDate;

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
        loadData();
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
        tag.setCellFactory(_ -> new SelectTableCell<>(tagIds(), this::tagName, this::tagColor));
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
        description.setCellFactory(_ -> new DescriptionTemplateTableCell("+ Add transaction",
                q -> repository.findByDescriptionLike(q, 5),
                this::applyDescriptionTemplate,
                this::tagColor,
                this::currencyIdOfAccount));
        description.setOnEditCommit(this::onDescriptionCommitted);

        amount.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getAmount()));
        amount.setCellFactory(_ -> new AmountTableCell<>(this::currencyIdOf,
                t -> t.getTransactionType() == TransactionType.EXPENSE
                        || (t.getTransactionType() == TransactionType.TRANSFER && t.getDirection() == 0),
                transaction -> transaction.getAmount() == 0));
        amount.setOnEditCommit(this::onAmountCommitted);
    }

    @Override
    protected void commit(Transaction item, Consumer<Transaction> apply, boolean createOnPlaceholder) {
        if (isPlaceholder(item) && createOnPlaceholder && item.getTransactionDate() != null) {
            lastEnteredDate = item.getTransactionDate();
        }
        super.commit(item, apply, createOnPlaceholder);
    }

    private void onDateCommitted(TableColumn.CellEditEvent<Transaction, LocalDate> event) {
        Transaction transaction = event.getRowValue();
        commit(transaction, t -> t.setTransactionDate(event.getNewValue()), false);
        if (transaction.getTransferId() != null) {
            for (Transaction item : table.getItems()) {
                if (item != transaction && transaction.getTransferId().equals(item.getTransferId())) {
                    item.setTransactionDate(event.getNewValue());
                }
            }
            table.refresh();
        }
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

    private void applyDescriptionTemplate(Transaction template, Transaction target) {
        target.setAccountId(template.getAccountId());
        target.setTagId(template.getTagId());
        target.setTransactionType(template.getTransactionType());
        target.setAmount(template.getAmount());
        target.setToAccountId(template.getToAccountId());
        target.setTransferId(template.getTransferId());
        target.setRate(template.getRate());
        target.setDirection(template.getDirection());
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

    private String tagColor(Long tagId) {
        if (tagId == null) {
            return null;
        }
        for (Tag tag : tags) {
            if (tag.getId().equals(tagId)) {
                return tag.getColor();
            }
        }
        return null;
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

    private Long currencyIdOfAccount(Long accountId) {
        if (accountId == null) {
            return null;
        }
        for (Account account : accounts) {
            if (account.getId().equals(accountId)) {
                return account.getCurrencyId();
            }
        }
        return null;
    }

    @Override
    protected Transaction newPlaceholder() {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.EXPENSE);
        transaction.setTransactionDate(lastEnteredDate != null ? lastEnteredDate : LocalDate.now());
        Transaction last = repository.findLastInserted();
        if (last != null) {
            transaction.setAccountId(last.getAccountId());
            transaction.setTagId(last.getTagId());
            if (last.getTransactionType() != TransactionType.TRANSFER) {
                transaction.setTransactionType(last.getTransactionType());
            }
        }
        if (transaction.getAccountId() == null && !accounts.isEmpty()) {
            transaction.setAccountId(accounts.getFirst().getId());
        }
        if (transaction.getTagId() == null && !tags.isEmpty()) {
            transaction.setTagId(tags.getFirst().getId());
        }
        return transaction;
    }

    @Override
    protected String deleteConfirmationText(Transaction transaction) {
        return "Delete this transaction?";
    }
}
