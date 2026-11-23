package com.bankingsystem.service;

import com.bankingsystem.domain.*;
import com.bankingsystem.persistence.AccountDAO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InterestService {
    private final AccountDAO accountDAO;

    public InterestService(AccountDAO accountDAO) {
        this.accountDAO = accountDAO;
    }

    public InterestRunResult processMonthlyInterest(YearMonth period) throws Exception {
        int credited = 0;
        int skipped = 0;
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        List<String> messages = new ArrayList<>();
        for (Account account : accountDAO.findInterestBearingAccounts()) {
            if (YearMonth.from(account.getOpenedOn()).isAfter(period)) {
                skipped++;
                continue;
            }
            BigDecimal interest = ((InterestBearing) account).calculateMonthlyInterest();
            if (interest.signum() <= 0) {
                skipped++;
                continue;
            }
            BigDecimal before = account.getBalance();
            BigDecimal after = before.add(interest).setScale(2, java.math.RoundingMode.HALF_UP);
            String reference = "INT-" + period + "-" + account.getAccountNumber();
            BankTransaction transaction = new BankTransaction(UUID.randomUUID().toString(), account.getAccountNumber(),
                    TransactionType.INTEREST_CREDIT, interest, LocalDateTime.now(), reference, after);
            try {
                accountDAO.commitBalanceChange(account.getAccountNumber(), before, after, transaction);
                credited++;
                total = total.add(interest);
            } catch (java.sql.SQLException ex) {
                if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("unique constraint failed: transactions.reference")) {
                    skipped++;
                    messages.add(account.getAccountNumber() + ": already credited for " + period);
                } else {
                    throw ex;
                }
            }
        }
        return new InterestRunResult(period, credited, skipped, total, messages);
    }

    public List<InterestRunResult> processOutstandingMonths() throws Exception {
        List<Account> accounts = accountDAO.findInterestBearingAccounts();
        if (accounts.isEmpty()) return List.of();
        YearMonth firstMonth = accounts.stream()
                .map(account -> YearMonth.from(account.getOpenedOn()))
                .min(YearMonth::compareTo)
                .orElse(YearMonth.now());
        YearMonth lastCompletedMonth = YearMonth.now().minusMonths(1);
        List<InterestRunResult> results = new ArrayList<>();
        for (YearMonth period = firstMonth; !period.isAfter(lastCompletedMonth); period = period.plusMonths(1)) {
            results.add(processMonthlyInterest(period));
        }
        return results;
    }

    public record InterestRunResult(YearMonth period, int accountsCredited, int accountsSkipped,
                                    BigDecimal totalInterest, List<String> messages) { }
}
