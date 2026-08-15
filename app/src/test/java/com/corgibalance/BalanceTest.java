package com.corgibalance;

import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;

public class BalanceTest {

    private static final String BALANCE_SQL =
            "SELECT COALESCE((SELECT initial_balance FROM accounts WHERE id = ?), 0) "
            + "+ COALESCE((SELECT SUM(CASE "
            + "WHEN transaction_type = 'EXPENSE' THEN -amount "
            + "WHEN transaction_type = 'TRANSFER' AND direction = 0 THEN -amount "
            + "ELSE amount END) FROM transactions WHERE account_id = ?), 0)";

    private static final String NORMALIZE_TRANSFER_SQL =
            "UPDATE transactions SET amount = ABS(amount), direction = 0 WHERE transaction_type = 'TRANSFER' AND amount < 0";
    private static final String NORMALIZE_EXPENSE_SQL =
            "UPDATE transactions SET amount = ABS(amount) WHERE transaction_type IN ('INCOME', 'EXPENSE') AND amount < 0";

    @Test
    public void balanceUsesSignedDirectionAndNegatesExpenses() throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE accounts (id INTEGER PRIMARY KEY, initial_balance INTEGER)");
            statement.execute("CREATE TABLE transactions (id INTEGER PRIMARY KEY, account_id INTEGER, "
                    + "amount INTEGER, transaction_type TEXT, direction INTEGER DEFAULT 1)");
            statement.execute("INSERT INTO accounts VALUES (1, 10000)");
            statement.execute("INSERT INTO transactions (account_id, amount, transaction_type) VALUES (1, 100000, 'INCOME')");
            statement.execute("INSERT INTO transactions (account_id, amount, transaction_type) VALUES (1, 5000, 'EXPENSE')");
            statement.execute("INSERT INTO transactions (account_id, amount, transaction_type, direction) VALUES (1, 3000, 'TRANSFER', 0)");
            statement.execute("INSERT INTO transactions (account_id, amount, transaction_type, direction) VALUES (1, 3000, 'TRANSFER', 1)");
            assertEquals("10000 + 100000 - 5000 - 3000 + 3000", 105000, balance(connection, 1));
        }
    }

    @Test
    public void migrationNormalizesLegacySignedAmounts() throws Exception {
        Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE accounts (id INTEGER PRIMARY KEY, initial_balance INTEGER)");
            statement.execute("CREATE TABLE transactions (id INTEGER PRIMARY KEY, account_id INTEGER, "
                    + "amount INTEGER, transaction_type TEXT, direction INTEGER DEFAULT 1)");
            statement.execute("INSERT INTO accounts VALUES (1, 0)");
            statement.execute("INSERT INTO transactions (account_id, amount, transaction_type) VALUES (1, 100000, 'INCOME')");
            statement.execute("INSERT INTO transactions (account_id, amount, transaction_type) VALUES (1, -1200, 'EXPENSE')");
            statement.execute("INSERT INTO transactions (account_id, amount, transaction_type) VALUES (1, 4500, 'EXPENSE')");
            statement.execute("INSERT INTO transactions (account_id, amount, transaction_type, direction) VALUES (1, -7000, 'TRANSFER', 1)");
            statement.executeUpdate(NORMALIZE_TRANSFER_SQL);
            statement.executeUpdate(NORMALIZE_EXPENSE_SQL);
        }
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT amount, transaction_type, direction FROM transactions ORDER BY id")) {
            assertAllPositive(resultSet);
        }
        assertEquals("100000 - 1200 - 4500 - 7000", 87300, balance(connection, 1));
    }

    private long balance(Connection connection, long accountId) throws Exception {
        try (java.sql.PreparedStatement statement = connection.prepareStatement(BALANCE_SQL)) {
            statement.setLong(1, accountId);
            statement.setLong(2, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0;
            }
        }
    }

    private void assertAllPositive(ResultSet resultSet) throws Exception {
        int count = 0;
        while (resultSet.next()) {
            count++;
            long amount = resultSet.getLong("amount");
            String type = resultSet.getString("transaction_type");
            int direction = resultSet.getInt("direction");
            if (amount < 0) {
                throw new AssertionError("negative amount stored for " + type + " id-" + count);
            }
            if ("TRANSFER".equals(type) && direction != 0) {
                throw new AssertionError("transfer source with negative amount must have direction=0, got " + direction);
            }
        }
        if (count != 4) {
            throw new AssertionError("expected 4 rows, got " + count);
        }
    }
}