package com.example.mef.demo.dashboard.settings;

import com.example.mef.demo.Services.AppSettingsKeys;
import com.example.mef.demo.Services.EnrollmentSettingsKeys;
import com.example.mef.demo.Services.ScheduleSettingsKeys;
import com.example.mef.demo.Services.SettingService;
import com.example.mef.demo.dashboard.common.DaysPicker;
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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

/**
 * Renders the Settings module page, extracted from
 * DashboardController.showSettingsPage. The license card itself is built by
 * LicenseCardBuilder; this class just lays out the page around it, plus the
 * general enrollment-rules card (minimum enrollment age).
 */
public class SettingsView {

    private final LicenseCardBuilder licenseCardBuilder;
    private final SettingService settingService;
    private final Runnable onLocaleChange;

    public SettingsView(LicenseCardBuilder licenseCardBuilder, SettingService settingService, Runnable onLocaleChange) {
        this.licenseCardBuilder = licenseCardBuilder;
        this.settingService = settingService;
        this.onLocaleChange = onLocaleChange;
    }

    /**
     * Renders the settings page into contentPane. If the license gets
     * activated, the page rebuilds itself (passing this same render call as
     * the onActivated callback) rather than reaching back into the
     * controller for {@code showModule(activeModule)} as the original code did.
     */
    public void render(BorderPane contentPane) {
        VBox schoolIdentityCard = buildSchoolIdentityCard();
        VBox languageCard = buildLanguageCard();
        VBox licenseCard = licenseCardBuilder.build(() -> render(contentPane));
        VBox enrollmentRulesCard = buildEnrollmentRulesCard();
        VBox crechePricingCard = buildCrechePricingCard();
        VBox scheduleRulesCard = buildScheduleRulesCard();

        VBox root = new VBox(20, schoolIdentityCard, languageCard, enrollmentRulesCard, crechePricingCard, scheduleRulesCard, licenseCard);
        root.setPadding(new Insets(24));

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        contentPane.setCenter(scroll);
    }

    /** Lets an administrator choose the name displayed for their school/centre. */
    private VBox buildSchoolIdentityCard() {
        Label title = new Label(I18n.t("settings.school_identity"));
        title.getStyleClass().add("workflow-title");
        Label hint = new Label(I18n.t("settings.school_identity_hint"));
        hint.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        TextField schoolName = new TextField(settingService.get(
                AppSettingsKeys.SCHOOL_NAME, AppSettingsKeys.SCHOOL_NAME_DEFAULT));
        schoolName.setPromptText(I18n.t("settings.school_name"));
        schoolName.setPrefWidth(360);

        Button save = new Button(I18n.t("action.save"));
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> {
            String value = schoolName.getText() == null ? "" : schoolName.getText().trim();
            if (value.isBlank()) {
                DialogUtil.error(I18n.t("settings.school_identity"), I18n.t("settings.school_name_required"));
                return;
            }
            settingService.set(AppSettingsKeys.SCHOOL_NAME, value, I18n.t("settings.school_name"));
            DialogUtil.info(I18n.t("settings.school_identity"), I18n.t("settings.school_name_saved"));
        });

