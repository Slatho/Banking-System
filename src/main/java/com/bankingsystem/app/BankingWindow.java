package com.bankingsystem.app;

import com.bankingsystem.controller.CustomerController;
import com.bankingsystem.controller.LoginController;
import com.bankingsystem.controller.ManagerController;
import com.bankingsystem.controller.PasswordChangeController;
import com.bankingsystem.controller.TellerController;
import com.bankingsystem.domain.Session;
import com.bankingsystem.domain.UserRole;
import com.bankingsystem.persistence.AccountDAO;
import com.bankingsystem.persistence.CredentialDAO;
import com.bankingsystem.persistence.CustomerDAO;
import com.bankingsystem.persistence.Database;
import com.bankingsystem.persistence.RegistrationDAO;
import com.bankingsystem.persistence.SeedData;
import com.bankingsystem.persistence.TransactionDAO;
import com.bankingsystem.service.AuthService;
import com.bankingsystem.service.BankingService;
import com.bankingsystem.service.InterestService;
import com.bankingsystem.service.MonthlyInterestScheduler;
import com.bankingsystem.view.CustomerDashboardView;
import com.bankingsystem.view.LoginView;
import com.bankingsystem.view.ManagerDashboardView;
import com.bankingsystem.view.ChangePasswordView;
import com.bankingsystem.view.TellerDashboardView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;


public final class BankingWindow extends Application {
    private Stage stage;
    private BankingService bankingService;
    private InterestService interestService;
    private LoginController loginController;
    private MonthlyInterestScheduler monthlyScheduler;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.stage = primaryStage;
        Database.initialize();
        SeedData.seed();

        CustomerDAO customers = new CustomerDAO();
        AccountDAO accounts = new AccountDAO();
        TransactionDAO transactions = new TransactionDAO();

        bankingService = new BankingService(
                customers, accounts, transactions, new RegistrationDAO()
        );
        interestService = new InterestService(accounts);
        monthlyScheduler = new MonthlyInterestScheduler(interestService);
        monthlyScheduler.start();
        loginController = new LoginController(new AuthService(new CredentialDAO(), customers));

        showLogin();
        stage.setTitle("Banking System");
        stage.setMinWidth(880);
        stage.setMinHeight(620);
        stage.setOnCloseRequest(event -> monthlyScheduler.close());
        stage.show();
    }

    private void showLogin() {
        LoginView view = new LoginView(loginController, this::showDashboard);
        stage.setScene(new Scene(view, 980, 680));
    }

    private void showDashboard(Session session) {
        if (session.mustChangePassword()) {
            PasswordChangeController passwordController = new PasswordChangeController(new CredentialDAO());
            stage.setScene(new Scene(new ChangePasswordView(session, passwordController,
                    this::showDashboard, this::showLogin), 980, 700));
            return;
        }

        if (session.role() == UserRole.MANAGER) {
            ManagerController controller = new ManagerController(session, new CredentialDAO());
            stage.setScene(new Scene(new ManagerDashboardView(session, controller, this::showLogin), 1120, 800));
        } else if (session.role() == UserRole.TELLER) {
            TellerController controller = new TellerController(bankingService, session);
            stage.setScene(new Scene(
                    new TellerDashboardView(session, controller, this::showLogin), 1100, 760
            ));
        } else {
            CustomerController controller = new CustomerController(bankingService, session);
            stage.setScene(new Scene(
                    new CustomerDashboardView(session, controller, this::showLogin), 1100, 760
            ));
        }
    }
}
