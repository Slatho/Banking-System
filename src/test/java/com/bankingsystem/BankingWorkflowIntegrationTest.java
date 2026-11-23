package com.bankingsystem;

import com.bankingsystem.domain.*;
import com.bankingsystem.persistence.*;
import com.bankingsystem.service.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BankingWorkflowIntegrationTest {
    @BeforeAll
    static void initialiseDatabase() throws Exception {
        var folder = Files.createTempDirectory("banking-system-test");
        System.setProperty("banking.db.path", folder.resolve("test.db").toString());
        Database.initialize();
        SeedData.seed();
    }

    @Test
    @Order(1)
    void tenSampleCustomersAccountsAndBranchesAreSeeded() throws Exception {
        assertEquals(10, new CustomerDAO().count());
        assertEquals(10, new AccountDAO().count());
        assertEquals("FRT", Branch.FRANCISTOWN.code());
        assertEquals("PAL", Branch.PALAPYE.code());
        assertEquals("GAB", Branch.GABORONE.code());
    }

    @Test
    @Order(2)
    void customerCanAuthenticateAndViewOwnAccounts() throws Exception {
        CustomerDAO customerDAO = new CustomerDAO();
        AuthService auth = new AuthService(new CredentialDAO(), customerDAO);
        var session = auth.authenticate("customer01", "Customer@2026!".toCharArray());
        assertTrue(session.isPresent());
        assertEquals(UserRole.CUSTOMER, session.get().role());
        assertTrue(auth.authenticate("customer01", "wrong-password".toCharArray()).isEmpty());
        BankingService service = service();
        assertEquals(1, service.getAccounts(session.get().customer().getCustomerId()).size());
    }

    @Test
    @Order(3)
    void depositDoesNotChangeBalanceUntilTellerApproves() throws Exception {
        BankingService service = service();
        AccountDAO accounts = new AccountDAO();
        TransactionDAO transactions = new TransactionDAO();
        Account account = accounts.findByCustomer("CUS-1001").get(0);
        BigDecimal before = account.getBalance();
        Session customer = customerSession("customer01");
        TransactionRequest request = service.submitDeposit(account.getAccountNumber(), new BigDecimal("25.00"), customer.userId(), "CUS-1001");
        assertEquals(TransactionStatus.PENDING, request.status());
        assertEquals(before, accounts.findByNumber(account.getAccountNumber()).orElseThrow().getBalance());
        service.approveTransaction(request.requestId(), tellerSession());
        assertEquals(before.add(new BigDecimal("25.00")), accounts.findByNumber(account.getAccountNumber()).orElseThrow().getBalance());
        assertTrue(transactions.findByAccount(account.getAccountNumber()).stream().anyMatch(t -> t.type() == TransactionType.DEPOSIT));
    }

    @Test
    @Order(4)
    void transferMovesFundsOnlyAfterApprovalAndCreatesBothLedgerEntries() throws Exception {
        BankingService service = service();
        AccountDAO accounts = new AccountDAO();
        TransactionDAO transactions = new TransactionDAO();
        Account source = accounts.findByCustomer("CUS-1004").get(0);
        Account destination = accounts.findByCustomer("CUS-1002").get(0);
        BigDecimal sourceBefore = source.getBalance();
        BigDecimal destinationBefore = destination.getBalance();
        Session customer = customerSession("customer04");
        TransactionRequest request = service.submitTransfer(source.getAccountNumber(), destination.getAccountNumber(),
                new BigDecimal("200.00"), customer.userId(), "CUS-1004");
        assertEquals(sourceBefore, accounts.findByNumber(source.getAccountNumber()).orElseThrow().getBalance());
        assertEquals(destinationBefore, accounts.findByNumber(destination.getAccountNumber()).orElseThrow().getBalance());
        service.approveTransaction(request.requestId(), tellerSession());
        assertEquals(sourceBefore.subtract(new BigDecimal("200.00")), accounts.findByNumber(source.getAccountNumber()).orElseThrow().getBalance());
        assertEquals(destinationBefore.add(new BigDecimal("200.00")), accounts.findByNumber(destination.getAccountNumber()).orElseThrow().getBalance());
        assertTrue(transactions.findByAccount(source.getAccountNumber()).stream().anyMatch(t -> t.type() == TransactionType.TRANSFER_OUT));
        assertTrue(transactions.findByAccount(destination.getAccountNumber()).stream().anyMatch(t -> t.type() == TransactionType.TRANSFER_IN));
    }

    @Test
    @Order(5)
    void rejectedRequestDoesNotChangeBalance() throws Exception {
        BankingService service = service();
        AccountDAO accounts = new AccountDAO();
        Account account = accounts.findByCustomer("CUS-1001").get(0);
        BigDecimal before = account.getBalance();
        Session customer = customerSession("customer01");
        TransactionRequest request = service.submitDeposit(account.getAccountNumber(), new BigDecimal("10.00"), customer.userId(), "CUS-1001");
        service.rejectTransaction(request.requestId(), tellerSession(), "Customer cancelled");
        assertEquals(before, accounts.findByNumber(account.getAccountNumber()).orElseThrow().getBalance());
        assertEquals(TransactionStatus.REJECTED, new TransactionDAO().findRequest(request.requestId()).orElseThrow().status());
    }

    @Test
    @Order(6)
    void managerCannotApproveTransactions() throws Exception {
        Session manager = new Session("manager-id", "manager", UserRole.MANAGER, null, false);
        TransactionRequest request = service().submitDeposit(
                new AccountDAO().findByCustomer("CUS-1001").get(0).getAccountNumber(), new BigDecimal("5.00"),
                customerSession("customer01").userId(), "CUS-1001");
        assertThrows(SecurityException.class, () -> service().approveTransaction(request.requestId(), manager));
        service().rejectTransaction(request.requestId(), tellerSession(), "Test request cleanup");
    }

    @Test
    @Order(7)
    void monthlyInterestRetryDoesNotDuplicateCredits() throws Exception {
        InterestService service = new InterestService(new AccountDAO());
        YearMonth period = YearMonth.now();
        var first = service.processMonthlyInterest(period);
        var second = service.processMonthlyInterest(period);
        assertTrue(first.accountsCredited() > 0);
        assertEquals(0, second.accountsCredited());
    }


    @Test
    @Order(8)
    void openingDepositWaitsForApprovalAndAccountUsesSelectedBranch() throws Exception {
        BankingService service = service();
        Session teller = tellerSession();
        Account account = service.openAccount("CUS-1001", AccountType.SAVINGS, new BigDecimal("125.00"),
                Branch.PALAPYE.code(), teller.userId());
        assertTrue(account.getAccountNumber().startsWith("PAL-"));
        assertEquals("PAL", account.getBranchCode());
        assertEquals(BigDecimal.ZERO.setScale(2), new AccountDAO().findByNumber(account.getAccountNumber()).orElseThrow().getBalance());
        TransactionRequest request = new TransactionDAO().findPendingRequests().stream()
                .filter(item -> item.accountNumber().equals(account.getAccountNumber()))
                .findFirst().orElseThrow();
        assertEquals(TransactionRequestType.OPENING_DEPOSIT, request.type());
        service.approveTransaction(request.requestId(), teller);
        assertEquals(new BigDecimal("125.00"), new AccountDAO().findByNumber(account.getAccountNumber()).orElseThrow().getBalance());
    }

    @Test
    @Order(9)
    void managerCanCreateDeactivateReactivateAndResetTellerAccounts() throws Exception {
        CredentialDAO credentials = new CredentialDAO();
        AuthService auth = new AuthService(credentials, new CustomerDAO());
        var managerSession = auth.authenticate("manager", "Manager@2026!".toCharArray()).orElseThrow();
        assertEquals(UserRole.MANAGER, managerSession.role());
        assertTrue(managerSession.mustChangePassword());

        PasswordChangeService passwordChanges = new PasswordChangeService(credentials);
        char[] managerNewPassword = "ManagerSecure#2026".toCharArray();
        passwordChanges.changeOwnPassword(managerSession, "Manager@2026!".toCharArray(), managerNewPassword, managerNewPassword);
        managerSession = auth.authenticate("manager", managerNewPassword).orElseThrow();
        assertFalse(managerSession.mustChangePassword());

        TellerManagementService management = new TellerManagementService(credentials, managerSession);
        String username = "staff" + java.util.UUID.randomUUID().toString().substring(0, 6);
        char[] firstTemporaryPassword = "InitialTemp#2026".toCharArray();
        management.createTeller(username, firstTemporaryPassword);
        var createdSession = auth.authenticate(username, firstTemporaryPassword).orElseThrow();
        assertEquals(UserRole.TELLER, createdSession.role());
        assertTrue(createdSession.mustChangePassword());
        assertTrue(management.listTellers().stream().anyMatch(t -> t.username().equalsIgnoreCase(username)));

        char[] resetTemporaryPassword = "ResetTemp#2026".toCharArray();
        management.resetTellerPassword(username, resetTemporaryPassword);
        assertTrue(auth.authenticate(username, firstTemporaryPassword).isEmpty());
        var resetSession = auth.authenticate(username, resetTemporaryPassword).orElseThrow();
        assertTrue(resetSession.mustChangePassword());

        management.setTellerActive(username, false);
        assertTrue(auth.authenticate(username, resetTemporaryPassword).isEmpty());
        management.setTellerActive(username, true);
        resetSession = auth.authenticate(username, resetTemporaryPassword).orElseThrow();

        char[] tellerNewPassword = "TellerSecure#2026".toCharArray();
        passwordChanges.changeOwnPassword(resetSession, resetTemporaryPassword, tellerNewPassword, tellerNewPassword);
        var finalSession = auth.authenticate(username, tellerNewPassword).orElseThrow();
        assertFalse(finalSession.mustChangePassword());
    }

    @Test
    @Order(10)
    void tellerCannotConstructTellerManagementService() throws Exception {
        Session teller = tellerSession();
        assertThrows(SecurityException.class, () -> new TellerManagementService(new CredentialDAO(), teller));
    }

    private static BankingService service() {
        return new BankingService(new CustomerDAO(), new AccountDAO(), new TransactionDAO(), new RegistrationDAO());
    }

    private static Session customerSession(String username) throws Exception {
        return new AuthService(new CredentialDAO(), new CustomerDAO())
                .authenticate(username, "Customer@2026!".toCharArray()).orElseThrow();
    }

    private static Session tellerSession() throws Exception {
        return new AuthService(new CredentialDAO(), new CustomerDAO())
                .authenticate("teller", "Teller@2026!".toCharArray()).orElseThrow();
    }
}
