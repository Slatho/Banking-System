package com.bankingsystem.persistence;

import com.bankingsystem.domain.UserCredential;
import com.bankingsystem.domain.UserRole;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public class CredentialDAO {
    public void createCustomerLogin(String customerId, String username, String passwordHash) throws SQLException {
        insert(new UserCredential(UUID.randomUUID().toString(), username, passwordHash,
                UserRole.CUSTOMER, customerId, true, false, LocalDateTime.now().toString()));
    }

    public void createTellerLogin(String username, String passwordHash) throws SQLException {
        createTellerLogin(username, passwordHash, false);
    }

    
    public void createTellerLogin(String username, String passwordHash, boolean mustChangePassword) throws SQLException {
        insert(new UserCredential(UUID.randomUUID().toString(), username, passwordHash,
                UserRole.TELLER, null, true, mustChangePassword, LocalDateTime.now().toString()));
    }

    public void createManagerLogin(String username, String passwordHash, boolean mustChangePassword) throws SQLException {
        insert(new UserCredential(UUID.randomUUID().toString(), username, passwordHash,
                UserRole.MANAGER, null, true, mustChangePassword, LocalDateTime.now().toString()));
    }

    public void insert(UserCredential credential) throws SQLException {
        String sql = "INSERT INTO user_credentials(user_id,username,password_hash,role,customer_id,active,must_change_password,created_at) VALUES(?,?,?,?,?,?,?,?)";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, credential.userId());
            p.setString(2, credential.username());
            p.setString(3, credential.passwordHash());
            p.setString(4, credential.role().name());
            p.setString(5, credential.customerId());
            p.setInt(6, credential.active() ? 1 : 0);
            p.setInt(7, credential.mustChangePassword() ? 1 : 0);
            p.setString(8, credential.createdAt() == null ? LocalDateTime.now().toString() : credential.createdAt());
            p.executeUpdate();
        }
    }

    public Optional<UserCredential> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM user_credentials WHERE username = ? COLLATE NOCASE";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, username);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<UserCredential> findByUserId(String userId) throws SQLException {
        String sql = "SELECT * FROM user_credentials WHERE user_id = ?";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, userId);
            try (ResultSet rs = p.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    
    public List<UserCredential> findAllTellers() throws SQLException {
        String sql = "SELECT user_id, username, '' AS password_hash, role, customer_id, active, must_change_password, created_at " +
                "FROM user_credentials WHERE role = 'TELLER' ORDER BY created_at DESC, username COLLATE NOCASE";
        List<UserCredential> tellers = new ArrayList<>();
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql);
             ResultSet rs = p.executeQuery()) {
            while (rs.next()) tellers.add(map(rs));
        }
        return tellers;
    }

    public void setTellerActive(String username, boolean active) throws SQLException {
        String sql = "UPDATE user_credentials SET active = ? WHERE username = ? COLLATE NOCASE AND role = 'TELLER'";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, active ? 1 : 0);
            p.setString(2, username);
            if (p.executeUpdate() != 1) throw new SQLException("Teller account was not found.");
        }
    }

    
    public void resetTellerPassword(String username, String passwordHash) throws SQLException {
        String sql = "UPDATE user_credentials SET password_hash = ?, must_change_password = 1 " +
                "WHERE username = ? COLLATE NOCASE AND role = 'TELLER'";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, passwordHash);
            p.setString(2, username);
            if (p.executeUpdate() != 1) throw new SQLException("Teller account was not found.");
        }
    }

    public void updatePassword(String userId, String passwordHash) throws SQLException {
        String sql = "UPDATE user_credentials SET password_hash = ?, must_change_password = 0 WHERE user_id = ? AND active = 1";
        try (Connection c = Database.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, passwordHash);
            p.setString(2, userId);
            if (p.executeUpdate() != 1) throw new SQLException("Active login account was not found.");
        }
    }

    private static UserCredential map(ResultSet rs) throws SQLException {
        return new UserCredential(
                rs.getString("user_id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                UserRole.valueOf(rs.getString("role")),
                rs.getString("customer_id"),
                rs.getInt("active") == 1,
                rs.getInt("must_change_password") == 1,
                rs.getString("created_at")
        );
    }
}
