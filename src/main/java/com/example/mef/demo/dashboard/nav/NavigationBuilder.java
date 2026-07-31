package com.example.mef.demo.dashboard.nav;

import com.example.mef.demo.Model.Module;
import com.example.mef.demo.Model.ModuleRegistry;
import com.example.mef.demo.util.I18n;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Builds the sidebar navigation: the fixed Dashboard / Monthly report
 * entries plus one button per registered Module. Extracted verbatim
 * (behavior unchanged) from DashboardController.buildNavigation /
 * navButton.
 *
 * The shell controller owns activeModule and showDashboard()/
 * monthlyReport.show()/showModule(Module) — this class only builds
 * buttons and wires them to the callbacks it's given, so it doesn't
 * need to know about activeModule at all. Callers should reset
 * activeModule to null inside onDashboard/onMonthlyReport before
 * rendering, same as the original inline lambdas did.
 */
@Component
public class NavigationBuilder {

    private static final Map<String, String> MODULE_ICONS = Map.ofEntries(
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

    @Autowired
    private ModuleRegistry registry;

    /**
     * Clears and rebuilds the sidebar into navigationBox.
     *
     * @param navigationBox    the VBox to populate (cleared first)
     * @param onDashboard      invoked when the "Dashboard" entry is clicked
     * @param onMonthlyReport  invoked when the "Monthly report" entry is clicked
     * @param onModule         invoked with the corresponding Module when a module entry is clicked
     */
    public void build(VBox navigationBox, Runnable onDashboard, Runnable onMonthlyReport, Consumer<Module> onModule) {
        navigationBox.getChildren().clear();   // ← vide TOUT (pas seulement les boutons)

        Button dashboard = navButton("🏠  " + I18n.t("nav.dashboard"));
        dashboard.setOnAction(event -> onDashboard.run());
        navigationBox.getChildren().add(dashboard);

        Button monthly = navButton("📋  " + I18n.t("nav.monthly_report"));
        monthly.setOnAction(event -> onMonthlyReport.run());
        navigationBox.getChildren().add(monthly);

        Label sep = new Label(I18n.t("nav.modules_section"));  // ← traduit, plus de "MODULES" en dur
        sep.getStyleClass().add("sidebar-section-label");
        sep.setMaxWidth(Double.MAX_VALUE);
        //navigationBox.getChildren().add(sep);

        for (Module module : registry.all()) {
            String icon = MODULE_ICONS.getOrDefault(module.table(), "•");
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