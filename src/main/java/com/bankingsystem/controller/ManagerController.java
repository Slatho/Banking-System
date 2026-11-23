package com.bankingsystem.controller;

import com.bankingsystem.domain.Session;
import com.bankingsystem.domain.UserCredential;
import com.bankingsystem.persistence.CredentialDAO;
import com.bankingsystem.service.TellerManagementService;

import java.sql.SQLException;
import java.util.List;


public class ManagerController {
    private final TellerManagementService service;

    public ManagerController(Session session, CredentialDAO credentialDAO) {
        this.service = new TellerManagementService(credentialDAO, session);
    }

    public UserCredential createTeller(String username, char[] temporaryPassword) throws SQLException {
        return service.createTeller(username, temporaryPassword);
    }

    public List<UserCredential> listTellers() throws SQLException {
        return service.listTellers();
    }

    public void deactivateTeller(String username) throws SQLException {
        service.setTellerActive(username, false);
    }

    public void reactivateTeller(String username) throws SQLException {
        service.setTellerActive(username, true);
    }

    public void resetTellerPassword(String username, char[] temporaryPassword) throws SQLException {
        service.resetTellerPassword(username, temporaryPassword);
    }
}
