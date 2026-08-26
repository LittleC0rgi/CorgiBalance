package com.corgibalance.repositories;

import com.corgibalance.models.AccountFolder;
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
public class AccountFolderRepository implements CrudRepository<AccountFolder> {

    private static final String FIND_ALL_SQL =
            "SELECT id, name, created_at, updated_at FROM account_folders ORDER BY name COLLATE NOCASE";
    private static final String INSERT_SQL =
            "INSERT INTO account_folders (name) VALUES (?)";
    private static final String UPDATE_SQL =
            "UPDATE account_folders SET name = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
    private static final String DELETE_SQL =
            "DELETE FROM account_folders WHERE id = ?";

    private final Database database;

    public AccountFolderRepository() {
        this(Database.getInstance());
    }

    public List<AccountFolder> findAll() {
        List<AccountFolder> folders = new ArrayList<>();
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    folders.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load account folders", e);
        }
        return folders;
    }

    public AccountFolder create(AccountFolder folder) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, folder.getName());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        folder.setId(keys.getLong(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create account folder", e);
        }
        return folder;
    }

    public void update(AccountFolder folder) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
                statement.setString(1, folder.getName());
                statement.setLong(2, folder.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update account folder", e);
        }
    }

    public void delete(AccountFolder folder) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
                statement.setLong(1, folder.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete account folder", e);
        }
    }

    private AccountFolder mapRow(ResultSet resultSet) throws SQLException {
        AccountFolder folder = new AccountFolder();
        folder.setId(resultSet.getLong("id"));
        folder.setName(resultSet.getString("name"));
        folder.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
        folder.setUpdatedAt(toLocalDateTime(resultSet.getTimestamp("updated_at")));
        return folder;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
