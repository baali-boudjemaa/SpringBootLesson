package com.example.mef.demo.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Objects;

/**
 * Owns the primary Stage and swaps its Scene's root from FXML.
 * Call {@link #init(Stage)} once from Main.start(), then use
 * {@link #switchTo(String, String)} from controllers to navigate.
 */
public final class SceneManager {

    private static Stage primaryStage;
    private static Scene scene;
    @Setter
    @Getter
    private static ApplicationContext applicationContext;

    private SceneManager() {}

    public static void init(Stage stage) {
        primaryStage = stage;
    }

    /**
     * Loads the given FXML (path relative to resources root, e.g.
     * "/fxml/login.fxml") and shows it, applying the given stylesheet
     * (e.g. "/css/style.css").
     */
    public static void switchTo(String fxmlPath, String cssPath) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            if (applicationContext != null) {
                loader.setControllerFactory(applicationContext::getBean);
            }
            Parent root = loader.load();

            if (scene == null) {
                scene = new Scene(root, 1100, 720);
                primaryStage.setScene(scene);
            } else {
                scene.setRoot(root);
            }

            if (cssPath != null) {
                scene.getStylesheets().setAll(
                        Objects.requireNonNull(SceneManager.class.getResource(cssPath)).toExternalForm());
            }

            primaryStage.show();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load view " + fxmlPath + ": " + e.getMessage(), e);
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }
}
