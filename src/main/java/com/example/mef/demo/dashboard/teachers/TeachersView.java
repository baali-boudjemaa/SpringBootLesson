package com.example.mef.demo.dashboard.teachers;

import com.example.mef.demo.Model.Employee;
import com.example.mef.demo.Services.EmployeeService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.enums.EmployeeRole;
import com.example.mef.demo.util.DialogUtil;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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

import java.util.List;

/** Typed CRUD screen for the "teachers" module (Employee entity). */
@Component
public class TeachersView {

    private final EmployeeService employeeService;

    private final ObservableList<Employee> rows = FXCollections.observableArrayList();
    private final TableView<Employee> table = new TableView<>(rows);

    private final TextField searchField = FormFactory.textField("Rechercher un employé...");
    private final TextField firstNameField = FormFactory.textField("Prénom");
    private final TextField lastNameField = FormFactory.textField("Nom");
    private final TextField emailField = FormFactory.textField("Email");
    private final TextField phoneField = FormFactory.textField("Téléphone");
    private final ComboBox<EmployeeRole> roleField = new ComboBox<>(FXCollections.observableArrayList(EmployeeRole.values()));
    private final TextArea certificationsField = new TextArea();

    private Employee selected;

    public TeachersView(EmployeeService employeeService) {
        this.employeeService = employeeService;
        roleField.setMaxWidth(Double.MAX_VALUE);
        certificationsField.setPromptText("Certifications");
        certificationsField.setPrefRowCount(3);
    }

    public void render(BorderPane contentPane, Label pageTitleLabel) {
        pageTitleLabel.setText("Personnel");

        table.getColumns().clear();
        TableColumn<Employee, String> number = new TableColumn<>("N°");
        number.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getEmployeeNumber()));
        TableColumn<Employee, String> name = new TableColumn<>("Nom");
        name.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getFirstName() + " " + d.getValue().getLastName()));
        name.setPrefWidth(180);
        TableColumn<Employee, String> role = new TableColumn<>("Rôle");
        role.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getRole() == null ? "" : d.getValue().getRole().name()));
        TableColumn<Employee, String> email = new TableColumn<>("Email");
        email.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getEmail()));
        email.setPrefWidth(180);
        table.getColumns().addAll(List.of(number, name, role, email));

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

        Button save = new Button("Enregistrer");
        save.getStyleClass().add("primary-button");
        save.setOnAction(e -> save());
        Button clear = new Button("Nouveau");
        clear.getStyleClass().add("secondary-button");
        clear.setOnAction(e -> clearForm());
        Button delete = new Button("Supprimer");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(e -> delete());

        return new VBox(12, new Label("Détails de l'employé"), grid, new HBox(8, save, clear, delete));
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
    }

    private void clearForm() {
        selected = null;
        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        phoneField.clear();
        roleField.setValue(null);
        certificationsField.clear();
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
                list -> rows.setAll(list),
                err -> DialogUtil.error("Erreur", "Échec du chargement : " + err.getMessage())
        );
    }
}