package com.example.mef.demo.dashboard.students;

import com.example.mef.demo.Model.Student;
import com.example.mef.demo.Services.StudentService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.dashboard.common.TableStyleKit;
import com.example.mef.demo.enums.BloodType;
import com.example.mef.demo.enums.Sexe;
import com.example.mef.demo.util.DateUtil;
import com.example.mef.demo.util.DialogUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

/** Typed CRUD screen for the "students" ("Enfants") module. */
@Component
public class StudentsView {

    private static final List<String> BLOOD_TYPES = List.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");

    private final StudentService studentService;

    private final ObservableList<Student> rows = FXCollections.observableArrayList();
    private final TableView<Student> table = new TableView<>(rows);
    {
        TableStyleKit.applyTheme(table, "students", TableStyleKit.AVATAR_ROW_HEIGHT);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private final TextField searchField = FormFactory.textField("Rechercher par nom ou téléphone...");
    private final Label countLabel = new Label();

    private final TextField firstNameField = FormFactory.textField("Prénom");
    private final TextField lastNameField = FormFactory.textField("Nom");
    private final ComboBox<Sexe> genderField = new ComboBox<>(FXCollections.observableArrayList(Sexe.values()));
    private final DatePicker dobField = new DatePicker();
    private final ComboBox<String> bloodTypeField = FormFactory.comboBox(BLOOD_TYPES);
    private final TextField phoneField = FormFactory.textField("Téléphone");
    private final TextField medicalInfoField = FormFactory.textField("Informations médicales");;


    private BorderPane layout;
    private VBox form;
    private Student selected;
    private Runnable onEnrollNew;

    public StudentsView(StudentService studentService) {
        this.studentService = studentService;
        genderField.setMaxWidth(Double.MAX_VALUE);
    }

    /** @param onEnrollNew invoked when the user wants to run the full enrollment wizard instead of a bare add. */
    public void render(BorderPane contentPane, Label pageTitleLabel, Runnable onEnrollNew) {
        this.onEnrollNew = onEnrollNew;
        pageTitleLabel.setText("Enfants");

        buildColumns();

        Label title = new Label("Enfants");
        title.getStyleClass().add("page-title");
        countLabel.getStyleClass().add("stat-caption");

        Button add = new Button("+  Ajouter un Enfant");
        add.getStyleClass().add("primary-button");
        add.setOnAction(e -> startCreate());

        Button wizard = new Button("Assistant d'inscription");
        wizard.getStyleClass().add("link-button");
        wizard.setOnAction(e -> this.onEnrollNew.run());

        HBox headerRow = new HBox(12, title);
        HBox.setHgrow(title, Priority.ALWAYS);
        headerRow.getChildren().addAll(wizard, add);
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
        reload();
    }

    private void buildColumns() {
        table.getColumns().clear();

        TableColumn<Student, Student> child = new TableColumn<>("ENFANT");
        child.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyObjectWrapper<>(d.getValue()));
        child.setCellFactory(col -> childCell());
        child.setPrefWidth(260);

        TableColumn<Student, String> age = new TableColumn<>("ÂGE");
        age.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyStringWrapper(ageLabel(d.getValue())));
        age.setCellFactory(col -> pillCell("#EEF2FF", "#4338CA"));
        age.setPrefWidth(90);

