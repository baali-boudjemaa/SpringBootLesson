package com.example.mef.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.logging.Logger;


@SpringBootApplication
public class DemoApplication  {

	public static void main(String[] args) {
        final Logger LOGGER = Logger.getLogger(DemoApplication.class.getName());

        SpringApplication.run(DemoApplication.class, args);
	}

}
