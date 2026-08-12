package com.rawdati.licensing;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.ResourceBundle;

/**
 * Controller for the JavaFX license generator UI.
 */
public class dashboardController implements Initializable {

    // Input controls
    @FXML
    private TextField machineIdField;

    @FXML
    private TextField privateKeyPathField;

    @FXML
    private Button browseButton;

    @FXML
    private ComboBox<LicenseGenerator.LicensePlan> planComboBox;

    @FXML
    private Button generateButton;

    // Output controls
    @FXML
    private VBox resultContainer;

    @FXML
    private TextArea activationKeyDisplay;

    @FXML
    private Label expirationDateLabel;

    @FXML
    private Button copyButton;

    @FXML
    private Label statusLabel;

    private File lastSelectedDirectory;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize license plan dropdown
        planComboBox.getItems().setAll(LicenseGenerator.LicensePlan.values());
        planComboBox.setValue(LicenseGenerator.LicensePlan.MONTHLY);

        // Setup file chooser button
        browseButton.setOnAction(e -> handleBrowsePrivateKey());

        // Setup generate button
        generateButton.setOnAction(e -> handleGenerateKey());

        // Setup copy button
        copyButton.setOnAction(e -> copyActivationKeyToClipboard());

        // Hide result container initially
        resultContainer.setVisible(false);

        // Add enter key support to generate button
        machineIdField.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER")) {
                handleGenerateKey();
            }
        });
    }

    @FXML
    private void handleBrowsePrivateKey() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Private Key File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("DER Files (*.der)", "*.der"),
                new FileChooser.ExtensionFilter("PEM Files (*.pem)", "*.pem"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        if (lastSelectedDirectory != null && lastSelectedDirectory.isDirectory()) {
            fileChooser.setInitialDirectory(lastSelectedDirectory);
        }

        Stage stage = (Stage) browseButton.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            privateKeyPathField.setText(selectedFile.getAbsolutePath());
            lastSelectedDirectory = selectedFile.getParentFile();
        }
    }

    @FXML
    private void handleGenerateKey() {
        // Validate inputs
        String machineId = machineIdField.getText().trim();
        String keyPath = privateKeyPathField.getText().trim();
        LicenseGenerator.LicensePlan plan = planComboBox.getValue();

        if (machineId.isEmpty()) {
            showError("Machine ID is required");
            return;
        }

        if (keyPath.isEmpty()) {
            showError("Private key path is required");
            return;
        }

        // Disable button during generation
        generateButton.setDisable(true);
        statusLabel.setText("Generating key...");

        // Run in background thread to avoid blocking UI
        Thread generationThread = new Thread(() -> {
            try {
                LicenseGenerator.LicenseKeyResult result = LicenseGenerator.generateKey(
                        machineId,
                        Path.of(keyPath),
                        plan
                );

                // Update UI on JavaFX thread
                Platform.runLater(() -> displayResult(result));

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Error generating key: " + e.getMessage());
                    generateButton.setDisable(false);
                    statusLabel.setText("Ready");
                });
            }
        });

        generationThread.setDaemon(true);
        generationThread.start();
    }

    private void displayResult(LicenseGenerator.LicenseKeyResult result) {
        activationKeyDisplay.setText(result.activationKey);
        activationKeyDisplay.setWrapText(true);

        expirationDateLabel.setText(
                String.format("Valid until: %s (%s)",
                        result.expiresAt,
                        result.plan.getDisplayName())
        );

        resultContainer.setVisible(true);
        statusLabel.setText("Key generated successfully");
        generateButton.setDisable(false);

        // Auto-select the key for easy copying
        activationKeyDisplay.selectAll();
    }

    private void copyActivationKeyToClipboard() {
        String key = activationKeyDisplay.getText();
        if (key != null && !key.isEmpty()) {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(key);
            clipboard.setContent(content);

            // Show feedback
            String originalText = copyButton.getText();
            copyButton.setText("Copied!");
            PauseTransition pause = new PauseTransition(javafx.util.Duration.seconds(2));
            pause.setOnFinished(e -> copyButton.setText(originalText));
            pause.play();
        }
    }

    private void showError(String message) {
        resultContainer.setVisible(false);
        statusLabel.setText("Error: " + message);
        statusLabel.setStyle("-fx-text-fill: #d32f2f;");
        generateButton.setDisable(false);
    }
}