package com.example.mef.demo.dashboard.students;

import com.example.mef.demo.Model.Student;
import com.example.mef.demo.Services.StudentService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FloatingPanel;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.dashboard.common.TableStyleKit;
import com.example.mef.demo.enums.BloodType;
import com.example.mef.demo.enums.Sexe;
import com.example.mef.demo.util.DateUtil;
import com.example.mef.demo.util.DialogUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

/**
 * Typed CRUD screen for the "students" ("Enfants") module, restyled to match the
 * Outcomings/Courses modules: filter toolbar, summary cards, and a floating (draggable)
 * details panel instead of a fixed side form.
 */
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

    private final TextField searchField = FormFactory.textField("Rechercher par nom ...");
    private final ComboBox<String> genderFilter = new ComboBox<>(
            FXCollections.observableArrayList("Tous", "Garçon", "Fille"));

    private final TextField firstNameField = FormFactory.textField("Prénom");
    private final TextField lastNameField = FormFactory.textField("Nom");
    private final ComboBox<Sexe> genderField = new ComboBox<>(FXCollections.observableArrayList(Sexe.values()));
    private final DatePicker dobField = new DatePicker();

    private final ComboBox<String> bloodTypeField = FormFactory.comboBox(BLOOD_TYPES);
    private final TextField medicalInfoField = FormFactory.textField("Informations médicales");

    private final Label footerCountLabel = new Label();
    private final HBox summaryCards = new HBox(14);

    private List<Student> allStudents = List.of();
    private Student selected;
    private VBox form;
    private Runnable onEnrollNew;

    /** Overlay Pane that the floating panel lives in, stacked on top of the normal screen content. */
    private Pane overlay;
    private FloatingPanel floatingForm;

    public StudentsView(StudentService studentService) {
        this.studentService = studentService;
        genderField.setMaxWidth(Double.MAX_VALUE);
        genderFilter.setValue("Tous");
    }

    /** @param onEnrollNew invoked when the user wants to run the full enrollment wizard instead of a bare add. */
    public void render(BorderPane contentPane, Label pageTitleLabel, Runnable onEnrollNew) {
        this.onEnrollNew = onEnrollNew;
        pageTitleLabel.setText("Enfants");

        buildColumns();
        wireRowDoubleClick();

        Label subtitle = new Label("Gérer les enfants inscrits");
        subtitle.getStyleClass().add("page-subtitle");

        searchField.getStyleClass().add("filter-field");
        genderFilter.getStyleClass().add("filter-field");
        genderFilter.setPrefWidth(130);
        dobField.getStyleClass().add("filter-field");
        Button add = new Button("+  Ajouter un Enfant");
        add.getStyleClass().add("primary-button");
        add.setOnAction(e -> startCreate());

        Button wizard = new Button("Assistant d'inscription");
        wizard.getStyleClass().add("link-button");
        wizard.setOnAction(e -> this.onEnrollNew.run());

        HBox filters = new HBox(10, genderFilter, searchField);
        filters.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        HBox toolbar = new HBox(12, filters, wizard, add);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("module-toolbar");

        footerCountLabel.getStyleClass().add("footer-stat");
        HBox footer = new HBox(20, footerCountLabel, new Region());
        footer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(footer.getChildren().get(1), Priority.ALWAYS);
        footer.getStyleClass().add("table-footer");

        for (Node n : summaryCards.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        VBox tableBlock = new VBox(0, table, footer);
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox center = new VBox(16, subtitle, toolbar, tableBlock, summaryCards);
        center.setPadding(new Insets(24));
        VBox.setVgrow(tableBlock, Priority.ALWAYS);

        if (form == null) {
            form = buildForm();
        }

        // Overlay hosts the floating panel; pickOnBounds(false) lets clicks pass through
        // to the table/buttons underneath wherever the overlay itself has no floating panel.
        overlay = new Pane();
        overlay.setPickOnBounds(false);

        StackPane root = new StackPane(center, overlay);
        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.getStyleClass().add("details-scroll");
        contentPane.setCenter(scrollPane);

        wireFilters();
        reload();
    }

    private void wireFilters() {
        searchField.textProperty().addListener((o, a, b) -> applyFilters());
        genderFilter.valueProperty().addListener((o, a, b) -> applyFilters());
    }

    /** Opens the floating details panel for a row when the user double-clicks it. */
    private void wireRowDoubleClick() {
        table.setRowFactory(tv -> {
            TableRow<Student> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    selectRow(row.getItem());
                }
            });
            return row;
        });
    }

    private void buildColumns() {
        table.getColumns().clear();

        TableColumn<Student, Student> child = new TableColumn<>("ENFANT");
        child.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        child.setCellFactory(col -> childCell());
        child.setPrefWidth(240);

        TableColumn<Student, String> age = new TableColumn<>("ÂGE");
        age.setCellValueFactory(d -> new ReadOnlyStringWrapper(ageLabel(d.getValue())));
        age.setCellFactory(col -> pillCell("#EEF2FF", "#4338CA"));
        age.setPrefWidth(90);

        TableColumn<Student, String> section = new TableColumn<>("SECTION");
        section.setCellValueFactory(d -> new ReadOnlyStringWrapper("—"));
        section.setPrefWidth(90);

        TableColumn<Student, String> groupage = new TableColumn<>("GROUPAGE");
        groupage.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getBloodType() == null ? "" : d.getValue().getBloodType().getLabel()));
        groupage.setCellFactory(col -> bloodCell());
        groupage.setPrefWidth(100);

        TableColumn<Student, String> inscription = new TableColumn<>("INSCRIPTION");
        inscription.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                DateUtil.frShort(d.getValue().getEnrollmentDate())));
        inscription.setPrefWidth(110);

        TableColumn<Student, String> notes = new TableColumn<>("INFORMATIONS MÉDICALES");
        notes.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getNotes()));
        notes.setCellFactory(col -> dashIfBlankCell());
        notes.setPrefWidth(160);

        TableColumn<Student, Student> actions = new TableColumn<>("ACTION");
        actions.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        actions.setCellFactory(col -> actionCell());
        actions.setPrefWidth(110);
        actions.setMaxWidth(120);

        table.getColumns().addAll(List.of(child, age, section, groupage, inscription, notes, actions));
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
                String genderLabel = genderLabel(s.getGender());
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

    private TableCell<Student, Student> actionCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Student item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Button view = iconBtn("fth-eye", "Voir");
                Button edit = iconBtn("fth-edit-2", "Modifier");
                Button del = iconBtn("fth-trash-2", "Supprimer");
                del.getStyleClass().add("icon-action-danger");

                view.setOnAction(e -> { table.getSelectionModel().select(item); selectRow(item); });
                edit.setOnAction(e -> { table.getSelectionModel().select(item); selectRow(item); });
                del.setOnAction(e -> { selected = item; delete(); });

                HBox box = new HBox(4, view, edit, del);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        };
    }

    private Button iconBtn(String icon, String tooltip) {
        Button btn = new Button();
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(14);
        btn.setGraphic(fi);
        btn.getStyleClass().add("icon-action-btn");
        btn.setTooltip(new Tooltip(tooltip));
        return btn;
    }

    private static String genderLabel(Sexe gender) {
        return gender == Sexe.FEMALE ? "Fille" : gender == Sexe.MALE ? "Garçon" : "—";
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
        FormFactory.addRow(grid, 5, "Médical", medicalInfoField);

        Button save = new Button("Enregistrer");
        save.getStyleClass().add("primary-button");
        save.setOnAction(e -> save());

        Button clear = new Button("+ Nouveau");
        clear.getStyleClass().add("secondary-button");
        clear.setOnAction(e -> startCreate());

        Button delete = new Button("Supprimer");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(e -> delete());

        // No title label here — the FloatingPanel header already shows "Détails de l'élève".
        // NOTE: no setPrefWidth() on this VBox — the panel sits inside a ScrollPane with
        // fitToWidth(true) (see FloatingPanel), and forcing a fixed prefWidth here fought
        // that constraint on the very first layout pass, which could resolve the GridPane's
        // input column to 0 width and make every field render invisible.
        return new VBox(12, grid, new HBox(8, save, clear, delete));
    }

    private void startCreate() {
        clearForm();
        showFormPanel();
    }

    private void showFormPanel() {
        if (floatingForm == null) {
            floatingForm = new FloatingPanel("Détails de l'élève", form, this::closeForm);
        }
        boolean wasAdded = !overlay.getChildren().contains(floatingForm);
        if (wasAdded) {
            overlay.getChildren().add(floatingForm);
        }
        double x = Math.max(24, overlay.getWidth() - floatingForm.getPrefWidth() - 24);
        floatingForm.positionAt(x, 24);
        floatingForm.toFront();

        if (wasAdded) {
            // Force an immediate CSS + layout pass now, before the panel is ever painted.
            // Without this, the GridPane's column widths can resolve on a stale/zero-width
            // parent chain the first time the panel is added to the overlay, leaving the
            // form's editors invisible until some later event (e.g. a manual resize)
            // triggers a fresh layout pass.
            floatingForm.applyCss();
            floatingForm.layout();
        }
    }

    private void closeForm() {
        if (floatingForm != null) {
            overlay.getChildren().remove(floatingForm);
        }
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
        bloodTypeField.setValue(student.getBloodType() == null ? null : student.getBloodType().getLabel());
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
        medicalInfoField.clear();
        table.getSelectionModel().clearSelection();
    }

    private void save() {
        if (firstNameField.getText().isBlank() || lastNameField.getText().isBlank() || genderField.getValue() == null ||
                bloodTypeField.getValue()==null || medicalInfoField.getText().isBlank() || dobField.getValue()==null) {
            DialogUtil.error("Champs requis", "Tout les Champs sont obligatoires.");
            return;
        }
        Student student = selected != null ? selected : new Student();
        student.setFirstName(firstNameField.getText().trim());
        student.setLastName(lastNameField.getText().trim());
        student.setGender(genderField.getValue());
        student.setDateOfBirth(dobField.getValue() == null ? null : dobField.getValue().atStartOfDay());
        student.setBloodType(BloodType.fromLabel(bloodTypeField.getValue()));
        student.setMedicalInfo(medicalInfoField.getText());

        AsyncTasks.run(
                () -> studentService.save(student),
                saved -> { clearForm(); closeForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de l'enregistrement : " + err.getMessage())
        );
    }

    private void delete() {
        if (selected == null) return;
        if (!DialogUtil.confirm("Confirmer", "Supprimer cet élève ?")) return;
        String id = selected.getId();
        AsyncTasks.run(
                () -> studentService.delete(id),
                () -> { clearForm(); closeForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de la suppression : " + err.getMessage())
        );
    }

    private void reload() {
        AsyncTasks.run(
                () -> studentService.search(""),
                list -> {
                    allStudents = list;
                    applyFilters();
                },
                err -> DialogUtil.error("Erreur", "Échec du chargement : " + err.getMessage())
        );
    }

    private void applyFilters() {
        String needle = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String genderVal = genderFilter.getValue();

        List<Student> filtered = allStudents.stream()
                .filter(s -> {
                    if (!needle.isBlank()) {
                        String first = s.getFirstName() == null ? "" : s.getFirstName().toLowerCase();
                        String last = s.getLastName() == null ? "" : s.getLastName().toLowerCase();
                        if (!first.contains(needle) && !last.contains(needle)) return false;
                    }
                    if (genderVal != null && !"Tous".equals(genderVal)) {
                        if (!genderLabel(s.getGender()).equals(genderVal)) return false;
                    }
                    return true;
                })
                .toList();

        rows.setAll(filtered);
        updateFooter(filtered);
        updateSummaryCards(allStudents);
    }

    private void updateFooter(List<Student> data) {
        footerCountLabel.setText(data.size() + (data.size() > 1 ? " enfants inscrits" : " enfant inscrit"));
    }

    private void updateSummaryCards(List<Student> data) {
        summaryCards.getChildren().clear();

        long boys = data.stream().filter(s -> s.getGender() == Sexe.MALE).count();
        long girls = data.stream().filter(s -> s.getGender() == Sexe.FEMALE).count();
        long withMedicalInfo = data.stream()
                .filter(s -> s.getMedicalInfo() != null && !s.getMedicalInfo().isBlank())
                .count();

        summaryCards.getChildren().addAll(
                summaryCard("fth-users", String.valueOf(data.size()), "Total Enfants", "#4338CA", "#EEF2FF"),
                summaryCard("fth-user", boys + " · " + girls, "Garçons · Filles", "#0E7490", "#CFFAFE"),
                summaryCard("fth-heart", String.valueOf(withMedicalInfo), "Infos Médicales", "#B91C1C", "#FEE2E2")
        );
        for (Node n : summaryCards.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
    }

    private HBox summaryCard(String icon, String value, String label, String accent, String bg) {
        FontIcon fi = new FontIcon(icon);
        fi.setIconSize(20);
        fi.setStyle("-fx-icon-color: " + accent + ";");
        StackPane iconWrap = new StackPane(fi);
        iconWrap.getStyleClass().add("stat-icon-wrap");
        iconWrap.setStyle("-fx-background-color: " + bg + ";");

        Label valLbl = new Label(value);
        valLbl.getStyleClass().add("stat-number");
        valLbl.setStyle("-fx-font-size: 20px;");
        Label capLbl = new Label(label);
        capLbl.getStyleClass().add("stat-caption");

        VBox text = new VBox(2, valLbl, capLbl);
        HBox card = new HBox(12, iconWrap, text);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("stat-box");
        card.setPadding(new Insets(14));
        return card;
    }
}