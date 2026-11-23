package com.bankingsystem.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public final class SavingsAccount extends Account implements InterestBearing {
    public static final BigDecimal MONTHLY_RATE = new BigDecimal("0.0005"); 

    public SavingsAccount(String accountNumber, Customer owner, BigDecimal balance,
                          String branch, AccountStatus status, LocalDate openedOn) {
        super(accountNumber, owner, balance, branch, status, openedOn);
    }

    @Override public AccountType getAccountType() { return AccountType.SAVINGS; }
    @Override protected boolean allowsWithdrawals() { return false; }
    @Override public void withdraw(BigDecimal amount) {
        throw new UnsupportedOperationException("Savings accounts do not allow withdrawals.");
    }
    @Override public BigDecimal calculateMonthlyInterest() {
        return getBalance().multiply(MONTHLY_RATE).setScale(2, RoundingMode.HALF_UP);
    }
}
