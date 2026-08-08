package org.example.repositories;

import org.example.services.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class SettingsRepository {

    private static final String GET_SQL =
            "SELECT value FROM settings WHERE key = ?";
    private static final String UPSERT_SQL =
            "INSERT INTO settings (key, value) VALUES (?, ?) "
            + "ON CONFLICT (key) DO UPDATE SET value = excluded.value, updated_at = CURRENT_TIMESTAMP";

    private final Database database;

    public SettingsRepository() {
        this(Database.getInstance());
    }

    public SettingsRepository(Database database) {
        this.database = database;
    }

    public Optional<String> get(String key) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(GET_SQL)) {
                statement.setString(1, key);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(resultSet.getString("value")) : Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load setting " + key, e);
        }
    }

    public Optional<Long> getLong(String key) {
        return get(key).map(Long::valueOf);
    }

    public void set(String key, String value) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(UPSERT_SQL)) {
                statement.setString(1, key);
                statement.setString(2, value);
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save setting " + key, e);
        }
    }

    public void setLong(String key, long value) {
        set(key, String.valueOf(value));
    }
}
