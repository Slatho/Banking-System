package com.bankingsystem.service;

import com.bankingsystem.domain.*;
import com.bankingsystem.persistence.AccountDAO;
import com.bankingsystem.persistence.CustomerDAO;
import com.bankingsystem.persistence.RegistrationDAO;
import com.bankingsystem.persistence.TransactionDAO;
import com.bankingsystem.security.PasswordHasher;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class BankingService {
    private final CustomerDAO customerDAO;
    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;
    private final RegistrationDAO registrationDAO;

    public BankingService(CustomerDAO customerDAO, AccountDAO accountDAO, TransactionDAO transactionDAO,
                          RegistrationDAO registrationDAO) {
        this.customerDAO = customerDAO;
        this.accountDAO = accountDAO;
        this.transactionDAO = transactionDAO;
        this.registrationDAO = registrationDAO;
    }

    public Customer registerCustomer(String firstName, String surname, String address, String phone,
                                     String email, boolean employed, String employerName, String employerAddress,
                                     String username, char[] password) throws SQLException {
        Customer customer = new Customer("CUS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                firstName, surname, address, phone, email, employed, employerName, employerAddress);
        registrationDAO.register(customer, username, PasswordHasher.hash(password));
        return customer;
    }

    public Account openAccount(String customerId, AccountType type, BigDecimal openingDeposit,
                               String branch, String tellerUserId) throws SQLException {
        Customer customer = customerDAO.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer ID was not found."));
        Branch selectedBranch = Branch.fromValue(branch);
        BigDecimal initial = openingDeposit == null ? BigDecimal.ZERO : openingDeposit.setScale(2, RoundingMode.HALF_UP);
        if (initial.signum() < 0) throw new IllegalArgumentException("Opening deposit cannot be negative.");
        if (type == AccountType.INVESTMENT && initial.compareTo(InvestmentAccount.MIN_OPENING_DEPOSIT) < 0) {
            throw new IllegalArgumentException("An Investment account requires an opening deposit of at least BWP 500.00.");
        }
        String accountNumber = selectedBranch.code() + "-ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Account account = switch (type) {
            case SAVINGS -> new SavingsAccount(accountNumber, customer, BigDecimal.ZERO, selectedBranch.code(), AccountStatus.ACTIVE, LocalDate.now());
            case INVESTMENT -> new InvestmentAccount(accountNumber, customer, BigDecimal.ZERO, selectedBranch.code(), AccountStatus.ACTIVE, LocalDate.now());
            case CHEQUE -> new ChequeAccount(accountNumber, customer, BigDecimal.ZERO, selectedBranch.code(), AccountStatus.ACTIVE, LocalDate.now());
        };
        accountDAO.create(account);
        if (initial.signum() > 0) {
            transactionDAO.createRequest(accountNumber, null, TransactionRequestType.OPENING_DEPOSIT, initial, tellerUserId);
        }
        return account;
    }

    public TransactionRequest submitDeposit(String accountNumber, BigDecimal amount, String submittedByUserId,
                                            String customerId) throws SQLException {
        Account account = requireAccount(accountNumber);
        verifyOwnership(account, customerId);
        account.deposit(amount);
        return transactionDAO.createRequest(accountNumber, null, TransactionRequestType.DEPOSIT, amount, submittedByUserId);
    }

    public TransactionRequest submitWithdrawal(String accountNumber, BigDecimal amount, String submittedByUserId,
                                               String customerId) throws SQLException {
        Account account = requireAccount(accountNumber);
        verifyOwnership(account, customerId);
        account.withdraw(amount);
        return transactionDAO.createRequest(accountNumber, null, TransactionRequestType.WITHDRAWAL, amount, submittedByUserId);
    }

    public TransactionRequest submitTransfer(String sourceAccountNumber, String destinationAccountNumber,
                                             BigDecimal amount, String submittedByUserId,
                                             String customerId) throws SQLException {
        Account source = requireAccount(sourceAccountNumber);
        verifyOwnership(source, customerId);
        if (sourceAccountNumber.equalsIgnoreCase(destinationAccountNumber == null ? "" : destinationAccountNumber.trim())) {
            throw new IllegalArgumentException("The source and destination accounts must be different.");
        }
        Account destination = requireAccount(destinationAccountNumber);
        source.withdraw(amount);
        destination.deposit(amount);
        return transactionDAO.createRequest(sourceAccountNumber, destinationAccountNumber,
                TransactionRequestType.TRANSFER, amount, submittedByUserId);
    }

    public List<Account> getAccounts(String customerId) throws SQLException {
        return accountDAO.findByCustomer(customerId);
    }

    public List<BankTransaction> getTransactions(String accountNumber) throws SQLException {
        return transactionDAO.findByAccount(accountNumber);
    }

    public List<TransactionRequest> getPendingRequests() throws SQLException {
        return transactionDAO.findPendingRequests();
    }

    public List<TransactionRequest> getRequestsBySubmitter(String userId) throws SQLException {
        return transactionDAO.findRequestsBySubmitter(userId);
    }

    public void approveTransaction(String requestId, Session tellerSession) throws SQLException {
        requireTeller(tellerSession);
        transactionDAO.approveRequest(requestId, tellerSession.userId());
    }

    public void rejectTransaction(String requestId, Session tellerSession, String note) throws SQLException {
        requireTeller(tellerSession);
        transactionDAO.rejectRequest(requestId, tellerSession.userId(), note);
    }

    private Account requireAccount(String accountNumber) throws SQLException {
        if (accountNumber == null || accountNumber.isBlank()) throw new IllegalArgumentException("Account number is required.");
        return accountDAO.findByNumber(accountNumber.trim())
                .orElseThrow(() -> new IllegalArgumentException("Account was not found."));
    }

    private static void verifyOwnership(Account account, String customerId) {
        if (customerId != null && !account.getOwner().getCustomerId().equals(customerId)) {
            throw new SecurityException("You are not authorised to use this account.");
        }
    }

    private static void requireTeller(Session session) {
        if (session == null || session.role() != UserRole.TELLER) {
            throw new SecurityException("Only a teller can approve or reject transactions.");
        }
    }
}
