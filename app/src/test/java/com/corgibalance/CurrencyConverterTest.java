package com.corgibalance;

import com.corgibalance.models.Currency;
import com.corgibalance.models.ExchangeRate;
import com.corgibalance.repositories.CurrencyRepository;
import com.corgibalance.repositories.ExchangeRateRepository;
import com.corgibalance.services.CurrencyConverter;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class CurrencyConverterTest {

    private static final Currency USD = currency(1L, "USD", "$", 2);

    private static CurrencyConverter converter(CurrencyRepository currencies, ExchangeRateRepository rates) {
        return new CurrencyConverter(currencies, rates);
    }

    private static CurrencyRepository currencies(Currency... currencies) {
        return new CurrencyRepository() {
            @Override
            public List<Currency> findAll() {
                return List.of(currencies);
            }
        };
    }

    private static ExchangeRateRepository rate(long from, long to, String rate) {
        return new ExchangeRateRepository() {
            @Override
            public Optional<ExchangeRate> findLatest(long fromCurrencyId, long toCurrencyId) {
                if (fromCurrencyId != from || toCurrencyId != to) {
                    return Optional.empty();
                }
                ExchangeRate exchangeRate = new ExchangeRate();
                exchangeRate.setFromCurrencyId(from);
                exchangeRate.setToCurrencyId(to);
                exchangeRate.setRate(new BigDecimal(rate));
                exchangeRate.setRateDate(LocalDate.now());
                return Optional.of(exchangeRate);
            }
        };
    }

    private static Currency currency(long id, String code, String symbol, int minorUnit) {
        Currency currency = new Currency();
        currency.setId(id);
        currency.setCode(code);
        currency.setSymbol(symbol);
        currency.setMinorUnit(minorUnit);
        return currency;
    }

    @Test
    public void convertReturnsAmountWhenSameCurrency() {
        CurrencyConverter converter = converter(currencies(USD), rate(1L, 2L, "90"));
        assertEquals(150000, converter.convert(150000, 1L, 1L));
    }

    @Test
    public void convertReturnsAmountWhenCurrencyUnknown() {
        CurrencyConverter converter = converter(currencies(USD), rate(1L, 2L, "90"));
        assertEquals(100, converter.convert(100, null, 1L));
        assertEquals(100, converter.convert(100, 1L, null));
        assertEquals(100, converter.convert(100, null, null));
    }

    @Test
    public void convertAppliesDirectRate() {
        CurrencyConverter converter = converter(currencies(USD), rate(1L, 2L, "90"));
        assertEquals(900000, converter.convert(10000, 1L, 2L));
    }

    @Test
    public void convertRoundsHalfUp() {
        CurrencyConverter converter = converter(currencies(USD), rate(1L, 2L, "1.5"));
        assertEquals(15002, converter.convert(10001, 1L, 2L));
    }

    @Test
    public void convertAppliesInverseRate() {
        CurrencyConverter converter = converter(currencies(USD), rate(1L, 2L, "2"));
        assertEquals(500, converter.convert(1000, 2L, 1L));
    }

    @Test
    public void convertRoundsInverseRateHalfUp() {
        CurrencyConverter converter = converter(currencies(USD), rate(1L, 2L, "3"));
        assertEquals(3333, converter.convert(10000, 2L, 1L));
    }

    @Test
    public void convertReturnsAmountWhenNoRateFound() {
        CurrencyConverter converter = converter(currencies(USD), new ExchangeRateRepository() {
            @Override
            public Optional<ExchangeRate> findLatest(long fromCurrencyId, long toCurrencyId) {
                return Optional.empty();
            }
        });
        assertEquals(100, converter.convert(100, 2L, 1L));
    }

    @Test
    public void formatDelegatesToCurrencyFormatter() {
        CurrencyConverter converter = converter(currencies(USD), rate(1L, 2L, "90"));
        assertEquals("$1 500.00", converter.format(150000, 1L));
    }
}
