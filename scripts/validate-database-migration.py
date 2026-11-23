import sqlite3

connection = sqlite3.connect(":memory:")
connection.execute("PRAGMA foreign_keys = ON")
connection.execute("""CREATE TABLE customers (
    customer_id TEXT PRIMARY KEY, first_name TEXT NOT NULL, surname TEXT NOT NULL,
    address TEXT NOT NULL, phone_number TEXT NOT NULL, email TEXT NOT NULL UNIQUE,
    employed INTEGER NOT NULL, employer_name TEXT, employer_address TEXT, created_at TEXT NOT NULL
)""")
connection.execute("""CREATE TABLE user_credentials (
    user_id TEXT PRIMARY KEY, username TEXT NOT NULL UNIQUE COLLATE NOCASE,
    password_hash TEXT NOT NULL, role TEXT NOT NULL CHECK (role IN ('CUSTOMER','TELLER')),
    customer_id TEXT UNIQUE, active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0,1)),
    created_at TEXT NOT NULL, FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    CHECK ((role = 'CUSTOMER' AND customer_id IS NOT NULL) OR (role = 'TELLER' AND customer_id IS NULL))
)""")
connection.execute("""CREATE TABLE accounts (
    account_number TEXT PRIMARY KEY, customer_id TEXT NOT NULL,
    account_type TEXT NOT NULL CHECK (account_type IN ('SAVINGS','INVESTMENT','CHEQUE')),
    balance NUMERIC NOT NULL DEFAULT 0 CHECK (balance >= 0), branch TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('ACTIVE','FROZEN','CLOSED')), opened_on TEXT NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
)""")
connection.execute("""CREATE TABLE transactions (
    transaction_id TEXT PRIMARY KEY, account_number TEXT NOT NULL,
    transaction_type TEXT NOT NULL CHECK (transaction_type IN ('OPENING_DEPOSIT','DEPOSIT','WITHDRAWAL','INTEREST_CREDIT')),
    amount NUMERIC NOT NULL CHECK (amount > 0), occurred_at TEXT NOT NULL,
    reference TEXT NOT NULL UNIQUE, balance_after NUMERIC NOT NULL CHECK (balance_after >= 0),
    FOREIGN KEY (account_number) REFERENCES accounts(account_number)
)""")
connection.execute("INSERT INTO customers VALUES(?,?,?,?,?,?,?,?,?,?)",
                   ("C1", "Test", "Customer", "Address", "71234567", "c@example.test", 0, None, None, "2026-01-01"))
connection.execute("INSERT INTO user_credentials VALUES(?,?,?,?,?,?,?)",
                   ("U1", "oldcustomer", "existing-hash", "CUSTOMER", "C1", 1, "2026-01-01"))
connection.execute("INSERT INTO user_credentials VALUES(?,?,?,?,?,?,?)",
                   ("U2", "oldteller", "teller-hash", "TELLER", None, 0, "2026-01-02"))
connection.execute("INSERT INTO accounts VALUES(?,?,?,?,?,?,?)",
                   ("A1", "C1", "CHEQUE", 100, "Palapye", "ACTIVE", "2026-01-01"))
connection.execute("INSERT INTO transactions VALUES(?,?,?,?,?,?,?)",
                   ("T1", "A1", "OPENING_DEPOSIT", 100, "2026-01-01T10:00:00", "OPEN-A1", 100))
connection.commit()

connection.execute("PRAGMA foreign_keys = OFF")
connection.execute("BEGIN")
connection.execute("""CREATE TABLE user_credentials_migration (
    user_id TEXT PRIMARY KEY, username TEXT NOT NULL UNIQUE COLLATE NOCASE,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('CUSTOMER','TELLER','MANAGER')),
    customer_id TEXT UNIQUE, active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0,1)),
    must_change_password INTEGER NOT NULL DEFAULT 0 CHECK (must_change_password IN (0,1)),
    created_at TEXT NOT NULL, FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    CHECK ((role = 'CUSTOMER' AND customer_id IS NOT NULL) OR (role IN ('TELLER','MANAGER') AND customer_id IS NULL))
)""")
connection.execute("""INSERT INTO user_credentials_migration
    (user_id,username,password_hash,role,customer_id,active,must_change_password,created_at)
    SELECT user_id,username,password_hash,role,customer_id,active,0,created_at FROM user_credentials""")
connection.execute("DROP TABLE user_credentials")
connection.execute("ALTER TABLE user_credentials_migration RENAME TO user_credentials")
connection.commit()
connection.execute("PRAGMA foreign_keys = ON")

rows = connection.execute("SELECT username, role, active, must_change_password FROM user_credentials ORDER BY user_id").fetchall()
assert rows == [("oldcustomer", "CUSTOMER", 1, 0), ("oldteller", "TELLER", 0, 0)], rows
connection.execute("INSERT INTO user_credentials VALUES(?,?,?,?,?,?,?,?)",
                   ("U3", "newmanager", "manager-hash", "MANAGER", None, 1, 1, "2026-01-03"))
assert connection.execute("PRAGMA foreign_key_check").fetchall() == []
print("PASS: existing customer and teller logins are preserved during credential migration")

connection.execute("ALTER TABLE accounts ADD COLUMN branch_code TEXT NOT NULL DEFAULT 'FRT'")
connection.execute("UPDATE accounts SET branch_code = CASE WHEN lower(branch) IN ('palapye','pal') THEN 'PAL' WHEN lower(branch) IN ('gaborone','gab') THEN 'GAB' ELSE 'FRT' END")
assert connection.execute("SELECT branch_code FROM accounts WHERE account_number='A1'").fetchone()[0] == "PAL"
print("PASS: existing accounts receive the correct branch code")
connection.commit()

connection.execute("PRAGMA foreign_keys = OFF")
connection.execute("BEGIN")
connection.execute("""CREATE TABLE transactions_migration (
    transaction_id TEXT PRIMARY KEY, account_number TEXT NOT NULL,
    transaction_type TEXT NOT NULL CHECK (transaction_type IN ('OPENING_DEPOSIT','DEPOSIT','WITHDRAWAL','TRANSFER_OUT','TRANSFER_IN','INTEREST_CREDIT')),
    amount NUMERIC NOT NULL CHECK (amount > 0), occurred_at TEXT NOT NULL,
    reference TEXT NOT NULL UNIQUE, balance_after NUMERIC NOT NULL CHECK (balance_after >= 0),
    FOREIGN KEY (account_number) REFERENCES accounts(account_number))""")
connection.execute("""INSERT INTO transactions_migration
    (transaction_id,account_number,transaction_type,amount,occurred_at,reference,balance_after)
    SELECT transaction_id,account_number,transaction_type,amount,occurred_at,reference,balance_after FROM transactions""")
connection.execute("DROP TABLE transactions")
connection.execute("ALTER TABLE transactions_migration RENAME TO transactions")
connection.commit()
connection.execute("PRAGMA foreign_keys = ON")
assert connection.execute("SELECT transaction_type FROM transactions WHERE transaction_id='T1'").fetchone()[0] == "OPENING_DEPOSIT"
connection.execute("INSERT INTO transactions VALUES(?,?,?,?,?,?,?)",
                   ("T2", "A1", "TRANSFER_OUT", 10, "2026-01-02T10:00:00", "REQ-1-OUT", 90))
assert connection.execute("PRAGMA foreign_key_check").fetchall() == []
print("PASS: old transaction history is retained and transfer ledger types are enabled")
print("PASS: upgraded customer, teller, manager, account and transaction foreign keys remain valid")
print("RESULT: 4 database migration checks passed")
