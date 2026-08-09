package com.example.mef.demo.dashboard.courses;

import com.example.mef.demo.Model.Classroom;
import com.example.mef.demo.Model.Course;
import com.example.mef.demo.Model.CourseScheduleSlot;
import com.example.mef.demo.Model.Employee;
import com.example.mef.demo.Services.ClassroomService;
import com.example.mef.demo.Services.CourseService;
import com.example.mef.demo.Services.EmployeeService;
import com.example.mef.demo.Services.ScheduleSettingsKeys;
import com.example.mef.demo.Services.SettingService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.enums.CourseStatus;
import com.example.mef.demo.util.DialogUtil;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/** Typed CRUD screen for the "courses" module (new Course entity). */
@Component
public class CoursesView {

    private final CourseService courseService;
    private final EmployeeService employeeService;
    private final ClassroomService classroomService;
    private final SettingService settingService;

    private final ObservableList<Course> rows = FXCollections.observableArrayList();
    private final TableView<Course> table = new TableView<>(rows);
    { com.example.mef.demo.dashboard.common.TableStyleKit.applyTheme(table, "courses"); }

    private final TextField searchField = FormFactory.textField("Rechercher un cours...");
    private final TextField nameField = FormFactory.textField("Nom du cours");
    private final TextField scheduleField = FormFactory.textField("Aucun horaire choisi");
    private final Button scheduleButton = new Button("Choisir…");
    private final TextField feeField = FormFactory.textField("Frais mensuels");
    private final ComboBox<Employee> teacherField = new ComboBox<>();
    private final ComboBox<Classroom> classroomField = new ComboBox<>();
    private final ComboBox<CourseStatus> statusField = new ComboBox<>(FXCollections.observableArrayList(CourseStatus.values()));

    Boolean  suppressSelectionListener ;
    private Course selected;

    public CoursesView(CourseService courseService, EmployeeService employeeService,
                       ClassroomService classroomService, SettingService settingService) {
        this.courseService = courseService;
        this.employeeService = employeeService;
        this.classroomService = classroomService;
        this.settingService = settingService;
        teacherField.setMaxWidth(Double.MAX_VALUE);
        classroomField.setMaxWidth(Double.MAX_VALUE);
        statusField.setMaxWidth(Double.MAX_VALUE);
        teacherField.setCellFactory(cb -> teacherCell());
        teacherField.setButtonCell(teacherCell());
        classroomField.setCellFactory(cb -> classroomCell());
        classroomField.setButtonCell(classroomCell());

        scheduleField.setEditable(false);
        scheduleField.setFocusTraversable(false);
        scheduleButton.getStyleClass().add("secondary-button");
        scheduleButton.setOnAction(e -> openSchedulePicker());
    }

    private void openSchedulePicker() {
        SchedulePickerDialog.show(scheduleButton.getScene() == null ? null : scheduleButton.getScene().getWindow(),
                        scheduleField.getText())
                .ifPresent(scheduleField::setText);
    }

