package com.example.mef.demo;

import java.util.logging.Logger;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import javafx.application.Application;
import javafx.application.Platform;

@SpringBootApplication
public class DemoApplication  {

    static {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
            // The JavaFX toolkit is already initialized by the app launcher.
        }
    }

	public static void main(String[] args) {
        final Logger LOGGER = Logger.getLogger(DemoApplication.class.getName());
        LOGGER.fine("Launching JavaFX application");
        Application.launch(JavaFxApplication.class, args);
	}

}
