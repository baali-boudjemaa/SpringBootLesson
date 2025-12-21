package com.example.mef.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AppSettings {
    @Value("${server.port}")
    private int port;
    @Value("${custom.message:Default Message}")
    private String message; // Fallback value if not defined
}