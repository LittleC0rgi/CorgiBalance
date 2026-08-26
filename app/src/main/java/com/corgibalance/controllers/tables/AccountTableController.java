package com.corgibalance.controllers.tables;

import com.corgibalance.components.table.AmountTableCell;
import com.corgibalance.components.table.SelectTableCell;
import com.corgibalance.components.table.TextTableCell;
import com.corgibalance.models.Account;
import com.corgibalance.models.AccountFolder;
import com.corgibalance.models.Currency;
import com.corgibalance.repositories.AccountFolderRepository;
import com.corgibalance.repositories.AccountRepository;
import com.corgibalance.services.CurrencyFormatter;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

import java.util.List;

public class AccountTableController extends BaseTableController<Account, AccountRepository> {

    private final CurrencyFormatter currencyFormatter = new CurrencyFormatter();
    private final AccountFolderRepository folderRepository = new AccountFolderRepository();

    @FXML
    private TableColumn<Account, String> name;
    @FXML
    private TableColumn<Account, Long> currency;
    @FXML
    private TableColumn<Account, Long> folder;
    @FXML
    private TableColumn<Account, Long> initialBalance;
    @FXML
    private TableColumn<Account, Long> balance;

    public AccountTableController() {
        super(new AccountRepository());
    }

    @Override
    protected void configureColumns() {
        name.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        name.setCellFactory(_ -> new TextTableCell<>(Account::getName, "+ Add account"));
        name.setOnEditCommit(this::onNameCommitted);

        currency.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getCurrencyId()));
        currency.setCellFactory(_ -> new SelectTableCell<>(currencyIds(), this::currencyName));
        currency.setOnEditCommit(this::onCurrencyCommitted);

        folder.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getFolderId()));
        folder.setCellFactory(_ -> new SelectTableCell<>(folderIds(), this::folderName, null, "None"));
        folder.setOnEditCommit(this::onFolderCommitted);

        initialBalance.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getInitialBalance()));
        initialBalance.setCellFactory(_ -> new AmountTableCell<>(Account::getCurrencyId));
        initialBalance.setOnEditCommit(this::onBalanceCommitted);

        balance.setCellValueFactory(cell -> {
            Long id = cell.getValue().getId();
            return new ReadOnlyObjectWrapper<>(id == null ? null : repository.currentBalance(id));
        });
        balance.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(Long value, boolean empty) {
                super.updateItem(value, empty);
                Account account = getTableRow() == null ? null : getTableRow().getItem();
                setText(empty || account == null || value == null
                        ? null
                        : currencyFormatter.format(value, account.getCurrencyId()));
            }
        });
    }

    private void onNameCommitted(TableColumn.CellEditEvent<Account, String> event) {
        String newName = event.getNewValue() == null ? "" : event.getNewValue().trim();
        if (newName.isEmpty()) {
            refresh();
            return;
        }
        commit(event.getRowValue(), account -> account.setName(newName), true);
    }

    private void onBalanceCommitted(TableColumn.CellEditEvent<Account, Long> event) {
        Account account = event.getRowValue();
        if (isPlaceholder(account)) {
            refresh();
            return;
        }
        commit(account, a -> a.setInitialBalance(event.getNewValue()), false);
    }

    private void onCurrencyCommitted(TableColumn.CellEditEvent<Account, Long> event) {
        Account account = event.getRowValue();
        account.setCurrencyId(event.getNewValue());
        if (!isPlaceholder(account)) {
            repository.update(account);
            refresh();
        }
    }

    private void onFolderCommitted(TableColumn.CellEditEvent<Account, Long> event) {
        Account account = event.getRowValue();
        if (isPlaceholder(account)) {
            refresh();
            return;
        }
        account.setFolderId(event.getNewValue());
        repository.update(account);
        refresh();
    }

    @Override
    protected Account newPlaceholder() {
        Account account = new Account();
        account.setCurrencyId(defaultCurrencyId());
        return account;
    }

    private List<Long> currencyIds() {
        return currencyFormatter.currencies().stream().map(Currency::getId).toList();
    }

    private String currencyName(Long currencyId) {
        if (currencyId == null) {
            return "";
        }
        var currency = currencyFormatter.currency(currencyId);
        return currency == null ? String.valueOf(currencyId) : currency.getName();
    }

    private List<Long> folderIds() {
        return folderRepository.findAll().stream().map(AccountFolder::getId).toList();
    }

    private String folderName(Long folderId) {
        if (folderId == null) {
            return "";
        }
        return folderRepository.findAll().stream()
                .filter(f -> f.getId().equals(folderId))
                .map(AccountFolder::getName)
                .findFirst()
                .orElse(String.valueOf(folderId));
    }

    private Long defaultCurrencyId() {
        List<Currency> currencies = currencyFormatter.currencies();
        return currencies.isEmpty() ? null : currencies.getFirst().getId();
    }

    @Override
    protected String deleteConfirmationText(Account account) {
        return "Delete account \"" + account.getName() + "\"?";
    }
}
