package com.corgibalance.repositories;

import com.corgibalance.models.RecurrenceInterval;
import com.corgibalance.models.RecurringTransaction;
import com.corgibalance.models.TransactionType;
import com.corgibalance.services.Database;

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

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RecurringTransactionRepository {

    private static final String FIND_ALL_SQL =
            "SELECT id, account_id, tag_id, amount, description, transaction_type, "
            + "start_date, next_date, end_date, interval, active, created_at, updated_at "
            + "FROM recurring_transactions "
            + "ORDER BY next_date";
    private static final String FIND_ACTIVE_UPCOMING_SQL =
            "SELECT id, account_id, tag_id, amount, description, transaction_type, "
            + "start_date, next_date, end_date, interval, active, created_at, updated_at "
            + "FROM recurring_transactions "
            + "WHERE active = 1 "
            + "ORDER BY next_date";
    private static final String INSERT_SQL =
            "INSERT INTO recurring_transactions (account_id, tag_id, amount, description, transaction_type, "
            + "start_date, next_date, end_date, interval, active) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE recurring_transactions SET account_id = ?, tag_id = ?, amount = ?, description = ?, "
            + "transaction_type = ?, start_date = ?, next_date = ?, end_date = ?, interval = ?, active = ?, "
            + "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
    private static final String DELETE_SQL =
            "DELETE FROM recurring_transactions WHERE id = ?";

    private final Database database;

    public RecurringTransactionRepository() {
        this(Database.getInstance());
    }

    public List<RecurringTransaction> findAll() {
        return query(FIND_ALL_SQL);
    }

    public List<RecurringTransaction> findActiveUpcoming() {
        return query(FIND_ACTIVE_UPCOMING_SQL);
    }

    public RecurringTransaction create(RecurringTransaction recurringTransaction) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                bind(statement, recurringTransaction, 1);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        recurringTransaction.setId(keys.getLong(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create recurring transaction", e);
        }
        return recurringTransaction;
    }

    public void update(RecurringTransaction recurringTransaction) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
                int index = bind(statement, recurringTransaction, 1);
                statement.setLong(index, recurringTransaction.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update recurring transaction", e);
        }
    }

    public void delete(RecurringTransaction recurringTransaction) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
                statement.setLong(1, recurringTransaction.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete recurring transaction", e);
        }
    }

    private int bind(PreparedStatement statement, RecurringTransaction recurringTransaction, int index) throws SQLException {
        statement.setLong(index++, recurringTransaction.getAccountId());
        if (recurringTransaction.getTagId() == null) {
            statement.setNull(index++, java.sql.Types.INTEGER);
        } else {
            statement.setLong(index++, recurringTransaction.getTagId());
        }
        statement.setLong(index++, recurringTransaction.getAmount());
        statement.setString(index++, recurringTransaction.getDescription());
        statement.setString(index++, recurringTransaction.getTransactionType().toString());
        statement.setString(index++, recurringTransaction.getStartDate().toString());
        statement.setString(index++, recurringTransaction.getNextDate().toString());
        statement.setString(index++, recurringTransaction.getEndDate() == null ? null : recurringTransaction.getEndDate().toString());
        statement.setString(index++, recurringTransaction.getInterval().toString());
        statement.setInt(index++, recurringTransaction.isActive() ? 1 : 0);
        return index;
    }

    private List<RecurringTransaction> query(String sql) {
        List<RecurringTransaction> recurringTransactions = new ArrayList<>();
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    recurringTransactions.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load recurring transactions", e);
        }
        return recurringTransactions;
    }

    private RecurringTransaction mapRow(ResultSet resultSet) throws SQLException {
        RecurringTransaction recurringTransaction = new RecurringTransaction();
        recurringTransaction.setId(resultSet.getLong("id"));
        recurringTransaction.setAccountId(resultSet.getLong("account_id"));
        long tagId = resultSet.getLong("tag_id");
        recurringTransaction.setTagId(resultSet.wasNull() ? null : tagId);
        recurringTransaction.setAmount(resultSet.getLong("amount"));
        recurringTransaction.setDescription(resultSet.getString("description"));
        recurringTransaction.setTransactionType(TransactionType.valueOf(resultSet.getString("transaction_type")));
        recurringTransaction.setStartDate(toLocalDate(resultSet.getString("start_date")));
        recurringTransaction.setNextDate(toLocalDate(resultSet.getString("next_date")));
        recurringTransaction.setEndDate(toLocalDate(resultSet.getString("end_date")));
        recurringTransaction.setInterval(RecurrenceInterval.valueOf(resultSet.getString("interval")));
        recurringTransaction.setActive(resultSet.getInt("active") == 1);
        recurringTransaction.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
        recurringTransaction.setUpdatedAt(toLocalDateTime(resultSet.getTimestamp("updated_at")));
        return recurringTransaction;
    }

    private LocalDate toLocalDate(String value) {
        return value == null ? null : LocalDate.parse(value);
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
