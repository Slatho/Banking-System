package com.bankingsystem.service;

import com.bankingsystem.domain.Customer;
import com.bankingsystem.domain.Session;
import com.bankingsystem.domain.UserCredential;
import com.bankingsystem.persistence.CredentialDAO;
import com.bankingsystem.persistence.CustomerDAO;
import com.bankingsystem.security.PasswordHasher;

import java.sql.SQLException;
import java.util.Optional;

public class AuthService {
    private final CredentialDAO credentialDAO;
    private final CustomerDAO customerDAO;

    public AuthService(CredentialDAO credentialDAO, CustomerDAO customerDAO) {
        this.credentialDAO = credentialDAO;
        this.customerDAO = customerDAO;
    }

    public Optional<Session> authenticate(String username, char[] password) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.length == 0) return Optional.empty();
        Optional<UserCredential> match = credentialDAO.findByUsername(username.trim());
        if (match.isEmpty()) return Optional.empty();
        UserCredential credential = match.get();
        if (!credential.active() || !PasswordHasher.verify(password, credential.passwordHash())) return Optional.empty();
        Customer customer = credential.customerId() == null ? null : customerDAO.findById(credential.customerId()).orElse(null);
        if (credential.customerId() != null && customer == null) return Optional.empty();
        return Optional.of(new Session(credential.userId(), credential.username(), credential.role(), customer, credential.mustChangePassword()));
    }
}
