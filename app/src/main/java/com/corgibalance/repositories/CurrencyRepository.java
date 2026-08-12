package com.corgibalance.repositories;

import com.corgibalance.components.table.CrudRepository;
import com.corgibalance.models.Currency;
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

public class CurrencyRepository implements CrudRepository<Currency> {

    private static final String FIND_ALL_SQL =
            "SELECT id, code, name, symbol, minor_unit, created_at, updated_at FROM currencies ORDER BY code";
    private static final String INSERT_SQL =
            "INSERT INTO currencies (code, name, symbol, minor_unit) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE currencies SET code = ?, name = ?, symbol = ?, minor_unit = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
    private static final String DELETE_SQL =
            "DELETE FROM currencies WHERE id = ?";

    private final Database database;

    public CurrencyRepository() {
        this(Database.getInstance());
    }

    public CurrencyRepository(Database database) {
        this.database = database;
    }

    @Override
    public List<Currency> findAll() {
        List<Currency> currencies = new ArrayList<>();
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    currencies.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load currencies", e);
        }
        return currencies;
    }

    @Override
    public Currency create(Currency currency) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, currency.getCode());
                statement.setString(2, currency.getName());
                statement.setString(3, currency.getSymbol());
                statement.setInt(4, currency.getMinorUnit());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        currency.setId(keys.getLong(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create currency", e);
        }
        return currency;
    }

    @Override
    public void update(Currency currency) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
                statement.setString(1, currency.getCode());
                statement.setString(2, currency.getName());
                statement.setString(3, currency.getSymbol());
                statement.setInt(4, currency.getMinorUnit());
                statement.setLong(5, currency.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update currency", e);
        }
    }

    @Override
    public void delete(Currency currency) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
                statement.setLong(1, currency.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete currency", e);
        }
    }

    private Currency mapRow(ResultSet resultSet) throws SQLException {
        Currency currency = new Currency();
        currency.setId(resultSet.getLong("id"));
        currency.setCode(resultSet.getString("code"));
        currency.setName(resultSet.getString("name"));
        currency.setSymbol(resultSet.getString("symbol"));
        currency.setMinorUnit(resultSet.getInt("minor_unit"));
        currency.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
        currency.setUpdatedAt(toLocalDateTime(resultSet.getTimestamp("updated_at")));
        return currency;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
