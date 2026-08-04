package com.example.mef.demo.dashboard.settings;

import com.example.mef.demo.Services.EnrollmentSettingsKeys;
import com.example.mef.demo.Services.SettingService;
import com.example.mef.demo.util.DialogUtil;
import com.example.mef.demo.util.I18n;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Renders the Settings module page, extracted from
 * DashboardController.showSettingsPage. The license card itself is built by
 * LicenseCardBuilder; this class just lays out the page around it, plus the
 * general enrollment-rules card (minimum enrollment age).
 */
public class SettingsView {

    private final LicenseCardBuilder licenseCardBuilder;
    private final SettingService settingService;

    public SettingsView(LicenseCardBuilder licenseCardBuilder, SettingService settingService) {
        this.licenseCardBuilder = licenseCardBuilder;
        this.settingService = settingService;
    }

    /**
     * Renders the settings page into contentPane. If the license gets
     * activated, the page rebuilds itself (passing this same render call as
     * the onActivated callback) rather than reaching back into the
     * controller for {@code showModule(activeModule)} as the original code did.
     */
    public void render(BorderPane contentPane) {
        VBox licenseCard = licenseCardBuilder.build(() -> render(contentPane));
        VBox enrollmentRulesCard = buildEnrollmentRulesCard();

        VBox root = new VBox(20, enrollmentRulesCard, licenseCard);
        root.setPadding(new Insets(24));

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        contentPane.setCenter(scroll);
    }

    /** "Enrollment rules" card: currently just the minimum enrollment age. */
    private VBox buildEnrollmentRulesCard() {
        Label title = new Label(I18n.t("settings.enrollment_rules.title"));
        title.getStyleClass().add("workflow-title");

        Label hint = new Label(I18n.t("settings.min_age.hint"));
        hint.setWrapText(true);

        TextField minAgeField = new TextField();
        minAgeField.setPromptText(I18n.t("settings.min_age.label"));
        minAgeField.setMaxWidth(120);
        int currentMinAge = settingService.getInt(EnrollmentSettingsKeys.MIN_AGE, EnrollmentSettingsKeys.MIN_AGE_DEFAULT);
        minAgeField.setText(String.valueOf(currentMinAge));

        TextField schoolyear = new TextField();
        schoolyear.setPromptText(I18n.t("settings.schoolyear.label"));
        schoolyear.setMaxWidth(120);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.getStyleClass().add("form-grid");
        grid.add(new Label(I18n.t("settings.min_age.label")), 0, 0);
        grid.add(minAgeField, 1, 0);

        grid.add(new Label(I18n.t("settings.schoolyear.hint")), 0, 1);
        grid.add(schoolyear, 1, 1);

        Button save = new Button(I18n.t("action.save"));
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> {
            String raw = minAgeField.getText() == null ? "" : minAgeField.getText().trim();
            int minAge;
            try {
                minAge = raw.isEmpty() ? 0 : Integer.parseInt(raw);
                if (minAge < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                DialogUtil.error(I18n.t("settings.enrollment_rules.title"), I18n.t("settings.min_age.invalid"));
                return;
            }
            settingService.set(EnrollmentSettingsKeys.MIN_AGE, String.valueOf(minAge), I18n.t("settings.min_age.label"));
            minAgeField.setText(String.valueOf(minAge));
            DialogUtil.info(I18n.t("settings.enrollment_rules.title"), I18n.t("settings.saved"));
        });

        HBox actions = new HBox(10, save);
        actions.setPadding(new Insets(4, 0, 0, 0));

        VBox card = new VBox(14, title, hint, grid, actions);
        card.getStyleClass().add("workflow-card");
        return card;
    }
}