    private ListCell<Employee> teacherCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Employee item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getFirstName() + " " + item.getLastName());
            }
        };
    }

    private ListCell<Classroom> classroomCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Classroom item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        };
    }

    public void render(BorderPane contentPane, Label pageTitleLabel) {
        pageTitleLabel.setText("Cours");

        table.getColumns().clear();
        TableColumn<Course, String> name = new TableColumn<>("Nom");
        name.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getName()));
        name.setPrefWidth(180);
        TableColumn<Course, String> teacher = new TableColumn<>("Enseignant");
        teacher.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getTeacher() == null ? "—" :
                        d.getValue().getTeacher().getFirstName() + " " + d.getValue().getTeacher().getLastName()));
        TableColumn<Course, String> classroom = new TableColumn<>("Classe");
        classroom.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getClassroom() == null ? "—" : d.getValue().getClassroom().getName()));
        TableColumn<Course, String> status = new TableColumn<>("Statut");
        status.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getStatus() == null ? "" : d.getValue().getStatus().name()));
        TableColumn<Course, Course> schedule = new TableColumn<>("Horaire");
        schedule.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        schedule.setCellFactory(column -> scheduleCell());
        schedule.setPrefWidth(260);
        table.getColumns().addAll(List.of(name, teacher, classroom, status, schedule));

        HBox toolbar = new HBox(10, searchField);
        toolbar.setPadding(new Insets(0, 0, 10, 0));
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.textProperty().addListener((obs, old, val) -> reload());

        VBox listPane = new VBox(10, toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        listPane.setMaxWidth(620);
         Course selected;

/** Prevents recursive selection events when clearing or updating the table. */
        boolean suppressSelectionListener = false;
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (suppressSelectionListener) {
                return;
            }
            if (val != null) {
                selectRow(val);
            }
        });
        VBox form = buildForm();

        BorderPane layout = new BorderPane();
        layout.setCenter(listPane);
        layout.setRight(form);
        BorderPane.setAlignment(listPane, Pos.TOP_LEFT);
        BorderPane.setMargin(form, new Insets(15, 10, 10, 16));
        layout.setPadding(new Insets(10, 20, 5, 0));
        form.setPrefWidth(320);
        form.getStyleClass().add("cours-details");
        VBox.setMargin(form,  new Insets(10, 20, 10, 20));
        contentPane.setCenter(layout);

        loadPickers();
        reload();
    }

    private void loadPickers() {
        AsyncTasks.run(employeeService::findTeachers,
                list -> teacherField.setItems(FXCollections.observableArrayList(list)),
                err -> DialogUtil.error("Erreur", "Échec du chargement des enseignants : " + err.getMessage()));
        AsyncTasks.run(classroomService::findAll,
                list -> classroomField.setItems(FXCollections.observableArrayList(list)),
                err -> DialogUtil.error("Erreur", "Échec du chargement des classes : " + err.getMessage()));
    }

    private VBox buildForm() {
        GridPane grid = FormFactory.sectionGrid();
        FormFactory.addRow(grid, 0, "Nom", nameField);
        FormFactory.addRow(grid, 1, "Enseignant", teacherField);
        FormFactory.addRow(grid, 2, "Classe", classroomField);
        HBox scheduleRow = new HBox(8, scheduleField, scheduleButton);
        HBox.setHgrow(scheduleField, Priority.ALWAYS);
        FormFactory.addRow(grid, 3, "Horaire", scheduleRow);
        FormFactory.addRow(grid, 4, "Frais/mois", feeField);
        FormFactory.addRow(grid, 5, "Statut", statusField);

        Button save = new Button("Enregistrer");
        save.getStyleClass().add("primary-button");
        save.setOnAction(e -> save());
        Button clear = new Button("Nouveau");
        clear.getStyleClass().add("secondary-button");
        clear.setOnAction(e -> clearForm());
        Button delete = new Button("Supprimer");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(e -> delete());

        return new VBox(12, new Label("Détails du cours"), grid, new HBox(8, save, clear, delete));
    }

    /** Compact day/time badges showing the course's selected timetable slots. */
    private TableCell<Course, Course> scheduleCell() {
        return new TableCell<>() {
            {
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Course course, boolean empty) {
                super.updateItem(course, empty);
                setText(null);
                if (empty || course == null) {
                    setGraphic(null);
                    return;
                }
                List<ScheduleValidator.Slot> slots = ScheduleValidator.parse(course.getSchedule());
                if (slots.isEmpty()) {
                    setGraphic(new Label("—"));
                    return;
                }
                FlowPane badges = new FlowPane(6, 4);
                badges.setAlignment(Pos.CENTER);
                int visible = Math.min(3, slots.size());
                for (int i = 0; i < visible; i++) {
                    ScheduleValidator.Slot slot = slots.get(i);
                    String label = shortDay(slot.day()) + " " + formatTime(slot.startMinutes())
                            + "–" + formatTime(slot.endMinutes());
                    badges.getChildren().add(com.example.mef.demo.dashboard.common.TableStyleKit
                            .pill(label, "#CFFAFE", "#0E7490"));
                }
                if (slots.size() > visible) {
                    badges.getChildren().add(com.example.mef.demo.dashboard.common.TableStyleKit
                            .pill("+" + (slots.size() - visible), "#E0F2FE", "#0369A1"));
                }
                setGraphic(badges);
            }
        };
    }

    private void selectRow(Course course) {
        if (course == null) {
            return;
        }
        selected = course;
        nameField.setText(course.getName());
        scheduleField.setText(course.getSchedule());
        feeField.setText(course.getMonthlyFee() == null ? "" : String.valueOf(course.getMonthlyFee()));
        teacherField.setValue(course.getTeacher());
        classroomField.setValue(course.getClassroom());
        statusField.setValue(course.getStatus());
    }

    private void clearForm() {
        suppressSelectionListener = true;
        try {
            selected = null;
            nameField.clear();
            scheduleField.clear();
            feeField.clear();
            teacherField.setValue(null);
            classroomField.setValue(null);
            statusField.setValue(null);

            if (table.getSelectionModel().getSelectedIndex() >= 0) {
                table.getSelectionModel().clearSelection();
            }
        } finally {
            suppressSelectionListener = false;
        }
    }

    private void save() {
        if (nameField.getText().isBlank()) {
            DialogUtil.error("Champs requis", "Le nom du cours est obligatoire.");
            return;
        }
        double fee;
        try {
            fee = feeField.getText().isBlank() ? 0.0 : Double.parseDouble(feeField.getText().trim().replace(",", "."));
        } catch (NumberFormatException ex) {
            DialogUtil.error("Valeur invalide", "Les frais mensuels doivent être un nombre.");
            return;
        }

        Course course = selected != null ? selected : new Course();
        course.setName(nameField.getText().trim());
        course.setSchedule(scheduleField.getText());
        course.replaceScheduleSlots(toScheduleSlots(scheduleField.getText()));
        course.setMonthlyFee(fee);
        course.setStatus(statusField.getValue() == null ? CourseStatus.ACTIVE : statusField.getValue());
        course.setTeacher(teacherField.getValue());
        course.setClassroom(classroomField.getValue());
        String teacherId = teacherField.getValue() == null ? null : teacherField.getValue().getId();
        String classroomId = classroomField.getValue() == null ? null : classroomField.getValue().getId();

        AsyncTasks.run(
                () -> {
                    List<Course> others = courseService.findAll();
                    Set<String> closedDays = ScheduleValidator.daysOf(
                            settingService.get(ScheduleSettingsKeys.CLOSED_DAYS, ScheduleSettingsKeys.CLOSED_DAYS_DEFAULT));
                    int enrolled = classroomId == null ? 0 : classroomService.countStudentsInClassroom(classroomId);
                    return ScheduleValidator.validate(course, others, closedDays, enrolled);
                },
                violations -> {
                    if (!violations.isEmpty()) {
                        DialogUtil.error("Conflit d'horaire",
                                "Impossible d'enregistrer ce cours :\n\n" + String.join("\n", violations));
                        return;
                    }
                    AsyncTasks.run(
                            () -> courseService.save(course, teacherId, classroomId),
                            saved -> { clearForm(); reload(); },
                            err -> DialogUtil.error("Erreur", "Échec de l'enregistrement : " + err.getMessage())
                    );
                },
                err -> DialogUtil.error("Erreur", "Échec de la validation de l'horaire : " + err.getMessage())
        );
    }

    private void delete() {
        if (selected == null) return;
        if (!DialogUtil.confirm("Confirmer", "Supprimer ce cours ?")) return;
        String id = selected.getId();
        AsyncTasks.run(
                () -> courseService.delete(id),
                () -> { clearForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de la suppression : " + err.getMessage())
        );
    }

    private void reload() {
        String needle = searchField.getText();
        AsyncTasks.run(
                () -> courseService.search(needle),
                list -> {
                    suppressSelectionListener = true;
                    try {
                        selected = null;
                        table.getSelectionModel().clearSelection();
                        rows.setAll(list);
                    } finally {
                        suppressSelectionListener = false;
                    }
                },
                err -> DialogUtil.error("Erreur", "Échec du chargement : " + err.getMessage())
        );
    }

    private static List<CourseScheduleSlot> toScheduleSlots(String schedule) {
        return ScheduleValidator.parse(schedule).stream()
                .map(slot -> new CourseScheduleSlot(
                        slot.day(), formatTime(slot.startMinutes()), formatTime(slot.endMinutes())))
                .toList();
    }

    private static String formatTime(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }

    private static String shortDay(String day) {
        return switch (day) {
            case "Lundi" -> "Lun";
            case "Mardi" -> "Mar";
            case "Mercredi" -> "Mer";
            case "Jeudi" -> "Jeu";
            case "Vendredi" -> "Ven";
            case "Samedi" -> "Sam";
            case "Dimanche" -> "Dim";
            default -> day;
        };
    }
}
