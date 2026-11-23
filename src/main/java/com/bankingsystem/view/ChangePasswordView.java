package com.bankingsystem.view;

import com.bankingsystem.controller.PasswordChangeController;
import com.bankingsystem.domain.Session;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.function.Consumer;


public class ChangePasswordView extends BorderPane {
    public ChangePasswordView(Session session, PasswordChangeController controller,
                              Consumer<Session> onComplete, Runnable logout) {
        setStyle("-fx-background-color: #f3f6fb;");
        VBox card = new VBox(14);
        card.setMaxWidth(470);
        card.setPadding(new Insets(32));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #d9e1ec; -fx-border-radius: 14;");

        Label title = new Label("Set your password");
        title.setStyle("-fx-text-fill: #132b46; -fx-font-size: 25px; -fx-font-weight: bold;");
        Label note = new Label("Your account is using a temporary password. Set a new password to continue.");
        note.setWrapText(true);
        note.setStyle("-fx-text-fill: #62748a;");

        PasswordField current = new PasswordField(); current.setPromptText("Temporary/current password");
        PasswordField next = new PasswordField(); next.setPromptText("New password (at least 10 characters)");
        PasswordField confirm = new PasswordField(); confirm.setPromptText("Confirm new password");
        Label message = new Label(); message.setWrapText(true);
        Button save = new Button("Change password and continue");
        save.setDefaultButton(true);
        save.setMaxWidth(Double.MAX_VALUE);
        save.setStyle("-fx-background-color: #173b63; -fx-text-fill: white; -fx-font-weight: bold;");
        save.setOnAction(event -> {
            char[] currentChars = current.getText().toCharArray();
            char[] nextChars = next.getText().toCharArray();
            char[] confirmChars = confirm.getText().toCharArray();
            try {
                controller.changePassword(session, currentChars, nextChars, confirmChars);
                current.clear(); next.clear(); confirm.clear();
                onComplete.accept(session.afterPasswordChange());
            } catch (Exception ex) {
                message.setText("Password was not changed: " + ex.getMessage());
                next.clear(); confirm.clear();
            } finally {
                java.util.Arrays.fill(currentChars, '\0');
                java.util.Arrays.fill(nextChars, '\0');
                java.util.Arrays.fill(confirmChars, '\0');
            }
        });
        Button signOut = new Button("Sign out");
        signOut.setOnAction(event -> logout.run());
        card.getChildren().addAll(title, new Label("Account: " + session.username()), note,
                current, next, confirm, message, save, signOut);
        setCenter(card);
        BorderPane.setAlignment(card, Pos.CENTER);
        BorderPane.setMargin(card, new Insets(28));
    }
}
