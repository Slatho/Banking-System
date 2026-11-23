package com.bankingsystem.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;


public abstract class Account {
    private final String accountNumber;
    private final Customer owner;
    private BigDecimal balance;
    private final Branch branch;
    private AccountStatus status;
    private final LocalDate openedOn;

    protected Account(String accountNumber, Customer owner, BigDecimal openingBalance,
                      String branch, AccountStatus status, LocalDate openedOn) {
        if (accountNumber == null || accountNumber.isBlank()) throw new IllegalArgumentException("Account number is required.");
        this.accountNumber = accountNumber.trim();
        this.owner = Objects.requireNonNull(owner, "Account owner is required.");
        this.balance = money(openingBalance);
        if (this.balance.signum() < 0) throw new IllegalArgumentException("Opening balance cannot be negative.");
        this.branch = Branch.fromValue(branch == null || branch.isBlank() ? "Francistown" : branch);
        this.status = Objects.requireNonNull(status, "Account status is required.");
        this.openedOn = openedOn == null ? LocalDate.now() : openedOn;
    }

    public void deposit(BigDecimal amount) { deposit(amount, null); }

    
    public void deposit(BigDecimal amount, String reference) {
        ensureActive();
        BigDecimal value = positiveMoney(amount);
        balance = balance.add(value).setScale(2, RoundingMode.HALF_UP);
    }

    public void withdraw(BigDecimal amount) {
        ensureActive();
        BigDecimal value = positiveMoney(amount);
        if (!allowsWithdrawals()) throw new UnsupportedOperationException("Withdrawals are not permitted on a Savings account.");
        if (balance.compareTo(value) < 0) throw new IllegalArgumentException("Insufficient funds.");
        balance = balance.subtract(value).setScale(2, RoundingMode.HALF_UP);
    }

    protected boolean allowsWithdrawals() { return true; }

    protected void ensureActive() {
        if (status != AccountStatus.ACTIVE) throw new IllegalStateException("The account is not active.");
    }

    protected static BigDecimal money(BigDecimal amount) {
        if (amount == null) throw new IllegalArgumentException("Amount is required.");
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    protected static BigDecimal positiveMoney(BigDecimal amount) {
        BigDecimal value = money(amount);
        if (value.signum() <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");
        return value;
    }

    
    public void synchronizeBalance(BigDecimal newBalance) { this.balance = money(newBalance); }

    public String getAccountNumber() { return accountNumber; }
    public Customer getOwner() { return owner; }
    public BigDecimal getBalance() { return balance; }
    public String getBranch() { return branch.displayName(); }
    public String getBranchCode() { return branch.code(); }
    public Branch getBranchValue() { return branch; }
    public AccountStatus getStatus() { return status; }
    public LocalDate getOpenedOn() { return openedOn; }
    public abstract AccountType getAccountType();
}
