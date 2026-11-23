package com.bankingsystem.view;

import com.bankingsystem.controller.TellerController;
import com.bankingsystem.domain.AccountType;
import com.bankingsystem.domain.Branch;
import com.bankingsystem.domain.Customer;
import com.bankingsystem.domain.TransactionRequest;
import com.bankingsystem.domain.TransactionRequestType;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class TellerDashboardView extends BorderPane {
    private final TellerController controller;
    private final TableView<TransactionRequest> pendingTable = new TableView<>();
    private final Label approvalFeedback = resultLabel();

    public TellerDashboardView(com.bankingsystem.domain.Session session, TellerController controller, Runnable logout) {
        this.controller = controller;
        setPadding(new Insets(18));
        setStyle("-fx-background-color: #f4f7fb;");
        Label heading = new Label("Bank Teller Workspace");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #173b63;");
        Label signedIn = new Label("Signed in as " + session.username());
        Button logoutButton = new Button("Log out");
        logoutButton.setOnAction(e -> logout.run());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(14, heading, signedIn, spacer, logoutButton);
        header.setPadding(new Insets(0, 0, 16, 0));
        setTop(header);

        TabPane tabs = new TabPane();
        tabs.getTabs().add(new Tab("Pending approvals", approvalsTab()));
        tabs.getTabs().add(new Tab("Counter requests", counterTransactionTab()));
        tabs.getTabs().add(new Tab("Register customer", customerRegistrationTab()));
        tabs.getTabs().add(new Tab("Open account", openAccountTab()));
        tabs.getTabs().forEach(t -> t.setClosable(false));
        setCenter(tabs);
        refreshPending();
    }

    private VBox approvalsTab() {
        configurePendingTable();
        Label description = new Label("Review every pending deposit, withdrawal, opening deposit and transfer. Account balances are updated only after approval.");
        description.setWrapText(true);
        Button refresh = new Button("Refresh requests");
        refresh.setOnAction(e -> refreshPending());
        Button approve = new Button("Approve selected");
        approve.setOnAction(e -> reviewSelected(true));
        Button reject = new Button("Reject selected");
        reject.setOnAction(e -> reviewSelected(false));
        HBox actions = new HBox(10, refresh, approve, reject);
        VBox box = new VBox(12, description, pendingTable, actions, approvalFeedback);
        box.setPadding(new Insets(22));
        VBox.setVgrow(pendingTable, Priority.ALWAYS);
        return box;
    }

    private void configurePendingTable() {
        TableColumn<TransactionRequest, String> submitted = new TableColumn<>("Submitted");
        submitted.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().submittedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        TableColumn<TransactionRequest, String> type = new TableColumn<>("Transaction");
        type.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().type().name().replace('_', ' ')));
        TableColumn<TransactionRequest, String> source = new TableColumn<>("Source account");
        source.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().accountNumber()));
        TableColumn<TransactionRequest, String> destination = new TableColumn<>("Destination account");
        destination.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().destinationAccountNumber() == null ? "—" : d.getValue().destinationAccountNumber()));
        TableColumn<TransactionRequest, String> amount = new TableColumn<>("Amount (BWP)");
        amount.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().amount().toPlainString()));
        TableColumn<TransactionRequest, String> user = new TableColumn<>("Submitted by");
        user.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().submittedBy()));
        TableColumn<TransactionRequest, String> reference = new TableColumn<>("Reference");
        reference.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().reference()));
        pendingTable.getColumns().setAll(submitted, type, source, destination, amount, user, reference);
        pendingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        pendingTable.setPlaceholder(new Label("There are no pending transactions to review."));
        pendingTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    private void refreshPending() {
        try {
            pendingTable.setItems(FXCollections.observableArrayList(controller.pendingRequests()));
            approvalFeedback.setText("Pending requests: " + pendingTable.getItems().size());
        } catch (Exception ex) {
            approvalFeedback.setText("Unable to load pending requests: " + ex.getMessage());
        }
    }

    private void reviewSelected(boolean approve) {
        TransactionRequest selected = pendingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            approvalFeedback.setText("Select a transaction request first.");
            return;
        }
        try {
            if (approve) {
                controller.approve(selected.requestId());
                refreshPending();
                approvalFeedback.setText("Transaction approved. Account balances and transaction history have been updated.");
            } else {
                controller.reject(selected.requestId(), "Rejected by teller");
                refreshPending();
                approvalFeedback.setText("Transaction request rejected. No account balance was changed.");
            }
        } catch (Exception ex) {
            approvalFeedback.setText("Unable to review transaction: " + ex.getMessage());
        }
    }

    private VBox customerRegistrationTab() {
        TextField first = field("First name");
        TextField surname = field("Surname");
        TextField address = field("Residential address");
        TextField phone = field("Phone number");
        TextField email = field("Email address");
        CheckBox employed = new CheckBox("Customer is employed");
        TextField employer = field("Employer name");
        TextField employerAddress = field("Employer address");
        employer.disableProperty().bind(employed.selectedProperty().not());
        employerAddress.disableProperty().bind(employed.selectedProperty().not());
        TextField username = field("Login username");
        PasswordField password = new PasswordField();
        password.setPromptText("Temporary password (8+ characters)");
        Button submit = new Button("Register customer");
        Label result = resultLabel();
        submit.setOnAction(e -> {
            try {
                Customer customer = controller.registerCustomer(first.getText(), surname.getText(), address.getText(),
                        phone.getText(), email.getText(), employed.isSelected(), employer.getText(), employerAddress.getText(),
                        username.getText(), password.getText().toCharArray());
                result.setText("Customer registered. Customer ID: " + customer.getCustomerId() + ". Give the customer their login details securely.");
                first.clear(); surname.clear(); address.clear(); phone.clear(); email.clear(); employer.clear(); employerAddress.clear(); username.clear(); password.clear(); employed.setSelected(false);
            } catch (Exception ex) {
                result.setText("Registration failed: " + ex.getMessage());
            }
        });
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        addField(grid, 0, "First name", first);
        addField(grid, 1, "Surname", surname);
        addField(grid, 2, "Address", address);
        addField(grid, 3, "Phone", phone);
        addField(grid, 4, "Email", email);
        grid.add(employed, 1, 5);
        addField(grid, 6, "Employer", employer);
        addField(grid, 7, "Employer address", employerAddress);
        addField(grid, 8, "Username", username);
        addField(grid, 9, "Temporary password", password);
        VBox box = new VBox(14, new Label("Create a customer record and an individual login."), grid, submit, result);
        box.setPadding(new Insets(22));
        return box;
    }

    private VBox openAccountTab() {
        TextField customerId = field("Customer ID from registration");
        ComboBox<AccountType> type = new ComboBox<>();
        type.getItems().setAll(AccountType.values());
        type.getSelectionModel().selectFirst();
        TextField opening = field("Opening deposit (BWP; use 500+ for Investment)");
        ComboBox<Branch> branch = new ComboBox<>();
        branch.getItems().setAll(Branch.values());
        branch.getSelectionModel().select(Branch.FRANCISTOWN);
        branch.setPrefWidth(430);
        Button submit = new Button("Open account");
        Label result = resultLabel();
        submit.setOnAction(e -> {
            try {
                BigDecimal amount = opening.getText().isBlank() ? BigDecimal.ZERO : new BigDecimal(opening.getText().trim());
                var account = controller.openAccount(customerId.getText(), type.getValue(), amount, branch.getValue().code());
                String depositMessage = amount.signum() > 0 ? " Opening deposit request is pending teller approval." : "";
                result.setText("Account opened: " + account.getAccountNumber() + " | " + account.getAccountType() +
                        " | " + account.getBranch() + " (" + account.getBranchCode() + ")." + depositMessage);
                customerId.clear();
                opening.clear();
                refreshPending();
            } catch (Exception ex) {
                result.setText("Account opening rejected: " + ex.getMessage());
            }
        });
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        addField(grid, 0, "Customer ID", customerId);
        addField(grid, 1, "Account type", type);
        addField(grid, 2, "Opening deposit", opening);
        addField(grid, 3, "Branch", branch);
        VBox box = new VBox(14, new Label("Cheque accounts require employment details. Opening deposits are held pending teller approval."), grid, submit, result);
        box.setPadding(new Insets(22));
        return box;
    }

    private VBox counterTransactionTab() {
        TextField account = field("Source account number");
        TextField destination = field("Destination account for transfer");
        TextField amount = field("Amount (BWP)");
        Button deposit = new Button("Submit deposit request");
        Button withdraw = new Button("Submit withdrawal request");
        Button transfer = new Button("Submit transfer request");
        Label result = resultLabel();
        deposit.setOnAction(e -> counterTransaction(account, destination, amount, TransactionRequestType.DEPOSIT, result));
        withdraw.setOnAction(e -> counterTransaction(account, destination, amount, TransactionRequestType.WITHDRAWAL, result));
        transfer.setOnAction(e -> counterTransaction(account, destination, amount, TransactionRequestType.TRANSFER, result));
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        addField(grid, 0, "Source account", account);
        addField(grid, 1, "Destination account", destination);
        addField(grid, 2, "Amount", amount);
        VBox box = new VBox(14, new Label("Submit a customer's counter transaction. Every request must be approved from the Pending approvals tab before funds move."),
                grid, new HBox(10, deposit, withdraw, transfer), result);
        box.setPadding(new Insets(22));
        return box;
    }

    private void counterTransaction(TextField account, TextField destination, TextField amount,
                                    TransactionRequestType type, Label result) {
        try {
            BigDecimal value = new BigDecimal(amount.getText().trim());
            TransactionRequest request = switch (type) {
                case DEPOSIT -> controller.submitDepositForCustomer(account.getText().trim(), value);
                case WITHDRAWAL -> controller.submitWithdrawalForCustomer(account.getText().trim(), value);
                case TRANSFER -> controller.submitTransferForCustomer(account.getText().trim(), destination.getText().trim(), value);
                case OPENING_DEPOSIT -> throw new IllegalArgumentException("Opening deposits are created during account opening.");
            };
            result.setText("Request submitted: " + request.reference() + ". Refresh Pending approvals to review it.");
            amount.clear();
            destination.clear();
            refreshPending();
        } catch (Exception ex) {
            result.setText("Transaction request rejected: " + ex.getMessage());
        }
    }

    private static TextField field(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefWidth(430);
        return field;
    }

    private static Label resultLabel() {
        Label label = new Label();
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: #245c43;");
        return label;
    }

    private static void addField(GridPane grid, int row, String label, Control field) {
        grid.add(new Label(label), 0, row);
        grid.add(field, 1, row);
    }
}
