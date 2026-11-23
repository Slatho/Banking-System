package com.bankingsystem.controller;

import com.bankingsystem.domain.*;
import com.bankingsystem.service.BankingService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class CustomerController {
    private final BankingService bankingService;
    private final Session session;

    public CustomerController(BankingService bankingService, Session session) {
        if (session.role() != UserRole.CUSTOMER || session.customer() == null) throw new SecurityException("Customer session required.");
        this.bankingService = bankingService;
        this.session = session;
    }

    public List<Account> accounts() throws SQLException {
        return bankingService.getAccounts(session.customer().getCustomerId());
    }

    public List<BankTransaction> transactions(String accountNumber) throws SQLException {
        Account account = accounts().stream().filter(a -> a.getAccountNumber().equals(accountNumber)).findFirst()
                .orElseThrow(() -> new SecurityException("Account does not belong to this customer."));
        return bankingService.getTransactions(account.getAccountNumber());
    }

    public List<TransactionRequest> requests() throws SQLException {
        return bankingService.getRequestsBySubmitter(session.userId());
    }

    public TransactionRequest deposit(String accountNumber, BigDecimal amount) throws SQLException {
        return bankingService.submitDeposit(accountNumber, amount, session.userId(), session.customer().getCustomerId());
    }

    public TransactionRequest withdraw(String accountNumber, BigDecimal amount) throws SQLException {
        return bankingService.submitWithdrawal(accountNumber, amount, session.userId(), session.customer().getCustomerId());
    }

    public TransactionRequest transfer(String sourceAccount, String destinationAccount, BigDecimal amount) throws SQLException {
        return bankingService.submitTransfer(sourceAccount, destinationAccount, amount, session.userId(), session.customer().getCustomerId());
    }

    public Session getSession() {
        return session;
    }
}
