package com.example.mef.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import java.util.logging.Logger;

import javafx.application.Application;

@SpringBootApplication
public class DemoApplication  {

	public static void main(String[] args) {
        final Logger LOGGER = Logger.getLogger(DemoApplication.class.getName());

        Application.launch(JavaFxApplication.class, args);
	}

}
