package com.bankingsystem.persistence;

import com.bankingsystem.domain.*;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public class AccountDAO {
    private static final String ACCOUNT_SELECT = "SELECT a.account_number,a.account_type,a.balance,a.branch,a.branch_code,a.status,a.opened_on, " +
            "c.customer_id,c.first_name,c.surname,c.address,c.phone_number,c.email,c.employed,c.employer_name,c.employer_address " +
            "FROM accounts a JOIN customers c ON c.customer_id=a.customer_id ";

    public Account create(Account account) throws SQLException {
        if (account.getBalance().signum() != 0) {
            throw new IllegalArgumentException("New accounts must start at zero. Opening deposits require teller approval.");
        }
        return saveAccount(account, false);
    }

    public Account createSeeded(Account account) throws SQLException {
        return saveAccount(account, true);
    }

    private Account saveAccount(Account account, boolean recordInitialBalance) throws SQLException {
        String insertAccount = "INSERT INTO accounts(account_number,customer_id,account_type,balance,branch,branch_code,status,opened_on) VALUES(?,?,?,?,?,?,?,?)";
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(insertAccount)) {
                    statement.setString(1, account.getAccountNumber());
                    statement.setString(2, account.getOwner().getCustomerId());
                    statement.setString(3, account.getAccountType().name());
                    statement.setBigDecimal(4, account.getBalance());
                    statement.setString(5, account.getBranch());
                    statement.setString(6, account.getBranchCode());
                    statement.setString(7, account.getStatus().name());
                    statement.setString(8, account.getOpenedOn().toString());
                    statement.executeUpdate();
                }
                if (recordInitialBalance && account.getBalance().signum() > 0) {
                    BankTransaction transaction = new BankTransaction(UUID.randomUUID().toString(), account.getAccountNumber(),
                            TransactionType.OPENING_DEPOSIT, account.getBalance(), LocalDateTime.now(),
                            "OPEN-" + account.getAccountNumber(), account.getBalance());
                    insertTransaction(connection, transaction);
                }
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                rollback(connection, ex);
                throw ex;
            }
        }
        return account;
    }

    public Optional<Account> findByNumber(String accountNumber) throws SQLException {
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(ACCOUNT_SELECT + "WHERE a.account_number=?")) {
            p.setString(1, accountNumber);
            try (ResultSet rs = p.executeQuery()) { return rs.next() ? Optional.of(mapAccount(rs)) : Optional.empty(); }
        }
    }

    public List<Account> findByCustomer(String customerId) throws SQLException {
        List<Account> accounts = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(ACCOUNT_SELECT + "WHERE a.customer_id=? ORDER BY a.opened_on,a.account_number")) {
            p.setString(1, customerId);
            try (ResultSet rs = p.executeQuery()) { while (rs.next()) accounts.add(mapAccount(rs)); }
        }
        return accounts;
    }

    public List<Account> findInterestBearingAccounts() throws SQLException {
        List<Account> accounts = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(ACCOUNT_SELECT +
                "WHERE a.status='ACTIVE' AND a.account_type IN ('SAVINGS','INVESTMENT') ORDER BY a.account_number")) {
            try (ResultSet rs = p.executeQuery()) { while (rs.next()) accounts.add(mapAccount(rs)); }
        }
        return accounts;
    }

    public void commitBalanceChange(String accountNumber, BigDecimal expectedOldBalance,
                                    BigDecimal newBalance, BankTransaction transaction) throws SQLException {
        String update = "UPDATE accounts SET balance=? WHERE account_number=? AND balance=? AND status='ACTIVE'";
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement p = c.prepareStatement(update)) {
                    p.setBigDecimal(1, newBalance); p.setString(2, accountNumber); p.setBigDecimal(3, expectedOldBalance);
                    if (p.executeUpdate() != 1) throw new SQLException("Account balance changed or account is not active. Refresh and retry.");
                }
                insertTransaction(c, transaction);
                c.commit();
            } catch (SQLException | RuntimeException ex) {
                rollback(c, ex);
                throw ex;
            }
        }
    }

    private static void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }

    public long count() throws SQLException {
        try (Connection c = Database.getConnection(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM accounts")) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    public static void insertTransaction(Connection connection, BankTransaction tx) throws SQLException {
        String sql = "INSERT INTO transactions(transaction_id,account_number,transaction_type,amount,occurred_at,reference,balance_after) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement p = connection.prepareStatement(sql)) {
            p.setString(1, tx.transactionId()); p.setString(2, tx.accountNumber()); p.setString(3, tx.type().name());
            p.setBigDecimal(4, tx.amount()); p.setString(5, tx.occurredAt().toString()); p.setString(6, tx.reference());
            p.setBigDecimal(7, tx.balanceAfter()); p.executeUpdate();
        }
    }

    private static Account mapAccount(ResultSet rs) throws SQLException {
        Customer customer = CustomerDAO.map(rs);
        String number = rs.getString("account_number");
        BigDecimal balance = rs.getBigDecimal("balance");
        String branch = rs.getString("branch_code");
        AccountStatus status = AccountStatus.valueOf(rs.getString("status"));
        LocalDate opened = LocalDate.parse(rs.getString("opened_on"));
        return switch (AccountType.valueOf(rs.getString("account_type"))) {
            case SAVINGS -> new SavingsAccount(number, customer, balance, branch, status, opened);
            case INVESTMENT -> new InvestmentAccount(number, customer, balance, branch, status, opened);
            case CHEQUE -> new ChequeAccount(number, customer, balance, branch, status, opened);
        };
    }
}
