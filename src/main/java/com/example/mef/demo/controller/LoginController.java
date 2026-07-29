package com.example.mef.demo.controller;


import com.example.mef.demo.Model.User;
import com.example.mef.demo.Service.AuthService;
import com.example.mef.demo.config.Session;
import com.example.mef.demo.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @Autowired
    private  AuthService authService;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        Optional<User> user = authService.login(username, password);
        if (user.isPresent()) {
            hideError();
            Session.login(user.get());
            SceneManager.switchTo("/fxml/dashboard.fxml", "/css/style.css");
        } else {
            showError("Invalid username or password.");
            passwordField.clear();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
