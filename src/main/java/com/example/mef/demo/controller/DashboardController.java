package com.example.mef.demo.controller;

import com.example.mef.demo.Model.Module;
import com.example.mef.demo.Model.ModuleRegistry;
import com.example.mef.demo.Model.User;
import com.example.mef.demo.Services.DynamicDatabaseService;
import com.example.mef.demo.config.Session;
import com.example.mef.demo.dashboard.attendance.AttendanceView;
import com.example.mef.demo.dashboard.classrooms.ClassroomsView;
import com.example.mef.demo.dashboard.courses.CoursesView;
import com.example.mef.demo.dashboard.enrollments.EnrollmentsView;
import com.example.mef.demo.dashboard.guardians.GuardiansView;
import com.example.mef.demo.dashboard.home.DashboardHomeView;
import com.example.mef.demo.dashboard.modules.ModuleTableView;
import com.example.mef.demo.dashboard.nav.NavigationBuilder;
import com.example.mef.demo.dashboard.payments.PaymentsView;
import com.example.mef.demo.dashboard.report.MonthlyReport;
import com.example.mef.demo.dashboard.reports.ReportsView;
import com.example.mef.demo.dashboard.search.GlobalSearch;
import com.example.mef.demo.dashboard.settings.BackupRestorePanel;
import com.example.mef.demo.dashboard.settings.LicenseCardBuilder;
import com.example.mef.demo.dashboard.settings.SettingsView;
import com.example.mef.demo.dashboard.students.EnrollmentWizard;
import com.example.mef.demo.dashboard.students.StudentEnrollmentWizard;
import com.example.mef.demo.dashboard.students.StudentsView;
import com.example.mef.demo.dashboard.teachers.TeachersView;
import com.example.mef.demo.dashboard.users.UsersView;
import com.example.mef.demo.util.BackupRestoreService;
import com.example.mef.demo.util.I18n;
import com.example.mef.demo.util.SceneManager;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class DashboardController {

    @FXML private BorderPane rootPane;
    @FXML private Label pageTitleLabel;
    @FXML private Label userInitialsLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Label brandLabel;
    @FXML private Label brandSubtitleLabel;
    @FXML private Button frButton;
    @FXML private Button arButton;
    @FXML private Button logoutButton;
    @FXML private VBox navigationBox;
    @FXML private BorderPane contentPane;

    private final DynamicDatabaseService dao;
    private final BackupRestorePanel backupRestorePanel;
    private final AttendanceView attendanceView;
    private final ClassroomsView classroomsView;
    private final DashboardHomeView dashboardHomeView;
    private final ModuleRegistry registry;
    private final NavigationBuilder navigationBuilder;
    private final SettingsView settingsView;
    private final StudentsView studentsView;
    private final TeachersView teachersView;
    private final GuardiansView guardiansView;
    private final CoursesView coursesView;
    private final EnrollmentsView enrollmentsView;
    private final EnrollmentWizard enrollmentWizard;
    private final PaymentsView paymentsView;
    private final ReportsView reportsView;
    private final UsersView usersView;

    private GlobalSearch globalSearch;
    private MonthlyReport monthlyReport;
    private Module activeModule;
    private String activeNavKey = "dashboard";

    public DashboardController(
            DynamicDatabaseService dao,
            BackupRestoreService backupRestoreService,
            AttendanceView attendanceView,
            ClassroomsView classroomsView,
            DashboardHomeView dashboardHomeView,
            LicenseCardBuilder licenseCardBuilder,
            ModuleRegistry registry,
            NavigationBuilder navigationBuilder,
            StudentsView studentsView,
            TeachersView teachersView,
            GuardiansView guardiansView,
            CoursesView coursesView,
            EnrollmentsView enrollmentsView,
            EnrollmentWizard enrollmentWizard,
            PaymentsView paymentsView,
            ReportsView reportsView,
            UsersView usersView) {

        this.dao = dao;
        this.backupRestorePanel = new BackupRestorePanel(backupRestoreService);
        this.attendanceView = attendanceView;
        this.classroomsView = classroomsView;
        this.dashboardHomeView = dashboardHomeView;
        this.registry = registry;
        this.navigationBuilder = navigationBuilder;
        this.settingsView = new SettingsView(licenseCardBuilder);
        this.studentsView = studentsView;
        this.teachersView = teachersView;
        this.guardiansView = guardiansView;
        this.coursesView = coursesView;
        this.enrollmentsView = enrollmentsView;
        this.enrollmentWizard = enrollmentWizard;
        this.paymentsView = paymentsView;
        this.reportsView = reportsView;
        this.usersView = usersView;
    }

    @FXML
    public void initialize() {
        User current = Session.getCurrentUser();
        if (current != null) {
            String name = current.getFullName() != null ? current.getFullName() : "Admin";
            userNameLabel.setText(name);
            userRoleLabel.setText(current.getRole() != null ? current.getRole().name() : "ADMIN");
            userInitialsLabel.setText(initialsOf(name));
        }

        monthlyReport = new MonthlyReport(contentPane, pageTitleLabel, dao);
        globalSearch = new GlobalSearch(rootPane, contentPane, registry, dao, this::showModule);

        I18n.setLocale(Locale.FRENCH);
        applyLocale();

        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.getAccelerators().put(
                        new KeyCodeCombination(KeyCode.K, KeyCombination.CONTROL_DOWN),
                        globalSearch::open
                );
            }
        });
    }

    @FXML
    private void openGlobalSearchFromBtn() {
        globalSearch.open();
    }

    @FXML
    private void handleLangFr() {
        I18n.setLocale(Locale.FRENCH);
        applyLocale();
    }

    @FXML
    private void handleLangAr() {
        I18n.setLocale(new Locale("ar"));
        applyLocale();
    }

    @FXML
    private void handleBackup() {
        backupRestorePanel.backup(rootPane.getScene().getWindow());
    }

    @FXML
    private void handleRestore() {
        backupRestorePanel.restore(rootPane.getScene().getWindow());
    }

    @FXML
    private void handleLogout() {
        Session.logout();
        SceneManager.switchTo("/fxml/login.fxml", "/css/style.css");
    }

    private void applyLocale() {
        boolean rtl = I18n.isRTL();
        rootPane.setNodeOrientation(rtl ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);

        if (brandLabel != null) brandLabel.setText(I18n.t("brand"));
        if (brandSubtitleLabel != null) brandSubtitleLabel.setText(I18n.t("brand.subtitle"));
        if (logoutButton != null) logoutButton.setText(I18n.t("action.logout"));

        if (frButton != null && arButton != null) {
            frButton.getStyleClass().setAll(rtl ? "lang-button" : "lang-button-active");
            arButton.getStyleClass().setAll(rtl ? "lang-button-active" : "lang-button");
        }

        navigationBuilder.build(
                navigationBox,
                activeNavKey,
                this::navigateDashboard,
                this::navigateMonthly,
                this::navigateModule
        );

        if (activeModule != null) {
            showModule(activeModule);
        } else {
            showDashboard();
        }
    }

    private void navigateDashboard() {
        activeModule = null;
        activeNavKey = "dashboard";
        showDashboard();
        rebuildNav();
    }

    private void navigateMonthly() {
        activeModule = null;
        activeNavKey = "monthly";
        monthlyReport.show();
        rebuildNav();
    }

    private void navigateModule(Module module) {
        activeNavKey = module.table();
        showModule(module);
        rebuildNav();
    }

    private void rebuildNav() {
        navigationBuilder.build(
                navigationBox,
                activeNavKey,
                this::navigateDashboard,
                this::navigateMonthly,
                this::navigateModule
        );
    }

    private static String initialsOf(String name) {
        if (name == null || name.isBlank()) return "A";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
        }
        return name.substring(0, 1).toUpperCase();
    }

    private void showDashboard() {
        dashboardHomeView.render(contentPane, pageTitleLabel);
    }

    private void showModule(Module module) {
        activeModule = module;
        pageTitleLabel.setText(I18n.t(module.titleKey()));

        if ("classes".equals(module.table())) {
            classroomsView.render(contentPane);
            return;
        }

        if ("attendance".equals(module.table())) {
            attendanceView.render(contentPane, pageTitleLabel);
            return;
        }

        if ("settings".equals(module.table())) {
            settingsView.render(contentPane);
            return;
        }

        if ("students".equals(module.table())) {
            studentsView.render(contentPane, pageTitleLabel,
                    () -> new StudentEnrollmentWizard(dao, registry, this::showModule).show(contentPane, pageTitleLabel));
            return;
        }

        if ("teachers".equals(module.table())) {
            teachersView.render(contentPane, pageTitleLabel);
            return;
        }

        if ("guardians".equals(module.table())) {
            guardiansView.render(contentPane, pageTitleLabel);
            return;
        }

        if ("courses".equals(module.table())) {
            coursesView.render(contentPane, pageTitleLabel);
            return;
        }

        if ("enrollments".equals(module.table())) {
            enrollmentsView.render(contentPane, pageTitleLabel,
                    () -> enrollmentWizard.show(contentPane, pageTitleLabel,
                            () -> showModule(registry.byTable("enrollments"))));
            return;
        }

        if ("payments".equals(module.table())) {
            paymentsView.render(contentPane, pageTitleLabel);
            return;
        }

        if ("reports".equals(module.table())) {
            reportsView.render(contentPane, pageTitleLabel);
            return;
        }

        if ("users".equals(module.table())) {
            usersView.render(contentPane, pageTitleLabel);
            return;
        }

        // No modules currently fall through to the generic table view —
        // every registered module now has a typed screen. Kept as a safety
        // net in case a new module is registered before its typed view exists.
        new ModuleTableView(dao).render(
                contentPane,
                module,
                () -> new StudentEnrollmentWizard(dao, registry, this::showModule).show(contentPane, pageTitleLabel)
        );
    }
}
