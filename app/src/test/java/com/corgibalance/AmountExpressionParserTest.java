package com.corgibalance;

import com.corgibalance.services.AmountExpressionParser;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class AmountExpressionParserTest {

    @Test
    public void additionAndSubtraction() {
        assertEquals(new BigDecimal("15"), AmountExpressionParser.evaluate("10 + 10 - 5"));
        assertEquals(new BigDecimal("7"), AmountExpressionParser.evaluate("3+4"));
    }

    @Test
    public void multiplicationAndDivision() {
        assertEquals(new BigDecimal("6"), AmountExpressionParser.evaluate("2 * 3"));
        assertEquals(new BigDecimal("2.5"), AmountExpressionParser.evaluate("5 / 2"));
    }

    @Test
    public void respectsOperatorPrecedence() {
        assertEquals(new BigDecimal("20"), AmountExpressionParser.evaluate("10 + 5 * 2"));
        assertEquals(new BigDecimal("4"), AmountExpressionParser.evaluate("10 - 3 * 2"));
    }

    @Test
    public void parenthesesOverridePrecedence() {
        assertEquals(new BigDecimal("30"), AmountExpressionParser.evaluate("(10 + 5) * 2"));
        assertEquals(new BigDecimal("4"), AmountExpressionParser.evaluate("(10 - 2) / 2"));
    }

    @Test
    public void unaryMinusAndPlus() {
        assertEquals(new BigDecimal("-5"), AmountExpressionParser.evaluate("-5"));
        assertEquals(new BigDecimal("5"), AmountExpressionParser.evaluate("+5"));
        assertEquals(new BigDecimal("5"), AmountExpressionParser.evaluate("10 + -5"));
    }

    @Test
    public void decimalsWithDotAndComma() {
        assertEquals(new BigDecimal("1.5"), AmountExpressionParser.evaluate("1.5"));
        assertEquals(new BigDecimal("1.5"), AmountExpressionParser.evaluate("1,5"));
        assertEquals(new BigDecimal("2.0"), AmountExpressionParser.evaluate("0.5 + 1.5"));
    }

    @Test
    public void ignoresWhitespaceAroundTokens() {
        assertEquals(new BigDecimal("3"), AmountExpressionParser.evaluate("  1  +  2  "));
        assertEquals(new BigDecimal("2"), AmountExpressionParser.evaluate("1 + 1"));
    }

    @Test
    public void rejectsInvalidInput() {
        assertFails("");
        assertFails("  ");
        assertFails("1 +");
        assertFails("(1 + 2");
        assertFails("1 2");
        assertFails("abc");
        assertFails("1.2.3");
    }

    @Test
    public void divisionByZeroFails() {
        assertFails("1 / 0");
    }

    private void assertFails(String expression) {
        try {
            AmountExpressionParser.evaluate(expression);
            fail("Expected NumberFormatException for expression: " + expression);
        } catch (NumberFormatException expected) {
            // expected
        }
    }
}
