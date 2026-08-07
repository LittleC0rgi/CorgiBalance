package org.example.repositories;

import org.example.components.table.CrudRepository;
import org.example.models.Account;
import org.example.services.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AccountRepository implements CrudRepository<Account> {

    private static final String FIND_ALL_SQL =
            "SELECT id, name, initial_balance, currency_id, created_at, updated_at FROM accounts ORDER BY name COLLATE NOCASE";
    private static final String INSERT_SQL =
            "INSERT INTO accounts (name, initial_balance, currency_id) VALUES (?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE accounts SET name = ?, initial_balance = ?, currency_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
    private static final String DELETE_SQL =
            "DELETE FROM accounts WHERE id = ?";

    private final Database database;

    public AccountRepository() {
        this(Database.getInstance());
    }

    public AccountRepository(Database database) {
        this.database = database;
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
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
