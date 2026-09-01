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
import javafx.scene.control.ComboBox;
import com.example.mef.demo.dashboard.common.TimeSlots;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
        Label title = new Label(I18n.t("settings.school_identity", "تسجيل الحضور"));
        title.getStyleClass().add("workflow-title");
        Label hint = new Label(I18n.t("settings.school_identity_hint", "تسجيل الحضور"));
        hint.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        TextField schoolName = new TextField(settingService.get(
                AppSettingsKeys.SCHOOL_NAME, AppSettingsKeys.SCHOOL_NAME_DEFAULT));
        schoolName.setPromptText(I18n.t("settings.school_name", "تسجيل الحضور"));
        schoolName.setPrefWidth(360);

        Button save = new Button(I18n.t("action.save", "تسجيل الحضور"));
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> {
            String value = schoolName.getText() == null ? "" : schoolName.getText().trim();
            if (value.isBlank()) {
                DialogUtil.error(I18n.t("settings.school_identity", "تسجيل الحضور"), I18n.t("settings.school_name_required", "تسجيل الحضور"));
                return;
            }
            settingService.set(AppSettingsKeys.SCHOOL_NAME, value, I18n.t("settings.school_name", "تسجيل الحضور"));
            DialogUtil.info(I18n.t("settings.school_identity", "تسجيل الحضور"), I18n.t("settings.school_name_saved", "تسجيل الحضور"));
        });

        VBox card = new VBox(12, title, hint, schoolName, save);
        card.getStyleClass().add("workflow-card");
        return card;
    }

    /** "Language" card: switch the app between French and Arabic. */
    private VBox buildLanguageCard() {
        Label title = new Label(I18n.t("settings.language.title", "تسجيل الحضور"));
        title.getStyleClass().add("workflow-title");

        Label hint = new Label(I18n.t("settings.language.hint", "تسجيل الحضور"));
        hint.setWrapText(true);

        boolean rtl = I18n.isRTL();

        Button frButton = new Button(I18n.t("settings.language.fr", "تسجيل الحضور"));
        frButton.getStyleClass().add(rtl ? "lang-button" : "lang-button-active");
        frButton.setOnAction(event -> {
            I18n.setLocale(Locale.FRENCH);
            settingService.set(AppSettingsKeys.LOCALE, "fr", "Langue de l'application");
            onLocaleChange.run();
        });

        Button arButton = new Button(I18n.t("settings.language.ar", "تسجيل الحضور"));
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

    /** "Enrollment rules" card: per-category age constraints. */
    private VBox buildEnrollmentRulesCard() {
        Label title = new Label(I18n.t("settings.enrollment_rules.title", "تسجيل الحضور"));
        title.getStyleClass().add("workflow-title");

        Label hint = new Label(I18n.t("settings.enrollment_rules.hint", "تسجيل الحضور"));
        hint.setWrapText(true);
        hint.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        // ── Creche ────────────────────────────────────────────────
        TextField crecheMin = new TextField(String.valueOf(
                settingService.getInt(EnrollmentSettingsKeys.CRECHE_MIN_AGE, EnrollmentSettingsKeys.CRECHE_MIN_AGE_DEFAULT)));
        crecheMin.setMaxWidth(70);
        TextField crecheMax = new TextField(String.valueOf(
                settingService.getInt(EnrollmentSettingsKeys.CRECHE_MAX_AGE, EnrollmentSettingsKeys.CRECHE_MAX_AGE_DEFAULT)));
        crecheMax.setMaxWidth(70);

        // ── Preparatoire ──────────────────────────────────────────
        TextField preparatoireMin = new TextField(String.valueOf(
                settingService.getInt(EnrollmentSettingsKeys.PREPARATOIRE_MIN_AGE, EnrollmentSettingsKeys.PREPARATOIRE_MIN_AGE_DEFAULT)));
        preparatoireMin.setMaxWidth(70);
        TextField preparatoireMax = new TextField(String.valueOf(
                settingService.getInt(EnrollmentSettingsKeys.PREPARATOIRE_MAX_AGE, EnrollmentSettingsKeys.PREPARATOIRE_MAX_AGE_DEFAULT)));
        preparatoireMax.setMaxWidth(70);

        // ── Soutien ───────────────────────────────────────────────
        TextField soutienMin = new TextField(String.valueOf(
                settingService.getInt(EnrollmentSettingsKeys.SOUTIEN_MIN_AGE, EnrollmentSettingsKeys.SOUTIEN_MIN_AGE_DEFAULT)));
        soutienMin.setMaxWidth(70);
        TextField soutienMax = new TextField(String.valueOf(
                settingService.getInt(EnrollmentSettingsKeys.SOUTIEN_MAX_AGE, EnrollmentSettingsKeys.SOUTIEN_MAX_AGE_DEFAULT)));
        soutienMax.setMaxWidth(70);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.getStyleClass().add("form-grid");

        // Header row
        Label colCategory = sectionHeader(I18n.t("settings.enrollment_rules.col_category", "تسجيل الحضور"));
        Label colMin = sectionHeader(I18n.t("settings.enrollment_rules.col_min", "تسجيل الحضور"));
        Label colMax = sectionHeader(I18n.t("settings.enrollment_rules.col_max", "تسجيل الحضور"));
        Label colNote = sectionHeader(I18n.t("settings.enrollment_rules.col_note", "تسجيل الحضور"));
        grid.addRow(0, colCategory, colMin, colMax, colNote);

        // Creche row
        grid.addRow(1,
                rowLabel(I18n.t("category.creche", "تسجيل الحضور")),
                crecheMin, crecheMax,
                noteLabel(I18n.t("settings.enrollment_rules.note_creche", "تسجيل الحضور")));

        // Preparatoire row
        grid.addRow(2,
                rowLabel(I18n.t("category.preparatoire", "تسجيل الحضور")),
                preparatoireMin, preparatoireMax,
                noteLabel(I18n.t("settings.enrollment_rules.note_preparatoire", "تسجيل الحضور")));

        // Soutien row
        grid.addRow(3,
                rowLabel(I18n.t("category.soutien", "تسجيل الحضور")),
                soutienMin, soutienMax,
                noteLabel(I18n.t("settings.enrollment_rules.note_soutien", "تسجيل الحضور")));

        Label zeroHint = new Label(I18n.t("settings.enrollment_rules.zero_hint", "تسجيل الحضور"));
        zeroHint.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        zeroHint.setWrapText(true);

        Button save = new Button(I18n.t("action.save", "تسجيل الحضور"));
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> {
            try {
                int cMin = parseAgeField(crecheMin);
                int cMax = parseAgeField(crecheMax);
                int pMin = parseAgeField(preparatoireMin);
                int pMax = parseAgeField(preparatoireMax);
                int sMin = parseAgeField(soutienMin);
                int sMax = parseAgeField(soutienMax);

                settingService.set(EnrollmentSettingsKeys.CRECHE_MIN_AGE, String.valueOf(cMin), "Creche min age");
                settingService.set(EnrollmentSettingsKeys.CRECHE_MAX_AGE, String.valueOf(cMax), "Creche max age");
                settingService.set(EnrollmentSettingsKeys.PREPARATOIRE_MIN_AGE, String.valueOf(pMin), "Preparatoire min age");
                settingService.set(EnrollmentSettingsKeys.PREPARATOIRE_MAX_AGE, String.valueOf(pMax), "Preparatoire max age");
                settingService.set(EnrollmentSettingsKeys.SOUTIEN_MIN_AGE, String.valueOf(sMin), "Soutien min age");
                settingService.set(EnrollmentSettingsKeys.SOUTIEN_MAX_AGE, String.valueOf(sMax), "Soutien max age");

                DialogUtil.info(title.getText(), I18n.t("settings.saved", "تسجيل الحضور"));
            } catch (IllegalArgumentException ex) {
                DialogUtil.error(title.getText(), ex.getMessage());
            }
        });

        HBox actions = new HBox(10, save);
        actions.setPadding(new Insets(4, 0, 0, 0));

        VBox card = new VBox(14, title, hint, grid, zeroHint, actions);
        card.getStyleClass().add("workflow-card");
        return card;
    }

    private int parseAgeField(TextField field) {
        String raw = field.getText() == null ? "" : field.getText().trim();
        try {
            int val = raw.isEmpty() ? 0 : Integer.parseInt(raw);
            if (val < 0) throw new NumberFormatException();
            return val;
        } catch (NumberFormatException e) {
            field.getStyleClass().add("field-error");
            throw new IllegalArgumentException(I18n.t("settings.min_age.invalid", "تسجيل الحضور"));
        }
    }

    private Label sectionHeader(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A; -fx-font-size: 12px;");
        return l;
    }

    private Label rowLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155; -fx-font-size: 13px;");
        return l;
    }

    private Label noteLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
        l.setWrapText(true);
        l.setMaxWidth(200);
        return l;
    }

    /**
     * "Schedule rules" card: weekly closure days used by course-schedule
     * validation (a course cannot be scheduled on one of these days).
     * Since course schedules are recurring weekly slots (day name + time,
     * no calendar date), weekly closure days are how this app models
     * "jours de congé/fermeture" — there is no dated holiday calendar.
     */
    private VBox buildScheduleRulesCard() {
        Label title = new Label(I18n.t("settings.schedule.title", "تسجيل الحضور"));
        title.getStyleClass().add("workflow-title");

        Label hint = new Label(I18n.t("settings.schedule.hint", "تسجيل الحضور"));
        hint.setWrapText(true);
        hint.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        DaysPicker closedDaysField = new DaysPicker();
        closedDaysField.setValue(settingService.get(ScheduleSettingsKeys.CLOSED_DAYS, ScheduleSettingsKeys.CLOSED_DAYS_DEFAULT));

        ComboBox<String> dayStartField = createTimePicker(settingService.get(
                ScheduleSettingsKeys.DAY_START, ScheduleSettingsKeys.DAY_START_DEFAULT));
        ComboBox<String> breakStartField = createTimePicker(settingService.get(
                ScheduleSettingsKeys.BREAK_START, ScheduleSettingsKeys.BREAK_START_DEFAULT));
        ComboBox<String> breakEndField = createTimePicker(settingService.get(
                ScheduleSettingsKeys.BREAK_END, ScheduleSettingsKeys.BREAK_END_DEFAULT));
        ComboBox<String> dayEndField = createTimePicker(settingService.get(
                ScheduleSettingsKeys.DAY_END, ScheduleSettingsKeys.DAY_END_DEFAULT));
        GridPane hoursGrid = new GridPane();
        hoursGrid.setHgap(12);
        hoursGrid.setVgap(10);
        hoursGrid.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 10; -fx-padding: 14;");
        Label hoursTitle = new Label(I18n.t("settings.schedule.hours", "تسجيل الحضور"));
        hoursTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A; -fx-font-size: 13px;");
        hoursGrid.add(hoursTitle, 0, 0, 2, 1);
        hoursGrid.addRow(1, scheduleLabel(I18n.t("settings.schedule.opening", "تسجيل الحضور")), dayStartField);
        hoursGrid.addRow(2, scheduleLabel(I18n.t("settings.schedule.break_start", "تسجيل الحضور")), breakStartField);
        hoursGrid.addRow(3, scheduleLabel(I18n.t("settings.schedule.break_end", "تسجيل الحضور")), breakEndField);
        hoursGrid.addRow(4, scheduleLabel(I18n.t("settings.schedule.closing", "تسجيل الحضور")), dayEndField);

        Button save = new Button(I18n.t("action.save", "تسجيل الحضور"));
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> {
            try {
                LocalTime dayStart = parseScheduleTime(dayStartField.getEditor().getText());
                LocalTime breakStart = parseScheduleTime(breakStartField.getEditor().getText());
                LocalTime breakEnd = parseScheduleTime(breakEndField.getEditor().getText());
                LocalTime dayEnd = parseScheduleTime(dayEndField.getEditor().getText());
                if (!dayStart.isBefore(breakStart) || !breakStart.isBefore(breakEnd) || !breakEnd.isBefore(dayEnd)) {
                    throw new IllegalArgumentException(I18n.t("settings.schedule.invalid_order", "تسجيل الحضور"));
                }
            } catch (IllegalArgumentException exception) {
                DialogUtil.error(I18n.t("settings.schedule.title", "تسجيل الحضور"), exception.getMessage());
                return;
            }
            settingService.set(ScheduleSettingsKeys.CLOSED_DAYS, closedDaysField.getValue(), I18n.t("settings.schedule.closed_days", "تسجيل الحضور"));
            settingService.set(ScheduleSettingsKeys.DAY_START, dayStartField.getEditor().getText().trim(), I18n.t("settings.schedule.opening", "تسجيل الحضور"));
            settingService.set(ScheduleSettingsKeys.BREAK_START, breakStartField.getEditor().getText().trim(), I18n.t("settings.schedule.break_start", "تسجيل الحضور"));
            settingService.set(ScheduleSettingsKeys.BREAK_END, breakEndField.getEditor().getText().trim(), I18n.t("settings.schedule.break_end", "تسجيل الحضور"));
            settingService.set(ScheduleSettingsKeys.DAY_END, dayEndField.getEditor().getText().trim(), I18n.t("settings.schedule.closing", "تسجيل الحضور"));
            DialogUtil.info(I18n.t("settings.schedule.title", "تسجيل الحضور"), I18n.t("settings.saved", "تسجيل الحضور"));
        });

        HBox actions = new HBox(10, save);
        actions.setPadding(new Insets(4, 0, 0, 0));

        Label closedDaysTitle = new Label(I18n.t("settings.schedule.closed_days", "تسجيل الحضور"));
        closedDaysTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #0F172A; -fx-font-size: 13px;");
        VBox card = new VBox(14, title, hint, hoursGrid, closedDaysTitle, closedDaysField.getNode(), actions);
        card.getStyleClass().add("workflow-card");
        return card;
    }

    /** Daily crèche pricing is shared by the enrollment/payment workflow. */
    private VBox buildCrechePricingCard() {
        Label title = new Label(I18n.t("settings.creche_pricing.title", "تسجيل الحضور"));
        title.getStyleClass().add("workflow-title");
        Label hint = new Label(I18n.t("settings.creche_pricing.hint", "تسجيل الحضور"));
        hint.setWrapText(true);

        TextField halfDayFee = new TextField(settingService.get(EnrollmentSettingsKeys.CRECHE_HALF_DAY_FEE, "0"));
        TextField halfDayLunchFee = new TextField(settingService.get(EnrollmentSettingsKeys.CRECHE_HALF_DAY_LUNCH_FEE, "0"));
        TextField fullDayFee = new TextField(settingService.get(EnrollmentSettingsKeys.CRECHE_FULL_DAY_FEE, "0"));
        TextField dayByDayFee = new TextField(settingService.get(EnrollmentSettingsKeys.CRECHE_DAY_BY_DAY_FEE, "0"));
        
        TextField dueDay = new TextField(settingService.get(EnrollmentSettingsKeys.CRECHE_DUE_DAY,
                String.valueOf(EnrollmentSettingsKeys.CRECHE_DUE_DAY_DEFAULT)));
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(10); grid.getStyleClass().add("form-grid");
        grid.addRow(0, new Label(I18n.t("settings.creche_pricing.half_day_fee", "تسجيل الحضور")), halfDayFee);
        grid.addRow(1, new Label(I18n.t("settings.creche_pricing.half_day_lunch_fee", "تسجيل الحضور")), halfDayLunchFee);
        grid.addRow(2, new Label(I18n.t("settings.creche_pricing.full_day_fee", "تسجيل الحضور")), fullDayFee);
        grid.addRow(3, new Label(I18n.t("settings.creche_pricing.day_by_day_fee", "تسجيل الحضور")), dayByDayFee);
        grid.addRow(4, new Label(I18n.t("settings.creche_pricing.due_day", "تسجيل الحضور")), dueDay);

        Button save = new Button(I18n.t("action.save", "تسجيل الحضور"));
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> {
            try {
                double hDayFee = Double.parseDouble(halfDayFee.getText().trim().replace(',', '.'));
                double hDayLunchFee = Double.parseDouble(halfDayLunchFee.getText().trim().replace(',', '.'));
                double fDayFee = Double.parseDouble(fullDayFee.getText().trim().replace(',', '.'));
                double dbDayFee = Double.parseDouble(dayByDayFee.getText().trim().replace(',', '.'));
                int day = Integer.parseInt(dueDay.getText().trim());
                if (hDayFee < 0 || hDayLunchFee < 0 || fDayFee < 0 || dbDayFee < 0 || day < 1 || day > 28) throw new NumberFormatException();
                
                settingService.set(EnrollmentSettingsKeys.CRECHE_HALF_DAY_FEE, String.valueOf(hDayFee), I18n.t("settings.creche_pricing.half_day_fee", "تسجيل الحضور"));
                settingService.set(EnrollmentSettingsKeys.CRECHE_HALF_DAY_LUNCH_FEE, String.valueOf(hDayLunchFee), I18n.t("settings.creche_pricing.half_day_lunch_fee", "تسجيل الحضور"));
                settingService.set(EnrollmentSettingsKeys.CRECHE_FULL_DAY_FEE, String.valueOf(fDayFee), I18n.t("settings.creche_pricing.full_day_fee", "تسجيل الحضور"));
                settingService.set(EnrollmentSettingsKeys.CRECHE_DAY_BY_DAY_FEE, String.valueOf(dbDayFee), I18n.t("settings.creche_pricing.day_by_day_fee", "تسجيل الحضور"));
                
                settingService.set(EnrollmentSettingsKeys.CRECHE_DUE_DAY, String.valueOf(day), I18n.t("settings.creche_pricing.due_day", "تسجيل الحضور"));
                DialogUtil.info(title.getText(), I18n.t("settings.saved", "تسجيل الحضور"));
            } catch (NumberFormatException ex) {
                DialogUtil.error(title.getText(), I18n.t("settings.creche_pricing.invalid", "تسجيل الحضور"));
            }
        });
        VBox card = new VBox(14, title, hint, grid, save);
        card.getStyleClass().add("workflow-card");
        return card;
    }


    private ComboBox<String> createTimePicker(String initialValue) {
        ComboBox<String> picker = new ComboBox<>();
        picker.getItems().addAll(TimeSlots.slots(LocalTime.of(6, 0), LocalTime.of(22, 0)));
        picker.setEditable(true);
        picker.setValue(initialValue);
        picker.setPrefWidth(120);
        picker.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #CBD5E1; "
                + "-fx-font-weight: bold; -fx-text-fill: #0F172A;");
        return picker;
    }

    private LocalTime parseScheduleTime(String value) {
        try {
            return LocalTime.parse(value == null ? "" : value.trim(), DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(I18n.t("settings.schedule.invalid_time", "تسجيل الحضور"));
        }
    }

    private Label scheduleLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #334155; -fx-font-size: 12px;");
        return label;
    }
}
