package com.example.mef.demo.dashboard.guardians;

import com.example.mef.demo.Model.Guardian;
import com.example.mef.demo.Model.Student;
import com.example.mef.demo.Services.GuardianService;
import com.example.mef.demo.Services.StudentService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.dashboard.common.TableStyleKit;
import com.example.mef.demo.util.DialogUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
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

/** Typed CRUD screen for the "guardians" module, styled to match StudentsView. */
@Component
public class GuardiansView {

    private final GuardianService guardianService;
    private final StudentService studentService;

    private final ObservableList<Guardian> rows = FXCollections.observableArrayList();
    private final TableView<Guardian> table = new TableView<>(rows);
    {
        TableStyleKit.applyTheme(table, "guardians", TableStyleKit.AVATAR_ROW_HEIGHT);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private final TextField searchField = FormFactory.textField("Rechercher un tuteur...");
    private final Label countLabel = new Label();

    private final TextField firstNameField = FormFactory.textField("Prénom");
    private final TextField lastNameField = FormFactory.textField("Nom");
    private final TextField phoneField = FormFactory.textField("Téléphone");
    private final TextField emailField = FormFactory.textField("Email");
    private final TextField relationField = FormFactory.textField("Relation (père, mère, ...)");
    private final TextField addressField = FormFactory.textField("Adresse");
    private final ComboBox<Student> studentField = new ComboBox<>();

    private BorderPane layout;
    private VBox form;
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
        table.setColumnResizePolicy(
                TableView.UNCONSTRAINED_RESIZE_POLICY
        );
        buildColumns();

        Label title = new Label("Tuteurs");
        title.getStyleClass().add("page-title");
        countLabel.getStyleClass().add("stat-caption");

        Button add = new Button("+  Ajouter un Tuteur");
        add.getStyleClass().add("primary-button");
        add.setOnAction(e -> startCreate());

        HBox headerRow = new HBox(12, title);
        HBox.setHgrow(title, Priority.ALWAYS);
        headerRow.getChildren().add(add);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox headerBlock = new VBox(4, headerRow, countLabel);

        searchField.getStyleClass().add("filter-field");
        searchField.textProperty().addListener((obs, old, val) -> reload());

        VBox listPane = new VBox(14, headerBlock, searchField, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        listPane.setPadding(new Insets(24));
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> selectRow(val));

        form = buildForm();

        layout = new BorderPane();
        layout.setCenter(listPane);

        contentPane.setCenter(layout);
        contentPane.setPadding(new Insets(20));
        loadStudents();
        reload();
    }

    private void buildColumns() {
        table.getColumns().clear();

        TableColumn<Guardian, Guardian> guardian = new TableColumn<>("TUTEUR");
        guardian.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        guardian.setCellFactory(col -> guardianCell());
        guardian.setPrefWidth(240);

        TableColumn<Guardian, String> relation = new TableColumn<>("RELATION");
        relation.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getRelation()));
        relation.setCellFactory(col -> pillCell("#EEF2FF", "#4338CA"));
        relation.setPrefWidth(110);

        TableColumn<Guardian, String> student = new TableColumn<>("ÉLÈVE");
        student.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getStudent() == null ? "—" :
                        d.getValue().getStudent().getFirstName() + " " + d.getValue().getStudent().getLastName()));
        student.setCellFactory(col -> dashIfBlankCell());
        student.setPrefWidth(160);

        TableColumn<Guardian, String> phone = new TableColumn<>("TÉLÉPHONE");
        phone.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getPhoneNumber()));
        phone.setCellFactory(col -> dashIfBlankCell());
        phone.setPrefWidth(130);

        TableColumn<Guardian, String> email = new TableColumn<>("EMAIL");
        email.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getEmail()));
        email.setCellFactory(col -> dashIfBlankCell());
        email.setPrefWidth(160);

        table.getColumns().addAll(List.of(guardian, relation, student, phone, email));
    }

    private TableCell<Guardian, Guardian> guardianCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Guardian g, boolean empty) {
                super.updateItem(g, empty);
                if (empty || g == null) {
                    setGraphic(null);
                    return;
                }
                String initials = TableStyleKit.initialsOf(g.getFirstName(), g.getLastName());
                String color = TableStyleKit.colorFor(g.getRelation() == null ? "" : g.getRelation());
                String fullName = (g.getFirstName() == null ? "" : g.getFirstName()) + " " +
                        (g.getLastName() == null ? "" : g.getLastName());
                String subtitle = g.getAddress() == null || g.getAddress().isBlank() ? "—" : g.getAddress();
                setGraphic(TableStyleKit.avatarNameCell(initials, color, fullName.trim(), subtitle));
            }
        };
    }

    private TableCell<Guardian, String> pillCell(String bg, String fg) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setGraphic(null);
                } else {
                    setGraphic(TableStyleKit.pill(item, bg, fg));
                }
            }
        };
    }

    private TableCell<Guardian, String> dashIfBlankCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null || item.isBlank() ? "—" : item);
            }
        };
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

        Button cancel = new Button("Annuler");
        cancel.getStyleClass().add("secondary-button");
        cancel.setOnAction(e -> closeForm());

        Button delete = new Button("Supprimer");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(e -> delete());

        HBox actions = new HBox(8, save, cancel, delete);
        VBox panel = new VBox(12, new Label("Détails du tuteur"), grid, actions);
        panel.getStyleClass().add("side-panel");
        panel.setPrefWidth(320);
        return panel;
    }

    private void startCreate() {
        clearForm();
        showFormPanel();
    }

    private void showFormPanel() {
        layout.setRight(form);
        BorderPane.setMargin(form, new Insets(0, 0, 0, 16));
    }

    private void closeForm() {
        layout.setRight(null);
        clearForm();
    }

    private void loadStudents() {
        AsyncTasks.run(
                studentService::findAll,
                list -> studentField.setItems(FXCollections.observableArrayList(list)),
                err -> DialogUtil.error("Erreur", "Échec du chargement des élèves : " + err.getMessage())
        );
    }

    private void selectRow(Guardian guardian) {
        selected = guardian;
        if (guardian == null) {
            return;
        }
        firstNameField.setText(guardian.getFirstName());
        lastNameField.setText(guardian.getLastName());
        relationField.setText(guardian.getRelation());
        phoneField.setText(guardian.getPhoneNumber());
        emailField.setText(guardian.getEmail());
        addressField.setText(guardian.getAddress());
        studentField.setValue(guardian.getStudent());
        showFormPanel();
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
                saved -> { closeForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de l'enregistrement : " + err.getMessage())
        );
    }

    private void delete() {
        if (selected == null) return;
        if (!DialogUtil.confirm("Confirmer", "Supprimer ce tuteur ?")) return;
        String id = selected.getId();
        AsyncTasks.run(
                () -> guardianService.delete(id),
                () -> { closeForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de la suppression : " + err.getMessage())
        );
    }

    private void reload() {
        String needle = searchField.getText();
        AsyncTasks.run(
                () -> guardianService.search(needle),
                list -> {
                    rows.setAll(list);
                    countLabel.setText(list.size() + (list.size() > 1 ? " tuteurs enregistrés" : " tuteur enregistré"));
                },
                err -> DialogUtil.error("Erreur", "Échec du chargement : " + err.getMessage())
        );
    }
}