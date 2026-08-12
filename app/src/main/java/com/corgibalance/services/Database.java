package com.corgibalance.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public final class Database {

    private static final Logger logger = Logger.getLogger(Database.class.getName());

    private static final Database INSTANCE = new Database();

    private static final String COUNT_EMPTY_SQL =
            "SELECT COUNT(*) FROM (SELECT 1 FROM %s LIMIT 1)";
    private static final String ADD_RATE_COLUMN_SQL =
            "ALTER TABLE transactions ADD COLUMN rate TEXT";
    private static final String TRANSACTIONS_SCHEMA_SQL =
            "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'transactions'";
    private static final String TRANSACTIONS_COLUMNS_SQL =
            "PRAGMA table_info(transactions)";
    private static final String RENAME_TRANSACTIONS_SQL =
            "ALTER TABLE transactions RENAME TO transactions_migrate";
    private static final String MIGRATE_TRANSACTIONS_SQL =
            "INSERT INTO transactions (id, account_id, tag_id, amount, description, transaction_type, transaction_date, created_at, updated_at) "
            + "SELECT id, account_id, tag_id, amount, description, transaction_type, transaction_date, created_at, updated_at "
            + "FROM transactions_migrate";
    private static final String DROP_TRANSACTIONS_MIGRATE_SQL =
            "DROP TABLE transactions_migrate";
    private static final String CREATE_INDEX_ACCOUNT_ID_SQL =
            "CREATE INDEX IF NOT EXISTS idx_transactions_account_id ON transactions(account_id)";
    private static final String CREATE_INDEX_TAG_ID_SQL =
            "CREATE INDEX IF NOT EXISTS idx_transactions_tag_id ON transactions(tag_id)";
    private static final String CREATE_INDEX_DATE_SQL =
            "CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions(transaction_date)";
    private static final String CREATE_INDEX_ACCOUNT_DATE_SQL =
            "CREATE INDEX IF NOT EXISTS idx_transactions_account_date ON transactions(account_id, transaction_date)";
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
        Path dbPath = dbPath();
        logger.info("Connecting to database: " + dbPath.toAbsolutePath());
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        initDatabase();
    }

    public synchronized Path dbPath() throws SQLException {
        try {
            Path dbDir = Path.of(System.getProperty("user.home"), ".corgibalance");
            Files.createDirectories(dbDir);
            return dbDir.resolve("corgibalance.db");
        } catch (IOException e) {
            throw new SQLException("Failed to create database directory", e);
        }
    }

    public synchronized void exportTo(Path target) throws SQLException {
        getConnection();
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new SQLException("Failed to prepare export file", e);
        }
        String escapedPath = target.toString().replace("'", "''");
        try (Statement statement = connection.createStatement()) {
            statement.execute("VACUUM INTO '" + escapedPath + "'");
        }
    }

    public synchronized void importFrom(Path source) throws SQLException {
        validateSource(source);
        close();
        try {
            Path dbPath = dbPath();
            Files.copy(source, dbPath, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(dbPath.resolveSibling("corgibalance.db-wal"));
            Files.deleteIfExists(dbPath.resolveSibling("corgibalance.db-shm"));
            Files.deleteIfExists(dbPath.resolveSibling("corgibalance.db-journal"));
            connect();
        } catch (IOException e) {
            throw new SQLException("Failed to import database", e);
        }
    }

    private void validateSource(Path source) throws SQLException {
        if (!Files.isRegularFile(source)) {
            throw new SQLException("Selected file does not exist");
        }
        try (Connection sourceConnection = DriverManager.getConnection("jdbc:sqlite:" + source);
             Statement statement = sourceConnection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM sqlite_master")) {
            resultSet.next();
        } catch (SQLException e) {
            throw new SQLException("Selected file is not a valid database: " + source, e);
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
             ResultSet result = statement.executeQuery(String.format(COUNT_EMPTY_SQL, table))) {
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
                statement.execute(ADD_RATE_COLUMN_SQL);
            }
        }
    }

    private String transactionsSchemaSql() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(TRANSACTIONS_SCHEMA_SQL)) {
            return resultSet.next() ? resultSet.getString(1) : null;
        }
    }

    private Set<String> transactionColumnNames() throws SQLException {
        Set<String> columns = new HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(TRANSACTIONS_COLUMNS_SQL)) {
            while (resultSet.next()) {
                columns.add(resultSet.getString("name"));
            }
        }
        return columns;
    }

    private void rebuildTransactionsTable() throws SQLException {
        logger.info("Migrating transactions table schema to support transfers");
        try (Statement statement = connection.createStatement()) {
            statement.execute(RENAME_TRANSACTIONS_SQL);
            statement.execute(TRANSACTIONS_SCHEMA);
            statement.execute(MIGRATE_TRANSACTIONS_SQL);
            statement.execute(DROP_TRANSACTIONS_MIGRATE_SQL);
            statement.execute(CREATE_INDEX_ACCOUNT_ID_SQL);
            statement.execute(CREATE_INDEX_TAG_ID_SQL);
            statement.execute(CREATE_INDEX_DATE_SQL);
            statement.execute(CREATE_INDEX_ACCOUNT_DATE_SQL);
        }
    }

    public synchronized void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
