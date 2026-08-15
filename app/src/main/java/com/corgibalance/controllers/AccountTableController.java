package com.corgibalance.controllers;

import com.corgibalance.components.table.AmountTableCell;
import com.corgibalance.components.table.TextTableCell;
import com.corgibalance.components.table.SelectTableCell;
import com.corgibalance.models.Account;
import com.corgibalance.models.Currency;
import com.corgibalance.repositories.AccountRepository;
import com.corgibalance.services.CurrencyFormatter;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;

import java.util.List;

public class AccountTableController extends BaseTableController<Account, AccountRepository> {

    private final CurrencyFormatter currencyFormatter = new CurrencyFormatter();

    @FXML
    private TableColumn<Account, String> name;
    @FXML
    private TableColumn<Account, Long> currency;
    @FXML
    private TableColumn<Account, Long> initialBalance;

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

        initialBalance.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getInitialBalance()));
        initialBalance.setCellFactory(_ -> new AmountTableCell<>(Account::getCurrencyId));
        initialBalance.setOnEditCommit(this::onBalanceCommitted);
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

    private Long defaultCurrencyId() {
        List<Currency> currencies = currencyFormatter.currencies();
        return currencies.isEmpty() ? null : currencies.getFirst().getId();
    }

    @Override
    protected String deleteConfirmationText(Account account) {
        return "Delete account \"" + account.getName() + "\"?";
    }
}
