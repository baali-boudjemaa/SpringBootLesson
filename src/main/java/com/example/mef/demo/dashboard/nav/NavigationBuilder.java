package com.example.mef.demo.dashboard.nav;



import com.example.mef.demo.Model.Module;
import com.example.mef.demo.Model.ModuleRegistry;
import com.example.mef.demo.util.I18n;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Builds the sidebar navigation: Dashboard + Monthly report shortcuts,
 * a "MODULES" section label, and one button per registered module.
 *
 * Extracted from DashboardController.buildNavigation(...) / navButton(...).
 *
 * IMPORTANT FIX vs. the original code: build() now clears ALL children
 * of navigationBox (navigationBox.getChildren().clear()) instead of only
 * removing Buttons (removeIf(n -> n instanceof Button)). The original
 * left the "MODULES" Label behind on every locale switch, so it stacked
 * up (visible as duplicated "MODULES" text after switching FR/AR a few
 * times). It's also now translated via I18n instead of hardcoded.
 */
public final class NavigationBuilder {

    private static final Map<String, String> ICONS = Map.ofEntries(
            Map.entry("students", "👶"),
            Map.entry("teachers", "👨‍🏫"),
            Map.entry("classes", "🏫"),
            Map.entry("guardians", "👪"),
            Map.entry("courses", "📚"),
            Map.entry("attendance", "✅"),
            Map.entry("enrollments", "📝"),
            Map.entry("payments", "💳"),
            Map.entry("reports", "📊"),
            Map.entry("users", "🔑"),
            Map.entry("settings", "⚙️")
    );

    private final VBox navigationBox;
    private final ModuleRegistry registry;
    private final Runnable onDashboard;
    private final Runnable onMonthlyReport;
    private final Consumer<Module> onModule;

    public NavigationBuilder(VBox navigationBox,
                             ModuleRegistry registry,
                             Runnable onDashboard,
                             Runnable onMonthlyReport,
                             Consumer<com.example.mef.demo.Model.Module> onModule) {
        this.navigationBox = navigationBox;
        this.registry = registry;
        this.onDashboard = onDashboard;
        this.onMonthlyReport = onMonthlyReport;
        this.onModule = onModule;
    }

    /** Rebuilds the sidebar from scratch. Call on init and after every locale change. */
    public void build() {
        navigationBox.getChildren().clear();

        Button dashboard = navButton("🏠  " + I18n.t("nav.dashboard"));
        dashboard.setOnAction(event -> onDashboard.run());
        navigationBox.getChildren().add(dashboard);

        Button monthly = navButton("📋  " + I18n.t("nav.monthly_report"));
        monthly.setOnAction(event -> onMonthlyReport.run());
        navigationBox.getChildren().add(monthly);

        Label sep = new Label(I18n.t("nav.modules_section"));
        sep.getStyleClass().add("sidebar-section-label");
        sep.setMaxWidth(Double.MAX_VALUE);
        navigationBox.getChildren().add(sep);

        for (com.example.mef.demo.Model.Module module : registry.all()) {
            String icon = ICONS.getOrDefault(module.table(), "•");
            Button button = navButton(icon + "  " + I18n.t(module.titleKey()));
            button.setOnAction(event -> onModule.accept(module));
            navigationBox.getChildren().add(button);
        }
    }

    private Button navButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }
}