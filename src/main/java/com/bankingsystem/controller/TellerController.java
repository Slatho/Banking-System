package com.bankingsystem.controller;

import com.bankingsystem.domain.*;
import com.bankingsystem.service.BankingService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class TellerController {
    private final BankingService bankingService;
    private final Session session;

    public TellerController(BankingService bankingService, Session session) {
        if (session.role() != UserRole.TELLER) throw new SecurityException("Bank teller session required.");
        this.bankingService = bankingService;
        this.session = session;
    }

    public Customer registerCustomer(String first, String surname, String address, String phone, String email,
                                     boolean employed, String employer, String employerAddress,
                                     String username, char[] password) throws SQLException {
        return bankingService.registerCustomer(first, surname, address, phone, email, employed,
                employer, employerAddress, username, password);
    }

    public Account openAccount(String customerId, AccountType type, BigDecimal openingDeposit, String branch) throws SQLException {
        return bankingService.openAccount(customerId, type, openingDeposit, branch, session.userId());
    }

    public TransactionRequest submitDepositForCustomer(String accountNumber, BigDecimal amount) throws SQLException {
        return bankingService.submitDeposit(accountNumber, amount, session.userId(), null);
    }

    public TransactionRequest submitWithdrawalForCustomer(String accountNumber, BigDecimal amount) throws SQLException {
        return bankingService.submitWithdrawal(accountNumber, amount, session.userId(), null);
    }

    public TransactionRequest submitTransferForCustomer(String sourceAccount, String destinationAccount, BigDecimal amount) throws SQLException {
        return bankingService.submitTransfer(sourceAccount, destinationAccount, amount, session.userId(), null);
    }

    public List<TransactionRequest> pendingRequests() throws SQLException {
        return bankingService.getPendingRequests();
    }

    public void approve(String requestId) throws SQLException {
        bankingService.approveTransaction(requestId, session);
    }

    public void reject(String requestId, String note) throws SQLException {
        bankingService.rejectTransaction(requestId, session, note);
    }
}
