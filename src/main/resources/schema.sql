CREATE TABLE IF NOT EXISTS customers (
    customer_id TEXT PRIMARY KEY,
    first_name TEXT NOT NULL,
    surname TEXT NOT NULL,
    address TEXT NOT NULL,
    phone_number TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    employed INTEGER NOT NULL CHECK (employed IN (0,1)),
    employer_name TEXT,
    employer_address TEXT,
    created_at TEXT NOT NULL,
    CHECK (employed = 0 OR (employer_name IS NOT NULL AND employer_address IS NOT NULL))
);

CREATE TABLE IF NOT EXISTS user_credentials (
    user_id TEXT PRIMARY KEY,
    username TEXT NOT NULL UNIQUE COLLATE NOCASE,
    password_hash TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('CUSTOMER','TELLER','MANAGER')),
    customer_id TEXT UNIQUE,
    active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0,1)),
    must_change_password INTEGER NOT NULL DEFAULT 0 CHECK (must_change_password IN (0,1)),
    created_at TEXT NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    CHECK ((role = 'CUSTOMER' AND customer_id IS NOT NULL) OR (role IN ('TELLER','MANAGER') AND customer_id IS NULL))
);

CREATE TABLE IF NOT EXISTS branches (
    branch_code TEXT PRIMARY KEY,
    branch_name TEXT NOT NULL UNIQUE
);

INSERT OR IGNORE INTO branches(branch_code, branch_name) VALUES ('FRT', 'Francistown');
INSERT OR IGNORE INTO branches(branch_code, branch_name) VALUES ('PAL', 'Palapye');
INSERT OR IGNORE INTO branches(branch_code, branch_name) VALUES ('GAB', 'Gaborone');

CREATE TABLE IF NOT EXISTS accounts (
    account_number TEXT PRIMARY KEY,
    customer_id TEXT NOT NULL,
    account_type TEXT NOT NULL CHECK (account_type IN ('SAVINGS','INVESTMENT','CHEQUE')),
    balance NUMERIC NOT NULL DEFAULT 0 CHECK (balance >= 0),
    branch TEXT NOT NULL,
    branch_code TEXT NOT NULL DEFAULT 'FRT',
    status TEXT NOT NULL CHECK (status IN ('ACTIVE','FROZEN','CLOSED')),
    opened_on TEXT NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    FOREIGN KEY (branch_code) REFERENCES branches(branch_code)
);

CREATE INDEX IF NOT EXISTS idx_accounts_customer ON accounts(customer_id);

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id TEXT PRIMARY KEY,
    account_number TEXT NOT NULL,
    transaction_type TEXT NOT NULL CHECK (transaction_type IN ('OPENING_DEPOSIT','DEPOSIT','WITHDRAWAL','TRANSFER_OUT','TRANSFER_IN','INTEREST_CREDIT')),
    amount NUMERIC NOT NULL CHECK (amount > 0),
    occurred_at TEXT NOT NULL,
    reference TEXT NOT NULL UNIQUE,
    balance_after NUMERIC NOT NULL CHECK (balance_after >= 0),
    FOREIGN KEY (account_number) REFERENCES accounts(account_number)
);

CREATE INDEX IF NOT EXISTS idx_transactions_account_date ON transactions(account_number, occurred_at DESC);

CREATE TABLE IF NOT EXISTS transaction_requests (
    request_id TEXT PRIMARY KEY,
    account_number TEXT NOT NULL,
    destination_account_number TEXT,
    transaction_type TEXT NOT NULL CHECK (transaction_type IN ('OPENING_DEPOSIT','DEPOSIT','WITHDRAWAL','TRANSFER')),
    amount NUMERIC NOT NULL CHECK (amount > 0),
    submitted_at TEXT NOT NULL,
    reference TEXT NOT NULL UNIQUE,
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    submitted_by_user_id TEXT NOT NULL,
    reviewed_by_user_id TEXT,
    reviewed_at TEXT,
    review_note TEXT,
    FOREIGN KEY (account_number) REFERENCES accounts(account_number),
    FOREIGN KEY (destination_account_number) REFERENCES accounts(account_number),
    FOREIGN KEY (submitted_by_user_id) REFERENCES user_credentials(user_id),
    FOREIGN KEY (reviewed_by_user_id) REFERENCES user_credentials(user_id),
    CHECK ((transaction_type = 'TRANSFER' AND destination_account_number IS NOT NULL) OR
           (transaction_type <> 'TRANSFER' AND destination_account_number IS NULL))
);

CREATE INDEX IF NOT EXISTS idx_transaction_requests_status_date ON transaction_requests(status, submitted_at);
CREATE INDEX IF NOT EXISTS idx_transaction_requests_submitter ON transaction_requests(submitted_by_user_id, submitted_at DESC);
