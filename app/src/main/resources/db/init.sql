PRAGMA foreign_keys = ON;


-- ============================================================
-- C-BALANCE
-- SQLite database initialization
--
-- Money:
--   INTEGER = minor currency units
--   100.50 RUB -> 10050
--   999.99 USD -> 99999
--
-- Exchange rates:
--   TEXT -> parsed as Java BigDecimal
--   Example: '91.5234'
-- ============================================================


-- ============================================================
-- Currencies
-- ============================================================


CREATE TABLE IF NOT EXISTS currencies (
    id INTEGER PRIMARY KEY AUTOINCREMENT,


    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    symbol TEXT NOT NULL,


    -- Number of decimal places in the currency.
    -- RUB/USD/EUR = 2
    -- JPY = 0
    minor_unit INTEGER NOT NULL DEFAULT 2
        CHECK (minor_unit >= 0 AND minor_unit <= 8),


    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);




-- ============================================================
-- Tags
-- ============================================================


CREATE TABLE IF NOT EXISTS tags (
    id INTEGER PRIMARY KEY AUTOINCREMENT,


    name TEXT NOT NULL UNIQUE,


    -- UI properties
    color TEXT,
    icon TEXT,


    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);




-- ============================================================
-- Account folders
-- ============================================================


CREATE TABLE IF NOT EXISTS account_folders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,


    name TEXT NOT NULL UNIQUE,


    -- Whether the folder is expanded (accounts visible) in the UI.
    is_expanded INTEGER NOT NULL DEFAULT 1,


    -- Parent folder for nesting. NULL = root-level folder.
    parent_id INTEGER,


    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (parent_id)
        REFERENCES account_folders(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);




-- ============================================================
-- Accounts
-- ============================================================


