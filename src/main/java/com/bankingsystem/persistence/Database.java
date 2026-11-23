package com.bankingsystem.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Scanner;

public final class Database {
    private static final String DB_PATH = System.getProperty("banking.db.path", System.getenv().getOrDefault("BANKING_DB_PATH", "data/banking-system.db"));
    private static final String URL = "jdbc:sqlite:" + DB_PATH;

    private Database() { }

    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    public static void initialize() throws SQLException, IOException {
        Path databaseFile = Path.of(DB_PATH);
        if (databaseFile.getParent() != null) Files.createDirectories(databaseFile.getParent());
        try (InputStream stream = Database.class.getResourceAsStream("/schema.sql")) {
            if (stream == null) throw new IOException("schema.sql was not found on the application classpath.");
            String sql = new Scanner(stream, java.nio.charset.StandardCharsets.UTF_8).useDelimiter("\\A").next();
            try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
                for (String command : sql.split(";")) {
                    if (!command.isBlank()) statement.execute(command.trim());
                }
            }
        }
        migrateUserCredentials();
        migrateAccountBranches();
        migrateTransactionTypes();
    }

    private static void migrateUserCredentials() throws SQLException {
        try (Connection connection = getConnection()) {
            String definition;
            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT sql FROM sqlite_master WHERE type='table' AND name='user_credentials'")) {
                try (ResultSet rs = query.executeQuery()) {
                    if (!rs.next()) return;
                    definition = rs.getString(1);
                }
            }
            boolean hasManagerRole = definition != null && definition.contains("'MANAGER'");
            boolean hasPasswordFlag = hasColumn(connection, "user_credentials", "must_change_password");
            if (hasManagerRole && hasPasswordFlag) return;
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = OFF");
            }
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP TABLE IF EXISTS user_credentials_migration");
                statement.execute("CREATE TABLE user_credentials_migration (" +
                        "user_id TEXT PRIMARY KEY," +
                        "username TEXT NOT NULL UNIQUE COLLATE NOCASE," +
                        "password_hash TEXT NOT NULL," +
                        "role TEXT NOT NULL CHECK (role IN ('CUSTOMER','TELLER','MANAGER'))," +
                        "customer_id TEXT UNIQUE," +
                        "active INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0,1))," +
                        "must_change_password INTEGER NOT NULL DEFAULT 0 CHECK (must_change_password IN (0,1))," +
                        "created_at TEXT NOT NULL," +
                        "FOREIGN KEY (customer_id) REFERENCES customers(customer_id)," +
                        "CHECK ((role = 'CUSTOMER' AND customer_id IS NOT NULL) OR (role IN ('TELLER','MANAGER') AND customer_id IS NULL))" +
                        ")");
                String passwordFlag = hasPasswordFlag ? "must_change_password" : "0";
                statement.execute("INSERT INTO user_credentials_migration " +
                        "(user_id,username,password_hash,role,customer_id,active,must_change_password,created_at) " +
                        "SELECT user_id,username,password_hash,role,customer_id,active," + passwordFlag + ",created_at FROM user_credentials");
                statement.execute("DROP TABLE user_credentials");
                statement.execute("ALTER TABLE user_credentials_migration RENAME TO user_credentials");
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
                try (Statement statement = connection.createStatement()) {
                    statement.execute("PRAGMA foreign_keys = ON");
                }
            }
        }
    }

    private static void migrateAccountBranches() throws SQLException {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            if (!hasColumn(connection, "accounts", "branch_code")) {
                statement.execute("ALTER TABLE accounts ADD COLUMN branch_code TEXT NOT NULL DEFAULT 'FRT'");
            }
            statement.execute("UPDATE accounts SET branch_code = CASE " +
                    "WHEN lower(branch) IN ('palapye','pal') THEN 'PAL' " +
                    "WHEN lower(branch) IN ('gaborone','gab') THEN 'GAB' " +
                    "ELSE 'FRT' END");
        }
    }

    private static void migrateTransactionTypes() throws SQLException {
        try (Connection connection = getConnection()) {
            String definition;
            try (PreparedStatement query = connection.prepareStatement(
                    "SELECT sql FROM sqlite_master WHERE type='table' AND name='transactions'")) {
                try (ResultSet rs = query.executeQuery()) {
                    if (!rs.next()) return;
                    definition = rs.getString(1);
                }
            }
            if (definition != null && definition.contains("TRANSFER_OUT") && definition.contains("TRANSFER_IN")) return;
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = OFF");
            }
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP TABLE IF EXISTS transactions_migration");
                statement.execute("CREATE TABLE transactions_migration (" +
                        "transaction_id TEXT PRIMARY KEY," +
                        "account_number TEXT NOT NULL," +
                        "transaction_type TEXT NOT NULL CHECK (transaction_type IN ('OPENING_DEPOSIT','DEPOSIT','WITHDRAWAL','TRANSFER_OUT','TRANSFER_IN','INTEREST_CREDIT'))," +
                        "amount NUMERIC NOT NULL CHECK (amount > 0)," +
                        "occurred_at TEXT NOT NULL," +
                        "reference TEXT NOT NULL UNIQUE," +
                        "balance_after NUMERIC NOT NULL CHECK (balance_after >= 0)," +
                        "FOREIGN KEY (account_number) REFERENCES accounts(account_number))");
                statement.execute("INSERT INTO transactions_migration " +
                        "(transaction_id,account_number,transaction_type,amount,occurred_at,reference,balance_after) " +
                        "SELECT transaction_id,account_number,transaction_type,amount,occurred_at,reference,balance_after FROM transactions");
                statement.execute("DROP TABLE transactions");
                statement.execute("ALTER TABLE transactions_migration RENAME TO transactions");
                statement.execute("CREATE INDEX IF NOT EXISTS idx_transactions_account_date ON transactions(account_number, occurred_at DESC)");
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
                try (Statement statement = connection.createStatement()) {
                    statement.execute("PRAGMA foreign_keys = ON");
                }
            }
        }
    }

    private static boolean hasColumn(Connection connection, String table, String column) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet columns = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("name"))) return true;
            }
        }
        return false;
    }
}
