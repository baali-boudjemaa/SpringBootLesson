package com.example.mef.demo.controller;

import com.example.mef.demo.license.LicenseValidator;
import com.example.mef.demo.license.MachineIdentifier;
import com.example.mef.demo.license.SettingsRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

@Component
public class ActivationController {

    @FXML private TextField machineIdField;
    @FXML private TextField licenseKeyField;
    @FXML private Label errorLabel;
    @FXML private Button activateButton;

    private final MachineIdentifier machineIdentifier;
    private final LicenseValidator licenseValidator;
    private final SettingsRepository settingsRepository;

    private static final String LICENSE_KEY_SETTING = "license_key";

    private Runnable onActivated;

    public ActivationController(MachineIdentifier machineIdentifier,
                                LicenseValidator licenseValidator,
                                SettingsRepository settingsRepository) {
        this.machineIdentifier = machineIdentifier;
        this.licenseValidator = licenseValidator;
        this.settingsRepository = settingsRepository;
    }

    @FXML
    public void initialize() {
        machineIdField.setText(machineIdentifier.getOrCreateMachineId());
    }

    /** Called by whoever loads this scene, to be notified once activation succeeds. */
    public void setOnActivated(Runnable callback) {
        this.onActivated = callback;
    }

    @FXML
    private void onActivate() {
        String machineId = machineIdentifier.getOrCreateMachineId();
        String candidate = licenseKeyField.getText().trim();

        if (licenseValidator.isValid(machineId, candidate)) {
            settingsRepository.set(LICENSE_KEY_SETTING, candidate);
            if (onActivated != null) {
                onActivated.run();
            }
        } else {
            errorLabel.setText("Invalid key for this machine. Try again.");
        }
    }
}