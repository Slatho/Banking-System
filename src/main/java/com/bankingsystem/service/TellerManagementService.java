package com.bankingsystem.service;

import com.bankingsystem.domain.Session;
import com.bankingsystem.domain.UserCredential;
import com.bankingsystem.domain.UserRole;
import com.bankingsystem.persistence.CredentialDAO;
import com.bankingsystem.security.PasswordHasher;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;


public class TellerManagementService {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9._-]{3,23}$");
    private final CredentialDAO credentialDAO;
    private final Session managerSession;

    public TellerManagementService(CredentialDAO credentialDAO, Session managerSession) {
        if (managerSession == null || managerSession.role() != UserRole.MANAGER) {
            throw new SecurityException("A manager session is required to manage teller accounts.");
        }
        this.credentialDAO = credentialDAO;
        this.managerSession = managerSession;
    }

    public UserCredential createTeller(String username, char[] temporaryPassword) throws SQLException {
        requireManager();
        String cleanUsername = validateUsername(username);
        validatePassword(temporaryPassword, "Temporary password");
        if (credentialDAO.findByUsername(cleanUsername).isPresent()) {
            throw new IllegalArgumentException("That username is already in use. Choose another username.");
        }
        credentialDAO.createTellerLogin(cleanUsername, PasswordHasher.hash(temporaryPassword), true);
        UserCredential created = credentialDAO.findByUsername(cleanUsername).orElseThrow(
                () -> new SQLException("The teller account was created but could not be retrieved."));
        return new UserCredential(created.userId(), created.username(), "", created.role(),
                created.customerId(), created.active(), created.mustChangePassword(), created.createdAt());
    }

    public List<UserCredential> listTellers() throws SQLException {
        requireManager();
        return credentialDAO.findAllTellers();
    }

    public void setTellerActive(String username, boolean active) throws SQLException {
        requireManager();
        credentialDAO.setTellerActive(username, active);
    }

    public void resetTellerPassword(String username, char[] temporaryPassword) throws SQLException {
        requireManager();
        validatePassword(temporaryPassword, "Temporary password");
        credentialDAO.resetTellerPassword(username, PasswordHasher.hash(temporaryPassword));
    }

    private void requireManager() throws SQLException {
        if (managerSession == null || managerSession.role() != UserRole.MANAGER) {
            throw new SecurityException("Only a manager can perform this operation.");
        }
        UserCredential current = credentialDAO.findByUserId(managerSession.userId())
                .orElseThrow(() -> new SecurityException("The manager account could not be found."));
        if (!current.active() || current.role() != UserRole.MANAGER) {
            throw new SecurityException("The manager account is inactive or no longer authorised.");
        }
    }

    private static String validateUsername(String username) {
        if (username == null || !USERNAME_PATTERN.matcher(username.trim()).matches()) {
            throw new IllegalArgumentException("Username must be 4–24 characters, start with a letter, and contain only letters, numbers, dots, underscores or hyphens.");
        }
        return username.trim();
    }

    private static void validatePassword(char[] password, String label) {
        if (password == null || password.length < 10) {
            throw new IllegalArgumentException(label + " must contain at least 10 characters.");
        }
    }
}
