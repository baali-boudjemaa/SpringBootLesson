package com.example.mef.demo.dashboard.settings;

import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/**
 * Renders the Settings module page, extracted from
 * DashboardController.showSettingsPage. The license card itself is built by
 * LicenseCardBuilder; this class just lays out the page around it.
 */
public class SettingsView {

    private final LicenseCardBuilder licenseCardBuilder;

    public SettingsView(LicenseCardBuilder licenseCardBuilder) {
        this.licenseCardBuilder = licenseCardBuilder;
    }

    /**
     * Renders the settings page into contentPane. If the license gets
     * activated, the page rebuilds itself (passing this same render call as
     * the onActivated callback) rather than reaching back into the
     * controller for {@code showModule(activeModule)} as the original code did.
     */
    public void render(BorderPane contentPane) {
        VBox licenseCard = licenseCardBuilder.build(() -> render(contentPane));

        VBox root = new VBox(20, licenseCard);
        root.setPadding(new Insets(24));

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        contentPane.setCenter(scroll);
    }
}
