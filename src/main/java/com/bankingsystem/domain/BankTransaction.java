package com.bankingsystem.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BankTransaction(
        String transactionId,
        String accountNumber,
        TransactionType type,
        BigDecimal amount,
        LocalDateTime occurredAt,
        String reference,
        BigDecimal balanceAfter) { }
