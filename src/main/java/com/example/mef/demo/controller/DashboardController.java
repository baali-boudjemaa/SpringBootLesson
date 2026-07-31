package com.example.mef.demo.controller;


import com.example.mef.demo.Model.Field;
import com.example.mef.demo.Model.Module;
import com.example.mef.demo.Model.User;
import com.example.mef.demo.Services.DynamicDatabaseService;
import com.example.mef.demo.config.Session;
import com.example.mef.demo.dashboard.report.MonthlyReport;
import com.example.mef.demo.dashboard.search.GlobalSearch;
import com.example.mef.demo.license.LicenseActivationDialog;
import com.example.mef.demo.license.LicenseValidator;
import com.example.mef.demo.license.MachineIdentifier;
import com.example.mef.demo.license.SettingsRepository;
import com.example.mef.demo.util.BackupRestoreService;
import com.example.mef.demo.util.DialogUtil;
import com.example.mef.demo.util.I18n;
import com.example.mef.demo.util.SceneManager;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;
import com.example.mef.demo.Model.ModuleRegistry;

import java.io.File;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

@Component
public class DashboardController {

    @FXML private BorderPane rootPane;
    @FXML private Label       pageTitleLabel;
    @FXML private Label       userLabel;
    @FXML private Label       brandLabel;
    @FXML private Label       brandSubtitleLabel;
    @FXML private Button      frButton;
    @FXML private Button      arButton;
    @FXML private Button      logoutButton;
    @FXML private VBox        navigationBox;
    @FXML private BorderPane  contentPane;

