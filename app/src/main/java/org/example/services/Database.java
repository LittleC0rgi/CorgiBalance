package org.example.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Database {

    private static final Logger logger = Logger.getLogger(Database.class.getName());

    private static final Database INSTANCE = new Database();

    private Connection connection;

    private Database() {
    }

    public static Database getInstance() {
        return INSTANCE;
    }

    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect();
        }
        return connection;
    }

    private void connect() throws SQLException {
        try {
            Path dbDir = Path.of(System.getProperty("user.home"), ".corgibalance");
            Files.createDirectories(dbDir);
            Path dbPath = dbDir.resolve("corgibalance.db");
            logger.info("Connecting to database: " + dbPath.toAbsolutePath());
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            initDatabase();
        } catch (IOException e) {
            throw new SQLException("Failed to create database directory", e);
        }
    }

    private void initDatabase() throws SQLException {
        try (InputStream input = Database.class.getResourceAsStream("/db/init.sql")) {
            if (input == null) {
                throw new SQLException("Database initialization script /db/init.sql not found");
            }
            String script = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            logger.info("Applying database initialization script (" + script.length() + " chars)");
            try (Statement statement = connection.createStatement()) {
                for (String sql : script.split(";\\s*")) {
                    if (sql.trim().isEmpty()) {
                        continue;
                    }
                    statement.execute(sql);
                }
            }
            logger.info("Database initialization completed");
        } catch (IOException e) {
            throw new SQLException("Failed to read database initialization script", e);
        }
    }

    public synchronized void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
