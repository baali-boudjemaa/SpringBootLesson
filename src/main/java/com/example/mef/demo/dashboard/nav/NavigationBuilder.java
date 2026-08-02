package com.example.mef.demo.dashboard.nav;

import com.example.mef.demo.Model.Module;
import com.example.mef.demo.Model.ModuleRegistry;
import com.example.mef.demo.util.I18n;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javafx.scene.paint.Color;
import java.util.Map;
import java.util.function.Consumer;
import org.kordamp.ikonli.javafx.FontIcon;

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
            Map.entry("students", "fth-users"),
            Map.entry("teachers", "fth-briefcase"),
            Map.entry("classes", "fth-layout"),
            Map.entry("guardians", "fth-users"),
            Map.entry("courses", "fth-book"),
            Map.entry("attendance", "fth-check-circle"),
            Map.entry("enrollments", "fth-edit"),
            Map.entry("payments", "fth-credit-card"),
            Map.entry("reports", "fth-bar-chart-2"),
            Map.entry("users", "fth-key"),
            Map.entry("settings", "fth-settings")
    );

    @Autowired
    private ModuleRegistry registry;

    /**
     * Clears and rebuilds the sidebar nav buttons into navigationBox.
     *
     * @param navigationBox    the VBox to populate with nav buttons (cleared first)
     * @param activeKey        "dashboard", "monthly", or a module table name
     * @param onDashboard      invoked when the "Dashboard" entry is clicked
     * @param onMonthlyReport  invoked when the "Monthly report" entry is clicked
     * @param onModule         invoked with the corresponding Module when a module entry is clicked
     */
    public void build(VBox navigationBox, String activeKey,
                      Runnable onDashboard, Runnable onMonthlyReport, Consumer<Module> onModule) {
        navigationBox.getChildren().clear();

        Button dashboard = navButton(I18n.t("nav.dashboard"), "fth-home", "dashboard".equals(activeKey));
        dashboard.setOnAction(event -> onDashboard.run());
        navigationBox.getChildren().add(dashboard);

        Button monthly = navButton(I18n.t("nav.monthly_report"), "fth-clipboard", "monthly".equals(activeKey));
        monthly.setOnAction(event -> onMonthlyReport.run());
        navigationBox.getChildren().add(monthly);

        for (Module module : registry.all()) {
            String icon = MODULE_ICONS.getOrDefault(module.table(), "fth-circle");
            boolean active = module.table().equals(activeKey);
            Button button = navButton(I18n.t(module.titleKey()), icon, active);
            button.setOnAction(event -> onModule.accept(module));
            navigationBox.getChildren().add(button);
        }
    }

    private Button navButton(String text, String iconLiteral, boolean active) {
        Button button = new Button(text);
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(16);
        icon.setIconColor(active ? Color.WHITE : Color.web("#94A3B8"));
        button.setGraphic(icon);
        button.getStyleClass().add(active ? "nav-button-active" : "nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }
}