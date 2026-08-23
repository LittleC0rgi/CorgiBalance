package com.corgibalance.controllers.tables;

import com.corgibalance.components.table.TextTableCell;
import com.corgibalance.models.Currency;
import com.corgibalance.repositories.CurrencyRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;

public class CurrencyTableController extends BaseTableController<Currency, CurrencyRepository> {

    @FXML
    private TableColumn<Currency, String> code;
    @FXML
    private TableColumn<Currency, String> name;
    @FXML
    private TableColumn<Currency, String> symbol;
    @FXML
    private TableColumn<Currency, String> minorUnit;

    public CurrencyTableController() {
        super(new CurrencyRepository());
    }

    @Override
    protected void configureColumns() {
        code.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getCode()));
        code.setCellFactory(_ -> new TextTableCell<>(Currency::getCode, "+ Add currency"));
        code.setOnEditCommit(this::onCodeCommitted);

        name.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        name.setCellFactory(_ -> new TextTableCell<>(Currency::getName, null));
        name.setOnEditCommit(this::onNameCommitted);

        symbol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSymbol()));
        symbol.setCellFactory(_ -> new TextTableCell<>(Currency::getSymbol, null));
        symbol.setOnEditCommit(this::onSymbolCommitted);

        minorUnit.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getMinorUnit())));
        minorUnit.setCellFactory(_ -> new TextTableCell<>(c -> String.valueOf(c.getMinorUnit()), null));
        minorUnit.setOnEditCommit(this::onMinorUnitCommitted);
    }

    private void onCodeCommitted(TableColumn.CellEditEvent<Currency, String> event) {
        String newCode = event.getNewValue() == null ? "" : event.getNewValue().trim();
        if (newCode.isEmpty()) {
            refresh();
            return;
        }
        commit(event.getRowValue(), currency -> currency.setCode(newCode), true);
    }

    private void onNameCommitted(TableColumn.CellEditEvent<Currency, String> event) {
        Currency currency = event.getRowValue();
        if (isPlaceholder(currency)) {
            refresh();
            return;
        }
        commit(currency, c -> c.setName(event.getNewValue()), false);
    }

    private void onSymbolCommitted(TableColumn.CellEditEvent<Currency, String> event) {
        Currency currency = event.getRowValue();
        if (isPlaceholder(currency)) {
            refresh();
            return;
        }
        commit(currency, c -> c.setSymbol(event.getNewValue()), false);
    }

    private void onMinorUnitCommitted(TableColumn.CellEditEvent<Currency, String> event) {
        Currency currency = event.getRowValue();
        if (isPlaceholder(currency)) {
            refresh();
            return;
        }
        try {
            int parsed = Integer.parseInt(event.getNewValue().trim());
            commit(currency, c -> c.setMinorUnit(parsed), false);
        } catch (NumberFormatException e) {
            refresh();
        }
    }

    @Override
    protected Currency newPlaceholder() {
        Currency currency = new Currency();
        currency.setName("");
        currency.setSymbol("");
        currency.setMinorUnit(2);
        return currency;
    }

    @Override
    protected String deleteConfirmationText(Currency currency) {
        return "Delete currency \"" + currency.getCode() + "\"?";
    }
}
