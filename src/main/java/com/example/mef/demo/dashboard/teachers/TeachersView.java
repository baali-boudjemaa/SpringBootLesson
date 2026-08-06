package com.example.mef.demo.dashboard.teachers;

import com.example.mef.demo.Model.Course;
import com.example.mef.demo.Model.Employee;
import com.example.mef.demo.Services.CourseService;
import com.example.mef.demo.Services.EmployeeService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.DaysPicker;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.dashboard.courses.ScheduleValidator;
import com.example.mef.demo.enums.EmployeeRole;
import com.example.mef.demo.util.DialogUtil;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/** Typed CRUD screen for the "teachers" module (Employee entity). */
@Component
public class TeachersView {

    private final EmployeeService employeeService;
    private final CourseService courseService;

    private final ObservableList<Employee> rows = FXCollections.observableArrayList();
    private final TableView<Employee> table = new TableView<>(rows);
    { com.example.mef.demo.dashboard.common.TableStyleKit.applyTheme(table, "teachers"); }

    private final TextField searchField = FormFactory.textField("Rechercher un employé...");
    private final Label countLabel = new Label();
    private final TextField firstNameField = FormFactory.textField("Prénom");
    private final TextField lastNameField = FormFactory.textField("Nom");
    private final TextField emailField = FormFactory.textField("Email");
    private final TextField phoneField = FormFactory.textField("Téléphone");
    private final ComboBox<EmployeeRole> roleField = new ComboBox<>(FXCollections.observableArrayList(EmployeeRole.values()));
    private final TextArea certificationsField = new TextArea();
    private final TextField availabilityField = FormFactory.textField("Aucun horaire défini");
    private final Button availabilityButton = new Button("Choisir…");
    private final Button timetableButton = new Button("📅  Emploi du temps");

    private String currentWorkingDays = "";
    private String currentWorkStart = "";
    private String currentWorkEnd = "";

    private Employee selected;

    public TeachersView(EmployeeService employeeService, CourseService courseService) {
        this.employeeService = employeeService;
        this.courseService = courseService;
        roleField.setMaxWidth(Double.MAX_VALUE);
        certificationsField.setPromptText("Certifications");
        certificationsField.setPrefRowCount(3);
        availabilityField.setEditable(false);
        availabilityField.setFocusTraversable(false);
        availabilityButton.getStyleClass().add("secondary-button");
        availabilityButton.setOnAction(e -> openAvailabilityPicker());
        timetableButton.getStyleClass().add("secondary-button");
        timetableButton.setOnAction(e -> showTeacherTimetable());
        updateAvailabilitySummary();
    }

    private void openAvailabilityPicker() {
        TeacherAvailabilityDialog.show(
                availabilityButton.getScene() == null ? null : availabilityButton.getScene().getWindow(),
                currentWorkingDays, currentWorkStart, currentWorkEnd
        ).ifPresent(result -> {
            currentWorkingDays = result.workingDays();
            currentWorkStart = result.workStartTime();
            currentWorkEnd = result.workEndTime();
            updateAvailabilitySummary();
        });
    }

    private void updateAvailabilitySummary() {
        if (currentWorkingDays == null || currentWorkingDays.isBlank()) {
            availabilityField.setText("");
            return;
        }
        String days = String.join(", ", currentWorkingDays.split(","));
        String hours = (currentWorkStart == null || currentWorkStart.isBlank()
                || currentWorkEnd == null || currentWorkEnd.isBlank())
                ? "" : "  ·  " + currentWorkStart + "–" + currentWorkEnd;
        availabilityField.setText(days + hours);
    }

