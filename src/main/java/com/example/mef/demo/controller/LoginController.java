package com.example.mef.demo.controller;

import com.example.mef.demo.Model.User;
import com.example.mef.demo.license.LicenseActivationDialog;
import com.example.mef.demo.service.AuthService;
import com.example.mef.demo.config.Session;
import com.example.mef.demo.enums.UserRole;
import com.example.mef.demo.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class LoginController {

    /* ── shared ── */
    @FXML private Label    formSubtitle;
    @FXML private Label    errorLabel;
    @FXML private Label    successLabel;
    @FXML private Button   primaryButton;
    @FXML private Button   toggleButton;
    @FXML private Label    toggleHint;

    /* ── login mode ── */
    @FXML private VBox          loginFields;
    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;

    /* ── register mode ── */
    @FXML private VBox          registerFields;
    @FXML private TextField     regNameField;
    @FXML private TextField     regEmailField;
    @FXML private PasswordField regPasswordField;
    @FXML private PasswordField regConfirmField;
    @FXML private ComboBox<String> regRoleCombo;

    @Autowired
    private AuthService authService;
    @FXML private VBox     trialBanner;
    @FXML private Button   activateNowButton;
    private boolean registerMode = false;
    @Autowired
    private LicenseActivationDialog licenseActivationDialog;

    private boolean trialExpired = false;
    @FXML
    private void initialize() {
        regRoleCombo.setItems(FXCollections.observableArrayList("USER", "ADMIN"));
        regRoleCombo.setValue("USER");

        trialExpired = !licenseActivationDialog.isUsable();
        if (trialExpired) {
            trialBanner.setVisible(true);
            trialBanner.setManaged(true);
            primaryButton.setDisable(true);
            toggleButton.setDisable(true);
            usernameField.setDisable(true);
            passwordField.setDisable(true);
            formSubtitle.setText("Activation requise pour continuer");
        }
    }

    /* ── toggle between login / register ── */
    @FXML
    private void handleToggleMode() {
        if (trialExpired) return; // block toggling into register mode too

        registerMode = !registerMode;
        clearMessages();

        loginFields.setVisible(!registerMode);
        loginFields.setManaged(!registerMode);
        registerFields.setVisible(registerMode);
        registerFields.setManaged(registerMode);

        if (registerMode) {
            formSubtitle.setText("Create a new account");
            primaryButton.setText("Create Account");
            toggleHint.setText("Already have an account?");
            toggleButton.setText("Sign In");
        } else {
            formSubtitle.setText("Sign in to continue");
            primaryButton.setText("Sign In");
            toggleHint.setText("Don't have an account?");
            toggleButton.setText("Create Account");
        }
    }

    /* ── main action button ── */
    @FXML
    private void handlePrimary() {
        if (trialExpired) return; // safety net even if disable somehow bypassed

        if (registerMode) {
            handleRegister();
        } else {
            handleLogin();
        }
    }

    /* ── login ── */
    private void handleLogin() {
        clearMessages();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        setBusy(true);
        Task<Optional<User>> task = new Task<>() {
            @Override
            protected Optional<User> call() {
                return authService.login(username, password);
            }
        };
        task.setOnSucceeded(e -> {
            setBusy(false);
            Optional<User> user = task.getValue();
            if (user.isPresent()) {
                Session.login(user.get());
                SceneManager.switchTo("/fxml/dashboard.fxml", "/css/style.css");
            } else {
                showError("Invalid email or password.");
                passwordField.clear();
            }
        });
        task.setOnFailed(e -> {
            setBusy(false);
            showError("Login failed: " + task.getException().getMessage());
        });
        startDaemonThread(task);
    }

    /* ── register ── */
    private void handleRegister() {
        clearMessages();

        String name     = regNameField.getText().trim();
        String email    = regEmailField.getText().trim();
        String password = regPasswordField.getText();
        String confirm  = regConfirmField.getText();
        String roleStr  = regRoleCombo.getValue();

        // Validation (fast — stays on FX thread)
        if (name.isEmpty())              { showError("Full name is required.");              return; }
        if (email.isEmpty())             { showError("Email is required.");                  return; }
        if (!email.contains("@"))        { showError("Enter a valid email address.");        return; }
        if (password.length() < 6)       { showError("Password must be at least 6 characters."); return; }
        if (!password.equals(confirm))   { showError("Passwords do not match.");             return; }

        UserRole role = UserRole.valueOf(roleStr);

        setBusy(true);
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return authService.register(name, email, password, role);
            }
        };
        task.setOnSucceeded(e -> {
            setBusy(false);
            if (task.getValue()) {
                showSuccess("Account created! You can now sign in.");
                // Switch back to login and pre-fill email
                registerMode = true;   // trick handleToggleMode into switching back
                handleToggleMode();
                usernameField.setText(email);
            } else {
                showError("An account with that email already exists.");
            }
        });
        task.setOnFailed(e -> {
            setBusy(false);
            showError("Registration failed: " + task.getException().getMessage());
        });
        startDaemonThread(task);
    }
    @FXML
    private void handleActivateNow() {
        SceneManager.switchTo("/fxml/activation.fxml", "/css/style.css");
        ActivationController controller = SceneManager.getApplicationContext()
                .getBean(ActivationController.class);
        controller.setOnActivated(() ->
                SceneManager.switchTo("/fxml/login.fxml", "/css/style.css"));
    }
    /* ── helpers ── */
    private void setBusy(boolean busy) {
        primaryButton.setDisable(busy);
        toggleButton.setDisable(busy);
        if (!busy) {
            primaryButton.setText(registerMode ? "Create Account" : "Sign In");
        } else {
            primaryButton.setText(registerMode ? "Creating…" : "Signing in…");
        }
    }

    private void startDaemonThread(Runnable r) {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.start();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void showSuccess(String msg) {
        successLabel.setText(msg);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
    }

    private void clearMessages() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }
}
