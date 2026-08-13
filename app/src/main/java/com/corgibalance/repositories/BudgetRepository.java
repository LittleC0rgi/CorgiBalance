package com.corgibalance.repositories;

import com.corgibalance.models.Budget;
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

public class BudgetRepository {

    private static final String FIND_ALL_SQL =
            "SELECT id, name, tag_id, planned_amount, start_date, end_date, created_at, updated_at FROM budgets ORDER BY name COLLATE NOCASE";
    private static final String INSERT_SQL =
            "INSERT INTO budgets (name, tag_id, planned_amount, start_date, end_date) VALUES (?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE budgets SET name = ?, tag_id = ?, planned_amount = ?, start_date = ?, end_date = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
    private static final String DELETE_SQL =
            "DELETE FROM budgets WHERE id = ?";

    private final Database database;

    public BudgetRepository() {
        this(Database.getInstance());
    }

    public BudgetRepository(Database database) {
        this.database = database;
    }

    public List<Budget> findAll() {
        List<Budget> budgets = new ArrayList<>();
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    budgets.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load budgets", e);
        }
        return budgets;
    }

    public Budget create(Budget budget) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, budget.getName());
                statement.setLong(2, budget.getTagId());
                statement.setLong(3, budget.getPlannedAmount());
                statement.setString(4, toString(budget.getStartDate()));
                statement.setString(5, toString(budget.getEndDate()));
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        budget.setId(keys.getLong(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create budget", e);
        }
        return budget;
    }

    public void update(Budget budget) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
                statement.setString(1, budget.getName());
                statement.setLong(2, budget.getTagId());
                statement.setLong(3, budget.getPlannedAmount());
                statement.setString(4, toString(budget.getStartDate()));
                statement.setString(5, toString(budget.getEndDate()));
                statement.setLong(6, budget.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update budget", e);
        }
    }

    public void delete(Budget budget) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
                statement.setLong(1, budget.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete budget", e);
        }
    }

    private Budget mapRow(ResultSet resultSet) throws SQLException {
        Budget budget = new Budget();
        budget.setId(resultSet.getLong("id"));
        budget.setName(resultSet.getString("name"));
        budget.setTagId(resultSet.getObject("tag_id", Long.class));
        budget.setPlannedAmount(resultSet.getLong("planned_amount"));
        budget.setStartDate(toLocalDate(resultSet.getString("start_date")));
        budget.setEndDate(toLocalDate(resultSet.getString("end_date")));
        budget.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
        budget.setUpdatedAt(toLocalDateTime(resultSet.getTimestamp("updated_at")));
        return budget;
    }

    private LocalDate toLocalDate(String value) {
        return value == null ? null : LocalDate.parse(value);
    }

    private String toString(LocalDate value) {
        return value == null ? null : value.toString();
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
