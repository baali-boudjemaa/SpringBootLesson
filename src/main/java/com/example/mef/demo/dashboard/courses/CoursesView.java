package com.example.mef.demo.dashboard.courses;

import com.example.mef.demo.Model.Classroom;
import com.example.mef.demo.Model.Course;
import com.example.mef.demo.Model.Employee;
import com.example.mef.demo.Services.ClassroomService;
import com.example.mef.demo.Services.CourseService;
import com.example.mef.demo.Services.EmployeeService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.enums.CourseStatus;
import com.example.mef.demo.util.DialogUtil;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.util.List;

/** Typed CRUD screen for the "courses" module (new Course entity). */
@Component
public class CoursesView {

    private final CourseService courseService;
    private final EmployeeService employeeService;
    private final ClassroomService classroomService;

    private final ObservableList<Course> rows = FXCollections.observableArrayList();
    private final TableView<Course> table = new TableView<>(rows);

    private final TextField searchField = FormFactory.textField("Rechercher un cours...");
    private final TextField nameField = FormFactory.textField("Nom du cours");
    private final TextField scheduleField = FormFactory.textField("Horaire");
    private final TextField feeField = FormFactory.textField("Frais mensuels");
    private final ComboBox<Employee> teacherField = new ComboBox<>();
    private final ComboBox<Classroom> classroomField = new ComboBox<>();
    private final ComboBox<CourseStatus> statusField = new ComboBox<>(FXCollections.observableArrayList(CourseStatus.values()));

    private Course selected;

    public CoursesView(CourseService courseService, EmployeeService employeeService, ClassroomService classroomService) {
        this.courseService = courseService;
        this.employeeService = employeeService;
        this.classroomService = classroomService;
        teacherField.setMaxWidth(Double.MAX_VALUE);
        classroomField.setMaxWidth(Double.MAX_VALUE);
        statusField.setMaxWidth(Double.MAX_VALUE);
        teacherField.setCellFactory(cb -> teacherCell());
        teacherField.setButtonCell(teacherCell());
        classroomField.setCellFactory(cb -> classroomCell());
        classroomField.setButtonCell(classroomCell());
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
        table.getColumns().addAll(List.of(name, teacher, classroom, status));

        HBox toolbar = new HBox(10, searchField);
        toolbar.setPadding(new Insets(0, 0, 10, 0));
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.textProperty().addListener((obs, old, val) -> reload());

        VBox listPane = new VBox(10, toolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> selectRow(val));

        VBox form = buildForm();
        BorderPane layout = new BorderPane();
        layout.setCenter(listPane);
        layout.setRight(form);
        BorderPane.setMargin(form, new Insets(0, 0, 0, 16));
        form.setPrefWidth(320);

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
        FormFactory.addRow(grid, 3, "Horaire", scheduleField);
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

    private void selectRow(Course course) {
        selected = course;
        if (course == null) { clearForm(); return; }
        nameField.setText(course.getName());
        scheduleField.setText(course.getSchedule());
        feeField.setText(course.getMonthlyFee() == null ? "" : String.valueOf(course.getMonthlyFee()));
        teacherField.setValue(course.getTeacher());
        classroomField.setValue(course.getClassroom());
        statusField.setValue(course.getStatus());
    }

    private void clearForm() {
        selected = null;
        nameField.clear();
        scheduleField.clear();
        feeField.clear();
        teacherField.setValue(null);
        classroomField.setValue(null);
        statusField.setValue(null);
        table.getSelectionModel().clearSelection();
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
        course.setMonthlyFee(fee);
        course.setStatus(statusField.getValue() == null ? CourseStatus.ACTIVE : statusField.getValue());
        String teacherId = teacherField.getValue() == null ? null : teacherField.getValue().getId();
        String classroomId = classroomField.getValue() == null ? null : classroomField.getValue().getId();

        AsyncTasks.run(
                () -> courseService.save(course, teacherId, classroomId),
                saved -> { clearForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de l'enregistrement : " + err.getMessage())
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
                list -> rows.setAll(list),
                err -> DialogUtil.error("Erreur", "Échec du chargement : " + err.getMessage())
        );
    }
}