package org.example.components.views;

import javafx.fxml.FXML;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.example.components.table.Cells;
import org.example.components.table.ColumnSpec;
import org.example.components.table.CrudTable;
import org.example.components.table.FormSpec;
import org.example.models.Account;
import org.example.models.Currency;
import org.example.repositories.AccountRepository;
import org.example.repositories.CurrencyRepository;
import org.example.services.CurrencyFormatter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountsView extends View {

    @FXML
    private VBox content;

    public AccountsView() {
        super("Accounts", "/fxml/views/accounts.fxml");
    }

    @FXML
    private void initialize() {
        AccountRepository accountRepository = new AccountRepository();
        CurrencyRepository currencyRepository = new CurrencyRepository();
        CurrencyFormatter currencyFormatter = new CurrencyFormatter();
        Map<Long, String> currencyLabels = new HashMap<>();
        List<Long> currencyIds = new ArrayList<>();
        for (Currency currency : currencyRepository.findAll()) {
            currencyIds.add(currency.getId());
            currencyLabels.put(currency.getId(), currency.getCode());
        }

        ColumnSpec<Account> name = ColumnSpec.<Account>builder("Name")
                .width(260)
                .value(Account::getName)
                .editable(Cells.editableText(), (account, value) -> account.setName((String) value))
                .form(FormSpec.text())
                .required()
                .build();
        ColumnSpec<Account> currency = ColumnSpec.<Account>builder("Currency")
                .width(120)
                .value(Account::getCurrencyId)
                .editable(Cells.comboEditable(currencyIds, currencyLabels),
                        (account, value) -> account.setCurrencyId((Long) value))
                .form(FormSpec.combo(currencyIds, currencyLabels))
                .required()
                .build();
        ColumnSpec<Account> balance = ColumnSpec.<Account>builder("Initial balance")
                .width(160)
                .value(Account::getInitialBalance)
                .editable(Cells.amountEditable(currencyFormatter, Account::getCurrencyId),
                        (account, value) -> account.setInitialBalance(
                                currencyFormatter.toMinorUnits((BigDecimal) value, account.getCurrencyId())))
                .form(FormSpec.decimal())
                .build();
        ColumnSpec<Account> currentBalance = ColumnSpec.<Account>builder("Balance")
                .width(140)
                .value(account -> accountRepository.currentBalance(account.getId()))
                .cellFactory(Cells.amountFormatter(currencyFormatter, Account::getCurrencyId))
                .build();

        CrudTable<Account> table = new CrudTable<>(
                "Accounts", accountRepository, Account::new, List.of(name, currency, balance, currentBalance));
        VBox.setVgrow(table, Priority.ALWAYS);
        content.getChildren().add(table);
    }
}
