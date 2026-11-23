package com.bankingsystem.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ChequeAccount extends Account {
    public ChequeAccount(String accountNumber, Customer owner, BigDecimal balance,
                         String branch, AccountStatus status, LocalDate openedOn) {
        super(accountNumber, owner, balance, branch, status, openedOn);
        if (!owner.isEmployed() || owner.getEmployerName() == null || owner.getEmployerAddress() == null) {
            throw new IllegalArgumentException("A Cheque account is available only to an employed customer with employer details.");
        }
    }
    @Override public AccountType getAccountType() { return AccountType.CHEQUE; }
}
