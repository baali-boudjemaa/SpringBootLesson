package com.example.mef.demo.controller;

import com.example.mef.demo.Model.Module;
import com.example.mef.demo.Model.ModuleRegistry;
import com.example.mef.demo.Model.User;
import com.example.mef.demo.Services.DynamicDatabaseService;
import com.example.mef.demo.config.Session;
import com.example.mef.demo.dashboard.attendance.AttendanceView;
import com.example.mef.demo.dashboard.classrooms.ClassroomsView;
import com.example.mef.demo.dashboard.home.DashboardHomeView;
import com.example.mef.demo.dashboard.modules.ModuleTableView;
import com.example.mef.demo.dashboard.nav.NavigationBuilder;
import com.example.mef.demo.dashboard.report.MonthlyReport;
import com.example.mef.demo.dashboard.search.GlobalSearch;
import com.example.mef.demo.dashboard.settings.BackupRestorePanel;
import com.example.mef.demo.dashboard.settings.LicenseCardBuilder;
import com.example.mef.demo.dashboard.settings.SettingsView;
import com.example.mef.demo.dashboard.students.StudentEnrollmentWizard;
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
    @FXML private Label userLabel;
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

    private GlobalSearch globalSearch;
    private MonthlyReport monthlyReport;
    private Module activeModule;

    public DashboardController(
            DynamicDatabaseService dao,
            BackupRestoreService backupRestoreService,
            AttendanceView attendanceView,
            ClassroomsView classroomsView,
            DashboardHomeView dashboardHomeView,
            LicenseCardBuilder licenseCardBuilder,
            ModuleRegistry registry,
            NavigationBuilder navigationBuilder) {

        this.dao = dao;
        this.backupRestorePanel = new BackupRestorePanel(backupRestoreService);
        this.attendanceView = attendanceView;
        this.classroomsView = classroomsView;
        this.dashboardHomeView = dashboardHomeView;
        this.registry = registry;
        this.navigationBuilder = navigationBuilder;
        this.settingsView = new SettingsView(licenseCardBuilder);
    }

    @FXML
    public void initialize() {
        User current = Session.getCurrentUser();
        if (current != null) {
            userLabel.setText(current.getFullName() + " · " + current.getRole());
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
                () -> {
                    activeModule = null;
                    showDashboard();
                },
                () -> {
                    activeModule = null;
                    monthlyReport.show();
                },
                this::showModule
        );

        if (activeModule != null) {
            showModule(activeModule);
        } else {
            showDashboard();
        }
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

        new ModuleTableView(dao).render(
                contentPane,
                module,
                () -> new StudentEnrollmentWizard(dao, registry, this::showModule).show(contentPane, pageTitleLabel)
        );
    }
}
