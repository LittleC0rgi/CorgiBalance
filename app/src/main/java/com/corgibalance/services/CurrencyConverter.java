package com.corgibalance.services;

import com.corgibalance.models.Currency;
import com.corgibalance.models.ExchangeRate;
import com.corgibalance.repositories.CurrencyRepository;
import com.corgibalance.repositories.ExchangeRateRepository;
import com.corgibalance.repositories.SettingsRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CurrencyConverter {

    public static final String BASE_CURRENCY_KEY = "overview.baseCurrencyId";

    private static final int CONVERSION_SCALE = 12;

    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final Map<Long, Currency> currencies = new HashMap<>();
    private CurrencyFormatter formatter;

    public CurrencyConverter() {
        this(new CurrencyRepository(), new ExchangeRateRepository());
    }

    public CurrencyConverter(CurrencyRepository currencyRepository, ExchangeRateRepository exchangeRateRepository) {
        this.currencyRepository = currencyRepository;
        this.exchangeRateRepository = exchangeRateRepository;
        for (Currency currency : currencyRepository.findAll()) {
            currencies.put(currency.getId(), currency);
        }
        this.formatter = new CurrencyFormatter(currencies.values());
    }

    public List<Currency> currencies() {
        return List.copyOf(currencies.values());
    }

    public void reload() {
        currencies.clear();
        for (Currency currency : currencyRepository.findAll()) {
            currencies.put(currency.getId(), currency);
        }
        this.formatter = new CurrencyFormatter(currencies.values());
    }

    public Currency currency(long id) {
        return currencies.get(id);
    }

    public long convert(long amountMinor, Long fromCurrencyId, Long toCurrencyId) {
        if (fromCurrencyId == null || toCurrencyId == null || fromCurrencyId.equals(toCurrencyId)) {
            return amountMinor;
        }
        Optional<BigDecimal> rate = exchangeRate(fromCurrencyId, toCurrencyId);
        return rate.map(bigDecimal -> new BigDecimal(amountMinor).multiply(bigDecimal)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()).orElse(amountMinor);
    }

    public Optional<BigDecimal> rate(long fromCurrencyId, long toCurrencyId) {
        return exchangeRate(fromCurrencyId, toCurrencyId);
    }

    private Optional<BigDecimal> exchangeRate(long fromCurrencyId, long toCurrencyId) {
        Optional<ExchangeRate> direct = exchangeRateRepository.findLatest(fromCurrencyId, toCurrencyId);
        if (direct.isPresent()) {
            return Optional.of(direct.get().getRate());
        }
        Optional<ExchangeRate> inverse = exchangeRateRepository.findLatest(toCurrencyId, fromCurrencyId);
        return inverse.map(exchangeRate -> BigDecimal.ONE.divide(exchangeRate.getRate(), CONVERSION_SCALE, RoundingMode.HALF_UP));
    }

    public String format(long minorUnits, Long currencyId) {
        return formatter.format(minorUnits, currencyId);
    }

    public Long baseCurrencyId(SettingsRepository settings) {
        Optional<Long> saved = settings.getLong(BASE_CURRENCY_KEY);
        if (saved.isPresent() && currency(saved.get()) != null) {
            return saved.get();
        }
        List<Currency> list = currencies();
        return list.isEmpty() ? null : list.getFirst().getId();
    }
}
