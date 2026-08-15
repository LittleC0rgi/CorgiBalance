package com.corgibalance.controllers;

import com.corgibalance.components.table.DateTableCell;
import com.corgibalance.components.table.SelectTableCell;
import com.corgibalance.components.table.TextTableCell;
import com.corgibalance.models.Currency;
import com.corgibalance.models.ExchangeRate;
import com.corgibalance.repositories.ExchangeRateRepository;
import com.corgibalance.services.CurrencyFormatter;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ExchangeRateTableController extends BaseTableController<ExchangeRate, ExchangeRateRepository> {

    private final CurrencyFormatter currencyFormatter = new CurrencyFormatter();

    @FXML
    private TableColumn<ExchangeRate, Long> fromCurrency;
    @FXML
    private TableColumn<ExchangeRate, Long> toCurrency;
    @FXML
    private TableColumn<ExchangeRate, String> rate;
    @FXML
    private TableColumn<ExchangeRate, LocalDate> rateDate;

    public ExchangeRateTableController() {
        super(new ExchangeRateRepository());
    }

    @Override
    protected void configureColumns() {
        fromCurrency.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getFromCurrencyId()));
        fromCurrency.setCellFactory(_ -> new SelectTableCell<>(currencyIds(), this::currencyName));
        fromCurrency.setOnEditCommit(this::onFromCommitted);

        toCurrency.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getToCurrencyId()));
        toCurrency.setCellFactory(_ -> new SelectTableCell<>(currencyIds(), this::currencyName));
        toCurrency.setOnEditCommit(this::onToCommitted);

        rate.setCellValueFactory(cell -> new SimpleStringProperty(rateText(cell.getValue())));
        rate.setCellFactory(_ -> new TextTableCell<>(this::rateText, "+ Add rate"));
        rate.setOnEditCommit(this::onRateCommitted);

        rateDate.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getRateDate()));
        rateDate.setCellFactory(_ -> new DateTableCell<>());
        rateDate.setOnEditCommit(this::onRateDateCommitted);
    }

    private void onFromCommitted(TableColumn.CellEditEvent<ExchangeRate, Long> event) {
        ExchangeRate exchangeRate = event.getRowValue();
        exchangeRate.setFromCurrencyId(event.getNewValue());
        if (!isPlaceholder(exchangeRate)) {
            repository.update(exchangeRate);
            refresh();
        }
    }

    private void onToCommitted(TableColumn.CellEditEvent<ExchangeRate, Long> event) {
        ExchangeRate exchangeRate = event.getRowValue();
        exchangeRate.setToCurrencyId(event.getNewValue());
        if (!isPlaceholder(exchangeRate)) {
            repository.update(exchangeRate);
            refresh();
        }
    }

    private void onRateCommitted(TableColumn.CellEditEvent<ExchangeRate, String> event) {
        ExchangeRate exchangeRate = event.getRowValue();
        String text = event.getNewValue() == null ? "" : event.getNewValue().trim();
        if (text.isEmpty()) {
            refresh();
            return;
        }
        try {
            commit(exchangeRate, er -> er.setRate(new BigDecimal(text)), true);
        } catch (NumberFormatException e) {
            refresh();
        }
    }

    private void onRateDateCommitted(TableColumn.CellEditEvent<ExchangeRate, LocalDate> event) {
        ExchangeRate exchangeRate = event.getRowValue();
        if (isPlaceholder(exchangeRate)) {
            refresh();
            return;
        }
        commit(exchangeRate, er -> er.setRateDate(event.getNewValue()), false);
    }

    private String rateText(ExchangeRate exchangeRate) {
        return exchangeRate.getRate() == null ? "" : exchangeRate.getRate().toPlainString();
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

    @Override
    protected ExchangeRate newPlaceholder() {
        ExchangeRate exchangeRate = new ExchangeRate();
        List<Currency> currencies = currencyFormatter.currencies();
        if (currencies.size() >= 2) {
            exchangeRate.setFromCurrencyId(currencies.get(0).getId());
            exchangeRate.setToCurrencyId(currencies.get(1).getId());
        }
        exchangeRate.setRateDate(LocalDate.now());
        return exchangeRate;
    }

    @Override
    protected String deleteConfirmationText(ExchangeRate exchangeRate) {
        return "Delete exchange rate " + currencyName(exchangeRate.getFromCurrencyId())
                + " -> " + currencyName(exchangeRate.getToCurrencyId())
                + " on " + exchangeRate.getRateDate() + "?";
    }
}