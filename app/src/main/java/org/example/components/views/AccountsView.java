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
        CurrencyRepository currencyRepository = new CurrencyRepository();
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
        ColumnSpec<Account> balance = ColumnSpec.<Account>builder("Initial balance")
                .width(160)
                .value(account -> account.getInitialBalance())
                .editable(Cells.longEditable(),
                        (account, value) -> account.setInitialBalance(((Number) value).longValue()))
                .form(FormSpec.number())
                .build();
        ColumnSpec<Account> currency = ColumnSpec.<Account>builder("Currency")
                .width(120)
                .value(Account::getCurrencyId)
                .editable(Cells.comboEditable(currencyIds, currencyLabels),
                        (account, value) -> account.setCurrencyId((Long) value))
                .form(FormSpec.combo(currencyIds, currencyLabels))
                .required()
                .build();

        CrudTable<Account> table = new CrudTable<>(
                "Accounts", new AccountRepository(), Account::new, List.of(name, balance, currency));
        VBox.setVgrow(table, Priority.ALWAYS);
        content.getChildren().add(table);
    }
}
