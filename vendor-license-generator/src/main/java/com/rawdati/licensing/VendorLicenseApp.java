package com.rawdati.licensing;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX application for generating vendor license keys.
 * Vendor-only utility - do not distribute to customers.
 */
public class VendorLicenseApp extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        try {
            // Load FXML from resources
            String fxmlResource = "license-generator.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlResource));

            if (loader.getLocation() == null) {
                throw new RuntimeException("Cannot find resource: " + fxmlResource);
            }

            VBox root = loader.load();
            Scene scene = new Scene(root, 900, 700);

            primaryStage.setTitle("Rawdati - Vendor License Generator");
            primaryStage.setScene(scene);
            primaryStage.setWidth(900);
            primaryStage.setHeight(700);
            primaryStage.setMinWidth(700);
            primaryStage.setMinHeight(600);

            // Apply styling
            String cssResource = "styles.css";
            String css = getClass().getResource(cssResource).toExternalForm();
            scene.getStylesheets().add(css);

            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Error loading UI: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}