    @Autowired
    private DynamicDatabaseService dao;
    @Autowired
    private MachineIdentifier machineIdentifier;
    @Autowired
    private LicenseValidator licenseValidator;
    @Autowired
    private SettingsRepository settingsRepository;
    @Autowired
    private BackupRestoreService backupRestoreService;
    private final ModuleRegistry registry = new ModuleRegistry();
    private       Module         activeModule = null;
    /** Extracted collaborators — built once dao/panes are available, in initialize(). */
    private MonthlyReport monthlyReport;
    private GlobalSearch  globalSearch;
    @Autowired
    private LicenseActivationDialog licenseActivationDialog;
    @FXML
    private void initialize() {
        User current = Session.getCurrentUser();
        if (current != null) {
            userLabel.setText(current.getFullName() + " · " + current.getRole());
        }

        monthlyReport = new MonthlyReport(contentPane, pageTitleLabel, dao);
        globalSearch = new GlobalSearch(rootPane, contentPane, registry, dao, this::showModule);
        I18n.setLocale(Locale.FRENCH);
        applyLocale();

        // Register Ctrl+K globally on the rootPane
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

    /* ── Language switching ───────────────────────────────────── */

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

    /**
     * Applies the current locale: sets RTL/LTR, updates static labels,
     * rebuilds nav, and refreshes the active view.
     */
    private void applyLocale() {
        boolean rtl = I18n.isRTL();
        rootPane.setNodeOrientation(
                rtl ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);

        if (brandLabel     != null) brandLabel.setText(I18n.t("brand"));
        if (brandSubtitleLabel != null) brandSubtitleLabel.setText(I18n.t("brand.subtitle"));
        if (logoutButton   != null) logoutButton.setText(I18n.t("action.logout"));

        if (frButton != null && arButton != null) {
            frButton.getStyleClass().setAll(rtl  ? "lang-button" : "lang-button-active");
            arButton.getStyleClass().setAll(rtl  ? "lang-button-active" : "lang-button");
        }


        buildNavigation();

        if (activeModule != null) {
            showModule(activeModule);
        } else {
            showDashboard();
        }
    }

    @FXML
    private void handleLogout() {
        Session.logout();
        SceneManager.switchTo("/fxml/login.fxml", "/css/style.css");
    }



    private void buildNavigation() {
        navigationBox.getChildren().clear();   // ← vide TOUT (pas seulement les boutons)

        Button dashboard = navButton("🏠  " + I18n.t("nav.dashboard"));
        dashboard.setOnAction(event -> { activeModule = null; showDashboard(); });
        navigationBox.getChildren().add(dashboard);

        Button monthly = navButton("📋  " + I18n.t("nav.monthly_report"));
        monthly.setOnAction(event -> { activeModule = null; monthlyReport.show(); });
        navigationBox.getChildren().add(monthly);

        Label sep = new Label(I18n.t("nav.modules_section"));  // ← traduit, plus de "MODULES" en dur
        sep.getStyleClass().add("sidebar-section-label");
        sep.setMaxWidth(Double.MAX_VALUE);
        //navigationBox.getChildren().add(sep);




        Map<String, String> icons = Map.ofEntries(
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
        for (Module module : registry.all()) {
            String icon = icons.getOrDefault(module.table(), "•");
            Button button = navButton(icon + "  " + I18n.t(module.titleKey()));
            button.setOnAction(event -> showModule(module));
            navigationBox.getChildren().add(button);
        }
    }

    private Button navButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }

    /* ── Dashboard ───────────────────────────────────────────────── */

    private void showDashboard() {
        pageTitleLabel.setText(I18n.t("nav.dashboard"));
        Label loading = new Label(I18n.t("table.loading"));
        contentPane.setCenter(loading);

        Task<DashboardData> task = new Task<>() {
            @Override
            protected DashboardData call() {
                long students  = safeCount("students");
                long teachers  = safeCount("teachers");
                long classes   = safeCount("classes");
                long payments  = safeCount("payments");
                double total   = dao.sum("payments", "amount");
                Map<String, Integer> attendance = dao.attendanceSummary();
                List<Map<String, String>> recent = safeFind("payments",
                        List.of("payment_date", "student_name", "amount", "method", "status"),
                        "payment_date DESC", 5);
                return new DashboardData(students, teachers, classes, payments, total, attendance, recent);
            }
        };
        task.setOnSucceeded(e -> buildDashboardUI(task.getValue()));
        task.setOnFailed(e -> contentPane.setCenter(new Label("Erreur lors du chargement.")));
        startDaemonThread(task);
    }

    private void buildDashboardUI(DashboardData d) {
        // ── Top stat cards ──────────────────────────────────────
        HBox statsRow = new HBox(14,
                statCard("👶", String.valueOf(d.students),  I18n.t("dashboard.students"), "#4F46E5"),
                statCard("👨‍🏫", String.valueOf(d.teachers), I18n.t("dashboard.teachers"), "#7C3AED"),
                statCard("🏫", String.valueOf(d.classes),   I18n.t("dashboard.classes"),  "#0F766E"),
                statCard("💳", String.valueOf(d.payments),  I18n.t("dashboard.payments"), "#15803D")
        );
        for (Node n : statsRow.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        // ── Attendance pie chart ─────────────────────────────────
        int present = d.attendance.getOrDefault("PRESENT", 0);
        int absent  = d.attendance.getOrDefault("ABSENT", 0);
        int late    = d.attendance.getOrDefault("LATE",   0);

        PieChart chart = new PieChart(FXCollections.observableArrayList(
                new PieChart.Data(I18n.t("dashboard.present") + " (" + present + ")", Math.max(present, 0.01)),
                new PieChart.Data(I18n.t("dashboard.absent")  + " (" + absent  + ")", Math.max(absent, 0.01)),
                new PieChart.Data(I18n.t("dashboard.late")    + " (" + late    + ")", Math.max(late, 0.01))
        ));
        chart.setTitle(I18n.t("dashboard.attendance"));
        chart.setLegendVisible(true);
        chart.setPrefHeight(240);

        VBox chartCard = new VBox(8, chart);
        chartCard.getStyleClass().add("monthly-card");
        chartCard.setPrefWidth(320);

        // ── Total revenue card ───────────────────────────────────
        VBox revenueCard = new VBox(8,
                new Label(I18n.t("dashboard.monthly_income")),
                labelWith(String.format("%.2f DA", d.totalPayments), "stat-number")
        );
        revenueCard.getStyleClass().add("monthly-card");
        revenueCard.setPadding(new Insets(20));
        ((Label) revenueCard.getChildren().get(0)).getStyleClass().add("section-title");
        HBox.setHgrow(revenueCard, Priority.ALWAYS);

        HBox middleRow = new HBox(14, chartCard, revenueCard);
        HBox.setHgrow(chartCard, Priority.ALWAYS);
        HBox.setHgrow(revenueCard, Priority.ALWAYS);

        // ── Recent payments ──────────────────────────────────────
        Label recentTitle = new Label(I18n.t("dashboard.recent_payments"));
        recentTitle.getStyleClass().add("section-title");

        VBox recentList = new VBox(8);
        if (d.recentPayments.isEmpty()) {
            recentList.getChildren().add(new Label(I18n.t("dashboard.no_payments")));
        } else {
            for (Map<String, String> row : d.recentPayments) {
                String name   = row.getOrDefault("student_name", "—");
                String amount = row.getOrDefault("amount", "0");
                String date   = row.getOrDefault("payment_date", "");
                String status = row.getOrDefault("status", "");

                Label nameLbl   = new Label(name);
                nameLbl.setStyle("-fx-font-weight: bold;");
                Label amountLbl = new Label(amount + " DA");
                amountLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #15803D;");
                Label dateLbl   = new Label(date);
                dateLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
                Label statusLbl = new Label(status);
                statusLbl.setStyle(badgeStyle(status) +
                        "-fx-padding: 1 8 1 8; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox row2 = new HBox(8, nameLbl, dateLbl, spacer, amountLbl, statusLbl);
                row2.setAlignment(Pos.CENTER_LEFT);
                row2.getStyleClass().add("recent-payment-row");
                recentList.getChildren().add(row2);
            }
        }

        VBox root = new VBox(20, statsRow, middleRow, recentTitle, recentList);
        root.setPadding(new Insets(24));
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        contentPane.setCenter(scroll);
    }

    private VBox statCard(String icon, String value, String label, String accentColor) {
        Label iconLbl  = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 24px;");
        Label valLbl   = new Label(value);
        valLbl.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + accentColor + ";");
        Label captLbl  = new Label(label);
        captLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        VBox card = new VBox(6, iconLbl, valLbl, captLbl);
        card.getStyleClass().add("stat-box");
        card.setPadding(new Insets(18));
        card.setMinWidth(150);
        return card;
    }

    private Label labelWith(String text, String styleClass) {
        Label l = new Label(text);
        l.getStyleClass().add(styleClass);
        return l;
    }

    private record DashboardData(
            long students, long teachers, long classes, long payments,
            double totalPayments, Map<String, Integer> attendance,
            List<Map<String, String>> recentPayments) {}

    /* ── Student enrollment wizard ───────────────────────────────── */

    private void showNewStudentWizard() {
        pageTitleLabel.setText(I18n.t("wizard.title"));
        contentPane.setCenter(new Label(I18n.t("table.loading")));

        Task<WizardData> loadTask = new Task<>() {
            @Override
            protected WizardData call() {
                List<String> classrooms = dao.findAll("classes", List.of("name"), "name").stream()
                        .map(row -> row.get("name")).toList();
                List<String> courses = dao.findAll("courses", List.of("name"), "name").stream()
                        .map(row -> row.get("name")).toList();
                return new WizardData(classrooms, courses);
            }
        };
        loadTask.setOnSucceeded(e -> buildStudentWizard(loadTask.getValue()));
        loadTask.setOnFailed(e -> contentPane.setCenter(new Label("Erreur : " + loadTask.getException().getMessage())));
        startDaemonThread(loadTask);
    }

    private record WizardData(List<String> classrooms, List<String> courses) {}

    private void buildStudentWizard(WizardData data) {
        // Step 1 — Student info
        TextField firstName   = textField(I18n.t("field.first_name"));
        TextField lastName    = textField(I18n.t("field.last_name"));
        ComboBox<String> gender = comboBox(List.of("Fille", "Garçon", "Autre"));
        DatePicker birthDate  = new DatePicker();
        birthDate.setPromptText(I18n.t("field.date_of_birth"));
        ComboBox<String> classroom = comboBox(data.classrooms());
        classroom.setEditable(true);
        ComboBox<String> bloodGroup = comboBox(List.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"));

        GridPane studentForm = sectionGrid();
        addRow(studentForm, 0, I18n.t("field.last_name"),     lastName);
        addRow(studentForm, 1, I18n.t("field.first_name"),    firstName);
        addRow(studentForm, 2, I18n.t("field.gender"),        gender);
        addRow(studentForm, 3, I18n.t("field.date_of_birth"), birthDate);
        addRow(studentForm, 4, I18n.t("field.classroom"),     classroom);
        addRow(studentForm, 5, I18n.t("field.blood_group"),   bloodGroup);

        // Step 2 — Guardian info
        TextField guardianFirstName = textField(I18n.t("field.first_name"));
        TextField guardianLastName  = textField(I18n.t("field.last_name"));
        ComboBox<String> relationship = comboBox(List.of("Mère", "Père", "Tuteur", "Autre"));
        TextField phone = textField(I18n.t("field.phone"));
        TextField email = textField(I18n.t("field.email"));

        GridPane guardianForm = sectionGrid();
        addRow(guardianForm, 0, I18n.t("field.last_name"),    guardianLastName);
        addRow(guardianForm, 1, I18n.t("field.first_name"),   guardianFirstName);
        addRow(guardianForm, 2, I18n.t("field.relationship"), relationship);
        addRow(guardianForm, 3, I18n.t("field.phone"),        phone);
        addRow(guardianForm, 4, I18n.t("field.email"),        email);

        // Step 3 — Payment
        ComboBox<String> course = comboBox(data.courses());
        course.setEditable(true);
        CheckBox firstPayment = new CheckBox(I18n.t("wizard.payment"));
        TextField amount  = textField(I18n.t("field.amount"));
        ComboBox<String> method   = comboBox(List.of("Cash", "Virement", "Carte", "Chèque"));
        ComboBox<String> category = comboBox(List.of("Scolarité", "Cours", "Transport", "Autre"));
        amount.setDisable(true);
        method.setDisable(true);
        category.setDisable(true);
        firstPayment.selectedProperty().addListener((obs, old, sel) -> {
            amount.setDisable(!sel);
            method.setDisable(!sel);
            category.setDisable(!sel);
        });
        GridPane courseForm = sectionGrid();
        addRow(courseForm, 0, I18n.t("field.course"), course);
        GridPane paymentForm = sectionGrid();
        addRow(paymentForm, 0, I18n.t("field.amount"),   amount);
        addRow(paymentForm, 1, I18n.t("field.method"),   method);
        addRow(paymentForm, 2, I18n.t("field.category"), category);

        // Nav buttons
        Button enroll   = new Button(I18n.t("wizard.enroll"));
        enroll.getStyleClass().add("success-button");
        enroll.setVisible(false);
        enroll.setManaged(false);
        Button clear    = new Button(I18n.t("action.clear"));
        clear.getStyleClass().add("secondary-button");
        Button previous = new Button(I18n.t("wizard.previous"));
        previous.getStyleClass().add("secondary-button");
        Button next     = new Button(I18n.t("wizard.next"));
        next.getStyleClass().add("primary-button");
        HBox actions = new HBox(10, previous, next, enroll, clear);
        actions.setAlignment(Pos.CENTER_LEFT);

        // Clear action
        clear.setOnAction(event -> {
            List.of(firstName, lastName, guardianFirstName, guardianLastName, phone, email, amount)
                    .forEach(f -> { f.setText(""); f.getStyleClass().remove("field-error"); });
            List.of(gender, classroom, relationship, course, method, category, bloodGroup)
                    .forEach(c -> c.setValue(null));
            birthDate.setValue(null);
            firstPayment.setSelected(false);
        });

        // Enroll action
        enroll.setOnAction(event -> {
            try {
                validateEnrollmentStep(0, firstName, lastName, gender, birthDate, classroom, bloodGroup,
                        guardianFirstName, guardianLastName, relationship, phone, email,
                        course, firstPayment, amount, method, category);
                validateEnrollmentStep(1, firstName, lastName, gender, birthDate, classroom, bloodGroup,
                        guardianFirstName, guardianLastName, relationship, phone, email,
                        course, firstPayment, amount, method, category);
                validateEnrollmentStep(2, firstName, lastName, gender, birthDate, classroom, bloodGroup,
                        guardianFirstName, guardianLastName, relationship, phone, email,
                        course, firstPayment, amount, method, category);

                Map<String, String> student = new LinkedHashMap<>();
                student.put("first_name",    firstName.getText());
                student.put("last_name",     lastName.getText());
                student.put("gender",        value(gender));
                student.put("date_of_birth", birthDate.getValue() == null ? "" : birthDate.getValue().toString());
                student.put("classroom",     value(classroom));
                student.put("status",        "ACTIVE");
                student.put("phone",         phone.getText());

                Map<String, String> guardian = new LinkedHashMap<>();
                guardian.put("first_name",   guardianFirstName.getText());
                guardian.put("last_name",    guardianLastName.getText());
                guardian.put("relationship", value(relationship));
                guardian.put("phone",        phone.getText());
                guardian.put("email",        email.getText());

                Map<String, String> payment = null;
                if (firstPayment.isSelected()) {
                    payment = new LinkedHashMap<>();
                    payment.put("amount",   amount.getText());
                    payment.put("method",   value(method));
                    payment.put("category", value(category));
                    payment.put("status",   "PAID");
                }

                final Map<String, String> paymentFinal = payment;
                final String studentName = lastName.getText().trim() + " " + firstName.getText().trim();

                enroll.setDisable(true);
                Task<Void> enrollTask = new Task<>() {
                    @Override
                    protected Void call() {
                        dao.createStudentEnrollment(student, guardian, value(course), paymentFinal);
                        return null;
                    }
                };
                enrollTask.setOnSucceeded(ev -> {
                    enroll.setDisable(false);
                    showEnrollSuccessCard(studentName);
                });
                enrollTask.setOnFailed(ev -> {
                    enroll.setDisable(false);
                    DialogUtil.error(I18n.t("wizard.enroll"), enrollTask.getException().getMessage());
                });
                startDaemonThread(enrollTask);
            } catch (RuntimeException e) {
                DialogUtil.error(I18n.t("wizard.enroll"), e.getMessage());
            }
        });

        // Wizard step rendering
        Label detailTitle = new Label();
        detailTitle.getStyleClass().add("workflow-title");
        VBox detailBody = new VBox(18);
        VBox detailCard = new VBox(18, detailTitle, detailBody, actions);
        detailCard.getStyleClass().add("workflow-card");
        HBox.setHgrow(detailCard, Priority.ALWAYS);

        List<String> stepTitles = List.of(
                I18n.t("wizard.student"),
                I18n.t("wizard.guardian"),
                I18n.t("wizard.payment")
        );
        List<Node> stepContent = List.of(
                studentForm,
                guardianForm,
                new VBox(12, courseForm, firstPayment, paymentForm)
        );
        List<Button> stepButtons = new ArrayList<>();
        VBox stepList = new VBox(8);
        stepList.getStyleClass().add("workflow-list");
        Label stepListTitle = new Label(I18n.t("wizard.enrollment"));
        stepListTitle.getStyleClass().add("workflow-list-title");
        stepList.getChildren().add(stepListTitle);

        int[] activeStep = {0};
        Runnable[] renderStep = new Runnable[1];
        renderStep[0] = () -> {
            detailTitle.setText(stepTitles.get(activeStep[0]));
            detailBody.getChildren().setAll(stepContent.get(activeStep[0]));
            for (int i = 0; i < stepButtons.size(); i++) {
                stepButtons.get(i).getStyleClass().remove("workflow-step-active");
                if (i == activeStep[0]) stepButtons.get(i).getStyleClass().add("workflow-step-active");
            }
            previous.setDisable(activeStep[0] == 0);
            next.setVisible(activeStep[0] < stepTitles.size() - 1);
            next.setManaged(activeStep[0] < stepTitles.size() - 1);
            enroll.setVisible(activeStep[0] == stepTitles.size() - 1);
            enroll.setManaged(activeStep[0] == stepTitles.size() - 1);
        };

        for (int i = 0; i < stepTitles.size(); i++) {
            int stepIndex = i;
            Button step = new Button((i + 1) + ". " + stepTitles.get(i));
            step.getStyleClass().add("workflow-step");
            step.setMaxWidth(Double.MAX_VALUE);
            step.setOnAction(event -> { activeStep[0] = stepIndex; renderStep[0].run(); });
            stepButtons.add(step);
            stepList.getChildren().add(step);
        }

        previous.setOnAction(event -> { if (activeStep[0] > 0) { activeStep[0]--; renderStep[0].run(); } });
        next.setOnAction(event -> {
            try {
                validateEnrollmentStep(activeStep[0], firstName, lastName, gender, birthDate, classroom, bloodGroup,
                        guardianFirstName, guardianLastName, relationship, phone, email,
                        course, firstPayment, amount, method, category);
                activeStep[0]++;
                renderStep[0].run();
            } catch (RuntimeException e) {
                DialogUtil.error(I18n.t("wizard.next"), e.getMessage());
            }
        });

        renderStep[0].run();

        HBox workflow = new HBox(22, stepList, detailCard);
        workflow.setAlignment(Pos.TOP_LEFT);

        VBox root = new VBox(18, workflow);
        root.setPadding(new Insets(24));
        contentPane.setCenter(root);
    }

    private void showEnrollSuccessCard(String studentName) {
        Label icon = new Label("✅");
        icon.setStyle("-fx-font-size: 40px;");
        Label title = new Label(I18n.t("wizard.success"));
        title.getStyleClass().add("success-card-title");
        Label body = new Label(studentName);
        body.getStyleClass().add("success-card-body");
        body.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #065F46;");

        Button newOne = new Button("➕  " + I18n.t("action.new_student"));
        newOne.getStyleClass().add("primary-button");
        newOne.setOnAction(e -> showNewStudentWizard());

        Button goToList = new Button("📋  " + I18n.t("nav.students"));
        goToList.getStyleClass().add("secondary-button");
        goToList.setOnAction(e -> {
            showModule(registry.byTable("students"));
        });

        HBox btns = new HBox(12, newOne, goToList);
        btns.setAlignment(Pos.CENTER);

        VBox card = new VBox(16, icon, title, body, btns);
        card.getStyleClass().add("success-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(480);

        StackPane center = new StackPane(card);
        center.setPadding(new Insets(60));
        contentPane.setCenter(center);
    }

    /* ── Module table view ───────────────────────────────────────── */

    private void showModule(Module module) {
        activeModule = module;
        pageTitleLabel.setText(I18n.t(module.titleKey()));
        if ("settings".equals(module.table())) {
            showSettingsPage();
            return;
        }
        TableView<Map<String, String>> table = new TableView<>();
        table.getStyleClass().addAll("data-table", module.table() + "-table");
        table.setFixedCellSize(38);
        table.setPlaceholder(new Label(I18n.t("table.no_records")));
        buildColumns(table, module);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.getStyleClass().add("form-grid");
        Map<String, Node> editors = buildForm(module, form);

        Button save   = new Button(I18n.t("action.save"));   save.getStyleClass().add("primary-button");
        Button clear  = new Button(I18n.t("action.clear"));  clear.getStyleClass().add("secondary-button");
        Button delete = new Button(I18n.t("action.delete")); delete.getStyleClass().add("danger-button");
        HBox actions = new HBox(10, save, clear, delete);
        actions.setAlignment(Pos.CENTER_LEFT);

        ObservableList<Map<String, String>> rows = FXCollections.observableArrayList();
        FilteredList<Map<String, String>> filteredRows = new FilteredList<>(rows, row -> true);
        table.setItems(filteredRows);

        TextField filter = textField(I18n.t("action.filter") + " " + I18n.t(module.titleKey()).toLowerCase());
        filter.getStyleClass().add("filter-field");
        filter.textProperty().addListener((obs, old, query) -> {
            String needle = query == null ? "" : query.trim().toLowerCase();
            filteredRows.setPredicate(row -> needle.isBlank() || row.values().stream()
                    .filter(v -> v != null)
                    .anyMatch(v -> v.toLowerCase().contains(needle)));
        });

        HBox tableToolbar = new HBox(10, filter);
        tableToolbar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(filter, Priority.ALWAYS);

        if ("students".equals(module.table())) {
            Button newStudent = new Button("➕  " + I18n.t("action.new_student"));
            newStudent.getStyleClass().add("primary-button");
            newStudent.setMinWidth(140);
            newStudent.setOnAction(event -> showNewStudentWizard());
            tableToolbar.getChildren().add(newStudent);
        }

        Runnable reload = () -> {
            Task<List<Map<String, String>>> loadTask = new Task<>() {
                @Override
                protected List<Map<String, String>> call() {
                    return dao.findAll(module.table(), module.columns(), module.orderBy());
                }
            };
            loadTask.setOnSucceeded(e -> rows.setAll(loadTask.getValue()));
            loadTask.setOnFailed(e -> DialogUtil.error("Chargement échoué", loadTask.getException().getMessage()));
            startDaemonThread(loadTask);
        };

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                module.fields().forEach(field -> {
                    String v = "password_hash".equals(field.column()) ? "" : selected.get(field.column());
                    setEditorValue(editors.get(field.column()), v);
                });
            }
        });

        clear.setOnAction(event -> {
            table.getSelectionModel().clearSelection();
            editors.values().forEach(ed -> setEditorValue(ed, ""));
        });

        save.setOnAction(event -> {
            try {
                Map<String, String> values = readEditors(module, editors);
                Map<String, String> selected = table.getSelectionModel().getSelectedItem();
                if ("users".equals(module.table()) && selected != null
                        && getEditorValue(editors.get("password_hash")).isBlank()) {
                    values.put("password_hash", selected.get("password_hash"));
                }
                final boolean isInsert = (selected == null);
                if (!isInsert) values.put("id", selected.get("id"));

                save.setDisable(true);
                Task<Void> saveTask = new Task<>() {
                    @Override protected Void call() {
                        if (isInsert) dao.insert(module.table(), module.columns(), values);
                        else          dao.update(module.table(), module.columns(), values);
                        return null;
                    }
                };
                saveTask.setOnSucceeded(e -> { save.setDisable(false); reload.run(); clear.fire(); });
                saveTask.setOnFailed(e -> {
                    save.setDisable(false);
                    DialogUtil.error(I18n.t("action.save"), saveTask.getException().getMessage());
                });
                startDaemonThread(saveTask);
            } catch (RuntimeException e) {
                DialogUtil.error(I18n.t("action.save"), e.getMessage());
            }
        });

        delete.setOnAction(event -> {
            Map<String, String> selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                DialogUtil.info(I18n.t("action.delete"), "Sélectionnez un enregistrement avant de supprimer.");
                return;
            }
            if (DialogUtil.confirm(I18n.t("action.delete"),
                    "Supprimer cet enregistrement de " + I18n.t(module.titleKey()).toLowerCase() + " ?")) {
                int id = Integer.parseInt(selected.get("id"));
                delete.setDisable(true);
                Task<Void> delTask = new Task<>() {
                    @Override protected Void call() { dao.delete(module.table(), id); return null; }
                };
                delTask.setOnSucceeded(e -> { delete.setDisable(false); reload.run(); clear.fire(); });
                delTask.setOnFailed(e -> {
                    delete.setDisable(false);
                    DialogUtil.error(I18n.t("action.delete"), delTask.getException().getMessage());
                });
                startDaemonThread(delTask);
            }
        });

        reload.run();

        VBox formPanel = new VBox(14, new Label(I18n.t("table.details")), form, actions);
        formPanel.getStyleClass().add("side-panel");
        VBox tablePanel = new VBox(10, tableToolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        HBox workspace = new HBox(18, tablePanel, formPanel);
        HBox.setHgrow(tablePanel, Priority.ALWAYS);
        workspace.setPadding(new Insets(24));

        contentPane.setCenter(workspace);
    }

    /* ── Column builder ─────────────────────────────────────────── */

    private void buildColumns(TableView<Map<String, String>> table, Module module) {
        TableColumn<Map<String, String>, String> id = new TableColumn<>("#");
        id.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().get("id")));
        id.setPrefWidth(48);
        id.setStyle("-fx-alignment: CENTER;");
        table.getColumns().add(id);

