package com.bankingsystem.domain;

public record Session(String userId, String username, UserRole role, Customer customer,
                      boolean mustChangePassword) {
    public Session(String userId, String username, UserRole role, Customer customer) {
        this(userId, username, role, customer, false);
    }

    public Session afterPasswordChange() {
        return new Session(userId, username, role, customer, false);
    }
}
