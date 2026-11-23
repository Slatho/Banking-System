package com.bankingsystem.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class AccountRulesTest {
    private Customer employedCustomer() {
        return new Customer("CUS-1", "Kabelo", "Dube", "Francistown", "71234567", "kabelo@example.bw", true, "GRS", "Francistown");
    }
    private Customer unemployedCustomer() {
        return new Customer("CUS-2", "Mpho", "Pheto", "Francistown", "71234568", "mpho@example.bw", false, null, null);
    }

    @Test void savingsAcceptsDepositsButRejectsWithdrawals() {
        SavingsAccount account = new SavingsAccount("ACC-1", employedCustomer(), new BigDecimal("100.00"), "Francistown", AccountStatus.ACTIVE, LocalDate.now());
        account.deposit(new BigDecimal("25.00"));
        assertEquals(new BigDecimal("125.00"), account.getBalance());
        assertThrows(UnsupportedOperationException.class, () -> account.withdraw(new BigDecimal("5.00")));
    }

    @Test void investmentRequiresAtLeastFiveHundredAndAllowsWithdrawal() {
        assertThrows(IllegalArgumentException.class, () -> InvestmentAccount.openNew("ACC-2", employedCustomer(), new BigDecimal("499.99"), "Francistown", AccountStatus.ACTIVE, LocalDate.now()));
        InvestmentAccount account = new InvestmentAccount("ACC-3", employedCustomer(), new BigDecimal("500.00"), "Francistown", AccountStatus.ACTIVE, LocalDate.now());
        account.withdraw(new BigDecimal("100.00"));
        assertEquals(new BigDecimal("400.00"), account.getBalance());
    }

    @Test void chequeAccountRequiresEmployment() {
        assertThrows(IllegalArgumentException.class, () -> new ChequeAccount("ACC-4", unemployedCustomer(), BigDecimal.ZERO, "Francistown", AccountStatus.ACTIVE, LocalDate.now()));
        assertDoesNotThrow(() -> new ChequeAccount("ACC-5", employedCustomer(), BigDecimal.ZERO, "Francistown", AccountStatus.ACTIVE, LocalDate.now()));
    }

    @Test void monthlyRatesAreAppliedCorrectly() {
        SavingsAccount savings = new SavingsAccount("ACC-6", employedCustomer(), new BigDecimal("10000.00"), "Francistown", AccountStatus.ACTIVE, LocalDate.now());
        InvestmentAccount investment = new InvestmentAccount("ACC-7", employedCustomer(), new BigDecimal("1000.00"), "Francistown", AccountStatus.ACTIVE, LocalDate.now());
        assertEquals(new BigDecimal("5.00"), savings.calculateMonthlyInterest());
        assertEquals(new BigDecimal("50.00"), investment.calculateMonthlyInterest());
    }

    @Test void withdrawalsCannotOverdraw() {
        InvestmentAccount account = new InvestmentAccount("ACC-8", employedCustomer(), new BigDecimal("500.00"), "Francistown", AccountStatus.ACTIVE, LocalDate.now());
        assertThrows(IllegalArgumentException.class, () -> account.withdraw(new BigDecimal("500.01")));
    }
}
