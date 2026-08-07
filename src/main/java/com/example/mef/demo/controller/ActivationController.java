package com.example.mef.demo.controller;

import com.example.mef.demo.license.LicenseActivationDialog;
import com.example.mef.demo.license.MachineIdentifier;
import javafx.fxml.FXML;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
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
    private final LicenseActivationDialog licenseActivationDialog;

    private Runnable onActivated;

    public ActivationController(MachineIdentifier machineIdentifier,
                                LicenseActivationDialog licenseActivationDialog) {
        this.machineIdentifier = machineIdentifier;
        this.licenseActivationDialog = licenseActivationDialog;
    }

    @FXML
    public void initialize() {
        machineIdField.setText(machineIdentifier.getOrCreateMachineId());
        licenseKeyField.textProperty().addListener((observable, oldValue, newValue) -> clearError());
    }

    /** Called by whoever loads this scene, to be notified once activation succeeds. */
    public void setOnActivated(Runnable callback) {
        this.onActivated = callback;
    }

    @FXML
    private void onActivate() {
        String candidate = licenseKeyField.getText().trim();

        if (candidate.isEmpty()) {
            showError("Veuillez entrer votre clé d'activation.");
            return;
        }

        try {
            licenseActivationDialog.activate(candidate);
            clearError();
            if (onActivated != null) {
                onActivated.run();
            }
        } catch (IllegalArgumentException exception) {
            showError("Cette clé d'activation est invalide pour cette machine.");
        }
    }

    @FXML
    private void onCopyMachineId() {
        ClipboardContent content = new ClipboardContent();
        content.putString(machineIdField.getText());
        Clipboard.getSystemClipboard().setContent(content);
        showError("Identifiant machine copié dans le presse-papiers.");
        errorLabel.getStyleClass().setAll("activation-success");
    }

    private void showError(String message) {
        errorLabel.getStyleClass().setAll("activation-error");
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
