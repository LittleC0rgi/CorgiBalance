package org.example.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
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
        applyScript("/db/init.sql");
        if (isTableEmpty("currencies")) {
            applyScript("/db/seed.sql");
        }
        migrateTransactions();
    }

    private void applyScript(String resource) throws SQLException {
        try (InputStream input = Database.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new SQLException("Database script " + resource + " not found");
            }
            String script = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            logger.info("Applying database script " + resource + " (" + script.length() + " chars)");
            try (Statement statement = connection.createStatement()) {
                for (String sql : script.split(";\\s*")) {
                    if (sql.trim().isEmpty()) {
                        continue;
                    }
                    statement.execute(sql);
                }
            }
            logger.info("Database script " + resource + " completed");
        } catch (IOException e) {
            throw new SQLException("Failed to read database script " + resource, e);
        }
    }

    private boolean isTableEmpty(String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                 "SELECT COUNT(*) FROM (SELECT 1 FROM " + table + " LIMIT 1)")) {
            return result.next() && result.getInt(1) == 0;
        }
    }

    private void migrateTransactions() throws SQLException {
        String schema = transactionsSchemaSql();
        boolean hasTransferType = schema != null && schema.contains("TRANSFER");
        if (!hasTransferType) {
            rebuildTransactionsTable();
        } else if (!transactionColumnNames().contains("rate")) {
            logger.info("Adding rate column to transactions table");
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE transactions ADD COLUMN rate TEXT");
            }
        }
    }

    private String transactionsSchemaSql() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                 "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'transactions'")) {
            return resultSet.next() ? resultSet.getString(1) : null;
        }
    }

    private Set<String> transactionColumnNames() throws SQLException {
        Set<String> columns = new HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("PRAGMA table_info(transactions)")) {
            while (resultSet.next()) {
                columns.add(resultSet.getString("name"));
            }
        }
        return columns;
    }

    private void rebuildTransactionsTable() throws SQLException {
        logger.info("Migrating transactions table schema to support transfers");
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE transactions RENAME TO transactions_migrate");
            statement.execute(TRANSACTIONS_SCHEMA);
            statement.execute(
                    "INSERT INTO transactions (id, account_id, tag_id, amount, description, transaction_type, transaction_date, created_at, updated_at) "
                    + "SELECT id, account_id, tag_id, amount, description, transaction_type, transaction_date, created_at, updated_at "
                    + "FROM transactions_migrate");
            statement.execute("DROP TABLE transactions_migrate");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_transactions_account_id ON transactions(account_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_transactions_tag_id ON transactions(tag_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions(transaction_date)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_transactions_account_date ON transactions(account_id, transaction_date)");
        }
    }

    private static final String TRANSACTIONS_SCHEMA =
            "CREATE TABLE transactions ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "account_id INTEGER NOT NULL,"
            + "tag_id INTEGER,"
            + "to_account_id INTEGER,"
            + "transfer_id INTEGER,"
            + "rate TEXT,"
            + "amount INTEGER NOT NULL,"
            + "description TEXT,"
            + "transaction_type TEXT NOT NULL CHECK (transaction_type IN ('INCOME', 'EXPENSE', 'TRANSFER')),"
            + "transaction_date TEXT NOT NULL,"
            + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            + "updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            + "FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE ON UPDATE CASCADE,"
            + "FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE SET NULL ON UPDATE CASCADE,"
            + "FOREIGN KEY (to_account_id) REFERENCES accounts(id) ON DELETE CASCADE ON UPDATE CASCADE"
            + ")";

    public synchronized void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