        TableColumn<Student, String> section = new TableColumn<>("SECTION");
        section.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyStringWrapper("—"));
        section.setPrefWidth(100);

        TableColumn<Student, String> groupage = new TableColumn<>("GROUPAGE");
        groupage.setCellValueFactory(d -> {
            return new javafx.beans.property.ReadOnlyStringWrapper(d.getValue().getBloodType().name());
        });
        groupage.setCellFactory(col -> bloodCell());
        groupage.setPrefWidth(110);

        TableColumn<Student, String> inscription = new TableColumn<>("INSCRIPTION");
        inscription.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyStringWrapper(
                DateUtil.frShort(d.getValue().getEnrollmentDate())));
        inscription.setPrefWidth(120);

        TableColumn<Student, String> phone = new TableColumn<>("TÉLÉPHONE");
        phone.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyStringWrapper(d.getValue().getPhone()));
        phone.setCellFactory(col -> dashIfBlankCell());
        phone.setPrefWidth(130);

        TableColumn<Student, String> notes = new TableColumn<>("INFORMATIONS MÉDICALES");
        notes.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyStringWrapper(d.getValue().getNotes()));
        notes.setCellFactory(col -> dashIfBlankCell());
        notes.setPrefWidth(140);

        table.getColumns().addAll(List.of(child, age, section, groupage, inscription, phone, notes));
    }

    private TableCell<Student, Student> childCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Student s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setGraphic(null);
                    return;
                }
                String initials = TableStyleKit.initialsOf(s.getFirstName(), s.getLastName());
                String color = TableStyleKit.colorFor(s.getGender() == null ? "" : s.getGender().name());
                String fullName = (s.getFirstName() == null ? "" : s.getFirstName()) + " " +
                        (s.getLastName() == null ? "" : s.getLastName());
                String genderLabel = s.getGender() == Sexe.FEMALE ? "Fille"
                        : s.getGender() == Sexe.MALE ? "Garçon" : "—";
                String subtitle = genderLabel + " · " + DateUtil.frShort(s.getDateOfBirth());
                setGraphic(TableStyleKit.avatarNameCell(initials, color, fullName.trim(), subtitle));
            }
        };
    }

    private TableCell<Student, String> pillCell(String bg, String fg) {
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

    private TableCell<Student, String> bloodCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setText("—");
                    setGraphic(null);
                } else {
                    setText(null);
                    setGraphic(TableStyleKit.pill("\uD83E\uDE78 " + item, "#FEE2E2", "#B91C1C"));
                }
            }
        };
    }

    private TableCell<Student, String> dashIfBlankCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null || item.isBlank() ? "—" : item);
            }
        };
    }

    private String ageLabel(Student s) {
        if (s.getDateOfBirth() == null) return "—";
        Period p = Period.between(s.getDateOfBirth().toLocalDate(), LocalDate.now());
        if (p.getYears() > 0) return p.getYears() + " ans";
        if (p.getMonths() > 0) return p.getMonths() + " mois";
        return p.getDays() + " j";
    }

    private VBox buildForm() {
        GridPane grid = FormFactory.sectionGrid();
        FormFactory.addRow(grid, 0, "Prénom", firstNameField);
        FormFactory.addRow(grid, 1, "Nom", lastNameField);
        FormFactory.addRow(grid, 2, "Genre", genderField);
        FormFactory.addRow(grid, 3, "Naissance", dobField);
        FormFactory.addRow(grid, 4, "Groupage", bloodTypeField);
        FormFactory.addRow(grid, 5, "Téléphone", phoneField);
        FormFactory.addRow(grid, 6, "Médical", medicalInfoField);

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
        VBox panel = new VBox(12, new Label("Détails de l'élève"), grid, actions);
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

    private void selectRow(Student student) {
        selected = student;
        if (student == null) {
            return;
        }
        firstNameField.setText(student.getFirstName());
        lastNameField.setText(student.getLastName());
        genderField.setValue(student.getGender());
        dobField.setValue(student.getDateOfBirth() == null ? null : student.getDateOfBirth().toLocalDate());
        bloodTypeField.setValue(student.getBloodType().name());
        phoneField.setText(student.getPhone());
        medicalInfoField.setText(student.getMedicalInfo());
        showFormPanel();
    }

    private void clearForm() {
        selected = null;
        firstNameField.clear();
        lastNameField.clear();
        genderField.setValue(null);
        dobField.setValue(null);
        bloodTypeField.setValue(null);
        phoneField.clear();
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
        student.setBloodType(BloodType.valueOf(bloodTypeField.getValue()));
        student.setPhone(phoneField.getText());
        student.setMedicalInfo(medicalInfoField.getText());

        AsyncTasks.run(
                () -> studentService.save(student),
                saved -> { closeForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de l'enregistrement : " + err.getMessage())
        );
    }

    private void delete() {
        if (selected == null) return;
        if (!DialogUtil.confirm("Confirmer", "Supprimer cet élève ?")) return;
        String id = selected.getId();
        AsyncTasks.run(
                () -> studentService.delete(id),
                () -> { closeForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de la suppression : " + err.getMessage())
        );
    }

    private void reload() {
        String needle = searchField.getText();
        AsyncTasks.run(
                () -> studentService.search(needle),
                list -> {
                    rows.setAll(list);
                    countLabel.setText(list.size() + (list.size() > 1 ? " enfants inscrits" : " enfant inscrit"));
                },
                err -> DialogUtil.error("Erreur", "Échec du chargement : " + err.getMessage())
        );
    }
}
