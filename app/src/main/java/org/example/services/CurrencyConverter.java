package org.example.services;

import org.example.models.Currency;
import org.example.models.ExchangeRate;
import org.example.repositories.CurrencyRepository;
import org.example.repositories.ExchangeRateRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CurrencyConverter {

    private static final int CONVERSION_SCALE = 12;

    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final Map<Long, Currency> currencies = new HashMap<>();

    public CurrencyConverter() {
        this(new CurrencyRepository(), new ExchangeRateRepository());
    }

    public CurrencyConverter(CurrencyRepository currencyRepository, ExchangeRateRepository exchangeRateRepository) {
        this.currencyRepository = currencyRepository;
        this.exchangeRateRepository = exchangeRateRepository;
        for (Currency currency : currencyRepository.findAll()) {
            currencies.put(currency.getId(), currency);
        }
    }

    public List<Currency> currencies() {
        return List.copyOf(currencies.values());
    }

    public void reload() {
        currencies.clear();
        for (Currency currency : currencyRepository.findAll()) {
            currencies.put(currency.getId(), currency);
        }
    }

    public Currency currency(long id) {
        return currencies.get(id);
    }

    public long convert(long amountMinor, Long fromCurrencyId, Long toCurrencyId) {
        if (fromCurrencyId == null || toCurrencyId == null || fromCurrencyId.equals(toCurrencyId)) {
            return amountMinor;
        }
        Optional<BigDecimal> rate = exchangeRate(fromCurrencyId, toCurrencyId);
        if (rate.isEmpty()) {
            return amountMinor;
        }
        return new BigDecimal(amountMinor).multiply(rate.get())
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private Optional<BigDecimal> exchangeRate(long fromCurrencyId, long toCurrencyId) {
        Optional<ExchangeRate> direct = exchangeRateRepository.findLatest(fromCurrencyId, toCurrencyId);
        if (direct.isPresent()) {
            return Optional.of(direct.get().getRate());
        }
        Optional<ExchangeRate> inverse = exchangeRateRepository.findLatest(toCurrencyId, fromCurrencyId);
        if (inverse.isPresent()) {
            return Optional.of(BigDecimal.ONE.divide(inverse.get().getRate(), CONVERSION_SCALE, RoundingMode.HALF_UP));
        }
        return Optional.empty();
    }

    public String format(long minorUnits, Long currencyId) {
        Currency currency = currencyId == null ? null : currencies.get(currencyId);
        if (currency == null) {
            return String.valueOf(minorUnits);
        }
        String symbol = currency.getSymbol() == null ? "" : currency.getSymbol();
        return symbol + formatMinorUnits(minorUnits, currency.getMinorUnit());
    }

    private String formatMinorUnits(long minorUnits, int minorUnit) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator('.');
        DecimalFormat formatter = new DecimalFormat("0", symbols);
        formatter.setGroupingUsed(true);
        formatter.setMaximumFractionDigits(minorUnit);
        formatter.setMinimumFractionDigits(minorUnit);
        BigDecimal value = minorUnit <= 0
                ? BigDecimal.valueOf(minorUnits)
                : BigDecimal.valueOf(minorUnits).movePointLeft(minorUnit);
        return formatter.format(value);
    }
}
