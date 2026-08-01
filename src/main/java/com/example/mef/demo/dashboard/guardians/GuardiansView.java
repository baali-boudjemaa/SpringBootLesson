package com.example.mef.demo.dashboard.guardians;

import com.example.mef.demo.Model.Guardian;
import com.example.mef.demo.Model.Student;
import com.example.mef.demo.Services.GuardianService;
import com.example.mef.demo.Services.StudentService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FormFactory;
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

/** Typed CRUD screen for the "guardians" module, with a real typed link to Student. */
@Component
public class GuardiansView {

    private final GuardianService guardianService;
    private final StudentService studentService;

    private final ObservableList<Guardian> rows = FXCollections.observableArrayList();
    private final TableView<Guardian> table = new TableView<>(rows);

    private final TextField searchField = FormFactory.textField("Rechercher un tuteur...");
    private final TextField firstNameField = FormFactory.textField("Prénom");
    private final TextField lastNameField = FormFactory.textField("Nom");
    private final TextField phoneField = FormFactory.textField("Téléphone");
    private final TextField emailField = FormFactory.textField("Email");
    private final TextField relationField = FormFactory.textField("Relation (père, mère, ...)");
    private final TextField addressField = FormFactory.textField("Adresse");
    private final ComboBox<Student> studentField = new ComboBox<>();

    private Guardian selected;

    public GuardiansView(GuardianService guardianService, StudentService studentService) {
        this.guardianService = guardianService;
        this.studentService = studentService;
        studentField.setMaxWidth(Double.MAX_VALUE);
        studentField.setCellFactory(cb -> studentCell());
        studentField.setButtonCell(studentCell());
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

    public void render(BorderPane contentPane, Label pageTitleLabel) {
        pageTitleLabel.setText("Tuteurs");

        table.getColumns().clear();
        TableColumn<Guardian, String> name = new TableColumn<>("Nom");
        name.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getFirstName() + " " + d.getValue().getLastName()));
        name.setPrefWidth(160);
        TableColumn<Guardian, String> relation = new TableColumn<>("Relation");
        relation.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getRelation()));
        TableColumn<Guardian, String> student = new TableColumn<>("Élève");
        student.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getStudent() == null ? "—" :
                        d.getValue().getStudent().getFirstName() + " " + d.getValue().getStudent().getLastName()));
        student.setPrefWidth(160);
        TableColumn<Guardian, String> phone = new TableColumn<>("Téléphone");
        phone.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getPhoneNumber()));
        table.getColumns().addAll(List.of(name, relation, student, phone));

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
        loadStudents();
        reload();
    }

    private void loadStudents() {
        AsyncTasks.run(
                studentService::findAll,
                list -> studentField.setItems(FXCollections.observableArrayList(list)),
                err -> DialogUtil.error("Erreur", "Échec du chargement des élèves : " + err.getMessage())
        );
    }

    private VBox buildForm() {
        GridPane grid = FormFactory.sectionGrid();
        FormFactory.addRow(grid, 0, "Prénom", firstNameField);
        FormFactory.addRow(grid, 1, "Nom", lastNameField);
        FormFactory.addRow(grid, 2, "Relation", relationField);
        FormFactory.addRow(grid, 3, "Téléphone", phoneField);
        FormFactory.addRow(grid, 4, "Email", emailField);
        FormFactory.addRow(grid, 5, "Adresse", addressField);
        FormFactory.addRow(grid, 6, "Élève", studentField);

        Button save = new Button("Enregistrer");
        save.getStyleClass().add("primary-button");
        save.setOnAction(e -> save());
        Button clear = new Button("Nouveau");
        clear.getStyleClass().add("secondary-button");
        clear.setOnAction(e -> clearForm());
        Button delete = new Button("Supprimer");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(e -> delete());

        return new VBox(12, new Label("Détails du tuteur"), grid, new HBox(8, save, clear, delete));
    }

    private void selectRow(Guardian guardian) {
        selected = guardian;
        if (guardian == null) { clearForm(); return; }
        firstNameField.setText(guardian.getFirstName());
        lastNameField.setText(guardian.getLastName());
        relationField.setText(guardian.getRelation());
        phoneField.setText(guardian.getPhoneNumber());
        emailField.setText(guardian.getEmail());
        addressField.setText(guardian.getAddress());
        studentField.setValue(guardian.getStudent());
    }

    private void clearForm() {
        selected = null;
        firstNameField.clear();
        lastNameField.clear();
        relationField.clear();
        phoneField.clear();
        emailField.clear();
        addressField.clear();
        studentField.setValue(null);
        table.getSelectionModel().clearSelection();
    }

    private void save() {
        if (firstNameField.getText().isBlank() || lastNameField.getText().isBlank() || relationField.getText().isBlank()) {
            DialogUtil.error("Champs requis", "Le prénom, le nom et la relation sont obligatoires.");
            return;
        }
        Guardian guardian = selected != null ? selected : new Guardian();
        guardian.setFirstName(firstNameField.getText().trim());
        guardian.setLastName(lastNameField.getText().trim());
        guardian.setRelation(relationField.getText().trim());
        guardian.setPhoneNumber(phoneField.getText().trim());
        guardian.setEmail(emailField.getText().trim());
        guardian.setAddress(addressField.getText());
        String studentId = studentField.getValue() == null ? null : studentField.getValue().getId();

        AsyncTasks.run(
                () -> guardianService.save(guardian, studentId),
                saved -> { clearForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de l'enregistrement : " + err.getMessage())
        );
    }

    private void delete() {
        if (selected == null) return;
        if (!DialogUtil.confirm("Confirmer", "Supprimer ce tuteur ?")) return;
        String id = selected.getId();
        AsyncTasks.run(
                () -> guardianService.delete(id),
                () -> { clearForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de la suppression : " + err.getMessage())
        );
    }

    private void reload() {
        String needle = searchField.getText();
        AsyncTasks.run(
                () -> guardianService.search(needle),
                list -> rows.setAll(list),
                err -> DialogUtil.error("Erreur", "Échec du chargement : " + err.getMessage())
        );
    }
}