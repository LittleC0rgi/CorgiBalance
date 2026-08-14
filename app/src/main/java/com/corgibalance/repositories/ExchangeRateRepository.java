package com.corgibalance.repositories;

import com.corgibalance.models.ExchangeRate;
import com.corgibalance.services.Database;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ExchangeRateRepository implements CrudRepository<ExchangeRate> {

    private static final String FIND_ALL_SQL =
            "SELECT id, from_currency_id, to_currency_id, rate, rate_date, created_at, updated_at "
            + "FROM exchange_rates "
            + "ORDER BY rate_date DESC, from_currency_id, to_currency_id";
    private static final String FIND_LATEST_SQL =
            "SELECT id, from_currency_id, to_currency_id, rate, rate_date, created_at, updated_at "
            + "FROM exchange_rates "
            + "WHERE from_currency_id = ? AND to_currency_id = ? "
            + "ORDER BY rate_date DESC, id DESC LIMIT 1";
    private static final String UPSERT_SQL =
            "INSERT INTO exchange_rates (from_currency_id, to_currency_id, rate, rate_date) "
            + "VALUES (?, ?, ?, ?) "
            + "ON CONFLICT (from_currency_id, to_currency_id, rate_date) "
            + "DO UPDATE SET rate = excluded.rate, updated_at = CURRENT_TIMESTAMP";
    private static final String INSERT_SQL =
            "INSERT INTO exchange_rates (from_currency_id, to_currency_id, rate, rate_date) "
            + "VALUES (?, ?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE exchange_rates SET from_currency_id = ?, to_currency_id = ?, rate = ?, "
            + "rate_date = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
    private static final String DELETE_SQL =
            "DELETE FROM exchange_rates WHERE id = ?";

    private final Database database;

    public ExchangeRateRepository() {
        this(Database.getInstance());
    }

    public List<ExchangeRate> findAll() {
        List<ExchangeRate> exchangeRates = new ArrayList<>();
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    exchangeRates.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load exchange rates", e);
        }
        return exchangeRates;
    }

    public ExchangeRate create(ExchangeRate exchangeRate) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                bind(statement, exchangeRate);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        exchangeRate.setId(keys.getLong(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create exchange rate", e);
        }
        return exchangeRate;
    }

    public void update(ExchangeRate exchangeRate) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
                bind(statement, exchangeRate);
                statement.setLong(5, exchangeRate.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update exchange rate", e);
        }
    }

    public void delete(ExchangeRate exchangeRate) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
                statement.setLong(1, exchangeRate.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete exchange rate", e);
        }
    }

    private void bind(PreparedStatement statement, ExchangeRate exchangeRate) throws SQLException {
        statement.setLong(1, exchangeRate.getFromCurrencyId());
        statement.setLong(2, exchangeRate.getToCurrencyId());
        statement.setString(3, exchangeRate.getRate().toPlainString());
        statement.setString(4, exchangeRate.getRateDate().toString());
    }

    public Optional<ExchangeRate> findLatest(long fromCurrencyId, long toCurrencyId) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(FIND_LATEST_SQL)) {
                statement.setLong(1, fromCurrencyId);
                statement.setLong(2, toCurrencyId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(mapRow(resultSet)) : Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load exchange rate", e);
        }
    }

    public void save(long fromCurrencyId, long toCurrencyId, BigDecimal rate, LocalDate rateDate) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(UPSERT_SQL)) {
                statement.setLong(1, fromCurrencyId);
                statement.setLong(2, toCurrencyId);
                statement.setString(3, rate.toPlainString());
                statement.setString(4, rateDate.toString());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save exchange rate", e);
        }
    }

    private ExchangeRate mapRow(ResultSet resultSet) throws SQLException {
        ExchangeRate exchangeRate = new ExchangeRate();
        exchangeRate.setId(resultSet.getLong("id"));
        exchangeRate.setFromCurrencyId(resultSet.getLong("from_currency_id"));
        exchangeRate.setToCurrencyId(resultSet.getLong("to_currency_id"));
        exchangeRate.setRate(new BigDecimal(resultSet.getString("rate")));
        exchangeRate.setRateDate(LocalDate.parse(resultSet.getString("rate_date")));
        exchangeRate.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
        exchangeRate.setUpdatedAt(toLocalDateTime(resultSet.getTimestamp("updated_at")));
        return exchangeRate;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
