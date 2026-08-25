package com.example.mef.demo.dashboard.enrollments;

import com.example.mef.demo.Model.Classroom;
import com.example.mef.demo.Model.Course;
import com.example.mef.demo.Model.Inscription;
import com.example.mef.demo.Model.Student;
import com.example.mef.demo.Services.ClassroomService;
import com.example.mef.demo.Services.CourseService;
import com.example.mef.demo.Services.EnrollmentService;
import com.example.mef.demo.Services.StudentService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.dashboard.common.TableStyleKit;
import com.example.mef.demo.enums.EnrollmentStatus;
import com.example.mef.demo.enums.SessionName;
import com.example.mef.demo.util.DialogUtil;
import com.example.mef.demo.util.I18n;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Typed CRUD screen for the "enrollments" module (Inscription entity), styled to match StudentsView. */
@Component
public class EnrollmentsView {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EnrollmentService enrollmentService;
    private final StudentService studentService;
    private final ClassroomService classroomService;
    private final CourseService courseService;

    private final ObservableList<Inscription> allRows = FXCollections.observableArrayList();
    private final ObservableList<Inscription> rows = FXCollections.observableArrayList();
    private final TableView<Inscription> table = new TableView<>(rows);
    {
        TableStyleKit.applyTheme(table, "enrollments", TableStyleKit.AVATAR_ROW_HEIGHT);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private final TextField searchField = FormFactory.textField("Rechercher par élève...");
    private final Label countLabel = new Label();

    private final ComboBox<Student> studentField = new ComboBox<>();
    private final ComboBox<Classroom> classroomField = new ComboBox<>();
    private final ComboBox<SessionName> sessionField = new ComboBox<>(FXCollections.observableArrayList(SessionName.values()));
    private final ComboBox<EnrollmentStatus> statusField = new ComboBox<>(FXCollections.observableArrayList(EnrollmentStatus.values()));

    /** One checkable chip per Course; a student can attend any number of them at once. */
    private final FlowPane coursesBox = new FlowPane(8, 8);
    private final List<CheckBox> courseChecks = new ArrayList<>();
    private final Label totalCostValue = new Label("—");

    private BorderPane layout;
    private VBox form;
    private Inscription selected;
    private Runnable onNewEnrollmentWizard;

    public EnrollmentsView(EnrollmentService enrollmentService, StudentService studentService,
                           ClassroomService classroomService, CourseService courseService) {
        this.enrollmentService = enrollmentService;
        this.studentService = studentService;
        this.classroomService = classroomService;
        this.courseService = courseService;
        studentField.setMaxWidth(Double.MAX_VALUE);
        classroomField.setMaxWidth(Double.MAX_VALUE);
        sessionField.setMaxWidth(Double.MAX_VALUE);
        statusField.setMaxWidth(Double.MAX_VALUE);
        studentField.setCellFactory(cb -> studentCell());
        studentField.setButtonCell(studentCell());
        // Explicit converter: without this, the button cell can fall back to
        // Student#toString() (e.g. "com.example...@1a2b3c") instead of the
        // name, particularly when the selected value isn't reference-equal
        // to an item already loaded into the combo's items list.
        studentField.setConverter(new javafx.util.StringConverter<Student>() {
            @Override
            public String toString(Student s) {
                return s == null ? "" : s.getFirstName() + " " + s.getLastName();
            }

            @Override
            public Student fromString(String s) {
                return studentField.getValue();
            }
        });

        classroomField.setCellFactory(cb -> classroomCell());
        classroomField.setButtonCell(classroomCell());
        classroomField.setConverter(new javafx.util.StringConverter<Classroom>() {
            @Override
            public String toString(Classroom c) {
                return c == null ? "" : c.getName();
            }

            @Override
            public Classroom fromString(String s) {
                return classroomField.getValue();
            }
        });
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

    /** @param onNewEnrollmentWizard invoked when the user wants to run the step-by-step enrollment wizard. */
    public void render(BorderPane contentPane, Label pageTitleLabel, Runnable onNewEnrollmentWizard) {
        this.onNewEnrollmentWizard = onNewEnrollmentWizard;
        pageTitleLabel.setText("Inscriptions");
        table.setColumnResizePolicy(
                TableView.UNCONSTRAINED_RESIZE_POLICY
        );
        buildColumns();

        Label title = new Label("Inscriptions");
        title.getStyleClass().add("page-title");
        countLabel.getStyleClass().add("stat-caption");

        Button add = new Button("+  Ajouter une Inscription");
        add.getStyleClass().add("primary-button");
        add.setOnAction(e -> startCreate());

        Button wizard = new Button(I18n.t("ewizard.title"));
        wizard.getStyleClass().add("link-button");
        wizard.setOnAction(e -> this.onNewEnrollmentWizard.run());

        HBox headerRow = new HBox(12, title);
        HBox.setHgrow(title, Priority.ALWAYS);
        headerRow.getChildren().addAll(wizard, add);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox headerBlock = new VBox(4, headerRow, countLabel);

        searchField.getStyleClass().add("filter-field");
        searchField.textProperty().addListener((obs, old, val) -> applyFilter());

        VBox listPane = new VBox(14, headerBlock, searchField, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        listPane.setPadding(new Insets(24));
        wireRowDoubleClick();

        form = buildForm();

        layout = new BorderPane();
        layout.setCenter(listPane);

        contentPane.setCenter(layout);
        contentPane.setPadding(new Insets(20));
        loadPickers();
        reload();
    }

    /** Opens the details panel for a row only when the user double-clicks it. */
    private void wireRowDoubleClick() {
        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Inscription> row = new javafx.scene.control.TableRow<>();
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

        TableColumn<Inscription, String> date = new TableColumn<>("DATE");
        date.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getDateInscription() == null ? "—" : d.getValue().getDateInscription().format(DATE_FORMAT)));
        date.setPrefWidth(100);

        TableColumn<Inscription, Inscription> student = new TableColumn<>("ÉLÈVE");
        student.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        student.setCellFactory(col -> studentAvatarCell());
        student.setPrefWidth(220);

        TableColumn<Inscription, String> classroom = new TableColumn<>("CLASSE");
        classroom.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().classroomsLabel()));
        classroom.setCellFactory(col -> dashIfBlankCell());
        classroom.setPrefWidth(130);

