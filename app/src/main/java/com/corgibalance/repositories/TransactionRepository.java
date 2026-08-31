package com.corgibalance.repositories;

import com.corgibalance.models.Transaction;
import com.corgibalance.models.TransactionType;
import com.corgibalance.services.Database;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TransactionRepository implements CrudRepository<Transaction> {

    private static final String FIND_ALL_SQL =
            "SELECT id, account_id, tag_id, amount, description, transaction_type, transaction_date, to_account_id, transfer_id, rate, direction, created_at, updated_at FROM transactions ORDER BY transaction_date ASC, id ASC";
    private static final String FIND_LATEST_SQL =
            "SELECT * FROM (SELECT id, account_id, tag_id, amount, description, transaction_type, transaction_date, to_account_id, transfer_id, rate, direction, created_at, updated_at FROM transactions ORDER BY transaction_date DESC, id DESC LIMIT ?) ORDER BY transaction_date ASC, id ASC";
    private static final String FIND_BY_DESCRIPTION_LIKE_SQL =
            "SELECT id, account_id, tag_id, amount, description, transaction_type, transaction_date, to_account_id, transfer_id, rate, direction, created_at, updated_at FROM transactions WHERE description LIKE ? ORDER BY transaction_date DESC, id DESC LIMIT ?";
    private static final String FIND_BY_ID_SQL =
            "SELECT id, account_id, tag_id, amount, description, transaction_type, transaction_date, to_account_id, transfer_id, rate, direction, created_at, updated_at FROM transactions WHERE id = ?";
    private static final String FIND_LAST_INSERTED_SQL =
            "SELECT id, account_id, tag_id, amount, description, transaction_type, transaction_date, to_account_id, transfer_id, rate, direction, created_at, updated_at FROM transactions ORDER BY id DESC LIMIT 1";
    private static final String FIND_SIBLING_SQL =
            "SELECT id, account_id, tag_id, amount, description, transaction_type, transaction_date, to_account_id, transfer_id, rate, direction, created_at, updated_at FROM transactions WHERE transfer_id = ? AND id != ?";
    private static final String INSERT_SQL =
            "INSERT INTO transactions (account_id, tag_id, amount, description, transaction_type, transaction_date, to_account_id, transfer_id, rate, direction) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SQL =
            "UPDATE transactions SET account_id = ?, tag_id = ?, amount = ?, description = ?, transaction_type = ?, transaction_date = ?, to_account_id = ?, transfer_id = ?, rate = ?, direction = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
    private static final String DELETE_BY_ID_SQL =
            "DELETE FROM transactions WHERE id = ?";
    private static final String DELETE_TRANSFER_PAIR_SQL =
            "DELETE FROM transactions WHERE transfer_id = ?";
    private static final String UPDATE_TRANSFER_ID_SQL =
            "UPDATE transactions SET transfer_id = ? WHERE id = ?";
    private static final String BALANCE_OF_SQL =
            "SELECT COALESCE((SELECT initial_balance FROM accounts WHERE id = ?), 0) "
            + "+ COALESCE((SELECT SUM(CASE "
            + "WHEN transaction_type = 'EXPENSE' THEN -amount "
            + "WHEN transaction_type = 'TRANSFER' AND direction = 0 THEN -amount "
            + "ELSE amount END) FROM transactions WHERE account_id = ?), 0)";
    private static final String TARGET_MINOR_UNITS_SQL =
            "SELECT c.minor_unit FROM accounts a JOIN currencies c ON c.id = a.currency_id WHERE a.id = ?";
    private static final String SUM_BY_CURRENCY_SQL =
            "SELECT a.currency_id AS currency_id, SUM(t.amount) AS total "
            + "FROM transactions t JOIN accounts a ON a.id = t.account_id "
            + "WHERE t.transaction_type = ? AND substr(t.transaction_date, 1, 7) = ? "
            + "GROUP BY a.currency_id";
    private static final String SUM_BY_TAG_SQL =
            "SELECT a.currency_id AS currency_id, SUM(t.amount) AS total "
            + "FROM transactions t JOIN accounts a ON a.id = t.account_id "
            + "WHERE t.transaction_type = ? AND t.tag_id = ? "
            + "AND t.transaction_date >= ? AND t.transaction_date <= ? "
            + "GROUP BY a.currency_id";
    private static final String SUM_GROUP_BY_TAG_SQL =
            "SELECT t.tag_id AS tag_id, a.currency_id AS currency_id, SUM(t.amount) AS total "
            + "FROM transactions t JOIN accounts a ON a.id = t.account_id "
            + "WHERE t.transaction_type = ? AND substr(t.transaction_date, 1, 7) = ? "
            + "AND t.tag_id IS NOT NULL "
            + "GROUP BY t.tag_id, a.currency_id";
    private static final String AVAILABLE_YEARS_SQL =
            "SELECT DISTINCT substr(transaction_date, 1, 4) AS year FROM transactions ORDER BY year DESC";
    private static final String LATEST_YEAR_MONTH_SQL =
            "SELECT substr(transaction_date, 1, 7) AS year_month FROM transactions "
            + "ORDER BY transaction_date DESC LIMIT 1";

    private final Database database;

    public TransactionRepository() {
        this(Database.getInstance());
    }

    public List<Transaction> findAll() {
        List<Transaction> transactions = new ArrayList<>();
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(FIND_ALL_SQL);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transactions.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load transactions", e);
        }
        return transactions;
    }

    public Transaction findLastInserted() {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(FIND_LAST_INSERTED_SQL);
                 ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapRow(resultSet) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load last inserted transaction", e);
        }
    }

    public List<Transaction> findLatest(int limit) {
        List<Transaction> transactions = new ArrayList<>();
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(FIND_LATEST_SQL)) {
                statement.setInt(1, limit);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        transactions.add(mapRow(resultSet));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load latest transactions", e);
        }
        return transactions;
    }

    public List<Transaction> findByDescriptionLike(String query, int limit) {
        List<Transaction> transactions = new ArrayList<>();
        try (Connection connection = database.newConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_BY_DESCRIPTION_LIKE_SQL)) {
            statement.setString(1, "%" + query + "%");
            statement.setInt(2, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    transactions.add(mapRow(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search transactions by description", e);
        }
        return transactions;
    }

    public Map<Long, Long> sumByCurrency(TransactionType type, int year, int month) {
        Map<Long, Long> totals = new HashMap<>();
        String monthPrefix = String.format("%04d-%02d", year, month);
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(SUM_BY_CURRENCY_SQL)) {
                statement.setString(1, type.toString());
                statement.setString(2, monthPrefix);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        totals.put(resultSet.getLong("currency_id"), resultSet.getLong("total"));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load transaction sums", e);
        }
        return totals;
    }

    public Map<Long, Long> sumByCurrency(TransactionType type, long tagId, LocalDate startDate, LocalDate endDate) {
        Map<Long, Long> totals = new HashMap<>();
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(SUM_BY_TAG_SQL)) {
                statement.setString(1, type.toString());
                statement.setLong(2, tagId);
                statement.setString(3, startDate.toString());
                statement.setString(4, endDate.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        totals.put(resultSet.getLong("currency_id"), resultSet.getLong("total"));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load transaction sums", e);
        }
        return totals;
    }

    public Map<Long, Map<Long, Long>> sumByTag(TransactionType type, int year, int month) {
        Map<Long, Map<Long, Long>> totals = new HashMap<>();
        String monthPrefix = String.format("%04d-%02d", year, month);
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(SUM_GROUP_BY_TAG_SQL)) {
                statement.setString(1, type.toString());
                statement.setString(2, monthPrefix);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        long tagId = resultSet.getLong("tag_id");
                        long currencyId = resultSet.getLong("currency_id");
                        totals.computeIfAbsent(tagId, _ -> new HashMap<>())
                                .put(currencyId, resultSet.getLong("total"));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load transaction sums by tag", e);
        }
        return totals;
    }

    public List<Integer> availableYears() {
        List<Integer> years = new ArrayList<>();
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(AVAILABLE_YEARS_SQL);
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    years.add(resultSet.getInt("year"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load available years", e);
        }
        return years;
    }

    public String latestYearMonth() {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(LATEST_YEAR_MONTH_SQL);
                 ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString("year_month") : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load latest transaction month", e);
        }
    }

    public Transaction create(Transaction transaction) {
        try {
            Connection connection = database.getConnection();
            if (transaction.getTransactionType() == TransactionType.TRANSFER) {
                createTransferPair(connection, transaction);
            } else {
                transaction.setAmount(Math.abs(transaction.getAmount()));
                Map<Long, Long> deltas = new HashMap<>();
                addDelta(deltas, transaction.getAccountId(), signedAmount(transaction));
                validateDeltas(connection, deltas);
                insertRow(connection, transaction);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create transaction", e);
        }
        return transaction;
    }

    public Transaction createTransfer(long fromAccountId, long toAccountId, long amount,
                                      String description, LocalDate transactionDate, BigDecimal rate) {
        if (fromAccountId == toAccountId) {
            throw new IllegalArgumentException("Accounts must be different");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        Transaction source = new Transaction();
        source.setTransactionType(TransactionType.TRANSFER);
        source.setAccountId(fromAccountId);
        source.setToAccountId(toAccountId);
        source.setAmount(amount);
        source.setDirection(0);
        source.setRate(rate == null ? null : rate.toPlainString());
        source.setDescription(description);
        source.setTransactionDate(transactionDate);
        return create(source);
    }

    public void update(Transaction transaction) {
        try {
            Connection connection = database.getConnection();
            if (transaction.getTransferId() != null) {
                updateTransferPair(connection, transaction);
            } else {
                transaction.setAmount(Math.abs(transaction.getAmount()));
                updateRowChecked(connection, transaction);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update transaction", e);
        }
    }

    public void delete(Transaction transaction) {
        try {
            Connection connection = database.getConnection();
            Transaction existing = findById(connection, transaction.getId());
            if (existing == null) {
                return;
            }
            Map<Long, Long> deltas = new HashMap<>();
            addDelta(deltas, existing.getAccountId(), -signedAmount(existing));
            if (existing.getTransferId() != null) {
                Transaction sibling = findSibling(connection, existing);
                if (sibling != null) {
                    addDelta(deltas, sibling.getAccountId(), -signedAmount(sibling));
                }
            }
            validateDeltas(connection, deltas);
            if (existing.getTransferId() != null) {
                deleteTransferPair(connection, existing.getTransferId());
            } else {
                deleteById(connection, existing.getId());
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete transaction", e);
        }
    }

    private void updateRowChecked(Connection connection, Transaction transaction) throws SQLException {
        Transaction existing = findById(connection, transaction.getId());
        if (existing == null) {
            updateRow(connection, transaction);
            return;
        }
        Map<Long, Long> deltas = new HashMap<>();
        addDelta(deltas, existing.getAccountId(), -signedAmount(existing));
        addDelta(deltas, transaction.getAccountId(), signedAmount(transaction));
        validateDeltas(connection, deltas);
        updateRow(connection, transaction);
    }

    private void deleteTransferPair(Connection connection, long groupId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_TRANSFER_PAIR_SQL)) {
            statement.setLong(1, groupId);
            statement.executeUpdate();
        }
    }

    private void deleteById(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(DELETE_BY_ID_SQL)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    private Transaction findById(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_BY_ID_SQL)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapRow(resultSet) : null;
            }
        }
    }

    private void createTransferPair(Connection connection, Transaction source) throws SQLException {
        if (source.getToAccountId() == null) {
            throw new IllegalArgumentException("Transfer requires a destination account");
        }
        long targetAmount = convertAmount(source.getAmount(), source.getRate(),
                targetMinorUnits(source.getToAccountId()));
        Map<Long, Long> deltas = new HashMap<>();
        addDelta(deltas, source.getAccountId(), signedAmount(source));
        addDelta(deltas, source.getToAccountId(), targetAmount);
        validateDeltas(connection, deltas);

        long groupId = insertRow(connection, source);
        source.setTransferId(groupId);
        updateTransferId(connection, groupId, groupId);

        Transaction target = new Transaction();
        target.setTransactionType(TransactionType.TRANSFER);
        target.setAccountId(source.getToAccountId());
        target.setToAccountId(source.getAccountId());
        target.setAmount(targetAmount);
        target.setDirection(1);
        target.setRate(source.getRate());
        target.setDescription(source.getDescription());
        target.setTransactionDate(source.getTransactionDate());
        target.setTransferId(groupId);
        insertRow(connection, target);
    }

    private void updateTransferPair(Connection connection, Transaction transaction) throws SQLException {
        transaction.setTransactionType(TransactionType.TRANSFER);
        transaction.setAmount(Math.abs(transaction.getAmount()));
        Transaction existing = findById(connection, transaction.getId());
        if (existing == null) {
            updateRow(connection, transaction);
            return;
        }
        Transaction sibling = findSibling(connection, transaction);
        if (sibling == null) {
            updateRow(connection, transaction);
            return;
        }
        Transaction source = transaction.getDirection() == 0 ? transaction : sibling;
        Transaction target = source == transaction ? sibling : transaction;
        long targetAmount = target.getAccountId() == null
                ? source.getAmount()
                : convertAmount(source.getAmount(), source.getRate(),
                        targetMinorUnits(target.getAccountId()));

        Map<Long, Long> deltas = new HashMap<>();
        addDelta(deltas, existing.getAccountId(), -signedAmount(existing));
        addDelta(deltas, transaction.getAccountId(), signedAmount(transaction));
        addDelta(deltas, sibling.getAccountId(), -signedAmount(sibling));
        addDelta(deltas, target.getAccountId(), targetAmount);
        validateDeltas(connection, deltas);

        source.setDirection(0);
        source.setToAccountId(target.getAccountId());
        source.setDescription(transaction.getDescription());
        source.setRate(transaction.getRate());
        source.setTransactionDate(transaction.getTransactionDate());
        source.setTransactionType(TransactionType.TRANSFER);
        target.setDirection(1);
        target.setToAccountId(source.getAccountId());
        target.setAmount(targetAmount);
        target.setDescription(transaction.getDescription());
        target.setRate(transaction.getRate());
        target.setTransactionDate(transaction.getTransactionDate());
        target.setTransactionType(TransactionType.TRANSFER);
        updateRow(connection, source);
        updateRow(connection, target);
    }

    private Transaction findSibling(Connection connection, Transaction transaction) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(FIND_SIBLING_SQL)) {
            statement.setLong(1, transaction.getTransferId());
            statement.setLong(2, transaction.getId());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapRow(resultSet) : null;
            }
        }
    }

    private long insertRow(Connection connection, Transaction transaction) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, transaction.getAccountId());
            setNullableLong(statement, 2, transaction.getTagId());
            statement.setLong(3, transaction.getAmount());
            statement.setString(4, transaction.getDescription());
            statement.setString(5, transaction.getTransactionType().toString());
            statement.setString(6, toString(transaction.getTransactionDate()));
            setNullableLong(statement, 7, transaction.getToAccountId());
            setNullableLong(statement, 8, transaction.getTransferId());
            statement.setString(9, transaction.getRate());
            statement.setInt(10, transaction.getDirection());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                long id = keys.next() ? keys.getLong(1) : -1;
                transaction.setId(id);
                return id;
            }
        }
    }

    private void updateRow(Connection connection, Transaction transaction) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_SQL)) {
            statement.setLong(1, transaction.getAccountId());
            setNullableLong(statement, 2, transaction.getTagId());
            statement.setLong(3, transaction.getAmount());
            statement.setString(4, transaction.getDescription());
            statement.setString(5, transaction.getTransactionType().toString());
            statement.setString(6, toString(transaction.getTransactionDate()));
            setNullableLong(statement, 7, transaction.getToAccountId());
            setNullableLong(statement, 8, transaction.getTransferId());
            statement.setString(9, transaction.getRate());
            statement.setInt(10, transaction.getDirection());
            statement.setLong(11, transaction.getId());
            statement.executeUpdate();
        }
    }

    private void updateTransferId(Connection connection, long id, long transferId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPDATE_TRANSFER_ID_SQL)) {
            statement.setLong(1, transferId);
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }

    private Transaction mapRow(ResultSet resultSet) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.setId(resultSet.getLong("id"));
        transaction.setAccountId(getNullableLong(resultSet, "account_id"));
        transaction.setTagId(getNullableLong(resultSet, "tag_id"));
        transaction.setAmount(resultSet.getLong("amount"));
        transaction.setDescription(resultSet.getString("description"));
        transaction.setTransactionType(TransactionType.valueOf(resultSet.getString("transaction_type")));
        transaction.setTransactionDate(toLocalDate(resultSet.getString("transaction_date")));
        transaction.setToAccountId(getNullableLong(resultSet, "to_account_id"));
        transaction.setTransferId(getNullableLong(resultSet, "transfer_id"));
        transaction.setRate(resultSet.getString("rate"));
        transaction.setDirection(resultSet.getInt("direction"));
        transaction.setCreatedAt(toLocalDateTime(resultSet.getTimestamp("created_at")));
        transaction.setUpdatedAt(toLocalDateTime(resultSet.getTimestamp("updated_at")));
        return transaction;
    }

    private long balanceOf(Connection connection, long accountId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(BALANCE_OF_SQL)) {
            statement.setLong(1, accountId);
            statement.setLong(2, accountId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0;
            }
        }
    }

    private void validateDeltas(Connection connection, Map<Long, Long> deltas) throws SQLException {
        for (Map.Entry<Long, Long> entry : deltas.entrySet()) {
            if (balanceOf(connection, entry.getKey()) + entry.getValue() < 0) {
                throw new IllegalArgumentException(
                        "Insufficient funds: the account balance cannot go below zero");
            }
        }
    }

    private void addDelta(Map<Long, Long> deltas, Long accountId, long delta) {
        if (accountId == null) {
            return;
        }
        deltas.merge(accountId, delta, Long::sum);
    }

    private long signedAmount(Transaction transaction) {
        if (transaction.getTransactionType() == TransactionType.EXPENSE) {
            return -transaction.getAmount();
        }
        if (transaction.getTransactionType() == TransactionType.TRANSFER && transaction.getDirection() == 0) {
            return -transaction.getAmount();
        }
        return transaction.getAmount();
    }

    private long convertAmount(long sourceAmountMinor, String rateString, int targetMinorUnits) {
        if (rateString == null || targetMinorUnits <= 0) {
            return sourceAmountMinor;
        }
        BigDecimal rate = new BigDecimal(rateString);
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            return sourceAmountMinor;
        }
        return rate.multiply(new BigDecimal(sourceAmountMinor))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private int targetMinorUnits(long accountId) {
        try {
            Connection connection = database.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(TARGET_MINOR_UNITS_SQL)) {
                statement.setLong(1, accountId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? resultSet.getInt("minor_unit") : 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load account currency", e);
        }
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
