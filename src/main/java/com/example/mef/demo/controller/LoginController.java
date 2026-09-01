package com.example.mef.demo.controller;

import com.example.mef.demo.Model.User;
import com.example.mef.demo.license.LicenseActivationDialog;
import com.example.mef.demo.service.AuthService;
import com.example.mef.demo.config.Session;
import com.example.mef.demo.enums.UserRole;
import com.example.mef.demo.util.I18n;
import com.example.mef.demo.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Component
public class LoginController {

    /* ── shared ── */
    @FXML private HBox     rootPane;
    @FXML private Label    brandLabel;
    @FXML private Label    brandSubtitleLabel;
    @FXML private Button   frButton;
    @FXML private Button   arButton;
    @FXML private Label    formSubtitle;
    @FXML private Label    errorLabel;
    @FXML private Label    successLabel;
    @FXML private Button   primaryButton;
    @FXML private Button   toggleButton;
    @FXML private Label    toggleHint;

    /* ── login mode ── */
    @FXML private VBox          loginFields;
    @FXML private Label         emailLabel;
    @FXML private TextField     usernameField;
    @FXML private Label         passwordLabel;
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
    @FXML private Label    trialBannerTitle;
    @FXML private Label    trialBannerMessage;
    @FXML private Button   activateNowButton;
    private boolean registerMode = false;
    @Autowired
    private LicenseActivationDialog licenseActivationDialog;

    private boolean trialExpired = false;

