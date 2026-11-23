package com.bankingsystem.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class MonthlyInterestScheduler implements AutoCloseable {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "monthly-interest-scheduler");
        thread.setDaemon(true);
        return thread;
    });
    private final InterestService interestService;

    public MonthlyInterestScheduler(InterestService interestService) {
        this.interestService = interestService;
    }

    public void start() {
        try {
            interestService.processOutstandingMonths();
        } catch (Exception ex) {
            System.err.println("Automatic interest catch-up failed: " + ex.getMessage());
        }
        scheduleNextRun();
    }

    private void scheduleNextRun() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.toLocalDate().withDayOfMonth(1).plusMonths(1).atStartOfDay().plusMinutes(5);
        long delay = Math.max(1, Duration.between(now, next).toMillis());
        executor.schedule(() -> {
            try {
                interestService.processOutstandingMonths();
            } catch (Exception ex) {
                System.err.println("Automatic monthly interest processing failed: " + ex.getMessage());
            } finally {
                scheduleNextRun();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
