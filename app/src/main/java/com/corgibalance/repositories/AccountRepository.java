package com.corgibalance.repositories;

import com.corgibalance.models.Account;
import com.corgibalance.services.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AccountRepository implements CrudRepository<Account> {

    private static final String FIND_ALL_SQL =
            "SELECT id, name, initial_balance, currency_id, created_at, updated_at FROM accounts ORDER BY name COLLATE NOCASE";
    private static final String INSERT_SQL =
            "INSERT INTO accounts (name, initial_balance, currency_id) VALUES (?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE accounts SET name = ?, initial_balance = ?, currency_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
    private static final String DELETE_SQL =
            "DELETE FROM accounts WHERE id = ?";
    private static final String CURRENT_BALANCE_SQL =
            "SELECT COALESCE((SELECT initial_balance FROM accounts WHERE id = ?), 0) "
            + "+ COALESCE((SELECT SUM(CASE "
            + "WHEN transaction_type = 'EXPENSE' THEN -amount "
            + "WHEN transaction_type = 'TRANSFER' AND direction = 0 THEN -amount "
            + "ELSE amount END) FROM transactions WHERE account_id = ?), 0)";

    private final Database database;

    public AccountRepository() {
        this(Database.getInstance());
    }

    public List<Account> findAll() {
        List<Account> accounts = new ArrayList<>();
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    accounts.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load accounts", e);
        }
        return accounts;
    }

    public long currentBalance(long accountId) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(CURRENT_BALANCE_SQL)) {
                statement.setLong(1, accountId);
                statement.setLong(2, accountId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? resultSet.getLong(1) : 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load account balance", e);
        }
    }

    public Account create(Account account) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, account.getName());
                statement.setLong(2, account.getInitialBalance());
                statement.setLong(3, account.getCurrencyId());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        account.setId(keys.getLong(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create account", e);
        }
        return account;
    }

    public void update(Account account) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
                statement.setString(1, account.getName());
                statement.setLong(2, account.getInitialBalance());
                statement.setLong(3, account.getCurrencyId());
                statement.setLong(4, account.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update account", e);
        }
    }

    public void delete(Account account) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
                statement.setLong(1, account.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete account", e);
        }
    }

    private Account mapRow(ResultSet resultSet) throws SQLException {
        Account account = new Account();
        account.setId(resultSet.getLong("id"));
        account.setName(resultSet.getString("name"));
        account.setInitialBalance(resultSet.getLong("initial_balance"));
        account.setCurrencyId(resultSet.getObject("currency_id", Long.class));
        account.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
        account.setUpdatedAt(toLocalDateTime(resultSet.getTimestamp("updated_at")));
        return account;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
