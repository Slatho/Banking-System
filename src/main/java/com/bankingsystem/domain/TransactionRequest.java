package com.bankingsystem.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionRequest(
        String requestId,
        String accountNumber,
        String destinationAccountNumber,
        TransactionRequestType type,
        BigDecimal amount,
        LocalDateTime submittedAt,
        String reference,
        TransactionStatus status,
        String submittedBy,
        String reviewedBy,
        LocalDateTime reviewedAt,
        String reviewNote) {
}
