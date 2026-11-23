package com.bankingsystem.service;

import com.bankingsystem.domain.Session;
import com.bankingsystem.domain.UserCredential;
import com.bankingsystem.persistence.CredentialDAO;
import com.bankingsystem.security.PasswordHasher;

import java.sql.SQLException;


public class PasswordChangeService {
    private final CredentialDAO credentialDAO;

    public PasswordChangeService(CredentialDAO credentialDAO) {
        this.credentialDAO = credentialDAO;
    }

    public void changeOwnPassword(Session session, char[] currentPassword, char[] newPassword,
                                  char[] confirmation) throws SQLException {
        if (session == null) throw new SecurityException("An authenticated session is required.");
        UserCredential credential = credentialDAO.findByUserId(session.userId())
                .orElseThrow(() -> new SecurityException("The login account could not be found."));
        if (!credential.active()) throw new SecurityException("This login account is inactive.");
        if (!PasswordHasher.verify(currentPassword, credential.passwordHash())) {
            throw new IllegalArgumentException("The current password is incorrect.");
        }
        if (newPassword == null || newPassword.length < 10) {
            throw new IllegalArgumentException("The new password must contain at least 10 characters.");
        }
        if (confirmation == null || !java.util.Arrays.equals(newPassword, confirmation)) {
            throw new IllegalArgumentException("The new password and confirmation do not match.");
        }
        if (PasswordHasher.verify(newPassword, credential.passwordHash())) {
            throw new IllegalArgumentException("Choose a password different from the current password.");
        }
        credentialDAO.updatePassword(session.userId(), PasswordHasher.hash(newPassword));
    }
}
