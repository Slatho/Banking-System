package com.bankingsystem.app;

import javafx.application.Application;









public final class BankingApplication {
    private BankingApplication() {
        
    }

    public static void main(String[] args) {
        Application.launch(BankingWindow.class, args);
    }
}
