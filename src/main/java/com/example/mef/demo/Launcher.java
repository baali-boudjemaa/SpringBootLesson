package com.example.mef.demo;

/**
 * Plain (non-JavaFX) entry point.
 *
 * When the JVM's main class extends javafx.application.Application (or calls
 * Application.launch from a class on the module path), the JVM checks for the
 * JavaFX runtime at startup and throws "JavaFX runtime components are missing"
 * if native libs are absent.
 *
 * By using this thin launcher — which has NO JavaFX import — the JVM skips
 * that check entirely and delegates to DemoApplication.main() which then
 * calls Application.launch() normally.
 */
public class Launcher {
    public static void main(String[] args) {
        DemoApplication.main(args);
    }
}
