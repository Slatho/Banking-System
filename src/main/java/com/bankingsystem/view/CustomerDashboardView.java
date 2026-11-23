package com.bankingsystem.view;

import com.bankingsystem.controller.CustomerController;
import com.bankingsystem.domain.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CustomerDashboardView extends BorderPane {
    private final CustomerController controller;
    private final ObservableList<Account> accountRows = FXCollections.observableArrayList();
    private final ObservableList<BankTransaction> transactionRows = FXCollections.observableArrayList();
    private final ObservableList<TransactionRequest> requestRows = FXCollections.observableArrayList();
    private final TableView<Account> accountsTable = new TableView<>(accountRows);
    private final TableView<BankTransaction> transactionsTable = new TableView<>(transactionRows);
    private final TableView<TransactionRequest> requestsTable = new TableView<>(requestRows);
    private final ComboBox<Account> accountPicker = new ComboBox<>();
    private final TextField amountField = new TextField();
    private final TextField destinationField = new TextField();
    private final Label feedback = new Label();

    public CustomerDashboardView(Session session, CustomerController controller, Runnable logout) {
        this.controller = controller;
        setPadding(new Insets(18));
        setStyle("-fx-background-color: #f4f7fb;");
        Label title = new Label("Welcome, " + session.customer().getFullName());
        title.setStyle("-fx-font-size: 23px; -fx-font-weight: bold; -fx-text-fill: #173b63;");
        Button logoutButton = new Button("Log out");
        logoutButton.setOnAction(e -> logout.run());
        HBox header = new HBox(12, title, new Region(), logoutButton);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setPadding(new Insets(0, 0, 16, 0));
        setTop(header);

        configureAccountTable();
        configureTransactionTable();
        configureRequestsTable();

        VBox accountsBox = new VBox(8, new Label("Your accounts"), accountsTable);
        VBox.setVgrow(accountsTable, Priority.ALWAYS);
        VBox transactionsBox = new VBox(8, new Label("Approved transaction history"), transactionsTable);
        VBox.setVgrow(transactionsTable, Priority.ALWAYS);
        SplitPane split = new SplitPane(accountsBox, transactionsBox);
        split.setDividerPositions(0.48);

        accountPicker.setPromptText("Select an account");
        accountPicker.setPrefWidth(230);
        accountPicker.setOnAction(e -> loadTransactions());
        amountField.setPromptText("Amount (BWP)");
        amountField.setPrefWidth(145);
        destinationField.setPromptText("Destination account for transfer");
        destinationField.setPrefWidth(240);

        Button deposit = new Button("Request deposit");
        Button withdraw = new Button("Request withdrawal");
        Button transfer = new Button("Request transfer");
        deposit.setOnAction(e -> performTransaction(TransactionRequestType.DEPOSIT));
        withdraw.setOnAction(e -> performTransaction(TransactionRequestType.WITHDRAWAL));
        transfer.setOnAction(e -> performTransaction(TransactionRequestType.TRANSFER));
        HBox actions = new HBox(8, new Label("Account"), accountPicker, amountField, destinationField, deposit, withdraw, transfer);
        actions.setPadding(new Insets(14, 0, 5, 0));
        feedback.setWrapText(true);
        feedback.setStyle("-fx-text-fill: #1f5b41;");

        VBox accountContent = new VBox(8, split, actions, feedback);
        VBox.setVgrow(split, Priority.ALWAYS);
        accountContent.setPadding(new Insets(10));
        ScrollPane accountsScroll = new ScrollPane(accountContent);
        accountsScroll.setFitToWidth(true);
        accountsScroll.setFitToHeight(true);

        VBox requestContent = new VBox(10, new Label("Track submitted transactions. Pending requests do not change account balances until a teller approves them."), requestsTable);
        requestContent.setPadding(new Insets(16));
        VBox.setVgrow(requestsTable, Priority.ALWAYS);
        TabPane tabs = new TabPane();
        Tab accountsTab = new Tab("Accounts and transactions", accountsScroll);
        Tab requestsTab = new Tab("My transaction requests", requestContent);
        accountsTab.setClosable(false);
        requestsTab.setClosable(false);
        tabs.getTabs().setAll(accountsTab, requestsTab);
        setCenter(tabs);
        loadAccounts();
        loadRequests();
    }

    private void configureAccountTable() {
        TableColumn<Account, String> number = new TableColumn<>("Account number");
        number.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAccountNumber()));
        TableColumn<Account, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAccountType().name()));
        TableColumn<Account, String> balance = new TableColumn<>("Balance (BWP)");
        balance.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBalance().toPlainString()));
        TableColumn<Account, String> branch = new TableColumn<>("Branch");
        branch.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBranch() + " (" + d.getValue().getBranchCode() + ")"));
        TableColumn<Account, String> status = new TableColumn<>("Status");
        status.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus().name()));
        accountsTable.getColumns().setAll(List.of(number, type, balance, branch, status));
        accountsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        accountsTable.setPrefHeight(230);
    }

    private void configureTransactionTable() {
        TableColumn<BankTransaction, String> date = new TableColumn<>("Date / time");
        date.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().occurredAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        TableColumn<BankTransaction, String> type = new TableColumn<>("Type");
        type.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().type().name().replace('_', ' ')));
        TableColumn<BankTransaction, String> amount = new TableColumn<>("Amount (BWP)");
        amount.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().amount().toPlainString()));
        TableColumn<BankTransaction, String> balance = new TableColumn<>("Balance after");
        balance.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().balanceAfter().toPlainString()));
        transactionsTable.getColumns().setAll(List.of(date, type, amount, balance));
        transactionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void configureRequestsTable() {
        TableColumn<TransactionRequest, String> date = new TableColumn<>("Submitted");
        date.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().submittedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        TableColumn<TransactionRequest, String> type = new TableColumn<>("Request");
        type.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().type().name().replace('_', ' ')));
        TableColumn<TransactionRequest, String> account = new TableColumn<>("Account");
        account.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().accountNumber()));
        TableColumn<TransactionRequest, String> destination = new TableColumn<>("Destination");
        destination.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().destinationAccountNumber() == null ? "—" : d.getValue().destinationAccountNumber()));
        TableColumn<TransactionRequest, String> amount = new TableColumn<>("Amount (BWP)");
        amount.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().amount().toPlainString()));
        TableColumn<TransactionRequest, String> status = new TableColumn<>("Status");
        status.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status().name()));
        TableColumn<TransactionRequest, String> review = new TableColumn<>("Teller / note");
        review.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().reviewNote() == null ?
                (d.getValue().reviewedBy() == null ? "Awaiting teller" : d.getValue().reviewedBy()) : d.getValue().reviewNote()));
        requestsTable.getColumns().setAll(List.of(date, type, account, destination, amount, status, review));
        requestsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        requestsTable.setPlaceholder(new Label("No transaction requests have been submitted."));
    }

    private void loadAccounts() {
        try {
            accountRows.setAll(controller.accounts());
            accountPicker.setItems(FXCollections.observableArrayList(accountRows));
            accountPicker.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(Account account) {
                    return account == null ? "" : account.getAccountNumber() + " - " + account.getAccountType().name();
                }
                @Override public Account fromString(String value) { return null; }
            });
            if (!accountRows.isEmpty()) accountPicker.getSelectionModel().selectFirst();
            else transactionRows.clear();
        } catch (Exception ex) {
            error("Could not load accounts", ex);
        }
    }

    private void loadTransactions() {
        Account selected = accountPicker.getValue();
        if (selected == null) {
            transactionRows.clear();
            return;
        }
        try {
            transactionRows.setAll(controller.transactions(selected.getAccountNumber()));
        } catch (Exception ex) {
            error("Could not load transaction history", ex);
        }
    }

    private void loadRequests() {
        try {
            requestRows.setAll(controller.requests());
        } catch (Exception ex) {
            error("Could not load transaction requests", ex);
        }
    }

    private void performTransaction(TransactionRequestType type) {
        Account selected = accountPicker.getValue();
        if (selected == null) {
            feedback.setText("Select an account first.");
            return;
        }
        try {
            BigDecimal amount = new BigDecimal(amountField.getText().trim());
            TransactionRequest request = switch (type) {
                case DEPOSIT -> controller.deposit(selected.getAccountNumber(), amount);
                case WITHDRAWAL -> controller.withdraw(selected.getAccountNumber(), amount);
                case TRANSFER -> controller.transfer(selected.getAccountNumber(), destinationField.getText().trim(), amount);
                case OPENING_DEPOSIT -> throw new IllegalArgumentException("Opening deposits are handled when an account is opened.");
            };
            feedback.setText(type.name().replace('_', ' ') + " request submitted. Reference: " + request.reference() + ". A teller must approve it before the balance changes.");
            amountField.clear();
            destinationField.clear();
            loadAccounts();
            loadTransactions();
            loadRequests();
        } catch (Exception ex) {
            feedback.setText("Request not submitted: " + ex.getMessage());
        }
    }

    private void error(String title, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK);
        alert.setHeaderText(title);
        alert.showAndWait();
    }
}
