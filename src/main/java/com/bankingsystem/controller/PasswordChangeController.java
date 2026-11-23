package com.bankingsystem.controller;

import com.bankingsystem.domain.Session;
import com.bankingsystem.persistence.CredentialDAO;
import com.bankingsystem.service.PasswordChangeService;

import java.sql.SQLException;


public class PasswordChangeController {
    private final PasswordChangeService service;

    public PasswordChangeController(CredentialDAO credentialDAO) {
        this.service = new PasswordChangeService(credentialDAO);
    }

    public void changePassword(Session session, char[] currentPassword, char[] newPassword,
                               char[] confirmation) throws SQLException {
        service.changeOwnPassword(session, currentPassword, newPassword, confirmation);
    }
}
