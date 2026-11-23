package com.bankingsystem.controller;

import com.bankingsystem.domain.Session;
import com.bankingsystem.service.AuthService;

import java.sql.SQLException;
import java.util.Optional;


public class LoginController {
    private final AuthService authService;
    public LoginController(AuthService authService) { this.authService = authService; }

    public Optional<Session> login(String username, char[] password) throws SQLException {
        return authService.authenticate(username, password);
    }
}
