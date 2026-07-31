package com.example.mef.demo;

import com.example.mef.demo.controller.ActivationController;
import com.example.mef.demo.license.LicenseActivationDialog;
import com.example.mef.demo.util.SceneManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Objects;

public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() {
        ApplicationContextInitializer<GenericApplicationContext> initializer =
                context -> {
                    context.registerBean(Application.class, () -> JavaFxApplication.this);
                    context.registerBean(Parameters.class, this::getParameters);
                };

        this.applicationContext = new SpringApplicationBuilder()
                .sources(DemoApplication.class)
                .web(WebApplicationType.NONE) // Disable the web server since we only need JavaFX
                .headless(false) // CRITICAL: Allow JavaFX to show GUI windows
                .initializers(initializer)
                .run(getParameters().getRaw().toArray(new String[0]));
        SceneManager.setApplicationContext(this.applicationContext);
    }

    @Override
    public void start(Stage primaryStage) {
        SceneManager.init(primaryStage);
        primaryStage.setTitle("School Admin");

        var iconStream = getClass().getResourceAsStream("/icons/school-admin.png");
        if (iconStream != null) {
            primaryStage.getIcons().add(new Image(iconStream));
        }

        LicenseActivationDialog activationDialog = applicationContext.getBean(LicenseActivationDialog.class);

        if (!activationDialog.isUsable()) {
            SceneManager.init(primaryStage);
            SceneManager.switchTo("/fxml/activation.fxml", "/css/style.css");
            ActivationController controller = applicationContext.getBean(ActivationController.class);
            controller.setOnActivated(() -> proceedToApp(primaryStage));
            return; // hard stop — no path to login/dashboard until activated
        }
        proceedToApp(primaryStage);

    }
    private void proceedToApp(Stage primaryStage) {
        SceneManager.init(primaryStage);
        primaryStage.setTitle("School Admin");
        primaryStage.setMaximized(true);
        var iconStream = getClass().getResourceAsStream("/icons/school-admin.png");
        if (iconStream != null) {
            primaryStage.getIcons().add(new javafx.scene.image.Image(iconStream));
        }
        SceneManager.switchTo("/fxml/login.fxml", "/css/style.css");
    }
    @Override
    public void stop() {
        this.applicationContext.close();
        Platform.exit();
    }
}
