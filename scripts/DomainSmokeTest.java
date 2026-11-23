import com.bankingsystem.domain.*;
import com.bankingsystem.security.PasswordHasher;
import java.math.BigDecimal;
import java.time.LocalDate;

public class DomainSmokeTest {
    private static int passed = 0;
    private static void check(String name, boolean ok) {
        if (!ok) throw new AssertionError("FAILED: " + name);
        passed++;
        System.out.println("PASS: " + name);
    }
    private static void expectThrows(String name, Class<? extends Throwable> type, Runnable action) {
        try { action.run(); }
        catch (Throwable ex) { check(name, type.isInstance(ex)); return; }
        check(name, false);
    }
    public static void main(String[] args) {
        Customer employed = new Customer("CUS-1", "Kabelo", "Dube", "Francistown", "71234567", "kabelo@example.bw", true, "GRS", "Francistown");
        Customer unemployed = new Customer("CUS-2", "Mpho", "Pheto", "Francistown", "71234568", "mpho@example.bw", false, null, null);
        SavingsAccount savings = new SavingsAccount("ACC-1", employed, new BigDecimal("10000.00"), "Francistown", AccountStatus.ACTIVE, LocalDate.now());
        savings.deposit(new BigDecimal("50.00"));
        check("Savings deposit raises the balance", savings.getBalance().compareTo(new BigDecimal("10050.00")) == 0);
        expectThrows("Savings withdrawal is rejected", UnsupportedOperationException.class, () -> savings.withdraw(new BigDecimal("10.00")));
        check("Savings interest uses 0.05 percent", savings.calculateMonthlyInterest().compareTo(new BigDecimal("5.03")) == 0);
        InvestmentAccount investment = new InvestmentAccount("ACC-2", employed, new BigDecimal("1000.00"), "Francistown", AccountStatus.ACTIVE, LocalDate.now());
        check("Investment interest uses 5 percent", investment.calculateMonthlyInterest().compareTo(new BigDecimal("50.00")) == 0);
        investment.withdraw(new BigDecimal("100.00"));
        check("Investment withdrawal succeeds when funded", investment.getBalance().compareTo(new BigDecimal("900.00")) == 0);
        expectThrows("Investment opening deposit below BWP 500 is rejected", IllegalArgumentException.class, () -> InvestmentAccount.openNew("ACC-3", employed, new BigDecimal("499.99"), "Francistown", AccountStatus.ACTIVE, LocalDate.now()));
        expectThrows("Cheque account rejects unemployed customer", IllegalArgumentException.class, () -> new ChequeAccount("ACC-4", unemployed, BigDecimal.ZERO, "Francistown", AccountStatus.ACTIVE, LocalDate.now()));
        expectThrows("Overdraft is rejected", IllegalArgumentException.class, () -> investment.withdraw(new BigDecimal("999.00")));
        String hash = PasswordHasher.hash("ProjectPass@2026".toCharArray());
        check("Correct password verifies against PBKDF2 hash", PasswordHasher.verify("ProjectPass@2026".toCharArray(), hash));
        check("Incorrect password fails verification", !PasswordHasher.verify("wrong-password".toCharArray(), hash));
        check("Password hash does not contain plaintext password", !hash.contains("ProjectPass@2026"));
        System.out.println("RESULT: " + passed + " checks passed.");
    }
}
