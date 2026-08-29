package com.corgibalance.services;

import java.math.BigDecimal;
import java.math.MathContext;

public final class AmountExpressionParser {

    private static final MathContext DIVISION_CONTEXT = MathContext.DECIMAL128;

    private final String src;
    private int pos;

    private AmountExpressionParser(String src) {
        this.src = src;
    }

    public static BigDecimal evaluate(String expression) {
        String normalized = (expression == null ? "" : expression.trim()).replace(',', '.');
        if (normalized.isEmpty()) {
            throw new NumberFormatException("Empty expression");
        }
        try {
            AmountExpressionParser parser = new AmountExpressionParser(normalized);
            BigDecimal value = parser.parseExpression();
            if (parser.pos < normalized.length()) {
                throw new NumberFormatException("Unexpected character at " + parser.pos);
            }
            return value;
        } catch (ArithmeticException e) {
            throw new NumberFormatException(e.getMessage());
        }
    }

    private BigDecimal parseExpression() {
        BigDecimal value = parseTerm();
        while (true) {
            if (match('+')) {
                value = value.add(parseTerm());
            } else if (match('-')) {
                value = value.subtract(parseTerm());
            } else {
                return value;
            }
        }
    }

    private BigDecimal parseTerm() {
        BigDecimal value = parseFactor();
        while (true) {
            if (match('*')) {
                value = value.multiply(parseFactor());
            } else if (match('/')) {
                value = value.divide(parseFactor(), DIVISION_CONTEXT);
            } else {
                return value;
            }
        }
    }

    private BigDecimal parseFactor() {
        if (match('+')) {
            return parseFactor();
        }
        if (match('-')) {
            return parseFactor().negate();
        }
        if (match('(')) {
            BigDecimal value = parseExpression();
            expect(')');
            return value;
        }
        return parseNumber();
    }

    private BigDecimal parseNumber() {
        skipWhitespace();
        int start = pos;
        while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) {
            pos++;
        }
        if (start == pos) {
            throw new NumberFormatException("Unexpected character at " + pos);
        }
        try {
            return new BigDecimal(src.substring(start, pos));
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Invalid number at " + start);
        }
    }

    private boolean match(char c) {
        skipWhitespace();
        if (pos < src.length() && src.charAt(pos) == c) {
            pos++;
            return true;
        }
        return false;
    }

    private void expect(char c) {
        skipWhitespace();
        if (pos >= src.length() || src.charAt(pos) != c) {
            throw new NumberFormatException("Expected '" + c + "' at " + pos);
        }
        pos++;
    }

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
            pos++;
        }
    }
}
