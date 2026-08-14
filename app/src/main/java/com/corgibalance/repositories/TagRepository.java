package com.corgibalance.repositories;

import com.corgibalance.models.Tag;
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
public class TagRepository implements CrudRepository<Tag> {

    private static final String FIND_ALL_SQL =
            "SELECT id, name, color, icon, created_at, updated_at FROM tags ORDER BY name COLLATE NOCASE";
    private static final String INSERT_SQL =
            "INSERT INTO tags (name, color, icon) VALUES (?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE tags SET name = ?, color = ?, icon = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
    private static final String DELETE_SQL =
            "DELETE FROM tags WHERE id = ?";

    private final Database database;

    public TagRepository() {
        this(Database.getInstance());
    }

    public List<Tag> findAll() {
        List<Tag> tags = new ArrayList<>();
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    tags.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load tags", e);
        }
        return tags;
    }

    public Tag create(Tag tag) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, tag.getName());
                statement.setString(2, tag.getColor());
                statement.setString(3, tag.getIcon());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        tag.setId(keys.getLong(1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create tag", e);
        }
        return tag;
    }

    public void update(Tag tag) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
                statement.setString(1, tag.getName());
                statement.setString(2, tag.getColor());
                statement.setString(3, tag.getIcon());
                statement.setLong(4, tag.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update tag", e);
        }
    }

    public void delete(Tag tag) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(DELETE_SQL)) {
                statement.setLong(1, tag.getId());
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete tag", e);
        }
    }

    private Tag mapRow(ResultSet resultSet) throws SQLException {
        Tag tag = new Tag();
        tag.setId(resultSet.getLong("id"));
        tag.setName(resultSet.getString("name"));
        tag.setColor(resultSet.getString("color"));
        tag.setIcon(resultSet.getString("icon"));
        tag.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
        tag.setUpdatedAt(toLocalDateTime(resultSet.getTimestamp("updated_at")));
        return tag;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
