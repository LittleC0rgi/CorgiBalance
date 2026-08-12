package com.corgibalance;

import com.corgibalance.models.Currency;
import com.corgibalance.services.CurrencyFormatter;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class CurrencyFormatterTest {

    private final Currency usd = currency(1L, "USD", "$", 2);
    private final Currency rub = currency(2L, "RUB", "₽", 2);
    private final Currency jpy = currency(3L, "JPY", "¥", 0);
    private final CurrencyFormatter formatter = new CurrencyFormatter(List.of(usd, rub, jpy));

    private static Currency currency(long id, String code, String symbol, int minorUnit) {
        Currency currency = new Currency();
        currency.setId(id);
        currency.setCode(code);
        currency.setSymbol(symbol);
        currency.setMinorUnit(minorUnit);
        return currency;
    }

    @Test
    public void formatAddsSymbolAndDotSeparator() {
        assertEquals("$1 500.00", formatter.format(150000, usd.getId()));
        assertEquals("₽5 000.00", formatter.format(500000, rub.getId()));
    }

    @Test
    public void formatWithoutFractionDigitsWhenMinorUnitIsZero() {
        assertEquals("¥1 500", formatter.format(1500, jpy.getId()));
    }

    @Test
    public void formatFallsBackToRawValueWhenCurrencyUnknown() {
        assertEquals("150000", formatter.format(150000, null));
    }

    @Test
    public void toPlainReturnsDecimalWithoutSymbolOrGrouping() {
        assertEquals("1500.00", formatter.toPlain(150000, usd.getId()));
        assertEquals("1500", formatter.toPlain(1500, jpy.getId()));
    }

    @Test
    public void parseAcceptsDotAndCommaSeparators() {
        assertEquals(new BigDecimal("1500"), formatter.parse("1500"));
        assertEquals(new BigDecimal("1500.50"), formatter.parse("1500.50"));
        assertEquals(new BigDecimal("1500.50"), formatter.parse("1500,50"));
    }

    @Test
    public void parseRejectsEmptyAndInvalidInput() {
        assertParseFails("");
        assertParseFails("  ");
        assertParseFails("abc");
        assertParseFails("1.2.3");
    }

    @Test
    public void toMinorUnitsConvertsWholeUnitsToMinor() {
        assertEquals(150000, formatter.toMinorUnits(new BigDecimal("1500"), usd.getId()));
        assertEquals(150050, formatter.toMinorUnits(new BigDecimal("1500.50"), usd.getId()));
    }

    @Test
    public void toMinorUnitsRoundsHalfUp() {
        assertEquals(150001, formatter.toMinorUnits(new BigDecimal("1500.005"), usd.getId()));
        assertEquals(-150001, formatter.toMinorUnits(new BigDecimal("-1500.005"), usd.getId()));
    }

    @Test
    public void toMinorUnitsKeepsValueWhenMinorUnitIsZero() {
        assertEquals(1500, formatter.toMinorUnits(new BigDecimal("1500"), jpy.getId()));
    }

    private void assertParseFails(String text) {
        try {
            formatter.parse(text);
            fail("Expected NumberFormatException for input: " + text);
        } catch (NumberFormatException expected) {
            // expected
        }
    }
}
