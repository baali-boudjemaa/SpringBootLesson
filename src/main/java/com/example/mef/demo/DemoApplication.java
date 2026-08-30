package com.example.mef.demo;

import java.util.logging.Logger;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import javafx.application.Application;
import javafx.application.Platform;

@SpringBootApplication
@EnableScheduling
public class DemoApplication  {

    static {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
            // The JavaFX toolkit is already initialized by the app launcher.
        }
    }

	public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("sun.jnu.encoding", "UTF-8");
        final Logger LOGGER = Logger.getLogger(DemoApplication.class.getName());
        LOGGER.fine("Launching JavaFX application");
        Application.launch(JavaFxApplication.class, args);
	}

}
