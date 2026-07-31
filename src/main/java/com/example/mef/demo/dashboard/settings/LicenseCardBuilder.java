package com.example.mef.demo.dashboard.settings;

import com.example.mef.demo.license.LicenseActivationDialog;
import com.example.mef.demo.license.LicenseValidator;
import com.example.mef.demo.license.MachineIdentifier;
import com.example.mef.demo.license.SettingsRepository;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Builds the "Licence & Activation" card shown at the top of the Settings
 * screen. Extracted verbatim (behavior unchanged) from
 * DashboardController.buildLicenseCard.
 */
@Component
public class LicenseCardBuilder {

    private static final String LICENSE_KEY_SETTING = "license_key";

    @Autowired
    private MachineIdentifier machineIdentifier;
    @Autowired
    private LicenseValidator licenseValidator;
    @Autowired
    private SettingsRepository settingsRepository;
    @Autowired
    private LicenseActivationDialog licenseActivationDialog;

    /**
     * @param onActivated invoked after a successful activation, so the
     *                     caller can re-render the settings screen in its
     *                     activated state (replaces the original
     *                     showModule(activeModule) self-rebuild call).
     */
    public VBox build(Runnable onActivated) {
        String machineId = machineIdentifier.getOrCreateMachineId();
        String storedKey = settingsRepository.get(LICENSE_KEY_SETTING);
        boolean activated = storedKey != null && licenseValidator.isValid(machineId, storedKey);

        // ── Header ────────────────────────────────────────────────
        Label icon = new Label("🛡️");
        icon.setStyle("-fx-font-size: 20px;");
        Label title = new Label("Licence & Activation");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        HBox header = new HBox(10, icon, title);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(18, header);
        card.setPadding(new Insets(28));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 2);");

        // ── Trial / status banner ────────────────────────────────
        if (!activated) {
            long daysLeft = licenseActivationDialog.getTrialDaysLeft();
            VBox banner = new VBox(4);
            banner.setPadding(new Insets(14, 18, 14, 18));
            banner.setStyle("-fx-background-color: #FEF3C7; -fx-background-radius: 8; "
                    + "-fx-border-color: #FDE68A; -fx-border-radius: 8; -fx-border-width: 1;");
            Label bannerBody = new Label(daysLeft > 0
                    ? "Il vous reste " + daysLeft + " jour" + (daysLeft > 1 ? "s" : "") + " d'essai gratuit."
                    : "Votre période d'essai est terminée. Activez pour continuer.");
            bannerBody.setStyle("-fx-text-fill: #92400E;");
            bannerBody.setWrapText(true);
            banner.getChildren().add(bannerBody);
            card.getChildren().add(banner);
        } else {
            VBox banner = new VBox(4);
            banner.setPadding(new Insets(14, 18, 14, 18));
            banner.setStyle("-fx-background-color: #D1FAE5; -fx-background-radius: 8; "
                    + "-fx-border-color: #A7F3D0; -fx-border-radius: 8; -fx-border-width: 1;");
            Label bannerBody = new Label("Licence active. Merci d'utiliser Rawdati !");
            bannerBody.setStyle("-fx-text-fill: #065F46; -fx-font-weight: bold;");
            banner.getChildren().add(bannerBody);
            card.getChildren().add(banner);
        }

        // ── Step 1: machine ID ───────────────────────────────────
        Label step1 = new Label("1. Votre Identifiant Machine");
        step1.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label step1Caption = new Label("Envoyez ce code à l'administrateur pour recevoir votre clé.");
        step1Caption.setStyle("-fx-text-fill: #64748B;");

        TextField idField = new TextField(machineId);
        idField.setEditable(false);
        idField.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10;");
        HBox.setHgrow(idField, Priority.ALWAYS);

        Button copyBtn = new Button("📋");
        copyBtn.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 12;");
        copyBtn.setTooltip(new Tooltip("Copier"));
        copyBtn.setOnAction(e -> {
            var clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            var content = new javafx.scene.input.ClipboardContent();
            content.putString(machineId);
            clipboard.setContent(content);
        });

        HBox idRow = new HBox(8, idField, copyBtn);

        VBox step1Box = new VBox(8, step1, step1Caption, idRow);

        // ── Step 2: activation key ───────────────────────────────
        Label step2 = new Label("2. Entrer la Clé d'Activation");
        step2.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        TextField keyField = new TextField();
        keyField.setPromptText("Collez votre clé ici...");
        keyField.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10;");
        HBox.setHgrow(keyField, Priority.ALWAYS);
        keyField.setDisable(activated);

        Button activateBtn = new Button("Activer");
        activateBtn.setStyle("-fx-background-color: #6D5EF5; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 24;");
        activateBtn.setDisable(activated);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #DC2626;");
        errorLabel.setWrapText(true);

        activateBtn.setOnAction(e -> {
            String candidate = keyField.getText() == null ? "" : keyField.getText().trim();
            if (licenseValidator.isValid(machineId, candidate)) {
                settingsRepository.set(LICENSE_KEY_SETTING, candidate);
                onActivated.run();
            } else {
                errorLabel.setText("Clé invalide pour cette machine.");
            }
        });

        HBox keyRow = new HBox(8, keyField, activateBtn);

        VBox step2Box = new VBox(10, step2, keyRow, errorLabel);
        step2Box.setPadding(new Insets(18));
        step2Box.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 10; "
                + "-fx-border-color: #F1F5F9; -fx-border-radius: 10; -fx-border-width: 1;");

        card.getChildren().addAll(step1Box, step2Box);
        return card;
    }
}