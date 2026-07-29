package com.example.mef.demo;

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
                .initializers(initializer)
                .run(getParameters().getRaw().toArray(new String[0]));
    }

    @Override
    public void start(Stage primaryStage) {
        SceneManager.init(primaryStage);
        primaryStage.setTitle("School Admin");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.getIcons().add(
                new Image(
                        Objects.requireNonNull(getClass()
                                .getResourceAsStream("/icons/school-admin.png"))
                )
        );
        SceneManager.switchTo("/fxml/login.fxml", "/css/style.css");
    }

    @Override
    public void stop() {
        this.applicationContext.close();
        Platform.exit();
    }
}