    @FXML
    private void initialize() {
        regRoleCombo.setItems(FXCollections.observableArrayList("USER", "ADMIN"));
        regRoleCombo.setValue("USER");
        regRoleCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(String role) {
                if (role == null) return "";
                return "ADMIN".equals(role) ? I18n.t("login.role_admin", "تسجيل الحضور") : I18n.t("login.role_user", "تسجيل الحضور");
            }

            @Override
            public String fromString(String string) {
                return string;
            }
        });

        trialExpired = !licenseActivationDialog.isUsable();

        I18n.setLocale(Locale.FRENCH);
        applyLocale();

        if (trialExpired) {
            trialBanner.setVisible(true);
            trialBanner.setManaged(true);
            primaryButton.setDisable(true);
            toggleButton.setDisable(true);
            usernameField.setDisable(true);
            passwordField.setDisable(true);
            formSubtitle.setText(I18n.t("login.subtitle_activation_required", "تسجيل الحضور"));
        }
    }

    /* ── language switcher ── */
    @FXML
    private void handleLangFr() {
        I18n.setLocale(Locale.FRENCH);
        applyLocale();
    }

    @FXML
    private void handleLangAr() {
        I18n.setLocale(new Locale("ar"));
        applyLocale();
    }

    private void applyLocale() {
        boolean rtl = I18n.isRTL();
        if (rootPane != null) {
            rootPane.setNodeOrientation(rtl ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
        }

        if (frButton != null && arButton != null) {
            frButton.getStyleClass().setAll(rtl ? "lang-button" : "lang-button-active");
            arButton.getStyleClass().setAll(rtl ? "lang-button-active" : "lang-button");
        }

        brandLabel.setText(I18n.t("brand", "تسجيل الحضور"));
        brandSubtitleLabel.setText(I18n.t("brand.subtitle", "تسجيل الحضور"));

        emailLabel.setText(I18n.t("field.email", "تسجيل الحضور"));
        usernameField.setPromptText(I18n.t("field.email", "تسجيل الحضور"));
        passwordLabel.setText(I18n.t("field.password", "تسجيل الحضور"));
        passwordField.setPromptText(I18n.t("field.password", "تسجيل الحضور"));

        regNameField.setPromptText(I18n.t("field.full_name", "تسجيل الحضور"));
        regEmailField.setPromptText(I18n.t("field.email", "تسجيل الحضور"));
        regPasswordField.setPromptText(I18n.t("field.password", "تسجيل الحضور"));
        regConfirmField.setPromptText(I18n.t("login.confirm_password", "تسجيل الحضور"));
        regRoleCombo.setPromptText(I18n.t("field.role", "تسجيل الحضور"));
        // force the combo box to re-render its selected/cell text with the new converter
        String currentRole = regRoleCombo.getValue();
        regRoleCombo.setValue(null);
        regRoleCombo.setValue(currentRole);

        trialBannerTitle.setText(I18n.t("login.trial_banner_title", "تسجيل الحضور"));
        trialBannerMessage.setText(I18n.t("login.trial_banner_message", "تسجيل الحضور"));
        activateNowButton.setText(I18n.t("login.activate_now", "تسجيل الحضور"));

        if (trialExpired) {
            formSubtitle.setText(I18n.t("login.subtitle_activation_required", "تسجيل الحضور"));
        } else if (registerMode) {
            formSubtitle.setText(I18n.t("login.subtitle_register", "تسجيل الحضور"));
            primaryButton.setText(I18n.t("login.register_button", "تسجيل الحضور"));
            toggleHint.setText(I18n.t("login.have_account", "تسجيل الحضور"));
            toggleButton.setText(I18n.t("login.signin_link", "تسجيل الحضور"));
        } else {
            formSubtitle.setText(I18n.t("login.subtitle_signin", "تسجيل الحضور"));
            primaryButton.setText(I18n.t("login.signin_button", "تسجيل الحضور"));
            toggleHint.setText(I18n.t("login.no_account", "تسجيل الحضور"));
            toggleButton.setText(I18n.t("login.register_link", "تسجيل الحضور"));
        }

        if (rootPane != null) {
            I18n.applyArabicFontRecursively(rootPane, rtl);
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
            formSubtitle.setText(I18n.t("login.subtitle_register", "تسجيل الحضور"));
            primaryButton.setText(I18n.t("login.register_button", "تسجيل الحضور"));
            toggleHint.setText(I18n.t("login.have_account", "تسجيل الحضور"));
            toggleButton.setText(I18n.t("login.signin_link", "تسجيل الحضور"));
        } else {
            formSubtitle.setText(I18n.t("login.subtitle_signin", "تسجيل الحضور"));
            primaryButton.setText(I18n.t("login.signin_button", "تسجيل الحضور"));
            toggleHint.setText(I18n.t("login.no_account", "تسجيل الحضور"));
            toggleButton.setText(I18n.t("login.register_link", "تسجيل الحضور"));
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
                showError(I18n.t("login.error_invalid_credentials", "تسجيل الحضور"));
                passwordField.clear();
            }
        });
        task.setOnFailed(e -> {
            setBusy(false);
            showError(I18n.t("login.error_login_failed", "تسجيل الحضور") + " " + task.getException().getMessage());
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
        if (name.isEmpty())              { showError(I18n.t("login.error_name_required", "تسجيل الحضور"));         return; }
        if (email.isEmpty())             { showError(I18n.t("login.error_email_required", "تسجيل الحضور"));         return; }
        if (!email.contains("@"))        { showError(I18n.t("login.error_email_invalid", "تسجيل الحضور"));          return; }
        if (password.length() < 6)       { showError(I18n.t("login.error_password_length", "تسجيل الحضور"));        return; }
        if (!password.equals(confirm))   { showError(I18n.t("login.error_password_mismatch", "تسجيل الحضور"));      return; }

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
                showSuccess(I18n.t("login.success_account_created", "تسجيل الحضور"));
                // Switch back to login and pre-fill email
                registerMode = true;   // trick handleToggleMode into switching back
                handleToggleMode();
                usernameField.setText(email);
            } else {
                showError(I18n.t("login.error_email_exists", "تسجيل الحضور"));
            }
        });
        task.setOnFailed(e -> {
            setBusy(false);
            showError(I18n.t("login.error_register_failed", "تسجيل الحضور") + " " + task.getException().getMessage());
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
            primaryButton.setText(registerMode ? I18n.t("login.register_button", "تسجيل الحضور") : I18n.t("login.signin_button", "تسجيل الحضور"));
        } else {
            primaryButton.setText(registerMode ? I18n.t("login.creating", "تسجيل الحضور") : I18n.t("login.signing_in", "تسجيل الحضور"));
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
