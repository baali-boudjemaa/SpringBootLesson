package com.example.mef.demo.controller;


import com.example.mef.demo.Services.LicenseService;
import com.example.mef.demo.Services.TrialService;
import com.example.mef.demo.util.DialogUtil;
import com.example.mef.demo.util.I18n;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LicenseController {

    @FXML private Label     trialBannerTitle;
    @FXML private Label     trialBannerMessage;
    @FXML private TextField machineIdField;
    @FXML private TextField activationKeyField;
    @FXML private Button    activateButton;

    @Autowired private LicenseService licenseService;
    @Autowired private TrialService trialService;

    @FXML
    private void initialize() {
        machineIdField.setText(licenseService.machineId());
        machineIdField.setEditable(false);
        refreshTrialBanner();
    }

    private void refreshTrialBanner() {
        if (licenseService.isActivated()) {
            trialBannerTitle.setText(I18n.t("license.activated_title"));
            trialBannerMessage.setText(I18n.t("license.activated_message"));
        } else {
            long days = trialService.daysRemaining();
            trialBannerTitle.setText(I18n.t("license.trial_active_title"));
            trialBannerMessage.setText(I18n.t("license.trial_remaining").replace("{days}", String.valueOf(days)));
        }
    }

    @FXML
    private void handleCopyMachineId() {
        ClipboardContent content = new ClipboardContent();
        content.putString(machineIdField.getText());
        Clipboard.getSystemClipboard().setContent(content);
    }

    @FXML
    private void handleActivate() {
        String key = activationKeyField.getText();
        if (key == null || key.isBlank()) {
            DialogUtil.error(I18n.t("license.activate"), I18n.t("license.key_required"));
            return;
        }
        activateButton.setDisable(true);
        Task<Void> task = new Task<>() {
            @Override protected Void call() { licenseService.activate(key); return null; }
        };
        task.setOnSucceeded(e -> {
            activateButton.setDisable(false);
            refreshTrialBanner();
            DialogUtil.info(I18n.t("license.activate"), I18n.t("license.activate_success"));
        });
        task.setOnFailed(e -> {
            activateButton.setDisable(false);
            DialogUtil.error(I18n.t("license.activate"), task.getException().getMessage());
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }
}