package com.bankingsystem.persistence;

import com.bankingsystem.domain.*;
import com.bankingsystem.security.PasswordHasher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class SeedData {
    private SeedData() { }

    public static void seed() throws Exception {
        CredentialDAO credentials = new CredentialDAO();
        if (credentials.findByUsername("teller").isEmpty()) {
            credentials.createTellerLogin("teller", PasswordHasher.hash("Teller@2026!".toCharArray()), true);
        }
        if (credentials.findByUsername("manager").isEmpty()) {
            credentials.createManagerLogin("manager", PasswordHasher.hash("Manager@2026!".toCharArray()), true);
        }
        CustomerDAO customers = new CustomerDAO();
        if (customers.count() > 0) return;

        AccountDAO accounts = new AccountDAO();
        RegistrationDAO registration = new RegistrationDAO();
        List<Sample> samples = List.of(
                new Sample("Thato", "Molefe", true, "GRS Francistown", "Plot 12, Francistown", AccountType.SAVINGS, "2450.00", Branch.FRANCISTOWN),
                new Sample("Kabelo", "Dube", true, "GRS Gaborone", "CBD, Gaborone", AccountType.INVESTMENT, "1800.00", Branch.GABORONE),
                new Sample("Mpho", "Pheto", false, null, null, AccountType.SAVINGS, "750.00", Branch.PALAPYE),
                new Sample("Onalenna", "Kgosi", true, "GRS Francistown", "Airport Road, Francistown", AccountType.CHEQUE, "3200.00", Branch.FRANCISTOWN),
                new Sample("Kago", "Modise", true, "GRS Palapye", "Main Mall, Palapye", AccountType.INVESTMENT, "500.00", Branch.PALAPYE),
                new Sample("Boitumelo", "Kelebeng", false, null, null, AccountType.SAVINGS, "1250.00", Branch.GABORONE),
                new Sample("Tshepo", "Motsamai", true, "GRS Francistown", "Blue Jacket Street", AccountType.CHEQUE, "980.00", Branch.FRANCISTOWN),
                new Sample("Amantle", "Seboni", true, "GRS Gaborone", "Block 8, Gaborone", AccountType.INVESTMENT, "2450.00", Branch.GABORONE),
                new Sample("Lesego", "Tshiamo", false, null, null, AccountType.SAVINGS, "890.00", Branch.PALAPYE),
                new Sample("Bontle", "Masego", true, "GRS Palapye", "Plateau Road, Palapye", AccountType.CHEQUE, "1500.00", Branch.PALAPYE)
        );
        int n = 1;
        for (Sample sample : samples) {
            String customerId = String.format("CUS-%04d", 1000 + n);
            Customer customer = new Customer(customerId, sample.firstName, sample.surname,
                    "Botswana", "+267 71" + String.format("%06d", n),
                    "customer" + n + "@demo.bw", sample.employed, sample.employer, sample.employerAddress);
            String username = String.format("customer%02d", n);
            registration.register(customer, username, PasswordHasher.hash("Customer@2026!".toCharArray()));
            String accountNumber = sample.branch.code() + "-ACC-" + String.format("%04d", 2000 + n);
            BigDecimal balance = new BigDecimal(sample.balance);
            Account account = switch (sample.type) {
                case SAVINGS -> new SavingsAccount(accountNumber, customer, balance, sample.branch.code(), AccountStatus.ACTIVE, LocalDate.now());
                case INVESTMENT -> new InvestmentAccount(accountNumber, customer, balance, sample.branch.code(), AccountStatus.ACTIVE, LocalDate.now());
                case CHEQUE -> new ChequeAccount(accountNumber, customer, balance, sample.branch.code(), AccountStatus.ACTIVE, LocalDate.now());
            };
            accounts.createSeeded(account);
            n++;
        }
    }

    private record Sample(String firstName, String surname, boolean employed, String employer,
                          String employerAddress, AccountType type, String balance, Branch branch) { }
}
