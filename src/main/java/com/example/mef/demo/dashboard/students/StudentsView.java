package com.example.mef.demo.dashboard.students;

import com.example.mef.demo.Model.Student;
import com.example.mef.demo.Services.StudentService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.dashboard.table.TableBuilder;
import com.example.mef.demo.enums.Sexe;
import com.example.mef.demo.util.DialogUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

/** Typed CRUD screen for the "students" module (replaces generic ModuleTableView for this table). */
@Component
public class StudentsView {

    private final StudentService studentService;

    private final ObservableList<Student> rows = FXCollections.observableArrayList();
    private final TableView<Student> table = new TableView<>(rows);

    private final TextField searchField = FormFactory.textField("Rechercher un élève...");
    private final TextField firstNameField = FormFactory.textField("Prénom");
    private final TextField lastNameField = FormFactory.textField("Nom");
    private final ComboBox<Sexe> genderField = new ComboBox<>(FXCollections.observableArrayList(Sexe.values()));
    private final DatePicker dobField = new DatePicker();
    private final TextArea medicalInfoField = new TextArea();

    private Student selected;
    private Runnable onEnrollNew;

    public StudentsView(StudentService studentService) {
        this.studentService = studentService;
        genderField.setMaxWidth(Double.MAX_VALUE);
        medicalInfoField.setPromptText("Informations médicales");
        medicalInfoField.setPrefRowCount(3);
    }

    /** @param onEnrollNew invoked when the user wants to run the full enrollment wizard instead of a bare add. */
    public void render(BorderPane contentPane, Label pageTitleLabel, Runnable onEnrollNew) {
        this.onEnrollNew = onEnrollNew;
        pageTitleLabel.setText("Élèves");

        buildColumns();

        Button enroll = new Button("+ Nouvel élève (assistant)");
        enroll.getStyleClass().add("primary-button");
        enroll.setOnAction(e -> onEnrollNew.run());

        HBox toolbar = new HBox(10, searchField, enroll);
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
        reload();
    }

    private void buildColumns() {
        table.getColumns().clear();
        TableColumn<Student, String> number = new TableColumn<>("N°");
        number.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyStringWrapper(d.getValue().getStudentNumber()));
        TableColumn<Student, String> name = new TableColumn<>("Nom");
        name.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyStringWrapper(
                d.getValue().getFirstName() + " " + d.getValue().getLastName()));
        name.setPrefWidth(180);
        TableColumn<Student, String> gender = new TableColumn<>("Genre");
        gender.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyStringWrapper(
                d.getValue().getGender() == null ? "" : d.getValue().getGender().name()));
        TableColumn<Student, String> age = new TableColumn<>("Âge");
        age.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyStringWrapper(ageLabel(d.getValue())));

        table.getColumns().addAll(List.of(number, name, gender, age));
    }

    private String ageLabel(Student s) {
        if (s.getDateOfBirth() == null) return "—";
        Period p = Period.between(s.getDateOfBirth().toLocalDate(), LocalDate.now());
        return p.getYears() > 0 ? p.getYears() + " ans" : p.getMonths() + " mois";
    }

    private VBox buildForm() {
        GridPane grid = FormFactory.sectionGrid();
        FormFactory.addRow(grid, 0, "Prénom", firstNameField);
        FormFactory.addRow(grid, 1, "Nom", lastNameField);
        FormFactory.addRow(grid, 2, "Genre", genderField);
        FormFactory.addRow(grid, 3, "Naissance", dobField);
        FormFactory.addRow(grid, 4, "Médical", medicalInfoField);

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
        return new VBox(12, new Label("Détails de l'élève"), grid, actions);
    }

    private void selectRow(Student student) {
        selected = student;
        if (student == null) {
            clearForm();
            return;
        }
        firstNameField.setText(student.getFirstName());
        lastNameField.setText(student.getLastName());
        genderField.setValue(student.getGender());
        dobField.setValue(student.getDateOfBirth() == null ? null : student.getDateOfBirth().toLocalDate());
        medicalInfoField.setText(student.getMedicalInfo());
    }

    private void clearForm() {
        selected = null;
        firstNameField.clear();
        lastNameField.clear();
        genderField.setValue(null);
        dobField.setValue(null);
        medicalInfoField.clear();
        table.getSelectionModel().clearSelection();
    }

    private void save() {
        if (firstNameField.getText().isBlank() || lastNameField.getText().isBlank()) {
            DialogUtil.error("Champs requis", "Le prénom et le nom sont obligatoires.");
            return;
        }
        Student student = selected != null ? selected : new Student();
        student.setFirstName(firstNameField.getText().trim());
        student.setLastName(lastNameField.getText().trim());
        student.setGender(genderField.getValue());
        student.setDateOfBirth(dobField.getValue() == null ? null : dobField.getValue().atStartOfDay());
        student.setMedicalInfo(medicalInfoField.getText());

        AsyncTasks.run(
                () -> studentService.save(student),
                saved -> { clearForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de l'enregistrement : " + err.getMessage())
        );
    }

    private void delete() {
        if (selected == null) return;
        if (!DialogUtil.confirm("Confirmer", "Supprimer cet élève ?")) return;
        String id = selected.getId();
        AsyncTasks.run(
                () -> studentService.delete(id),
                () -> { clearForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de la suppression : " + err.getMessage())
        );
    }

    private void reload() {
        String needle = searchField.getText();
        AsyncTasks.run(
                () -> studentService.search(needle),
                list -> rows.setAll(list),
                err -> DialogUtil.error("Erreur", "Échec du chargement : " + err.getMessage())
        );
    }
}