        List<Field> visibleFields = module.fields().stream()
                .filter(f -> !"password_hash".equals(f.column()))
                .toList();

        for (int i = 0; i < visibleFields.size(); i++) {
            Field field = visibleFields.get(i);
            TableColumn<Map<String, String>, String> column = new TableColumn<>(field.label().toUpperCase());
            column.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().get(field.column())));
            if (i == visibleFields.size() - 1) {
                column.prefWidthProperty().bind(table.widthProperty().subtract(48).subtract(140 * (visibleFields.size() - 1)).subtract(2));
            } else {
                column.setPrefWidth(140);
            }
            // ... rest of cellFactory logic unchanged
            table.getColumns().add(column);
        }
    }
    /** Returns inline badge color style based on status/gender value. */
    private String badgeStyle(String value) {
        if (value == null) return "";
        return switch (value.toUpperCase()) {
            case "ACTIVE", "PRESENT", "PAID", "COMPLETED", "FEMALE", "FILLE" ->
                    "-fx-background-color: #D1FAE5; -fx-text-fill: #065F46;";
            case "INACTIVE", "ABSENT", "OVERDUE", "DROPPED" ->
                    "-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B;";
            case "LATE", "PENDING" ->
                    "-fx-background-color: #FEF3C7; -fx-text-fill: #92400E;";
            case "MALE", "GARÇON" ->
                    "-fx-background-color: #DBEAFE; -fx-text-fill: #1E40AF;";
            default ->
                    "-fx-background-color: #F1F5F9; -fx-text-fill: #475569;";
        };
    }

    /* ── Form builder ───────────────────────────────────────────── */

    private Map<String, Node> buildForm(Module module, GridPane form) {
        Map<String, Node> editors = new LinkedHashMap<>();
        int row = 0;
        for (Field field : module.fields()) {
            Label label = new Label(field.label());
            Node editor = field.options().isEmpty() ? new TextField() : new ComboBox<String>();
            if (editor instanceof TextField tf) tf.setPromptText(field.label());
            if (editor instanceof ComboBox<?> cb) {
                @SuppressWarnings("unchecked")
                ComboBox<String> typed = (ComboBox<String>) cb;
                typed.setItems(FXCollections.observableArrayList(field.options()));
                typed.setMaxWidth(Double.MAX_VALUE);
            }
            editors.put(field.column(), editor);
            form.add(label, 0, row);
            form.add(editor, 1, row);
            GridPane.setHgrow(editor, Priority.ALWAYS);
            row++;
        }
        return editors;
    }

    private Map<String, String> readEditors(Module module, Map<String, Node> editors) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Field field : module.fields()) {
            String v = getEditorValue(editors.get(field.column()));
            if ("password_hash".equals(field.column())) {
                v = v.isBlank() ? BCrypt.hashpw("changeme", BCrypt.gensalt()) : BCrypt.hashpw(v, BCrypt.gensalt());
            }
            if ("created_at".equals(field.column()) && v.isBlank()) {
                v = LocalDate.now().toString();
            }
            values.put(field.column(), v);
        }
        return values;
    }

    private String getEditorValue(Node editor) {
        if (editor instanceof TextField tf) return tf.getText();
        if (editor instanceof ComboBox<?> cb) {
            Object v = cb.getValue();
            return v == null ? "" : v.toString();
        }
        return "";
    }

    private void setEditorValue(Node editor, String value) {
        if (editor instanceof TextField tf) tf.setText(value == null ? "" : value);
        if (editor instanceof ComboBox<?> cb) {
            @SuppressWarnings("unchecked")
            ComboBox<String> typed = (ComboBox<String>) cb;
            typed.setValue(value == null || value.isBlank() ? null : value);
        }
    }

    /* ── Helpers ─────────────────────────────────────────────────── */

    private TextField textField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private ComboBox<String> comboBox(List<String> options) {
        ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList(options));
        cb.setMaxWidth(Double.MAX_VALUE);
        return cb;
    }

    private GridPane sectionGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.getStyleClass().add("form-grid");
        ColumnConstraints label = new ColumnConstraints(120);
        ColumnConstraints input = new ColumnConstraints();
        input.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(label, input);
        return grid;
    }

    private void addRow(GridPane grid, int row, String label, Node editor) {
        grid.add(new Label(label), 0, row);
        grid.add(editor, 1, row);
        GridPane.setHgrow(editor, Priority.ALWAYS);
    }

    private void requireField(TextField field, String labelKey) {
        if (field.getText() == null || field.getText().isBlank()) {
            field.getStyleClass().add("field-error");
            throw new IllegalArgumentException(I18n.t(labelKey) + " est requis.");
        }
        field.getStyleClass().remove("field-error");
    }
    private void requireDate(DatePicker picker, String labelKey) {
        if (picker.getValue() == null) {
            picker.getStyleClass().add("field-error");
            throw new IllegalArgumentException(I18n.t(labelKey) + " est requis.");
        }
        picker.getStyleClass().remove("field-error");
    }
    private void requireCombo(ComboBox<String> cb, String labelKey) {
        if (value(cb).isBlank()) {
            throw new IllegalArgumentException(I18n.t(labelKey) + " est requis.");
        }
    }

    private void validateEnrollmentStep(int step, TextField fn, TextField ln,
                                        ComboBox<String> gender, DatePicker birthDate, ComboBox<String> cls, ComboBox<String> bloodGroup,
                                        TextField gFn, TextField gLn, ComboBox<String> relationship, TextField phone, TextField email,
                                        ComboBox<String> course, CheckBox firstPayment, TextField amount, ComboBox<String> method, ComboBox<String> category) {
        if (step == 0) {
            requireField(ln, "field.last_name");
            requireField(fn, "field.first_name");
            requireCombo(gender, "field.gender");
            requireDate(birthDate, "field.date_of_birth");
            requireCombo(cls, "field.classroom");
            requireCombo(bloodGroup, "field.blood_group");
        }
        if (step == 1) {
            requireField(gLn, "field.last_name");
            requireField(gFn, "field.first_name");
            requireCombo(relationship, "field.relationship");
            requireField(phone, "field.phone");
            requireField(email, "field.email");
        }
        if (step == 2) {
            requireCombo(course, "field.course");
            if (firstPayment.isSelected()) {
                requireField(amount, "field.amount");
                requireCombo(method, "field.method");
                requireCombo(category, "field.category");
            }
        }
    }


    @FXML
    private void handleBackup() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.t("backup.choose_location"));
        chooser.setInitialFileName("rawdati_backup_" + LocalDate.now() + ".dump");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PostgreSQL Dump", "*.dump"));
        File target = chooser.showSaveDialog(rootPane.getScene().getWindow());
        if (target == null) return;

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                backupRestoreService.backup(target);
                return null;
            }
        };
        task.setOnSucceeded(e -> DialogUtil.info(I18n.t("backup.title"), I18n.t("backup.success")));
        task.setOnFailed(e -> DialogUtil.error(I18n.t("backup.title"), task.getException().getMessage()));
        startDaemonThread(task);
    }

    @FXML
    private void handleRestore() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.t("restore.choose_file"));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PostgreSQL Dump", "*.dump"));
        File source = chooser.showOpenDialog(rootPane.getScene().getWindow());
        if (source == null) return;

        if (!DialogUtil.confirm(I18n.t("restore.title"), I18n.t("restore.confirm_overwrite"))) {
            return;
        }

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                backupRestoreService.restore(source);
                return null;
            }
        };
        task.setOnSucceeded(e -> DialogUtil.info(I18n.t("restore.title"), I18n.t("restore.success")));
        task.setOnFailed(e -> DialogUtil.error(I18n.t("restore.title"), task.getException().getMessage()));
        startDaemonThread(task);
    }
    private String value(ComboBox<String> cb) {
        String v = cb.getValue();
        return v == null ? "" : v.trim();
    }

    private long safeCount(String table) {
        try { return dao.count(table); } catch (Exception e) { return 0; }
    }

    private List<Map<String, String>> safeFind(String table, List<String> cols, String order, int limit) {
        try {
            List<Map<String, String>> all = dao.findAll(table, cols, order);
            return all.size() > limit ? all.subList(0, limit) : all;
        } catch (Exception e) { return List.of(); }
    }

    private void startDaemonThread(Runnable r) {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.start();
    }

    private static final String LICENSE_KEY_SETTING = "license_key";

    /** Builds the "Licence & Activation" card shown at the top of the Settings module. */
    private static final String TRIAL_START_SETTING  = "trial_start_date";
    private static final int    TRIAL_DAYS            = 7;

    /** Builds the "Licence & Activation" card, styled to match the app mockup. */
    private VBox buildLicenseCard() {
        String machineId = machineIdentifier.getOrCreateMachineId();
        String storedKey = settingsRepository.get(LICENSE_KEY_SETTING);
        boolean activated = storedKey != null && licenseValidator.isValid(machineId, storedKey);

        // ── Header ────────────────────────────────────────────────
        Label icon = new Label("🛡️");
        icon.setStyle("-fx-font-size: 20px;");
        Label title = new Label("Licence & Activation");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        HBox header = new HBox(10, icon, title);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(18, header);
        card.setPadding(new Insets(28));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 2);");

        // ── Trial / status banner ────────────────────────────────
        if (!activated) {
            long daysLeft = licenseActivationDialog.getTrialDaysLeft();
            VBox banner = new VBox(4);
            banner.setPadding(new Insets(14, 18, 14, 18));
            banner.setStyle("-fx-background-color: #FEF3C7; -fx-background-radius: 8; "
                    + "-fx-border-color: #FDE68A; -fx-border-radius: 8; -fx-border-width: 1;");
            Label bannerBody = new Label(daysLeft > 0
                    ? "Il vous reste " + daysLeft + " jour" + (daysLeft > 1 ? "s" : "") + " d'essai gratuit."
                    : "Votre période d'essai est terminée. Activez pour continuer.");
            bannerBody.setStyle("-fx-text-fill: #92400E;");
            bannerBody.setWrapText(true);
            banner.getChildren().add(bannerBody);
            card.getChildren().add(banner);
        } else {
            VBox banner = new VBox(4);
            banner.setPadding(new Insets(14, 18, 14, 18));
            banner.setStyle("-fx-background-color: #D1FAE5; -fx-background-radius: 8; "
                    + "-fx-border-color: #A7F3D0; -fx-border-radius: 8; -fx-border-width: 1;");
            Label bannerBody = new Label("Licence active. Merci d'utiliser Rawdati !");
            bannerBody.setStyle("-fx-text-fill: #065F46; -fx-font-weight: bold;");
            banner.getChildren().add(bannerBody);
            card.getChildren().add(banner);
        }

        // ── Step 1: machine ID ───────────────────────────────────
        Label step1 = new Label("1. Votre Identifiant Machine");
        step1.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label step1Caption = new Label("Envoyez ce code à l'administrateur pour recevoir votre clé.");
        step1Caption.setStyle("-fx-text-fill: #64748B;");

        TextField idField = new TextField(machineId);
        idField.setEditable(false);
        idField.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10;");
        HBox.setHgrow(idField, Priority.ALWAYS);

        Button copyBtn = new Button("📋");
        copyBtn.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 12;");
        copyBtn.setTooltip(new Tooltip("Copier"));
        copyBtn.setOnAction(e -> {
            var clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            var content = new javafx.scene.input.ClipboardContent();
            content.putString(machineId);
            clipboard.setContent(content);
        });

        HBox idRow = new HBox(8, idField, copyBtn);

        VBox step1Box = new VBox(8, step1, step1Caption, idRow);

        // ── Step 2: activation key ───────────────────────────────
        Label step2 = new Label("2. Entrer la Clé d'Activation");
        step2.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        TextField keyField = new TextField();
        keyField.setPromptText("Collez votre clé ici...");
        keyField.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: #E2E8F0; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10;");
        HBox.setHgrow(keyField, Priority.ALWAYS);
        keyField.setDisable(activated);

        Button activateBtn = new Button("Activer");
        activateBtn.setStyle("-fx-background-color: #6D5EF5; -fx-text-fill: white; "
                + "-fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 24;");
        activateBtn.setDisable(activated);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #DC2626;");
        errorLabel.setWrapText(true);

        activateBtn.setOnAction(e -> {
            String candidate = keyField.getText() == null ? "" : keyField.getText().trim();
            if (licenseValidator.isValid(machineId, candidate)) {
                settingsRepository.set(LICENSE_KEY_SETTING, candidate);
                showModule(activeModule); // rebuild the page in its activated state
            } else {
                errorLabel.setText("Clé invalide pour cette machine.");
            }
        });

        HBox keyRow = new HBox(8, keyField, activateBtn);

        VBox step2Box = new VBox(10, step2, keyRow, errorLabel);
        step2Box.setPadding(new Insets(18));
        step2Box.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 10; "
                + "-fx-border-color: #F1F5F9; -fx-border-radius: 10; -fx-border-width: 1;");

        card.getChildren().addAll(step1Box, step2Box);
        return card;
    }

    /** Reads (or initializes) a trial-start date and returns days remaining. */
    private long computeTrialDaysLeft() {
        String stored = settingsRepository.get(TRIAL_START_SETTING);
        LocalDate start;
        if (stored == null || stored.isBlank()) {
            start = LocalDate.now();
            settingsRepository.set(TRIAL_START_SETTING, start.toString());
        } else {
            start = LocalDate.parse(stored);
        }
        long elapsed = Period.between(start, LocalDate.now()).getDays();
        return Math.max(0, TRIAL_DAYS - elapsed);
    }
    private void showSettingsPage() {
        VBox licenseCard = buildLicenseCard();

        VBox root = new VBox(20, licenseCard);
        root.setPadding(new Insets(24));

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        contentPane.setCenter(scroll);
    }
}