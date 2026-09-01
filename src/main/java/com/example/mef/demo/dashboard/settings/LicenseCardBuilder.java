package com.example.mef.demo.dashboard.settings;

import com.example.mef.demo.license.LicenseActivationDialog;
import com.example.mef.demo.license.MachineIdentifier;
import com.example.mef.demo.util.I18n;
import javafx.geometry.Insets;
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
 * DashboardController.buildLicenseCard, then wired to I18n so it follows
 * the FR/AR toggle like the rest of the Settings page.
 */
@Component
public class LicenseCardBuilder {

    @Autowired
    private MachineIdentifier machineIdentifier;
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
        boolean activated = licenseActivationDialog.isAlreadyActivated();

        // ── Header ────────────────────────────────────────────────
        Label title = new Label("Licence et activation");
        title.getStyleClass().add("workflow-title");
        title.setStyle("-fx-text-fill: #0F172A;");
        Label subtitle = new Label("Gérez l'état de la licence et l'activation du programme.");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        VBox heading = new VBox(3, title, subtitle);

        // Same left edge and heading style as every other settings card.
        VBox card = new VBox(18, heading);
        card.setPadding(new Insets(40));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 2);");

        // ── Trial / status banner ────────────────────────────────
        if (!activated) {
            long daysLeft = licenseActivationDialog.getTrialDaysLeft();
            VBox banner = new VBox(4);
            banner.setPadding(new Insets(14, 18, 14, 18));
            banner.setStyle("-fx-background-color: #FEF3C7; -fx-background-radius: 8; "
                    + "-fx-border-color: #FDE68A; -fx-border-radius: 8; -fx-border-width: 1;");
            String bannerText;
            if (daysLeft > 0) {
                String key = daysLeft > 1 ? "license.trial_remaining_other" : "license.trial_remaining_one";
                bannerText = I18n.t(key, "تسجيل الحضور").replace("{days}", String.valueOf(daysLeft));
            } else {
                bannerText = I18n.t("license.trial_expired", "تسجيل الحضور");
            }
            Label bannerBody = new Label(bannerText);
            bannerBody.setStyle("-fx-text-fill: #92400E;");
            bannerBody.setWrapText(true);
            banner.getChildren().add(bannerBody);
            card.getChildren().add(banner);
        } else {
            VBox banner = new VBox(4);
            banner.setPadding(new Insets(14, 18, 14, 18));
            banner.setStyle("-fx-background-color: #D1FAE5; -fx-background-radius: 8; "
                    + "-fx-border-color: #A7F3D0; -fx-border-radius: 8; -fx-border-width: 1;");
            Label bannerBody = new Label(I18n.t("license.active_message", "تسجيل الحضور"));
            bannerBody.setStyle("-fx-text-fill: #065F46; -fx-font-weight: bold;");
            banner.getChildren().add(bannerBody);

            licenseActivationDialog.getDaysUntilExpiry().ifPresent(daysLeft -> {
                String daysText;
                if (daysLeft <= 0) {
                    daysText = I18n.t("license.expires_today", "تسجيل الحضور");
                } else if (daysLeft == 1) {
                    daysText = I18n.t("license.days_left_one", "تسجيل الحضور");
                } else {
                    daysText = I18n.t("license.days_left_other", "تسجيل الحضور").replace("{days}", String.valueOf(daysLeft));
                }
                Label daysLabel = new Label(daysText);
                daysLabel.setStyle("-fx-text-fill: #065F46;");
                banner.getChildren().add(daysLabel);
            });

            card.getChildren().add(banner);
        }

        // ── Step 1: machine ID ───────────────────────────────────
        Label step1 = new Label(I18n.t("license.step1_title", "تسجيل الحضور"));
        step1.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label step1Caption = new Label(I18n.t("license.step1_caption", "تسجيل الحضور"));
        step1Caption.setStyle("-fx-text-fill: #64748B;");

        TextField idField = new TextField(machineId);
        idField.setEditable(false);
        idField.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10;");
        HBox.setHgrow(idField, Priority.ALWAYS);

        Button copyBtn = new Button("📋");
        copyBtn.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 12;");
        copyBtn.setTooltip(new Tooltip(I18n.t("license.copy_tooltip", "تسجيل الحضور")));
        copyBtn.setOnAction(e -> {
            var clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            var content = new javafx.scene.input.ClipboardContent();
            content.putString(machineId);
            clipboard.setContent(content);
        });

        HBox idRow = new HBox(8, idField, copyBtn);

        VBox step1Box = new VBox(8, step1, step1Caption, idRow);

        // ── Step 2: activation key ───────────────────────────────
        Label step2 = new Label(I18n.t("license.step2_title", "تسجيل الحضور"));
        step2.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        TextField keyField = new TextField();
        keyField.setPromptText(I18n.t("license.key_placeholder", "تسجيل الحضور"));
        keyField.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10;");
        HBox.setHgrow(keyField, Priority.ALWAYS);
        keyField.setDisable(activated);

        Button activateBtn = new Button(I18n.t("license.activate_button", "تسجيل الحضور"));
        activateBtn.setStyle("-fx-background-color: #6D5EF5; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 24;");
        activateBtn.setDisable(activated);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #DC2626;");
        errorLabel.setWrapText(true);

        activateBtn.setOnAction(e -> {
            String candidate = keyField.getText() == null ? "" : keyField.getText().trim();
            try {
                licenseActivationDialog.activate(candidate);
                onActivated.run();
            } catch (IllegalArgumentException exception) {
                errorLabel.setText(I18n.t("license.invalid_key", "تسجيل الحضور"));
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
