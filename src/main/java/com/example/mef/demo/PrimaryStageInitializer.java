package com.example.mef.demo;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class PrimaryStageInitializer implements ApplicationListener<StageReadyEvent> {

    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        Stage stage = event.getStage();
        
        Label label = new Label("Hello from Spring Boot + JavaFX!");
        StackPane layout = new StackPane();
        layout.getChildren().add(label);
        
        Scene scene = new Scene(layout, 400, 300);
        
        stage.setScene(scene);
        stage.setTitle("Spring Boot JavaFX Demo");
        stage.show();
    }
}
