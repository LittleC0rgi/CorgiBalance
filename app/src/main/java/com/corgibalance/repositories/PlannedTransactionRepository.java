package com.corgibalance.repositories;

import com.corgibalance.models.PlannedTransaction;
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
public class PlannedTransactionRepository implements CrudRepository<PlannedTransaction> {

    private static final String FIND_ALL_SQL =
            "SELECT id, account_id, tag_id, amount, description, transaction_type, planned_date, created_at, updated_at "
            + "FROM planned_transactions ORDER BY planned_date, id";
    private static final String FIND_BY_DATE_RANGE_SQL =
            "SELECT id, account_id, tag_id, amount, description, transaction_type, planned_date, created_at, updated_at "
            + "FROM planned_transactions WHERE planned_date >= ? AND planned_date <= ? ORDER BY planned_date, id";
    private static final String INSERT_SQL =
            "INSERT INTO planned_transactions (account_id, tag_id, amount, description, transaction_type, planned_date) "
            + "VALUES (?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE planned_transactions SET account_id = ?, tag_id = ?, amount = ?, description = ?, "
            + "transaction_type = ?, planned_date = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
    private static final String DELETE_SQL =
            "DELETE FROM planned_transactions WHERE id = ?";

    private final Database database;

    public PlannedTransactionRepository() {
        this(Database.getInstance());
    }

    public List<PlannedTransaction> findAll() {
        return query(FIND_ALL_SQL);
    }

    public List<PlannedTransaction> findByDateRange(LocalDate from, LocalDate to) {
        List<PlannedTransaction> plannedTransactions = new ArrayList<>();
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(FIND_BY_DATE_RANGE_SQL)) {
                statement.setString(1, from.toString());
                statement.setString(2, to.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        plannedTransactions.add(mapRow(resultSet));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load planned transactions", e);
        }
        return plannedTransactions;
    }

    private List<PlannedTransaction> query(String sql) {
        List<PlannedTransaction> plannedTransactions = new ArrayList<>();
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    plannedTransactions.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load planned transactions", e);
        }
        return plannedTransactions;
    }

    @Override
    public PlannedTransaction create(PlannedTransaction plannedTransaction) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, plannedTransaction.getAccountId());
                setNullableLong(statement, 2, plannedTransaction.getTagId());
                statement.setLong(3, plannedTransaction.getAmount());
                statement.setString(4, plannedTransaction.getDescription());
                statement.setString(5, plannedTransaction.getTransactionType().toString());
                statement.setString(6, plannedTransaction.getPlannedDate().toString());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        plannedTransaction.setId(keys.getLong(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create planned transaction", e);
        }
        return plannedTransaction;
    }

    @Override
    public void update(PlannedTransaction plannedTransaction) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
                statement.setLong(1, plannedTransaction.getAccountId());
                setNullableLong(statement, 2, plannedTransaction.getTagId());
                statement.setLong(3, plannedTransaction.getAmount());
                statement.setString(4, plannedTransaction.getDescription());
                statement.setString(5, plannedTransaction.getTransactionType().toString());
                statement.setString(6, plannedTransaction.getPlannedDate().toString());
                statement.setLong(7, plannedTransaction.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update planned transaction", e);
        }
    }

    @Override
    public void delete(PlannedTransaction plannedTransaction) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
                statement.setLong(1, plannedTransaction.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete planned transaction", e);
        }
    }

    private PlannedTransaction mapRow(ResultSet resultSet) throws SQLException {
        PlannedTransaction plannedTransaction = new PlannedTransaction();
        plannedTransaction.setId(resultSet.getLong("id"));
        plannedTransaction.setAccountId(getNullableLong(resultSet, "account_id"));
        plannedTransaction.setTagId(getNullableLong(resultSet, "tag_id"));
        plannedTransaction.setAmount(resultSet.getLong("amount"));
        plannedTransaction.setDescription(resultSet.getString("description"));
        plannedTransaction.setTransactionType(TransactionType.valueOf(resultSet.getString("transaction_type")));
        plannedTransaction.setPlannedDate(LocalDate.parse(resultSet.getString("planned_date")));
        plannedTransaction.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
        plannedTransaction.setUpdatedAt(toLocalDateTime(resultSet.getTimestamp("updated_at")));
        return plannedTransaction;
    }

    private Long getNullableLong(ResultSet resultSet, String column) throws SQLException {
        long value = resultSet.getLong(column);
        return resultSet.wasNull() ? null : value;
    }

    private void setNullableLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
        } else {
            statement.setLong(index, value);
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}