    public void render(BorderPane contentPane, Label pageTitleLabel) {
        pageTitleLabel.setText("Personnel");

        table.getColumns().clear();
        TableColumn<Employee, String> number = new TableColumn<>("N°");
        number.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getEmployeeNumber()));
        number.setPrefWidth(140);
        TableColumn<Employee, String> name = new TableColumn<>("Nom");
        name.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getFirstName() + " " + d.getValue().getLastName()));
        name.setPrefWidth(140);
        TableColumn<Employee, String> role = new TableColumn<>("Rôle");
        role.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getRole() == null ? "" : d.getValue().getRole().name()));
        TableColumn<Employee, String> email = new TableColumn<>("Email");
        email.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getEmail()));
        email.setPrefWidth(140);
        table.getColumns().addAll(List.of(number, name, role, email));

        Label title = new Label("Personnel");
        title.getStyleClass().add("page-title");
        countLabel.getStyleClass().add("stat-caption");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox headerRow = new HBox(12, title, spacer);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        VBox headerBlock = new VBox(4, headerRow, countLabel);

        searchField.getStyleClass().add("filter-field");
        searchField.textProperty().addListener((obs, old, val) -> reload());

        VBox listPane = new VBox(18, headerBlock, searchField, table);
        listPane.setPadding(new Insets(24, 20, 24, 24));
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> selectRow(val));

        VBox form = buildForm();
        ScrollPane formScroll = new ScrollPane(form);
        formScroll.setFitToWidth(true);
        formScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        formScroll.getStyleClass().add("panel-scroll");
        formScroll.setPrefWidth(320);
        formScroll.setMinWidth(300);

        BorderPane layout = new BorderPane();
        layout.setCenter(listPane);
        layout.setRight(formScroll);
        BorderPane.setMargin(formScroll, new Insets(24, 24, 24, 0));

        contentPane.setCenter(layout);
        reload();
    }

    private VBox buildForm() {
        GridPane grid = FormFactory.sectionGrid();
        FormFactory.addRow(grid, 0, "Prénom", firstNameField);
        FormFactory.addRow(grid, 1, "Nom", lastNameField);
        FormFactory.addRow(grid, 2, "Email", emailField);
        FormFactory.addRow(grid, 3, "Téléphone", phoneField);
        FormFactory.addRow(grid, 4, "Rôle", roleField);
        FormFactory.addRow(grid, 5, "Certifications", certificationsField);
        HBox availabilityRow = new HBox(8, availabilityField, availabilityButton);
        availabilityRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(availabilityField, Priority.ALWAYS);
        FormFactory.addRow(grid, 6, "Disponibilité", availabilityRow);

        Button save = new Button("Enregistrer");
        save.getStyleClass().add("primary-button");
        save.setOnAction(e -> save());
        Button clear = new Button("Nouveau");
        clear.getStyleClass().add("secondary-button");
        clear.setOnAction(e -> clearForm());
        Button delete = new Button("Supprimer");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(e -> delete());

        HBox actions = new HBox(8, save, clear, delete);

        Label formTitle = new Label("Détails de l'employé");
        formTitle.getStyleClass().add("section-title");

        VBox panel = new VBox(14, formTitle, grid, actions, timetableButton);
        panel.getStyleClass().add("side-panel");
        panel.setPrefWidth(320);
        panel.setMinWidth(300);
        return panel;
    }

    /** Modal dialog listing every course/session taught by the selected teacher. */
    private void showTeacherTimetable() {
        if (selected == null) {
            DialogUtil.info("Emploi du temps", "Sélectionnez d'abord un enseignant dans la liste.");
            return;
        }
        Employee teacher = selected;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (timetableButton.getScene() != null) {
            dialog.initOwner(timetableButton.getScene().getWindow());
        }
        dialog.setTitle("Emploi du temps — " + teacher.getFirstName() + " " + teacher.getLastName());
        dialog.setMinWidth(420);
        dialog.setMinHeight(360);

        ListView<String> listView = new ListView<>();
        listView.setPlaceholder(new Label("Aucune séance planifiée pour cet enseignant."));

        Label loading = new Label("Chargement...");
        VBox root = new VBox(12, loading);
        root.setPadding(new Insets(20));
        root.setMinSize(400, 320);

        AsyncTasks.run(
                courseService::findAll,
                allCourses -> {
                    root.getChildren().remove(loading);

                    record Row(String day, int start, String label) {}
                    List<Row> lines = new java.util.ArrayList<>();
                    for (Course c : allCourses) {
                        if (c.getTeacher() == null || !teacher.getId().equals(c.getTeacher().getId())) continue;
                        for (ScheduleValidator.Slot slot : ScheduleValidator.parse(c.getSchedule())) {
                            String classroomName = c.getClassroom() == null ? "—" : c.getClassroom().getName();
                            lines.add(new Row(slot.day(), slot.startMinutes(),
                                    slot.day() + "  " + formatSlot(slot) + "   —   " + c.getName() + " (" + classroomName + ")"));
                        }
                    }
                    lines.sort(Comparator.comparing(Row::day, Comparator.comparingInt(DaysPicker.DAYS::indexOf))
                            .thenComparingInt(Row::start));
                    listView.getItems().setAll(lines.stream().map(Row::label).toList());

                    Button closeBtn = new Button("Fermer");
                    closeBtn.getStyleClass().add("secondary-button");
                    closeBtn.setOnAction(ev -> dialog.close());

                    root.getChildren().addAll(listView, new HBox(10, closeBtn));
                },
                err -> {
                    root.getChildren().remove(loading);
                    root.getChildren().add(new Label("Erreur : " + err.getMessage()));
                }
        );

        dialog.setScene(new Scene(root, 420, 380));
        dialog.showAndWait();
    }

    private static String formatSlot(ScheduleValidator.Slot slot) {
        return String.format("%02d:%02d-%02d:%02d",
                slot.startMinutes() / 60, slot.startMinutes() % 60,
                slot.endMinutes() / 60, slot.endMinutes() % 60);
    }

    private void selectRow(Employee employee) {
        selected = employee;
        if (employee == null) { clearForm(); return; }
        firstNameField.setText(employee.getFirstName());
        lastNameField.setText(employee.getLastName());
        emailField.setText(employee.getEmail());
        phoneField.setText(employee.getPhoneNumber());
        roleField.setValue(employee.getRole());
        certificationsField.setText(employee.getCertifications());
        currentWorkingDays = employee.getWorkingDays() == null ? "" : employee.getWorkingDays();
        currentWorkStart = employee.getWorkStartTime() == null ? "" : employee.getWorkStartTime();
        currentWorkEnd = employee.getWorkEndTime() == null ? "" : employee.getWorkEndTime();
        updateAvailabilitySummary();
    }

    private void clearForm() {
        selected = null;
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        phoneField.clear();
        roleField.setValue(null);
        certificationsField.clear();
        currentWorkingDays = "";
        currentWorkStart = "";
        currentWorkEnd = "";
        updateAvailabilitySummary();
        table.getSelectionModel().clearSelection();
    }

    private void save() {
        if (firstNameField.getText().isBlank() || lastNameField.getText().isBlank() || emailField.getText().isBlank()) {
            DialogUtil.error("Champs requis", "Le prénom, le nom et l'email sont obligatoires.");
            return;
        }
        Employee employee = selected != null ? selected : new Employee();
        employee.setFirstName(firstNameField.getText().trim());
        employee.setLastName(lastNameField.getText().trim());
        employee.setEmail(emailField.getText().trim());
        employee.setPhoneNumber(phoneField.getText().trim());
        employee.setRole(roleField.getValue());
        employee.setCertifications(certificationsField.getText());
        employee.setWorkingDays(currentWorkingDays.isBlank() ? null : currentWorkingDays);
        employee.setWorkStartTime(currentWorkStart.isBlank() ? null : currentWorkStart);
        employee.setWorkEndTime(currentWorkEnd.isBlank() ? null : currentWorkEnd);

        AsyncTasks.run(
                () -> employeeService.save(employee),
                saved -> { clearForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de l'enregistrement : " + err.getMessage())
        );
    }

    private void delete() {
        if (selected == null) return;
        if (!DialogUtil.confirm("Confirmer", "Supprimer cet employé ?")) return;
        String id = selected.getId();
        AsyncTasks.run(
                () -> employeeService.delete(id),
                () -> { clearForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de la suppression : " + err.getMessage())
        );
    }

    private void reload() {
        String needle = searchField.getText();
        AsyncTasks.run(
                () -> employeeService.search(needle),
                list -> {
                    rows.setAll(list);
                    countLabel.setText(list.size() + " employé" + (list.size() > 1 ? "s" : ""));
                },
                err -> DialogUtil.error("Erreur", "Échec du chargement : " + err.getMessage())
        );
    }
}