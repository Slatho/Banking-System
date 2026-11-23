package com.bankingsystem.view;

import com.bankingsystem.controller.LoginController;
import com.bankingsystem.domain.Session;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.function.Consumer;


public class LoginView extends BorderPane {
    public LoginView(LoginController controller, Consumer<Session> onLogin) {
        setStyle("-fx-background-color: #f3f6fb;");
        VBox card = new VBox(16);
        card.setMaxWidth(390);
        card.setPadding(new Insets(34));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #d9e1ec; -fx-border-radius: 14;");

        Label badge = new Label("BANKING SERVICES");
        badge.setStyle("-fx-text-fill: #486581; -fx-font-size: 10px; -fx-font-weight: bold;");
        Label title = new Label("Banking System");
        title.setStyle("-fx-text-fill: #132b46; -fx-font-size: 29px; -fx-font-weight: bold;");
        Label subtitle = new Label("Sign in using your assigned banking credentials.");
        subtitle.setStyle("-fx-text-fill: #62748a; -fx-font-size: 13px;");

        TextField username = new TextField();
        username.setPromptText("Username");
        username.setPrefHeight(42);
        PasswordField password = new PasswordField();
        password.setPromptText("Password");
        password.setPrefHeight(42);
        Label message = new Label();
        message.setStyle("-fx-text-fill: #b42318;");
        Button login = new Button("Sign in");
        login.setDefaultButton(true);
        login.setMaxWidth(Double.MAX_VALUE);
        login.setPrefHeight(42);
        login.setStyle("-fx-background-color: #173b63; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6;");
        login.setOnAction(event -> {
            try {
                var result = controller.login(username.getText(), password.getText().toCharArray());
                if (result.isPresent()) {
                    password.clear();
                    onLogin.accept(result.get());
                } else {
                    message.setText("Login was not successful. Check your details or account status.");
                    password.clear();
                }
            } catch (Exception ex) {
                message.setText("Unable to sign in: " + ex.getMessage());
            }
        });
        card.getChildren().addAll(badge, title, subtitle, new Label("Username"), username,
                new Label("Password"), password, message, login);
        setCenter(card);
        BorderPane.setAlignment(card, Pos.CENTER);
        BorderPane.setMargin(card, new Insets(30));
    }
}
