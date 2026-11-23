package com.bankingsystem.domain;

public record UserCredential(String userId, String username, String passwordHash,
                              UserRole role, String customerId, boolean active,
                              boolean mustChangePassword, String createdAt) { }
