package com.example.mef.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class DevConfig {
    @Bean
    public String dataSource() {
        return "H2 In-Memory DB for Dev";
    }
}
