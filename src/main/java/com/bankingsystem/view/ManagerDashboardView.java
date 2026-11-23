package com.bankingsystem.view;

import com.bankingsystem.controller.ManagerController;
import com.bankingsystem.domain.Session;
import com.bankingsystem.domain.UserCredential;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;


public class ManagerDashboardView extends BorderPane {
    private final ManagerController controller;
    private final TableView<UserCredential> tellerTable = new TableView<>();

    public ManagerDashboardView(Session session, ManagerController controller, Runnable logout) {
        this.controller = controller;
        setPadding(new Insets(18));
        setStyle("-fx-background-color: #f4f7fb;");

        Label heading = new Label("Staff Administration");
        heading.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #173b63;");
        Label signedIn = new Label("Manager: " + session.username());
        Button logoutButton = new Button("Log out");
        logoutButton.setOnAction(event -> logout.run());
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(14, heading, signedIn, spacer, logoutButton);
        header.setPadding(new Insets(0, 0, 16, 0));
        setTop(header);

        TabPane tabs = new TabPane();
        tabs.getTabs().add(new Tab("Teller accounts", tellerManagementTab()));
        tabs.getTabs().forEach(tab -> tab.setClosable(false));
        setCenter(tabs);
        refreshTellers();
    }

    private ScrollPane tellerManagementTab() {
        Label introduction = new Label("Create teller logins, review staff access, deactivate or reactivate accounts, and issue temporary passwords.");
        introduction.setWrapText(true);

        TextField username = new TextField();
        username.setPromptText("Username (4–24 characters)");
        username.setPrefWidth(280);
        PasswordField temporaryPassword = new PasswordField();
        temporaryPassword.setPromptText("Temporary password (10+ characters)");
        temporaryPassword.setPrefWidth(280);
        Button create = new Button("Create teller account");
        Label createMessage = messageLabel();
        create.setOnAction(event -> {
            char[] password = temporaryPassword.getText().toCharArray();
            try {
                UserCredential created = controller.createTeller(username.getText(), password);
                createMessage.setText("Teller '" + created.username() + "' created. Give the temporary password to the teller securely; they must change it at first sign-in.");
                username.clear(); temporaryPassword.clear();
                refreshTellers();
            } catch (Exception ex) {
                createMessage.setText("Could not create teller: " + ex.getMessage());
            } finally {
                java.util.Arrays.fill(password, '\0');
            }
        });
        GridPane createGrid = new GridPane();
        createGrid.setHgap(12); createGrid.setVgap(10);
        createGrid.add(new Label("Username"), 0, 0); createGrid.add(username, 1, 0);
        createGrid.add(new Label("Temporary password"), 0, 1); createGrid.add(temporaryPassword, 1, 1);
        VBox createBox = new VBox(10, sectionTitle("Create a teller account"), createGrid, create, createMessage);
        createBox.setPadding(new Insets(16));
        createBox.setStyle("-fx-background-color: white; -fx-background-radius: 9; -fx-border-color: #dce4ee; -fx-border-radius: 9;");

        configureTable();
        tellerTable.setPrefHeight(300);
        Button refresh = new Button("Refresh list");
        refresh.setOnAction(event -> refreshTellers());
        Button toggleStatus = new Button("Deactivate / reactivate selected");
        Label statusMessage = messageLabel();
        toggleStatus.setOnAction(event -> {
            UserCredential selected = tellerTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                statusMessage.setText("Select a teller from the table first.");
                return;
            }
            try {
                if (selected.active()) controller.deactivateTeller(selected.username());
                else controller.reactivateTeller(selected.username());
                statusMessage.setText("Teller '" + selected.username() + "' is now " + (selected.active() ? "inactive" : "active") + ".");
                refreshTellers();
            } catch (Exception ex) {
                statusMessage.setText("Could not update teller account: " + ex.getMessage());
            }
        });
        VBox listBox = new VBox(10, sectionTitle("Registered tellers"), tellerTable,
                new HBox(10, refresh, toggleStatus), statusMessage);
        listBox.setPadding(new Insets(16));
        listBox.setStyle("-fx-background-color: white; -fx-background-radius: 9; -fx-border-color: #dce4ee; -fx-border-radius: 9;");

        PasswordField resetPassword = new PasswordField();
        resetPassword.setPromptText("New temporary password (10+ characters)");
        resetPassword.setPrefWidth(280);
        Button reset = new Button("Reset selected teller password");
        Label resetMessage = messageLabel();
        reset.setOnAction(event -> {
            UserCredential selected = tellerTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                resetMessage.setText("Select a teller from the table first.");
                return;
            }
            char[] password = resetPassword.getText().toCharArray();
            try {
                controller.resetTellerPassword(selected.username(), password);
                resetMessage.setText("A temporary password has been set for '" + selected.username() + "'. They must change it at next sign-in.");
                resetPassword.clear();
                refreshTellers();
            } catch (Exception ex) {
                resetMessage.setText("Password reset failed: " + ex.getMessage());
            } finally {
                java.util.Arrays.fill(password, '\0');
            }
        });
        VBox resetBox = new VBox(10, sectionTitle("Reset a teller password"),
                new Label("Select a teller above, enter a temporary password, then reset the account."),
                new HBox(10, resetPassword, reset), resetMessage);
        resetBox.setPadding(new Insets(16));
        resetBox.setStyle("-fx-background-color: white; -fx-background-radius: 9; -fx-border-color: #dce4ee; -fx-border-radius: 9;");

        VBox content = new VBox(14, introduction, createBox, listBox, resetBox);
        content.setPadding(new Insets(18));
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #f4f7fb; -fx-background-color: #f4f7fb;");
        return scrollPane;
    }

    private void configureTable() {
        TableColumn<UserCredential, String> username = new TableColumn<>("Username");
        username.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().username()));
        username.setPrefWidth(200);

        TableColumn<UserCredential, String> status = new TableColumn<>("Status");
        status.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().active() ? "Active" : "Inactive"));
        status.setPrefWidth(120);

        TableColumn<UserCredential, String> created = new TableColumn<>("Created");
        created.setCellValueFactory(data -> new SimpleStringProperty(formatDate(data.getValue().createdAt())));
        created.setPrefWidth(190);

        tellerTable.getColumns().setAll(username, status, created);
        tellerTable.setPlaceholder(new Label("No teller accounts have been created yet."));
        tellerTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    private void refreshTellers() {
        try {
            tellerTable.getItems().setAll(controller.listTellers());
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Unable to load teller accounts: " + ex.getMessage(), ButtonType.OK);
            alert.setHeaderText("Teller list unavailable");
            alert.showAndWait();
        }
    }

    private static String formatDate(String value) {
        if (value == null || value.isBlank()) return "—";
        try { return LocalDateTime.parse(value).toLocalDate().toString(); }
        catch (DateTimeParseException ignored) { return value; }
    }

    private static Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #173b63;");
        return label;
    }

    private static Label messageLabel() {
        Label label = new Label();
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: #245c43;");
        return label;
    }
}