        VBox card = new VBox(12, title, hint, schoolName, save);
        card.getStyleClass().add("workflow-card");
        return card;
    }

    /** "Language" card: switch the app between French and Arabic. */
    private VBox buildLanguageCard() {
        Label title = new Label(I18n.t("settings.language.title"));
        title.getStyleClass().add("workflow-title");

        Label hint = new Label(I18n.t("settings.language.hint"));
        hint.setWrapText(true);

        boolean rtl = I18n.isRTL();

        Button frButton = new Button(I18n.t("settings.language.fr"));
        frButton.getStyleClass().add(rtl ? "lang-button" : "lang-button-active");
        frButton.setOnAction(event -> {
            I18n.setLocale(Locale.FRENCH);
            settingService.set(AppSettingsKeys.LOCALE, "fr", "Langue de l'application");
            onLocaleChange.run();
        });

        Button arButton = new Button(I18n.t("settings.language.ar"));
        arButton.getStyleClass().add(rtl ? "lang-button-active" : "lang-button");
        arButton.setOnAction(event -> {
            I18n.setLocale(new Locale("ar"));
            settingService.set(AppSettingsKeys.LOCALE, "ar", "Langue de l'application");
            onLocaleChange.run();
        });

        HBox buttons = new HBox(10, frButton, arButton);

        VBox card = new VBox(14, title, hint, buttons);
        card.getStyleClass().add("workflow-card");
        return card;
    }

    /** "Enrollment rules" card: currently just the minimum enrollment age. */
    private VBox buildEnrollmentRulesCard() {
        Label title = new Label(I18n.t("settings.enrollment_rules.title"));
        title.getStyleClass().add("workflow-title");

        Label hint = new Label(I18n.t("settings.min_age.hint"));
        hint.setWrapText(true);

        TextField minAgeField = new TextField();
        minAgeField.setPromptText(I18n.t("settings.min_age.label"));
        minAgeField.setMaxWidth(70);
        int currentMinAge = settingService.getInt(EnrollmentSettingsKeys.MIN_AGE, EnrollmentSettingsKeys.MIN_AGE_DEFAULT);
        minAgeField.setText(String.valueOf(currentMinAge));

        TextField schoolyear = new TextField();
        schoolyear.setPromptText(I18n.t("settings.schoolyear.label"));
        schoolyear.setMaxWidth(140);

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

    /**
     * "Schedule rules" card: weekly closure days used by course-schedule
     * validation (a course cannot be scheduled on one of these days).
     * Since course schedules are recurring weekly slots (day name + time,
     * no calendar date), weekly closure days are how this app models
     * "jours de congé/fermeture" — there is no dated holiday calendar.
     */
    private VBox buildScheduleRulesCard() {
        Label title = new Label(I18n.t("settings.schedule.title"));
        title.getStyleClass().add("workflow-title");

        Label hint = new Label(I18n.t("settings.schedule.hint"));
        hint.setWrapText(true);
        hint.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        DaysPicker closedDaysField = new DaysPicker();
        closedDaysField.setValue(settingService.get(ScheduleSettingsKeys.CLOSED_DAYS, ScheduleSettingsKeys.CLOSED_DAYS_DEFAULT));

        TextField dayStartField = new TextField(settingService.get(
                ScheduleSettingsKeys.DAY_START, ScheduleSettingsKeys.DAY_START_DEFAULT));
        TextField breakStartField = new TextField(settingService.get(
                ScheduleSettingsKeys.BREAK_START, ScheduleSettingsKeys.BREAK_START_DEFAULT));
        TextField breakEndField = new TextField(settingService.get(
                ScheduleSettingsKeys.BREAK_END, ScheduleSettingsKeys.BREAK_END_DEFAULT));
        TextField dayEndField = new TextField(settingService.get(
                ScheduleSettingsKeys.DAY_END, ScheduleSettingsKeys.DAY_END_DEFAULT));
        for (TextField field : List.of(dayStartField, breakStartField, breakEndField, dayEndField)) {
            field.setPromptText("08:00");
            field.setPrefColumnCount(8);
            field.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #CBD5E1; "
                    + "-fx-padding: 8 10; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        }

        GridPane hoursGrid = new GridPane();
        hoursGrid.setHgap(12);
        hoursGrid.setVgap(10);
        hoursGrid.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 10; -fx-padding: 14;");
        Label hoursTitle = new Label(I18n.t("settings.schedule.hours"));
        hoursTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A; -fx-font-size: 13px;");
        hoursGrid.add(hoursTitle, 0, 0, 2, 1);
        hoursGrid.addRow(1, scheduleLabel(I18n.t("settings.schedule.opening")), dayStartField);
        hoursGrid.addRow(2, scheduleLabel(I18n.t("settings.schedule.break_start")), breakStartField);
        hoursGrid.addRow(3, scheduleLabel(I18n.t("settings.schedule.break_end")), breakEndField);
        hoursGrid.addRow(4, scheduleLabel(I18n.t("settings.schedule.closing")), dayEndField);

        Button save = new Button(I18n.t("action.save"));
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> {
            try {
                LocalTime dayStart = parseScheduleTime(dayStartField.getText());
                LocalTime breakStart = parseScheduleTime(breakStartField.getText());
                LocalTime breakEnd = parseScheduleTime(breakEndField.getText());
                LocalTime dayEnd = parseScheduleTime(dayEndField.getText());
                if (!dayStart.isBefore(breakStart) || !breakStart.isBefore(breakEnd) || !breakEnd.isBefore(dayEnd)) {
                    throw new IllegalArgumentException(I18n.t("settings.schedule.invalid_order"));
                }
            } catch (IllegalArgumentException exception) {
                DialogUtil.error(I18n.t("settings.schedule.title"), exception.getMessage());
                return;
            }
            settingService.set(ScheduleSettingsKeys.CLOSED_DAYS, closedDaysField.getValue(), I18n.t("settings.schedule.closed_days"));
            settingService.set(ScheduleSettingsKeys.DAY_START, dayStartField.getText().trim(), I18n.t("settings.schedule.opening"));
            settingService.set(ScheduleSettingsKeys.BREAK_START, breakStartField.getText().trim(), I18n.t("settings.schedule.break_start"));
            settingService.set(ScheduleSettingsKeys.BREAK_END, breakEndField.getText().trim(), I18n.t("settings.schedule.break_end"));
            settingService.set(ScheduleSettingsKeys.DAY_END, dayEndField.getText().trim(), I18n.t("settings.schedule.closing"));
            DialogUtil.info(I18n.t("settings.schedule.title"), I18n.t("settings.saved"));
        });

        HBox actions = new HBox(10, save);
        actions.setPadding(new Insets(4, 0, 0, 0));

        Label closedDaysTitle = new Label(I18n.t("settings.schedule.closed_days"));
        closedDaysTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A; -fx-font-size: 13px;");
        VBox card = new VBox(14, title, hint, hoursGrid, closedDaysTitle, closedDaysField.getNode(), actions);
        card.getStyleClass().add("workflow-card");
        return card;
    }

    /** Daily crèche pricing is shared by the enrollment/payment workflow. */
    private VBox buildCrechePricingCard() {
        Label title = new Label(I18n.t("settings.creche_pricing.title"));
        title.getStyleClass().add("workflow-title");
        Label hint = new Label(I18n.t("settings.creche_pricing.hint"));
        hint.setWrapText(true);

        TextField dailyFee = new TextField(settingService.get(EnrollmentSettingsKeys.CRECHE_DAILY_FEE, "0"));
        TextField dueDay = new TextField(settingService.get(EnrollmentSettingsKeys.CRECHE_DUE_DAY,
                String.valueOf(EnrollmentSettingsKeys.CRECHE_DUE_DAY_DEFAULT)));
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.getStyleClass().add("form-grid");
        grid.addRow(0, new Label(I18n.t("settings.creche_pricing.daily_fee")), dailyFee);
        grid.addRow(1, new Label(I18n.t("settings.creche_pricing.due_day")), dueDay);

        Button save = new Button(I18n.t("action.save"));
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> {
            try {
                double fee = Double.parseDouble(dailyFee.getText().trim().replace(',', '.'));
                int day = Integer.parseInt(dueDay.getText().trim());
                if (fee < 0 || day < 1 || day > 28) throw new NumberFormatException();
                settingService.set(EnrollmentSettingsKeys.CRECHE_DAILY_FEE, String.valueOf(fee), I18n.t("settings.creche_pricing.daily_fee"));
                settingService.set(EnrollmentSettingsKeys.CRECHE_DUE_DAY, String.valueOf(day), I18n.t("settings.creche_pricing.due_day"));
                DialogUtil.info(title.getText(), I18n.t("settings.saved"));
            } catch (NumberFormatException ex) {
                DialogUtil.error(title.getText(), I18n.t("settings.creche_pricing.invalid"));
            }
        });
        VBox card = new VBox(14, title, hint, grid, save);
        card.getStyleClass().add("workflow-card");
        return card;
    }

    private LocalTime parseScheduleTime(String value) {
        try {
            return LocalTime.parse(value == null ? "" : value.trim(), DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(I18n.t("settings.schedule.invalid_time"));
        }
    }

    private Label scheduleLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #334155; -fx-font-size: 12px;");
        return label;
    }
}
