package com.corgibalance.controllers.tables;

import com.corgibalance.components.HeroIcon;
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
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class TransactionTableController extends PagedTableController<Transaction, TransactionRepository> {

    private List<Account> accounts;
    private List<Tag> tags;
    private LocalDate lastEnteredDate;

    private Long accountFilter;
    private Long tagFilter;
    private TransactionType typeFilter;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String descriptionFilter;

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
        tag.setCellFactory(_ -> new SelectTableCell<>(tagIds(), this::tagName, this::tagColor, "") {
            @Override
            public void startEdit() {
                Transaction transaction = getTableRow() == null ? null : getTableRow().getItem();
                if (transaction != null && transaction.getTransactionType() == TransactionType.TRANSFER) {
                    return;
                }
                super.startEdit();
            }
        });
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

        amount.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getAmount()));
        amount.setCellFactory(_ -> new AmountTableCell<>(this::currencyIdOf,
                t -> t.getTransactionType() == TransactionType.EXPENSE
                        || (t.getTransactionType() == TransactionType.TRANSFER && t.getDirection() == 0),
                transaction -> transaction.getAmount() == 0));
        amount.setOnEditCommit(this::onAmountCommitted);

        description.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDescription()));
        description.setCellFactory(_ -> new DescriptionTemplateTableCell("+ Add transaction",
                q -> repository.findByDescriptionLike(q, 5),
                this::applyDescriptionTemplate,
                this::tagColor,
                this::currencyIdOfAccount,
                this::accountName));
        description.setOnEditCommit(this::onDescriptionCommitted);

        if (paginationBar != null) {
            attachColumnFilters();
        }
    }

    private void attachColumnFilters() {
        attachDateFilter(date);
        attachChoiceFilter(account, accountIds(), this::accountName, "All accounts", v -> accountFilter = v);
        attachChoiceFilter(tag, tagIds(), this::tagName, "All tags", v -> tagFilter = v);
        attachChoiceFilter(type, List.of(TransactionType.INCOME, TransactionType.EXPENSE, TransactionType.TRANSFER),
                this::typeName, "All types", v -> typeFilter = v);
        attachDescriptionFilter(description);
    }

    private void attachDateFilter(TableColumn<?, ?> column) {
        DatePicker from = new DatePicker();
        from.setPromptText("From");
        DatePicker to = new DatePicker();
        to.setPromptText("To");
        Button clear = new Button("Clear");
        clear.getStyleClass().addAll("btn", "btn--transparent");

        FilterMenu menu = new FilterMenu(new VBox(6, from, to, clear));
        Runnable apply = () -> {
            dateFrom = from.getValue();
            dateTo = to.getValue();
            updateFilter();
            menu.setActive(dateFrom != null || dateTo != null);
        };
        from.valueProperty().addListener((_, _, _) -> apply.run());
        to.valueProperty().addListener((_, _, _) -> apply.run());
        clear.setOnAction(_ -> {
            from.setValue(null);
            to.setValue(null);
        });
        installHeader(column, menu);
    }

    private <V> void attachChoiceFilter(TableColumn<?, ?> column, List<V> values, Function<V, String> labelFor,
                                        String allLabel, Consumer<V> store) {
        ComboBox<V> combo = new ComboBox<>();
        combo.getStyleClass().add("selector");
        combo.setItems(FXCollections.observableArrayList(values));
        combo.getItems().add(0, null);
        combo.setValue(null);
        combo.setCellFactory(_ -> choiceListCell(labelFor, allLabel));
        combo.setButtonCell(choiceListCell(labelFor, allLabel));
        combo.setMaxWidth(Double.MAX_VALUE);

        FilterMenu menu = new FilterMenu(combo);
        combo.valueProperty().addListener((_, _, value) -> {
            store.accept(value);
            updateFilter();
            menu.setActive(value != null);
        });
        installHeader(column, menu);
    }

    private void attachDescriptionFilter(TableColumn<?, ?> column) {
        TextField field = new TextField();
        field.setPromptText("Search description");
        field.getStyleClass().add("input");

        FilterMenu menu = new FilterMenu(field);
        field.textProperty().addListener((_, _, value) -> {
            descriptionFilter = value;
            updateFilter();
            menu.setActive(value != null && !value.isBlank());
        });
        installHeader(column, menu);
    }

    private <V> ListCell<V> choiceListCell(Function<V, String> labelFor, String allLabel) {
        return new ListCell<>() {
            @Override
            protected void updateItem(V value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    setText(null);
                } else if (value == null) {
                    setText(allLabel);
                } else {
                    setText(labelFor.apply(value));
                }
            }
        };
    }

    private void installHeader(TableColumn<?, ?> column, FilterMenu menu) {
        Label title = new Label(column.getText());
        title.getStyleClass().add("column-header__title");
        HBox header = new HBox(4, title, menu.button);
        header.setAlignment(Pos.CENTER_LEFT);
        column.setText(null);
        column.setGraphic(header);
    }

    private void updateFilter() {
        setFilter(this::matchesFilter);
    }

    private boolean matchesFilter(Transaction transaction) {
        if (accountFilter != null && !accountFilter.equals(transaction.getAccountId())) {
            return false;
        }
        if (tagFilter != null && !Objects.equals(tagFilter, transaction.getTagId())) {
            return false;
        }
        if (typeFilter != null && typeFilter != transaction.getTransactionType()) {
            return false;
        }
        LocalDate date = transaction.getTransactionDate();
        if (date != null && (dateFrom != null && date.isBefore(dateFrom) || dateTo != null && date.isAfter(dateTo))) {
            return false;
        }
        if (descriptionFilter != null && !descriptionFilter.isBlank()) {
            String description = transaction.getDescription() == null ? "" : transaction.getDescription();
            if (!description.toLowerCase().contains(descriptionFilter.toLowerCase())) {
                return false;
            }
        }
        return true;
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

    private static final class FilterMenu {
        private final Button button = new Button();
        private final Popup popup = new Popup();

        private FilterMenu(javafx.scene.Node content) {
            button.setGraphic(new HeroIcon(HeroIcon.Icon.FILTER));
            button.getStyleClass().add("column-header__filter");
            button.addEventFilter(MouseEvent.MOUSE_CLICKED, Event::consume);
            button.setOnAction(_ -> {
                if (popup.isShowing()) {
                    popup.hide();
                } else {
                    Bounds bounds = button.localToScreen(button.getBoundsInLocal());
                    popup.show(button, bounds.getMinX(), bounds.getMaxY() + 4);
                }
            });

            VBox box = new VBox(6, content);
            box.getStyleClass().add("filter-popup");
            popup.getContent().add(box);
            popup.setAutoHide(true);
        }

        private void setActive(boolean active) {
            if (active) {
                if (!button.getStyleClass().contains("column-header__filter--active")) {
                    button.getStyleClass().add("column-header__filter--active");
                }
            } else {
                button.getStyleClass().remove("column-header__filter--active");
            }
        }
    }
}
