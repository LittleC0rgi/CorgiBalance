-- ============================================================
-- Default currencies
-- ============================================================


INSERT OR IGNORE INTO currencies (
    code,
    name,
    symbol,
    minor_unit
)
VALUES
    ('RUB', 'Russian Ruble', '₽', 2),
    ('USD', 'US Dollar', '$', 2),
    ('EUR', 'Euro', '€', 2);




-- ============================================================
-- Default tags
-- ============================================================


INSERT OR IGNORE INTO tags (
    name,
    color,
    icon
)
VALUES
    ('Food',          '#FF6B6B', 'utensils'),
    ('Transport',     '#4D96FF', 'car'),
    ('Entertainment', '#9B59B6', 'gamepad'),
    ('Shopping',      '#F39C12', 'shopping-bag'),
    ('Health',        '#2ECC71', 'heart'),
    ('Salary',        '#27AE60', 'wallet');
