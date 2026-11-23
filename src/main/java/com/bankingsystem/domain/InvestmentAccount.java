package com.bankingsystem.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public final class InvestmentAccount extends Account implements InterestBearing {
    public static final BigDecimal MIN_OPENING_DEPOSIT = new BigDecimal("500.00");
    public static final BigDecimal MONTHLY_RATE = new BigDecimal("0.05"); 

    public InvestmentAccount(String accountNumber, Customer owner, BigDecimal balance,
                             String branch, AccountStatus status, LocalDate openedOn) {
        super(accountNumber, owner, balance, branch, status, openedOn);
    }

    
    public static InvestmentAccount openNew(String accountNumber, Customer owner, BigDecimal openingDeposit,
                                            String branch, AccountStatus status, LocalDate openedOn) {
        if (openingDeposit == null || openingDeposit.compareTo(MIN_OPENING_DEPOSIT) < 0) {
            throw new IllegalArgumentException("An Investment account requires an opening deposit of at least BWP 500.00.");
        }
        return new InvestmentAccount(accountNumber, owner, openingDeposit, branch, status, openedOn);
    }

    @Override public AccountType getAccountType() { return AccountType.INVESTMENT; }
    @Override public BigDecimal calculateMonthlyInterest() {
        return getBalance().multiply(MONTHLY_RATE).setScale(2, RoundingMode.HALF_UP);
    }
}