        TableColumn<Inscription, String> session = new TableColumn<>("SESSION");
        session.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getSession() == null ? "—" : d.getValue().getSession().name()));
        session.setCellFactory(col -> pillCell("#EEF2FF", "#4338CA"));
        session.setPrefWidth(130);

        TableColumn<Inscription, String> status = new TableColumn<>("STATUT");
        status.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getStatus() == null ? "—" : d.getValue().getStatus().name()));
        status.setCellFactory(col -> statusCell());
        status.setPrefWidth(110);

        table.getColumns().addAll(List.of(date, student, classroom, session, status));
    }

    private TableCell<Inscription, Inscription> studentAvatarCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Inscription i, boolean empty) {
                super.updateItem(i, empty);
                if (empty || i == null || i.getStudent() == null) {
                    setGraphic(null);
                    return;
                }
                Student s = i.getStudent();
                String initials = TableStyleKit.initialsOf(s.getFirstName(), s.getLastName());
                String color = TableStyleKit.colorFor(s.getGender() == null ? "" : s.getGender().name());
                String fullName = (s.getFirstName() == null ? "" : s.getFirstName()) + " " +
                        (s.getLastName() == null ? "" : s.getLastName());
                String subtitle = i.classroomsLabel();
                setGraphic(TableStyleKit.avatarNameCell(initials, color, fullName.trim(), subtitle));
            }
        };
    }

    private TableCell<Inscription, String> pillCell(String bg, String fg) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank() || "—".equals(item)) {
                    setGraphic(null);
                    setText(empty ? null : "—");
                } else {
                    setText(null);
                    setGraphic(TableStyleKit.pill(item, bg, fg));
                }
            }
        };
    }

    private TableCell<Inscription, String> statusCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank() || "—".equals(item)) {
                    setGraphic(null);
                    setText(empty ? null : "—");
                    return;
                }
                setText(null);
                boolean active = "ACTIVE".equalsIgnoreCase(item);
                String bg = active ? "#DCFCE7" : "#FEE2E2";
                String fg = active ? "#15803D" : "#B91C1C";
                setGraphic(TableStyleKit.pill(item, bg, fg));
            }
        };
    }

    private TableCell<Inscription, String> dashIfBlankCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null || item.isBlank() ? "—" : item);
            }
        };
    }

    private void loadPickers() {
        AsyncTasks.run(studentService::findAll,
                list -> studentField.setItems(FXCollections.observableArrayList(list)),
                err -> DialogUtil.error("Erreur", "Échec du chargement des élèves : " + err.getMessage()));
        AsyncTasks.run(classroomService::findAll,
                list -> classroomField.setItems(FXCollections.observableArrayList(list)),
                err -> DialogUtil.error("Erreur", "Échec du chargement des classes : " + err.getMessage()));
        AsyncTasks.run(courseService::findAll,
                this::buildCourseChips,
                err -> DialogUtil.error("Erreur", "Échec du chargement des cours : " + err.getMessage()));
    }

    /** (Re)builds the course chips; called once courses are loaded, keeping any prior selection selected by id. */
    private void buildCourseChips(List<Course> courses) {
        List<String> previouslyChecked = courseChecks.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> ((Course) cb.getUserData()).getId())
                .toList();

        coursesBox.getChildren().clear();
        courseChecks.clear();

        if (courses.isEmpty()) {
            Label none = new Label("Aucun cours disponible.");
            none.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
            coursesBox.getChildren().add(none);
            return;
        }

        for (Course course : courses) {
            CheckBox cb = new CheckBox(course.getName() + " (" + formatFee(course.getMonthlyFee()) + ")");
            cb.setUserData(course);
            cb.getStyleClass().add("room-chip");
            cb.setMinWidth(Region.USE_PREF_SIZE);
            cb.setSelected(previouslyChecked.contains(course.getId()));
            cb.selectedProperty().addListener((obs, was, isNow) -> updateTotalCost());
            courseChecks.add(cb);
            coursesBox.getChildren().add(cb);
        }
        updateTotalCost();
    }

    private void updateTotalCost() {
        double total = courseChecks.stream()
                .filter(CheckBox::isSelected)
                .mapToDouble(cb -> {
                    Double fee = ((Course) cb.getUserData()).getMonthlyFee();
                    return fee == null ? 0 : fee;
                })
                .sum();
        totalCostValue.setText(formatFee(total));
    }

    private static String formatFee(Double fee) {
        if (fee == null) return "—";
        return String.format(java.util.Locale.FRENCH, "%,.2f DA", fee);
    }

    private VBox buildForm() {
        GridPane grid = FormFactory.sectionGrid();
        FormFactory.addRow(grid, 0, "Élève", studentField);
        FormFactory.addRow(grid, 1, "Classe", classroomField);
        FormFactory.addRow(grid, 2, "Session", sessionField);
        FormFactory.addRow(grid, 3, "Statut", statusField);

        ScrollPane coursesScroll = new ScrollPane(coursesBox);
        coursesScroll.setFitToWidth(true);
        coursesScroll.setPrefHeight(90);
        coursesScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        FormFactory.addRow(grid, 4, "Cours", coursesScroll);

        totalCostValue.getStyleClass().add("field-label");
        FormFactory.addRow(grid, 5, "Coût mensuel (cours)", totalCostValue);

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
        VBox panel = new VBox(12, new Label("Détails de l'inscription"), grid, actions);
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

    private void selectRow(Inscription inscription) {
        selected = inscription;
        if (inscription == null) {
            return;
        }
        studentField.setValue(inscription.getStudent());
        List<Classroom> classrooms = inscription.getClassrooms();
        classroomField.setValue(classrooms.isEmpty() ? null : classrooms.get(0));
        sessionField.setValue(inscription.getSession());
        statusField.setValue(inscription.getStatus());

        List<String> existingCourseIds = inscription.getCourses() == null ? List.of()
                : inscription.getCourses().stream().map(Course::getId).toList();
        for (CheckBox cb : courseChecks) {
            cb.setSelected(existingCourseIds.contains(((Course) cb.getUserData()).getId()));
        }
        updateTotalCost();

        showFormPanel();
    }

    private void clearForm() {
        selected = null;
        studentField.setValue(null);
        classroomField.setValue(null);
        sessionField.setValue(null);
        statusField.setValue(null);
        courseChecks.forEach(cb -> cb.setSelected(false));
        updateTotalCost();
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
        List<String> courseIds = courseChecks.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> ((Course) cb.getUserData()).getId())
                .toList();

        AsyncTasks.run(
                () -> enrollmentService.save(inscription, studentId, classroomId, null, courseIds),
                saved -> { closeForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de l'enregistrement : " + err.getMessage())
        );
    }

    private void delete() {
        if (selected == null) return;
        if (!DialogUtil.confirm("Confirmer", "Supprimer cette inscription ?")) return;
        String id = selected.getId();
        AsyncTasks.run(
                () -> enrollmentService.delete(id),
                () -> { closeForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de la suppression : " + err.getMessage())
        );
    }

    private void reload() {
        AsyncTasks.run(
                enrollmentService::findAll,
                list -> {
                    allRows.setAll(list);
                    applyFilter();
                },
                err -> DialogUtil.error("Erreur", "Échec du chargement : " + err.getMessage())
        );
    }

    private void applyFilter() {
        String needle = searchField.getText();
        if (needle == null || needle.isBlank()) {
            rows.setAll(allRows);
        } else {
            String lower = needle.trim().toLowerCase();
            rows.setAll(allRows.filtered(i -> {
                Student s = i.getStudent();
                if (s == null) return false;
                String full = ((s.getFirstName() == null ? "" : s.getFirstName()) + " " +
                        (s.getLastName() == null ? "" : s.getLastName())).toLowerCase();
                return full.contains(lower);
            }));
        }
        countLabel.setText(rows.size() + (rows.size() > 1 ? " inscriptions" : " inscription"));
    }
}