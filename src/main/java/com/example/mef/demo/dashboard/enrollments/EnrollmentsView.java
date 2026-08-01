package com.example.mef.demo.dashboard.enrollments;

import com.example.mef.demo.Model.Classroom;
import com.example.mef.demo.Model.Inscription;
import com.example.mef.demo.Model.Student;
import com.example.mef.demo.Services.ClassroomService;
import com.example.mef.demo.Services.EnrollmentService;
import com.example.mef.demo.Services.StudentService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.enums.EnrollmentStatus;
import com.example.mef.demo.enums.SessionName;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/** Typed CRUD screen for the "enrollments" module (Inscription entity). */
@Component
public class EnrollmentsView {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EnrollmentService enrollmentService;
    private final StudentService studentService;
    private final ClassroomService classroomService;

    private final ObservableList<Inscription> rows = FXCollections.observableArrayList();
    private final TableView<Inscription> table = new TableView<>(rows);
    { com.example.mef.demo.dashboard.common.TableStyleKit.applyTheme(table, "enrollments"); }

    private final ComboBox<Student> studentField = new ComboBox<>();
    private final ComboBox<Classroom> classroomField = new ComboBox<>();
    private final ComboBox<SessionName> sessionField = new ComboBox<>(FXCollections.observableArrayList(SessionName.values()));
    private final ComboBox<EnrollmentStatus> statusField = new ComboBox<>(FXCollections.observableArrayList(EnrollmentStatus.values()));

    private Inscription selected;

    public EnrollmentsView(EnrollmentService enrollmentService, StudentService studentService, ClassroomService classroomService) {
        this.enrollmentService = enrollmentService;
        this.studentService = studentService;
        this.classroomService = classroomService;
        studentField.setMaxWidth(Double.MAX_VALUE);
        classroomField.setMaxWidth(Double.MAX_VALUE);
        sessionField.setMaxWidth(Double.MAX_VALUE);
        statusField.setMaxWidth(Double.MAX_VALUE);
        studentField.setCellFactory(cb -> studentCell());
        studentField.setButtonCell(studentCell());
        classroomField.setCellFactory(cb -> classroomCell());
        classroomField.setButtonCell(classroomCell());
    }

    private ListCell<Student> studentCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Student item, boolean empty) {
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
        pageTitleLabel.setText("Inscriptions");

        table.getColumns().clear();
        TableColumn<Inscription, String> date = new TableColumn<>("Date");
        date.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getDateInscription() == null ? "" : d.getValue().getDateInscription().format(DATE_FORMAT)));
        TableColumn<Inscription, String> student = new TableColumn<>("Élève");
        student.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getStudent() == null ? "—" :
                        d.getValue().getStudent().getFirstName() + " " + d.getValue().getStudent().getLastName()));
        student.setPrefWidth(160);
        TableColumn<Inscription, String> classroom = new TableColumn<>("Classe");
        classroom.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getClassroom() == null ? "—" : d.getValue().getClassroom().getName()));
        TableColumn<Inscription, String> status = new TableColumn<>("Statut");
        status.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getStatus() == null ? "" : d.getValue().getStatus().name()));
        table.getColumns().addAll(List.of(date, student, classroom, status));

        VBox listPane = new VBox(10, table);
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
        AsyncTasks.run(studentService::findAll,
                list -> studentField.setItems(FXCollections.observableArrayList(list)),
                err -> DialogUtil.error("Erreur", "Échec du chargement des élèves : " + err.getMessage()));
        AsyncTasks.run(classroomService::findAll,
                list -> classroomField.setItems(FXCollections.observableArrayList(list)),
                err -> DialogUtil.error("Erreur", "Échec du chargement des classes : " + err.getMessage()));
    }

    private VBox buildForm() {
        GridPane grid = FormFactory.sectionGrid();
        FormFactory.addRow(grid, 0, "Élève", studentField);
        FormFactory.addRow(grid, 1, "Classe", classroomField);
        FormFactory.addRow(grid, 2, "Session", sessionField);
        FormFactory.addRow(grid, 3, "Statut", statusField);

        Button save = new Button("Enregistrer");
        save.getStyleClass().add("primary-button");
        save.setOnAction(e -> save());
        Button clear = new Button("Nouveau");
        clear.getStyleClass().add("secondary-button");
        clear.setOnAction(e -> clearForm());
        Button delete = new Button("Supprimer");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(e -> delete());

        return new VBox(12, new Label("Détails de l'inscription"), grid, new HBox(8, save, clear, delete));
    }

    private void selectRow(Inscription inscription) {
        selected = inscription;
        if (inscription == null) { clearForm(); return; }
        studentField.setValue(inscription.getStudent());
        classroomField.setValue(inscription.getClassroom());
        sessionField.setValue(inscription.getSession());
        statusField.setValue(inscription.getStatus());
    }

    private void clearForm() {
        selected = null;
        studentField.setValue(null);
        classroomField.setValue(null);
        sessionField.setValue(null);
        statusField.setValue(null);
        table.getSelectionModel().clearSelection();
    }

    private void save() {
        if (studentField.getValue() == null || classroomField.getValue() == null) {
            DialogUtil.error("Champs requis", "L'élève et la classe sont obligatoires.");
            return;
        }
        Inscription inscription = selected != null ? selected : new Inscription();
        inscription.setSession(sessionField.getValue() == null ? SessionName.JOURNEE_COMPLETE : sessionField.getValue());
        inscription.setStatus(statusField.getValue() == null ? EnrollmentStatus.ACTIVE : statusField.getValue());
        String studentId = studentField.getValue().getId();
        String classroomId = classroomField.getValue().getId();

        AsyncTasks.run(
                () -> enrollmentService.save(inscription, studentId, classroomId),
                saved -> { clearForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de l'enregistrement : " + err.getMessage())
        );
    }

    private void delete() {
        if (selected == null) return;
        if (!DialogUtil.confirm("Confirmer", "Supprimer cette inscription ?")) return;
        String id = selected.getId();
        AsyncTasks.run(
                () -> enrollmentService.delete(id),
                () -> { clearForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de la suppression : " + err.getMessage())
        );
    }

    private void reload() {
        AsyncTasks.run(
                enrollmentService::findAll,
                list -> rows.setAll(list),
                err -> DialogUtil.error("Erreur", "Échec du chargement : " + err.getMessage())
        );
    }
}