CREATE TABLE IF NOT EXISTS accounts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,


    name TEXT NOT NULL,


    -- Stored in the smallest currency unit.
    --
    -- Example:
    -- 1000.50 RUB -> 100050
    initial_balance INTEGER NOT NULL DEFAULT 0,


    currency_id INTEGER NOT NULL,


    folder_id INTEGER,


    -- Whether the account is hidden from the Overview.
    is_hidden INTEGER NOT NULL DEFAULT 0
        CHECK (is_hidden IN (0, 1)),


    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,


    FOREIGN KEY (currency_id)
        REFERENCES currencies(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,


    FOREIGN KEY (folder_id)
        REFERENCES account_folders(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);




-- ============================================================
-- Transactions
-- ============================================================


CREATE TABLE IF NOT EXISTS transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,


    account_id INTEGER NOT NULL,
    tag_id INTEGER,


    -- For TRANSFER transactions the account the money moved to.
    to_account_id INTEGER,


    -- Groups the two rows of a transfer (minus on source, plus on target).
    transfer_id INTEGER,


    -- Exchange rate used for a cross-currency transfer:
    -- target_amount_minor = source_amount_minor * rate, each amount in its own
    -- currency's minor units. Null means 1:1 (same-currency transfer).
    rate TEXT,


    -- Direction for TRANSFER rows (ignored for INCOME/EXPENSE):
    -- 0 = money leaves account_id (transfer sender)
    -- 1 = money arrives at account_id (transfer receiver)
    direction INTEGER NOT NULL DEFAULT 1
        CHECK (direction IN (0, 1)),


    -- Stored in minor currency units, always positive.
    -- The sign lives in transaction_type (EXPENSE) / direction (TRANSFER).
    --
    -- Examples:
    -- 500000 = 5000.00
    -- 129900 = 1299.00
    amount INTEGER NOT NULL CHECK (amount >= 0),


    description TEXT,


    transaction_type TEXT NOT NULL
        CHECK (
            transaction_type IN (
                'INCOME',
                'EXPENSE',
                'TRANSFER'
            )
        ),


    transaction_date TEXT NOT NULL,


    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,


    FOREIGN KEY (account_id)
        REFERENCES accounts(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,


    FOREIGN KEY (tag_id)
        REFERENCES tags(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,


    FOREIGN KEY (to_account_id)
        REFERENCES accounts(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);




-- ============================================================
-- Transfers
-- ============================================================


CREATE TABLE IF NOT EXISTS transfers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,


    from_account_id INTEGER NOT NULL,
    to_account_id INTEGER NOT NULL,


    -- Stored in minor currency units.
    amount INTEGER NOT NULL
        CHECK (amount > 0),


    description TEXT,


    transfer_date TEXT NOT NULL,


    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,


    CHECK (
        from_account_id != to_account_id
    ),


    FOREIGN KEY (from_account_id)
        REFERENCES accounts(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,


    FOREIGN KEY (to_account_id)
        REFERENCES accounts(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);




-- ============================================================
-- Recurring transactions
-- ============================================================


CREATE TABLE IF NOT EXISTS recurring_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,


    account_id INTEGER NOT NULL,
    tag_id INTEGER,


    -- Stored in minor currency units.
    amount INTEGER NOT NULL,


    description TEXT,


    transaction_type TEXT NOT NULL
        CHECK (
            transaction_type IN (
                'INCOME',
                'EXPENSE'
            )
        ),


    start_date TEXT NOT NULL,
    next_date TEXT NOT NULL,
    end_date TEXT,


    interval TEXT NOT NULL
        CHECK (
            interval IN (
                'DAILY',
                'WEEKLY',
                'MONTHLY',
                'YEARLY'
            )
        ),


    active INTEGER NOT NULL DEFAULT 1
        CHECK (active IN (0, 1)),


    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,


    CHECK (
        end_date IS NULL
        OR end_date >= start_date
    ),


    FOREIGN KEY (account_id)
        REFERENCES accounts(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,


    FOREIGN KEY (tag_id)
        REFERENCES tags(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);




-- ============================================================
-- Planned transactions
-- ============================================================


CREATE TABLE IF NOT EXISTS planned_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,


    account_id INTEGER NOT NULL,
    tag_id INTEGER,


    -- Stored in minor currency units.
    amount INTEGER NOT NULL CHECK (amount >= 0),


    description TEXT,


    transaction_type TEXT NOT NULL
        CHECK (
            transaction_type IN (
                'INCOME',
                'EXPENSE'
            )
        ),


    planned_date TEXT NOT NULL,


    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,


    FOREIGN KEY (account_id)
        REFERENCES accounts(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,


    FOREIGN KEY (tag_id)
        REFERENCES tags(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);




-- ============================================================
-- Budgets
-- ============================================================


CREATE TABLE IF NOT EXISTS budgets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,


    name TEXT NOT NULL,


    tag_id INTEGER NOT NULL,


    -- Stored in minor currency units.
    planned_amount INTEGER NOT NULL
        CHECK (planned_amount >= 0),


    start_date TEXT NOT NULL,
    end_date TEXT NOT NULL,


    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,


    CHECK (
        end_date >= start_date
    ),


    FOREIGN KEY (tag_id)
        REFERENCES tags(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);




-- ============================================================
-- Exchange rates
-- ============================================================


CREATE TABLE IF NOT EXISTS exchange_rates (
    id INTEGER PRIMARY KEY AUTOINCREMENT,


    from_currency_id INTEGER NOT NULL,
    to_currency_id INTEGER NOT NULL,


    -- Stored as TEXT to preserve exact decimal representation.
    --
    -- Example:
    -- '91.5234'
    --
    -- Java:
    -- new BigDecimal(rate)
    rate TEXT NOT NULL,


    rate_date TEXT NOT NULL,


    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,


    CHECK (
        from_currency_id != to_currency_id
    ),


    CHECK (
        length(rate) > 0
    ),


    UNIQUE (
        from_currency_id,
        to_currency_id,
        rate_date
    ),


    FOREIGN KEY (from_currency_id)
        REFERENCES currencies(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,


    FOREIGN KEY (to_currency_id)
        REFERENCES currencies(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);




-- ============================================================
-- Settings
-- ============================================================


CREATE TABLE IF NOT EXISTS settings (
    key TEXT PRIMARY KEY,


    value TEXT NOT NULL,


    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);




-- ============================================================
-- Indexes
-- ============================================================


CREATE INDEX IF NOT EXISTS idx_transactions_account_id
    ON transactions(account_id);


CREATE INDEX IF NOT EXISTS idx_transactions_tag_id
    ON transactions(tag_id);


CREATE INDEX IF NOT EXISTS idx_transactions_date
    ON transactions(transaction_date);


CREATE INDEX IF NOT EXISTS idx_transactions_account_date
    ON transactions(
        account_id,
        transaction_date
    );




CREATE INDEX IF NOT EXISTS idx_recurring_transactions_next_date
    ON recurring_transactions(next_date);


CREATE INDEX IF NOT EXISTS idx_recurring_transactions_account_id
    ON recurring_transactions(account_id);


CREATE INDEX IF NOT EXISTS idx_planned_transactions_date
    ON planned_transactions(planned_date);




CREATE INDEX IF NOT EXISTS idx_budgets_tag_id
    ON budgets(tag_id);


CREATE INDEX IF NOT EXISTS idx_budgets_dates
    ON budgets(
        start_date,
        end_date
    );




CREATE INDEX IF NOT EXISTS idx_exchange_rates_date
    ON exchange_rates(rate_date);


CREATE INDEX IF NOT EXISTS idx_exchange_rates_currencies
    ON exchange_rates(
        from_currency_id,
        to_currency_id,
        rate_date
    );


