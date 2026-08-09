package org.example.services;

import org.example.models.Currency;
import org.example.repositories.CurrencyRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CurrencyFormatter {

    private final Map<Long, Currency> currencies;

    public CurrencyFormatter() {
        this(new CurrencyRepository().findAll());
    }

    public CurrencyFormatter(Collection<Currency> currencies) {
        this.currencies = new HashMap<>();
        for (Currency currency : currencies) {
            this.currencies.put(currency.getId(), currency);
        }
    }

    public Currency currency(Long currencyId) {
        return currencyId == null ? null : currencies.get(currencyId);
    }

    public int minorUnit(Long currencyId) {
        Currency currency = currency(currencyId);
        return currency == null ? 0 : currency.getMinorUnit();
    }

    public String format(long minorUnits, Long currencyId) {
        Currency currency = currency(currencyId);
        if (currency == null) {
            return String.valueOf(minorUnits);
        }
        String symbol = currency.getSymbol() == null ? "" : currency.getSymbol();
        return symbol + formatMinorUnits(minorUnits, currency.getMinorUnit());
    }

    public String toPlain(long minorUnits, Long currencyId) {
        return toUnits(minorUnits, minorUnit(currencyId)).toPlainString();
    }

    public BigDecimal parse(String text) throws NumberFormatException {
        String normalized = (text == null ? "" : text.trim()).replace(',', '.');
        if (normalized.isEmpty()) {
            throw new NumberFormatException("Amount is empty");
        }
        return new BigDecimal(normalized);
    }

    public long toMinorUnits(BigDecimal units, Long currencyId) {
        return units.movePointRight(minorUnit(currencyId))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private String formatMinorUnits(long minorUnits, int minorUnit) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,##0", symbols);
        formatter.setGroupingUsed(true);
        formatter.setMaximumFractionDigits(minorUnit);
        formatter.setMinimumFractionDigits(minorUnit);
        return formatter.format(toUnits(minorUnits, minorUnit));
    }

    private BigDecimal toUnits(long minorUnits, int minorUnit) {
        return minorUnit <= 0
                ? BigDecimal.valueOf(minorUnits)
                : BigDecimal.valueOf(minorUnits).movePointLeft(minorUnit);
    }
}
