package com.bankingsystem.persistence;

import com.bankingsystem.domain.BankTransaction;
import com.bankingsystem.domain.TransactionRequest;
import com.bankingsystem.domain.TransactionRequestType;
import com.bankingsystem.domain.TransactionStatus;
import com.bankingsystem.domain.TransactionType;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TransactionDAO {
    public TransactionRequest createRequest(String accountNumber, String destinationAccountNumber,
                                            TransactionRequestType type, BigDecimal amount,
                                            String submittedByUserId) throws SQLException {
        BigDecimal value = amount.setScale(2, java.math.RoundingMode.HALF_UP);
        if (value.signum() <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");
        if (type == TransactionRequestType.TRANSFER && (destinationAccountNumber == null || destinationAccountNumber.isBlank())) {
            throw new IllegalArgumentException("A destination account is required for a transfer.");
        }
        if (type != TransactionRequestType.TRANSFER && destinationAccountNumber != null) {
            throw new IllegalArgumentException("A destination account is only used for transfers.");
        }
        verifyActiveSubmitter(submittedByUserId);
        String requestId = UUID.randomUUID().toString();
        String reference = "REQ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase();
        LocalDateTime submittedAt = LocalDateTime.now();
        String sql = "INSERT INTO transaction_requests(request_id,account_number,destination_account_number,transaction_type,amount,submitted_at,reference,status,submitted_by_user_id) VALUES(?,?,?,?,?,?,?,?,?)";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, requestId);
            statement.setString(2, accountNumber);
            statement.setString(3, destinationAccountNumber);
            statement.setString(4, type.name());
            statement.setBigDecimal(5, value);
            statement.setString(6, submittedAt.toString());
            statement.setString(7, reference);
            statement.setString(8, TransactionStatus.PENDING.name());
            statement.setString(9, submittedByUserId);
            statement.executeUpdate();
        }
        return findRequest(requestId).orElseThrow(() -> new SQLException("Transaction request could not be loaded after submission."));
    }

    public List<BankTransaction> findByAccount(String accountNumber) throws SQLException {
        List<BankTransaction> result = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE account_number=? ORDER BY occurred_at DESC";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, accountNumber);
            try (ResultSet rs = p.executeQuery()) {
                while (rs.next()) result.add(mapTransaction(rs));
            }
        }
        return result;
    }

    public List<TransactionRequest> findPendingRequests() throws SQLException {
        List<TransactionRequest> result = new ArrayList<>();
        String sql = requestSelect() + " WHERE r.status='PENDING' ORDER BY r.submitted_at ASC";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) result.add(mapRequest(rs));
        }
        return result;
    }

    public List<TransactionRequest> findRequestsBySubmitter(String userId) throws SQLException {
        List<TransactionRequest> result = new ArrayList<>();
        String sql = requestSelect() + " WHERE r.submitted_by_user_id=? ORDER BY r.submitted_at DESC";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) result.add(mapRequest(rs));
            }
        }
        return result;
    }

    public Optional<TransactionRequest> findRequest(String requestId) throws SQLException {
        String sql = requestSelect() + " WHERE r.request_id=?";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, requestId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapRequest(rs)) : Optional.empty();
            }
        }
    }

    public void approveRequest(String requestId, String tellerUserId) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                verifyActiveTeller(connection, tellerUserId);
                RequestData request = loadPendingRequest(connection, requestId);
                AccountData source = loadActiveAccount(connection, request.accountNumber);
                BigDecimal amount = request.amount;
                LocalDateTime now = LocalDateTime.now();
                BigDecimal sourceAfter;
                switch (request.type) {
                    case OPENING_DEPOSIT, DEPOSIT -> {
                        sourceAfter = source.balance.add(amount).setScale(2, java.math.RoundingMode.HALF_UP);
                        updateBalance(connection, request.accountNumber, source.balance, sourceAfter);
                        insertApprovedTransaction(connection, request.accountNumber,
                                request.type == TransactionRequestType.OPENING_DEPOSIT ? TransactionType.OPENING_DEPOSIT : TransactionType.DEPOSIT,
                                amount, now, request.reference, sourceAfter);
                    }
                    case WITHDRAWAL -> {
                        validateWithdrawal(source, amount);
                        sourceAfter = source.balance.subtract(amount).setScale(2, java.math.RoundingMode.HALF_UP);
                        updateBalance(connection, request.accountNumber, source.balance, sourceAfter);
                        insertApprovedTransaction(connection, request.accountNumber, TransactionType.WITHDRAWAL,
                                amount, now, request.reference, sourceAfter);
                    }
                    case TRANSFER -> {
                        validateWithdrawal(source, amount);
                        if (request.accountNumber.equalsIgnoreCase(request.destinationAccountNumber)) {
                            throw new SQLException("The source and destination accounts must be different.");
                        }
                        AccountData destination = loadActiveAccount(connection, request.destinationAccountNumber);
                        sourceAfter = source.balance.subtract(amount).setScale(2, java.math.RoundingMode.HALF_UP);
                        BigDecimal destinationAfter = destination.balance.add(amount).setScale(2, java.math.RoundingMode.HALF_UP);
                        updateBalance(connection, request.accountNumber, source.balance, sourceAfter);
                        updateBalance(connection, request.destinationAccountNumber, destination.balance, destinationAfter);
                        insertApprovedTransaction(connection, request.accountNumber, TransactionType.TRANSFER_OUT,
                                amount, now, request.reference + "-OUT", sourceAfter);
                        insertApprovedTransaction(connection, request.destinationAccountNumber, TransactionType.TRANSFER_IN,
                                amount, now, request.reference + "-IN", destinationAfter);
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE transaction_requests SET status='APPROVED',reviewed_by_user_id=?,reviewed_at=? WHERE request_id=? AND status='PENDING'")) {
                    statement.setString(1, tellerUserId);
                    statement.setString(2, now.toString());
                    statement.setString(3, requestId);
                    if (statement.executeUpdate() != 1) throw new SQLException("The transaction request has already been reviewed.");
                }
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                rollback(connection, ex);
                throw ex;
            }
        }
    }

    public void rejectRequest(String requestId, String tellerUserId, String note) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                verifyActiveTeller(connection, tellerUserId);
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE transaction_requests SET status='REJECTED',reviewed_by_user_id=?,reviewed_at=?,review_note=? WHERE request_id=? AND status='PENDING'")) {
                    statement.setString(1, tellerUserId);
                    statement.setString(2, LocalDateTime.now().toString());
                    statement.setString(3, note == null || note.isBlank() ? "Rejected by teller" : note.trim());
                    statement.setString(4, requestId);
                    if (statement.executeUpdate() != 1) throw new SQLException("Pending transaction request was not found.");
                }
                connection.commit();
            } catch (SQLException | RuntimeException ex) {
                rollback(connection, ex);
                throw ex;
            }
        }
    }


    private static void verifyActiveSubmitter(String userId) throws SQLException {
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM user_credentials WHERE user_id=? AND active=1 AND role IN ('CUSTOMER','TELLER')")) {
            statement.setString(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new SecurityException("Only an active customer or teller can submit a transaction request.");
            }
        }
    }

    private static String requestSelect() {
        return "SELECT r.*,submitter.username AS submitted_by,reviewer.username AS reviewed_by " +
                "FROM transaction_requests r " +
                "JOIN user_credentials submitter ON submitter.user_id=r.submitted_by_user_id " +
                "LEFT JOIN user_credentials reviewer ON reviewer.user_id=r.reviewed_by_user_id";
    }

    private static TransactionRequest mapRequest(ResultSet rs) throws SQLException {
        String reviewedAt = rs.getString("reviewed_at");
        return new TransactionRequest(
                rs.getString("request_id"),
                rs.getString("account_number"),
                rs.getString("destination_account_number"),
                TransactionRequestType.valueOf(rs.getString("transaction_type")),
                rs.getBigDecimal("amount"),
                LocalDateTime.parse(rs.getString("submitted_at")),
                rs.getString("reference"),
                TransactionStatus.valueOf(rs.getString("status")),
                rs.getString("submitted_by"),
                rs.getString("reviewed_by"),
                reviewedAt == null ? null : LocalDateTime.parse(reviewedAt),
                rs.getString("review_note"));
    }

    private static BankTransaction mapTransaction(ResultSet rs) throws SQLException {
        return new BankTransaction(rs.getString("transaction_id"), rs.getString("account_number"),
                TransactionType.valueOf(rs.getString("transaction_type")), rs.getBigDecimal("amount"),
                LocalDateTime.parse(rs.getString("occurred_at")), rs.getString("reference"), rs.getBigDecimal("balance_after"));
    }

    private static void verifyActiveTeller(Connection connection, String tellerUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM user_credentials WHERE user_id=? AND role='TELLER' AND active=1")) {
            statement.setString(1, tellerUserId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new SecurityException("Only an active teller can approve or reject transactions.");
            }
        }
    }

    private static RequestData loadPendingRequest(Connection connection, String requestId) throws SQLException {
        String sql = "SELECT account_number,destination_account_number,transaction_type,amount,reference FROM transaction_requests WHERE request_id=? AND status='PENDING'";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, requestId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new SQLException("Pending transaction request was not found or has already been reviewed.");
                String destination = rs.getString("destination_account_number");
                return new RequestData(rs.getString("account_number"), destination,
                        TransactionRequestType.valueOf(rs.getString("transaction_type")), rs.getBigDecimal("amount"), rs.getString("reference"));
            }
        }
    }

    private static AccountData loadActiveAccount(Connection connection, String accountNumber) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT balance,status,account_type FROM accounts WHERE account_number=?")) {
            statement.setString(1, accountNumber);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new SQLException("Account " + accountNumber + " was not found.");
                if (!"ACTIVE".equals(rs.getString("status"))) throw new SQLException("Account " + accountNumber + " is not active.");
                return new AccountData(rs.getBigDecimal("balance"), rs.getString("account_type"));
            }
        }
    }

    private static void validateWithdrawal(AccountData account, BigDecimal amount) throws SQLException {
        if ("SAVINGS".equals(account.accountType)) throw new SQLException("Savings accounts do not allow withdrawals or outgoing transfers.");
        if (account.balance.compareTo(amount) < 0) throw new SQLException("Insufficient funds at the time of approval.");
    }

    private static void updateBalance(Connection connection, String accountNumber,
                                      BigDecimal expected, BigDecimal updated) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE accounts SET balance=? WHERE account_number=? AND balance=? AND status='ACTIVE'")) {
            statement.setBigDecimal(1, updated);
            statement.setString(2, accountNumber);
            statement.setBigDecimal(3, expected);
            if (statement.executeUpdate() != 1) throw new SQLException("Account balance changed while the transaction was being approved. Refresh and retry.");
        }
    }

    private static void insertApprovedTransaction(Connection connection, String accountNumber, TransactionType type,
                                                  BigDecimal amount, LocalDateTime occurredAt, String reference,
                                                  BigDecimal balanceAfter) throws SQLException {
        String sql = "INSERT INTO transactions(transaction_id,account_number,transaction_type,amount,occurred_at,reference,balance_after) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, accountNumber);
            statement.setString(3, type.name());
            statement.setBigDecimal(4, amount);
            statement.setString(5, occurredAt.toString());
            statement.setString(6, reference);
            statement.setBigDecimal(7, balanceAfter);
            statement.executeUpdate();
        }
    }

    private static void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }

    private record RequestData(String accountNumber, String destinationAccountNumber,
                               TransactionRequestType type, BigDecimal amount, String reference) { }

    private record AccountData(BigDecimal balance, String accountType) { }
}
