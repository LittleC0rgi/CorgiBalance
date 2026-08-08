package org.example.repositories;

import org.example.models.RecurrenceInterval;
import org.example.models.RecurringTransaction;
import org.example.models.TransactionType;
import org.example.services.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    private final Database database;

    public RecurringTransactionRepository() {
        this(Database.getInstance());
    }

    public RecurringTransactionRepository(Database database) {
        this.database = database;
    }

    public List<RecurringTransaction> findAll() {
        return query(FIND_ALL_SQL);
    }

    public List<RecurringTransaction> findActiveUpcoming() {
        return query(FIND_ACTIVE_UPCOMING_SQL);
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
        recurringTransaction.setTagId(resultSet.getObject("tag_id", Long.class));
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
