package com.example.mef.demo.controller;


import com.example.mef.demo.Model.User;
import com.example.mef.demo.Repository.UserRepository;
import com.example.mef.demo.config.Session;
import com.example.mef.demo.util.DialogUtil;
import com.example.mef.demo.util.SceneManager;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;
import com.example.mef.demo.service.DynamicDatabaseService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DashboardController {

    @FXML private Label pageTitleLabel;
    @FXML private Label userLabel;
    @FXML private VBox navigationBox;
    @FXML private BorderPane contentPane;
    
    @Autowired
    private DynamicDatabaseService dao;
    
    private final List<Module> modules = new ArrayList<>();

    @FXML
    private void initialize() {

        User current = Session.getCurrentUser();
        if (current != null) {
            userLabel.setText(current.getFullName() + " · " + current.getRole());
        }

        registerModules();
        buildNavigation();
        showDashboard();
    }

    @FXML
    private void handleLogout() {
        Session.logout();
        SceneManager.switchTo("/fxml/login.fxml", "/css/style.css");
    }

    private void registerModules() {
        modules.add(new Module("Students", "students", "last_name, first_name",
                List.of(
                        new Field("first_name", "First name"),
                        new Field("last_name", "Last name"),
                        new Field("gender", "Gender", List.of("Female", "Male", "Other")),
                        new Field("date_of_birth", "Birth date"),
                        new Field("classroom", "Classroom"),
                        new Field("status", "Status", List.of("ACTIVE", "INACTIVE"))
                )));
        modules.add(new Module("Teachers", "teachers", "last_name, first_name",
                List.of(
                        new Field("first_name", "First name"),
                        new Field("last_name", "Last name"),
                        new Field("email", "Email"),
                        new Field("phone", "Phone"),
                        new Field("specialty", "Specialty"),
                        new Field("status", "Status", List.of("ACTIVE", "INACTIVE"))
                )));
        modules.add(new Module("Classes", "classes", "name",
                List.of(
                        new Field("name", "Class"),
                        new Field("grade_level", "Grade level"),
                        new Field("room", "Room"),
                        new Field("teacher_name", "Teacher"),
                        new Field("capacity", "Capacity"),
                        new Field("status", "Status", List.of("ACTIVE", "INACTIVE"))
                )));
        modules.add(new Module("Guardians", "guardians", "last_name, first_name",
                List.of(
                        new Field("first_name", "First name"),
                        new Field("last_name", "Last name"),
                        new Field("relationship", "Relationship"),
                        new Field("phone", "Phone"),
                        new Field("email", "Email"),
                        new Field("student_name", "Student")
                )));
        modules.add(new Module("Courses", "courses", "name",
                List.of(
                        new Field("name", "Course"),
                        new Field("teacher_name", "Teacher"),
                        new Field("classroom", "Classroom"),
                        new Field("schedule", "Schedule"),
                        new Field("monthly_fee", "Monthly fee"),
                        new Field("status", "Status", List.of("ACTIVE", "INACTIVE"))
                )));
        modules.add(new Module("Attendance", "attendance", "attendance_date DESC",
                List.of(
                        new Field("attendance_date", "Date"),
                        new Field("student_name", "Student"),
                        new Field("course_name", "Course"),
                        new Field("status", "Status", List.of("PRESENT", "ABSENT", "LATE")),
                        new Field("notes", "Notes")
                )));
        modules.add(new Module("Enrollments", "enrollments", "enrollment_date DESC",
                List.of(
                        new Field("enrollment_date", "Date"),
                        new Field("student_name", "Student"),
                        new Field("course_name", "Course"),
                        new Field("status", "Status", List.of("ACTIVE", "COMPLETED", "DROPPED"))
                )));
        modules.add(new Module("Payments", "payments", "payment_date DESC",
                List.of(
                        new Field("payment_date", "Date"),
                        new Field("student_name", "Student"),
                        new Field("amount", "Amount"),
                        new Field("method", "Method", List.of("Cash", "Card", "Transfer", "Check")),
                        new Field("category", "Category", List.of("Tuition", "Course", "Transport", "Other")),
                        new Field("status", "Status", List.of("PAID", "PENDING", "OVERDUE"))
                )));
        modules.add(new Module("Reports", "reports", "created_at DESC",
                List.of(
                        new Field("title", "Title"),
                        new Field("report_type", "Type", List.of("Academic", "Financial", "Attendance", "General")),
                        new Field("created_at", "Date"),
                        new Field("summary", "Summary")
                )));
        modules.add(new Module("Login & Roles", "users", "full_name",
                List.of(
                        new Field("username", "Username"),
                        new Field("password_hash", "Password"),
                        new Field("full_name", "Full name"),
                        new Field("role", "Role", List.of("ADMIN", "TEACHER", "STAFF"))
                )));
        modules.add(new Module("Settings", "settings", "setting_key",
                List.of(
                        new Field("setting_key", "Setting"),
                        new Field("setting_value", "Value"),
                        new Field("description", "Description")
                )));
    }

    private void buildNavigation() {
        navigationBox.getChildren().clear();
        Button dashboard = navButton("Dashboard");
        dashboard.setOnAction(event -> showDashboard());
        navigationBox.getChildren().add(dashboard);

        for (Module module : modules) {
            Button button = navButton(module.title());
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

    private void showDashboard() {
        pageTitleLabel.setText("Dashboard");
        Label loading = new Label("Loading dashboard…");
        contentPane.setCenter(loading);

        Task<DashboardData> task = new Task<>() {
            @Override
            protected DashboardData call() {
                long students  = dao.count("students");
                long teachers  = dao.count("teachers");
                long classes   = dao.count("classes");
                long courses   = dao.count("courses");
                long guardians = dao.count("guardians");
                long payments  = dao.count("payments");
                double total   = dao.sum("payments", "amount");
                Map<String, Integer> attendance = dao.attendanceSummary();
                return new DashboardData(students, teachers, classes, courses,
                        guardians, payments, total, attendance);
            }
        };
        task.setOnSucceeded(e -> {
            DashboardData d = task.getValue();

            GridPane stats = new GridPane();
            stats.setHgap(14);
            stats.setVgap(14);
            stats.add(stat("Students",  d.students),  0, 0);
            stats.add(stat("Teachers",  d.teachers),  1, 0);
            stats.add(stat("Classes",   d.classes),   2, 0);
            stats.add(stat("Courses",   d.courses),   3, 0);
            stats.add(stat("Payments",  "$" + String.format("%.2f", d.totalPayments)), 0, 1);

            PieChart chart = new PieChart(FXCollections.observableArrayList(
                    new PieChart.Data("Present", d.attendance.getOrDefault("PRESENT", 0)),
                    new PieChart.Data("Absent",  d.attendance.getOrDefault("ABSENT",  0)),
                    new PieChart.Data("Late",    d.attendance.getOrDefault("LATE",    0))
            ));
            chart.setTitle("Attendance");
            chart.setLegendVisible(true);

            TextArea report = new TextArea();
            report.setEditable(false);
            report.setWrapText(true);
            report.setText("""
                    School summary

                    Students: %d
                    Teachers: %d
                    Classes: %d
                    Guardians: %d
                    Active courses: %d
                    Payments recorded: %d
                    Total collected: $%.2f
                    """.formatted(
                    d.students, d.teachers, d.classes,
                    d.guardians, d.courses, d.payments, d.totalPayments));

            HBox bottom = new HBox(16, chart, report);
            HBox.setHgrow(chart, Priority.ALWAYS);
            HBox.setHgrow(report, Priority.ALWAYS);

            VBox root = new VBox(18, stats, bottom);
            root.setPadding(new Insets(24));
            contentPane.setCenter(root);
        });
        task.setOnFailed(e -> contentPane.setCenter(new Label("Failed to load dashboard.")));
        startDaemonThread(task);
    }

    private record DashboardData(
            long students, long teachers, long classes, long courses,
            long guardians, long payments, double totalPayments,
            Map<String, Integer> attendance) {}

    private void showNewStudentWizard() {
        pageTitleLabel.setText("New Student");
        contentPane.setCenter(new Label("Loading…"));

        // Load combo data off the FX thread first, then build the form
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
        loadTask.setOnFailed(e -> contentPane.setCenter(new Label("Failed to load wizard data.")));
        startDaemonThread(loadTask);
    }

    private record WizardData(List<String> classrooms, List<String> courses) {}

    private void buildStudentWizard(WizardData data) {
        TextField firstName = textField("First name");
        TextField lastName = textField("Last name");
        ComboBox<String> gender = comboBox(List.of("Female", "Male", "Other"));
        DatePicker birthDate = new DatePicker();
        ComboBox<String> classroom = comboBox(data.classrooms());
        classroom.setEditable(true);

        TextField guardianFirstName = textField("First name");
        TextField guardianLastName = textField("Last name");
        ComboBox<String> relationship = comboBox(List.of("Mother", "Father", "Guardian", "Other"));
        TextField phone = textField("Phone");
        TextField email = textField("Email");

        ComboBox<String> course = comboBox(data.courses());
        course.setEditable(true);

        CheckBox firstPayment = new CheckBox("Record first payment");
        TextField amount = textField("Amount");
        ComboBox<String> method = comboBox(List.of("Cash", "Card", "Transfer", "Check"));
        ComboBox<String> category = comboBox(List.of("Tuition", "Course", "Transport", "Other"));
        amount.setDisable(true);
        method.setDisable(true);
        category.setDisable(true);
        firstPayment.selectedProperty().addListener((obs, old, selected) -> {
            amount.setDisable(!selected);
            method.setDisable(!selected);
            category.setDisable(!selected);
        });

        GridPane studentForm = sectionGrid();
        addRow(studentForm, 0, "First name", firstName);
        addRow(studentForm, 1, "Last name", lastName);
        addRow(studentForm, 2, "Gender", gender);
        addRow(studentForm, 3, "Birth date", birthDate);
        addRow(studentForm, 4, "Class", classroom);

        GridPane guardianForm = sectionGrid();
        addRow(guardianForm, 0, "First name", guardianFirstName);
        addRow(guardianForm, 1, "Last name", guardianLastName);
        addRow(guardianForm, 2, "Relationship", relationship);
        addRow(guardianForm, 3, "Phone", phone);
        addRow(guardianForm, 4, "Email", email);

        GridPane courseForm = sectionGrid();
        addRow(courseForm, 0, "Course", course);

        GridPane paymentForm = sectionGrid();
        addRow(paymentForm, 0, "Amount", amount);
        addRow(paymentForm, 1, "Method", method);
        addRow(paymentForm, 2, "Category", category);

        Button enroll = new Button("Enroll Student");
        enroll.getStyleClass().add("primary-button");
        enroll.setVisible(false);
        enroll.setManaged(false);
        Button clear = new Button("Clear");
        clear.getStyleClass().add("secondary-button");
        Button previous = new Button("Previous");
        previous.getStyleClass().add("secondary-button");
        Button next = new Button("Next");
        next.getStyleClass().add("primary-button");
        HBox actions = new HBox(10, previous, next, enroll, clear);
        actions.setAlignment(Pos.CENTER_LEFT);

        clear.setOnAction(event -> {
            List.of(firstName, lastName, guardianFirstName, guardianLastName, phone, email, amount)
                    .forEach(TextField::clear);
            gender.setValue(null);
            birthDate.setValue(null);
            classroom.setValue(null);
            relationship.setValue(null);
            course.setValue(null);
            method.setValue(null);
            category.setValue(null);
            firstPayment.setSelected(false);
        });

        enroll.setOnAction(event -> {
            try {
                require(firstName, "Student first name");
                require(lastName, "Student last name");
                require(classroom, "Class");
                require(guardianFirstName, "Guardian first name");
                require(guardianLastName, "Guardian last name");
                if (firstPayment.isSelected()) {
                    require(amount, "Payment amount");
                }

                Map<String, String> student = new LinkedHashMap<>();
                student.put("first_name", firstName.getText());
                student.put("last_name", lastName.getText());
                student.put("gender", value(gender));
                student.put("date_of_birth", birthDate.getValue() == null ? "" : birthDate.getValue().toString());
                student.put("classroom", value(classroom));
                student.put("status", "ACTIVE");

                Map<String, String> guardian = new LinkedHashMap<>();
                guardian.put("first_name", guardianFirstName.getText());
                guardian.put("last_name", guardianLastName.getText());
                guardian.put("relationship", value(relationship));
                guardian.put("phone", phone.getText());
                guardian.put("email", email.getText());

                Map<String, String> payment = null;
                if (firstPayment.isSelected()) {
                    payment = new LinkedHashMap<>();
                    payment.put("amount", amount.getText());
                    payment.put("method", value(method));
                    payment.put("category", value(category));
                    payment.put("status", "PAID");
                }

                final Map<String, String> paymentFinal = payment;
                final String studentFirst = firstName.getText().trim();
                final String studentLast = lastName.getText().trim();
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
                    DialogUtil.info("Student enrolled", studentFirst + " " + studentLast + " has been added.");
                    clear.fire();
                });
                enrollTask.setOnFailed(ev -> {
                    enroll.setDisable(false);
                    DialogUtil.error("Could not enroll student", enrollTask.getException().getMessage());
                });
                startDaemonThread(enrollTask);
            } catch (RuntimeException e) {
                DialogUtil.error("Could not enroll student", e.getMessage());
            }
        });


        Label detailTitle = new Label();
        detailTitle.getStyleClass().add("workflow-title");
        VBox detailBody = new VBox(18);
        VBox detailCard = new VBox(18, detailTitle, detailBody, actions);
        detailCard.getStyleClass().add("workflow-card");
        HBox.setHgrow(detailCard, Priority.ALWAYS);

        List<String> stepTitles = List.of("Student", "Guardian", "Course", "Payment");
        List<Node> stepContent = List.of(
                studentForm,
                guardianForm,
                courseForm,
                new VBox(12, firstPayment, paymentForm)
        );
        List<Button> stepButtons = new ArrayList<>();
        VBox stepList = new VBox(8);
        stepList.getStyleClass().add("workflow-list");
        Label stepListTitle = new Label("Enrollment workflow");
        stepListTitle.getStyleClass().add("workflow-list-title");
        stepList.getChildren().add(stepListTitle);

        int[] activeStep = {0};
        Runnable[] renderStep = new Runnable[1];
        renderStep[0] = () -> {
            detailTitle.setText(stepTitles.get(activeStep[0]));
            detailBody.getChildren().setAll(stepContent.get(activeStep[0]));
            for (int i = 0; i < stepButtons.size(); i++) {
                stepButtons.get(i).getStyleClass().remove("workflow-step-active");
                if (i == activeStep[0]) {
                    stepButtons.get(i).getStyleClass().add("workflow-step-active");
                }
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
            step.setOnAction(event -> {
                activeStep[0] = stepIndex;
                renderStep[0].run();
            });
            stepButtons.add(step);
            stepList.getChildren().add(step);
        }

        previous.setOnAction(event -> {
            if (activeStep[0] > 0) {
                activeStep[0]--;
                renderStep[0].run();
            }
        });

        next.setOnAction(event -> {
            try {
                validateEnrollmentStep(activeStep[0], firstName, lastName, classroom, guardianFirstName, guardianLastName);
                activeStep[0]++;
                renderStep[0].run();
            } catch (RuntimeException e) {
                DialogUtil.error("Missing information", e.getMessage());
            }
        });

        renderStep[0].run();

        HBox workflow = new HBox(22, stepList, detailCard);
        workflow.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(detailCard, Priority.ALWAYS);

        VBox root = new VBox(18, workflow);
        root.setPadding(new Insets(24));
        contentPane.setCenter(root);
    }

    private VBox stat(String label, Object value) {
        Label number = new Label(String.valueOf(value));
        number.getStyleClass().add("stat-number");
        Label caption = new Label(label);
        caption.getStyleClass().add("stat-caption");
        VBox box = new VBox(6, number, caption);
        box.getStyleClass().add("stat-box");
        box.setMinWidth(180);
        return box;
    }

    private void showModule(Module module) {
        pageTitleLabel.setText(module.title());

        TableView<Map<String, String>> table = new TableView<>();
        table.getStyleClass().add("data-table");
        buildColumns(table, module);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.getStyleClass().add("form-grid");
        Map<String, Node> editors = buildForm(module, form);

        Button save = new Button("Save");
        save.getStyleClass().add("primary-button");
        Button clear = new Button("Clear");
        clear.getStyleClass().add("secondary-button");
        Button delete = new Button("Delete");
        delete.getStyleClass().add("danger-button");
        HBox actions = new HBox(10, save, clear, delete);
        actions.setAlignment(Pos.CENTER_LEFT);

        ObservableList<Map<String, String>> rows = FXCollections.observableArrayList();
        FilteredList<Map<String, String>> filteredRows = new FilteredList<>(rows, row -> true);
        table.setItems(filteredRows);

        TextField filter = textField("Filter " + module.title().toLowerCase());
        filter.getStyleClass().add("filter-field");
        filter.textProperty().addListener((obs, old, query) -> {
            String needle = query == null ? "" : query.trim().toLowerCase();
            filteredRows.setPredicate(row -> needle.isBlank() || row.values().stream()
                    .filter(value -> value != null)
                    .anyMatch(value -> value.toLowerCase().contains(needle)));
        });

        HBox tableToolbar = new HBox(10, filter);
        tableToolbar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(filter, Priority.ALWAYS);
        if ("students".equals(module.table())) {
            Button newStudent = new Button("New Student");
            newStudent.getStyleClass().add("primary-button");
            newStudent.setMinWidth(120);
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
            loadTask.setOnFailed(e -> DialogUtil.error("Load failed", loadTask.getException().getMessage()));
            startDaemonThread(loadTask);
        };

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                module.fields().forEach(field -> {
                    String value = "password_hash".equals(field.column()) ? "" : selected.get(field.column());
                    setEditorValue(editors.get(field.column()), value);
                });
            }
        });

        clear.setOnAction(event -> {
            table.getSelectionModel().clearSelection();
            editors.values().forEach(editor -> setEditorValue(editor, ""));
        });

        save.setOnAction(event -> {
            try {
                Map<String, String> values = readEditors(module, editors);
                Map<String, String> selected = table.getSelectionModel().getSelectedItem();
                if ("users".equals(module.table())
                        && selected != null
                        && getEditorValue(editors.get("password_hash")).isBlank()) {
                    values.put("password_hash", selected.get("password_hash"));
                }
                final boolean isInsert = (selected == null);
                if (!isInsert) values.put("id", selected.get("id"));

                save.setDisable(true);
                Task<Void> saveTask = new Task<>() {
                    @Override
                    protected Void call() {
                        if (isInsert) dao.insert(module.table(), module.columns(), values);
                        else          dao.update(module.table(), module.columns(), values);
                        return null;
                    }
                };
                saveTask.setOnSucceeded(e -> { save.setDisable(false); reload.run(); clear.fire(); });
                saveTask.setOnFailed(e -> {
                    save.setDisable(false);
                    DialogUtil.error("Could not save", saveTask.getException().getMessage());
                });
                startDaemonThread(saveTask);
            } catch (RuntimeException e) {
                DialogUtil.error("Could not save", e.getMessage());
            }
        });

        delete.setOnAction(event -> {
            Map<String, String> selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                DialogUtil.info("Select a row", "Choose a record before deleting.");
                return;
            }
            if (DialogUtil.confirm("Delete record", "Delete the selected " + module.title().toLowerCase() + " record?")) {
                int id = Integer.parseInt(selected.get("id"));
                delete.setDisable(true);
                Task<Void> delTask = new Task<>() {
                    @Override protected Void call() { dao.delete(module.table(), id); return null; }
                };
                delTask.setOnSucceeded(e -> { delete.setDisable(false); reload.run(); clear.fire(); });
                delTask.setOnFailed(e -> {
                    delete.setDisable(false);
                    DialogUtil.error("Delete failed", delTask.getException().getMessage());
                });
                startDaemonThread(delTask);
            }
        });

        reload.run();

        VBox formPanel = new VBox(14, new Label("Details"), form, actions);
        formPanel.getStyleClass().add("side-panel");
        VBox tablePanel = new VBox(10, tableToolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        HBox workspace = new HBox(18, tablePanel, formPanel);
        HBox.setHgrow(tablePanel, Priority.ALWAYS);
        workspace.setPadding(new Insets(24));
        contentPane.setCenter(workspace);
    }

    private void buildColumns(TableView<Map<String, String>> table, Module module) {
        TableColumn<Map<String, String>, String> id = new TableColumn<>("ID");
        id.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().get("id")));
        id.setPrefWidth(60);
        table.getColumns().add(id);

        for (Field field : module.fields()) {
            if ("password_hash".equals(field.column())) {
                continue;
            }
            TableColumn<Map<String, String>, String> column = new TableColumn<>(field.label());
            column.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().get(field.column())));
            column.setPrefWidth(140);
            table.getColumns().add(column);
        }
    }

    private Map<String, Node> buildForm(Module module, GridPane form) {
        Map<String, Node> editors = new LinkedHashMap<>();
        int row = 0;
        for (Field field : module.fields()) {
            Label label = new Label(field.label());
            Node editor = field.options().isEmpty() ? new TextField() : new ComboBox<String>();
            if (editor instanceof TextField textField) {
                textField.setPromptText(field.label());
            }
            if (editor instanceof ComboBox<?> comboBox) {
                @SuppressWarnings("unchecked")
                ComboBox<String> typed = (ComboBox<String>) comboBox;
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
            String value = getEditorValue(editors.get(field.column()));
            if ("password_hash".equals(field.column())) {
                value = value.isBlank() ? BCrypt.hashpw("changeme", BCrypt.gensalt()) : BCrypt.hashpw(value, BCrypt.gensalt());
            }
            if ("created_at".equals(field.column()) && value.isBlank()) {
                value = LocalDate.now().toString();
            }
            values.put(field.column(), value);
        }
        return values;
    }

    private String getEditorValue(Node editor) {
        if (editor instanceof TextField textField) {
            return textField.getText();
        }
        if (editor instanceof ComboBox<?> comboBox) {
            Object value = comboBox.getValue();
            return value == null ? "" : value.toString();
        }
        return "";
    }

    private void setEditorValue(Node editor, String value) {
        if (editor instanceof TextField textField) {
            textField.setText(value == null ? "" : value);
        }
        if (editor instanceof ComboBox<?> comboBox) {
            @SuppressWarnings("unchecked")
            ComboBox<String> typed = (ComboBox<String>) comboBox;
            typed.setValue(value == null || value.isBlank() ? null : value);
        }
    }

    private TextField textField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private ComboBox<String> comboBox(List<String> options) {
        ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(options));
        comboBox.setMaxWidth(Double.MAX_VALUE);
        return comboBox;
    }

    private GridPane sectionGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.getStyleClass().add("form-grid");
        return grid;
    }

    private void addRow(GridPane grid, int row, String label, Node editor) {
        grid.add(new Label(label), 0, row);
        grid.add(editor, 1, row);
        GridPane.setHgrow(editor, Priority.ALWAYS);
    }

    private VBox panel(String title, Node content) {
        VBox panel = new VBox(14, new Label(title), content);
        panel.getStyleClass().add("side-panel");
        panel.setMaxWidth(Double.MAX_VALUE);
        return panel;
    }

    private void require(TextField field, String label) {
        if (field.getText() == null || field.getText().isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
    }

    private void require(ComboBox<String> comboBox, String label) {
        if (value(comboBox).isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
    }

    private void validateEnrollmentStep(
            int step,
            TextField firstName,
            TextField lastName,
            ComboBox<String> classroom,
            TextField guardianFirstName,
            TextField guardianLastName
    ) {
        if (step == 0) {
            require(firstName, "Student first name");
            require(lastName, "Student last name");
            require(classroom, "Class");
        }
        if (step == 1) {
            require(guardianFirstName, "Guardian first name");
            require(guardianLastName, "Guardian last name");
        }
    }

    private String value(ComboBox<String> comboBox) {
        String value = comboBox.getValue();
        return value == null ? "" : value.trim();
    }

    private record Field(String column, String label, List<String> options) {
        Field(String column, String label) {
            this(column, label, List.of());
        }
    }

    private record Module(String title, String table, String orderBy, List<Field> fields) {
        List<String> columns() {
            return fields.stream().map(Field::column).toList();
        }
    }

    private void startDaemonThread(Runnable r) {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.start();
    }
}
