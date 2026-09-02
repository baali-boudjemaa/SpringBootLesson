package com.example.mef.demo.dashboard.enrollments;

import com.example.mef.demo.Model.Classroom;
import com.example.mef.demo.Model.Course;
import com.example.mef.demo.Model.Inscription;
import com.example.mef.demo.Model.Student;
import com.example.mef.demo.Services.ClassroomService;
import com.example.mef.demo.Services.CourseService;
import com.example.mef.demo.Services.EnrollmentService;
import com.example.mef.demo.Services.SettingService;
import com.example.mef.demo.Services.StudentService;
import com.example.mef.demo.Services.EnrollmentSettingsKeys;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.dashboard.common.TableStyleKit;
import com.example.mef.demo.enums.Category;
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
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
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
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
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
    private final SettingService settingService;

    // Per-category age rules (نفس القيم المستخدمة في معالج التسجيل), loaded once in render().
    private int crecheMinAge;
    private int crecheMaxAge;
    private int preparatoireMinAge;
    private int preparatoireMaxAge;
    private int soutienMinAge;
    private int soutienMaxAge;

    private final ObservableList<Inscription> allRows = FXCollections.observableArrayList();
    private final ObservableList<Inscription> rows = FXCollections.observableArrayList();
    private final TableView<Inscription> table = new TableView<>(rows);
    {
        TableStyleKit.applyTheme(table, "enrollments", TableStyleKit.AVATAR_ROW_HEIGHT);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private final TextField searchField = FormFactory.textField("");
    private final Label countLabel = new Label();

    private final ComboBox<Student> studentField = new ComboBox<>();
    private final ComboBox<Classroom> classroomField = new ComboBox<>();
    private final ComboBox<SessionName> sessionField = new ComboBox<>(FXCollections.observableArrayList(SessionName.values()));
    /** Label for {@link #sessionField}, kept as a field so both can be hidden together
     *  for Soutien (دعم) classrooms, which have no notion of "نوع الحصة". */
    private final Label sessionFieldLabel = new Label();
    private final ComboBox<EnrollmentStatus> statusField = new ComboBox<>(FXCollections.observableArrayList(EnrollmentStatus.values()));

    /** One checkable chip per Course; a student can attend any number of them at once. */
    private final FlowPane coursesBox = new FlowPane(8, 8);
    private final List<CheckBox> courseChecks = new ArrayList<>();
    private final Label totalCostValue = new Label("—");
    /** "الدروس" block (label + chips) and the total-cost row, kept as fields so they can be
     *  hidden together: only دعم (Soutien) classrooms have courses — حضانة/تحضيري never do. */
    private VBox coursesBlock;
    private HBox totalCostRow;

    /** Full course catalog as loaded from the server; buildCourseChips() renders a filtered subset of this. */
    private final List<Course> allCourses = new ArrayList<>();

    private BorderPane layout;
    private VBox form;
    private Inscription selected;
    private Runnable onNewEnrollmentWizard;

    public EnrollmentsView(EnrollmentService enrollmentService, StudentService studentService,
                           ClassroomService classroomService, CourseService courseService,
                           SettingService settingService) {
        this.enrollmentService = enrollmentService;
        this.studentService = studentService;
        this.classroomService = classroomService;
        this.courseService = courseService;
        this.settingService = settingService;
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
                return c == null ? "" : classroomWithCategoryLabel(c);
            }

            @Override
            public Classroom fromString(String s) {
                return classroomField.getValue();
            }
        });

        sessionField.setCellFactory(cb -> sessionListCell());
        sessionField.setButtonCell(sessionListCell());
        statusField.setCellFactory(cb -> enrollmentStatusListCell());
        statusField.setButtonCell(enrollmentStatusListCell());

        // Whenever the selected classroom changes, rebuild the course chips to show only
        // the courses that belong to that classroom, and show/hide two blocks that only
        // apply to Soutien (دعم) classrooms: the "نوع الحصة" field (no session type outside
        // Soutien) and the "الدروس" block (only Soutien classrooms have courses at all —
        // حضانة and تحضيري never do).
        classroomField.valueProperty().addListener((obs, oldClass, newClass) -> {
            buildCourseChips(filterCoursesForSelectedClassroom());
            boolean isSoutien = newClass != null && newClass.getCategory() == Category.SOUTIEN;
            sessionFieldLabel.setVisible(!isSoutien);
            sessionFieldLabel.setManaged(!isSoutien);
            sessionField.setVisible(!isSoutien);
            sessionField.setManaged(!isSoutien);
            if (isSoutien) {
                sessionField.setValue(null);
            }
            if (coursesBlock != null) {
                coursesBlock.setVisible(isSoutien);
                coursesBlock.setManaged(isSoutien);
            }
            if (totalCostRow != null) {
                totalCostRow.setVisible(isSoutien);
                totalCostRow.setManaged(isSoutien);
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
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(classroomWithCategoryLabel(item));
                    setGraphic(categoryDot(item.getCategory()));
                    setContentDisplay(ContentDisplay.LEFT);
                    setGraphicTextGap(8);
                }
            }
        };
    }

    private String categoryLabel(Category category) {
        return switch (category) {
            case CRECHE -> I18n.t("category.creche", "تسجيل الحضور");
            case PREPARATOIRE -> I18n.t("category.preparatoire", "تسجيل الحضور");
            case SOUTIEN -> I18n.t("category.soutien", "تسجيل الحضور");
        };
    }

    /** "اسم القسم — نوع الفئة", used both in the table's "القسم" column and the form combo. */
    private String classroomWithCategoryLabel(Classroom classroom) {
        if (classroom == null) return "—";
        return classroom.getCategory() == null
                ? classroom.getName()
                : classroom.getName() + "  —  " + categoryLabel(classroom.getCategory());
    }

    /** Small colored dot showing a classroom's category (نوع الفئة), used next to its name. */
    private Node categoryDot(Category category) {
        Circle dot = new Circle(4);
        dot.setFill(Color.web(categoryColorHex(category)));
        return dot;
    }

    private String categoryColorHex(Category category) {
        if (category == null) return "#94A3B8"; // no category set → neutral grey
        return switch (category) {
            case CRECHE -> "#EC4899";        // pink — حضانة
            case PREPARATOIRE -> "#2563EB";  // blue — تحضيري
            case SOUTIEN -> "#10B981";       // green — دعم
        };
    }

    private ListCell<SessionName> sessionListCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(SessionName item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : sessionLabel(item));
            }
        };
    }

    private ListCell<EnrollmentStatus> enrollmentStatusListCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(EnrollmentStatus item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : enrollmentStatusLabel(item));
            }
        };
    }

    /** @param onNewEnrollmentWizard invoked when the user wants to run the step-by-step enrollment wizard. */
    public void render(BorderPane contentPane, Label pageTitleLabel, Runnable onNewEnrollmentWizard) {
        this.onNewEnrollmentWizard = onNewEnrollmentWizard;
        searchField.setPromptText(I18n.t("enrollment.search", "تسجيل الحضور"));
        pageTitleLabel.setText(I18n.t("enrollment.title", "تسجيل الحضور"));
        table.setColumnResizePolicy(
                TableView.UNCONSTRAINED_RESIZE_POLICY
        );
        buildColumns();

        Label title = new Label(I18n.t("enrollment.title", "تسجيل الحضور"));
        title.getStyleClass().add("page-title");
        countLabel.getStyleClass().add("stat-caption");

        Button add = new Button("+  " + I18n.t("enrollment.add", "تسجيل الحضور"));
        add.getStyleClass().add("primary-button");
        add.setOnAction(e -> startCreate());

        Button wizard = new Button(I18n.t("ewizard.title", "تسجيل الحضور"));
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

        TableColumn<Inscription, String> date = new TableColumn<>(I18n.t("enrollment.table.date", "تسجيل الحضور"));
        date.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getDateInscription() == null ? "—" : d.getValue().getDateInscription().format(DATE_FORMAT)));
        date.setPrefWidth(100);

        TableColumn<Inscription, Inscription> student = new TableColumn<>(I18n.t("enrollment.table.student", "تسجيل الحضور"));
        student.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        student.setCellFactory(col -> studentAvatarCell());
        student.setPrefWidth(220);

        TableColumn<Inscription, String> classroom = new TableColumn<>(I18n.t("enrollment.table.classroom", "تسجيل الحضور"));
        classroom.setCellValueFactory(d -> new ReadOnlyStringWrapper(classroomWithCategoryLabel(d.getValue().getClassroom())));
        classroom.setCellFactory(col -> dashIfBlankCell());
        classroom.setPrefWidth(170);

        TableColumn<Inscription, String> session = new TableColumn<>(I18n.t("enrollment.table.session", "تسجيل الحضور"));
        session.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getSession() == null ? "—" : sessionLabel(d.getValue().getSession())));
        session.setCellFactory(col -> pillCell("#EEF2FF", "#4338CA"));
        session.setPrefWidth(130);

        TableColumn<Inscription, String> status = new TableColumn<>(I18n.t("enrollment.table.status", "تسجيل الحضور"));
        status.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getStatus() == null ? "—" : enrollmentStatusLabel(d.getValue().getStatus())));
        status.setCellFactory(col -> statusCell());
        status.setPrefWidth(110);

        // NEW: one pill per enrolled course
        TableColumn<Inscription, Inscription> courses = new TableColumn<>(I18n.t("enrollment.table.courses", "تسجيل الحضور"));
        courses.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        courses.setCellFactory(col -> coursesSummaryCell());
        courses.setPrefWidth(240);

        table.getColumns().addAll(List.of(date, student, classroom, session, status, courses));
    }

    private String sessionLabel(SessionName session) {
        return switch (session.name()) {
            case "MATINEE" -> I18n.t("session.matinee", "تسجيل الحضور");
            case "MATINEE_AVEC_REPAS" -> I18n.t("session.matinee_avec_repas", "تسجيل الحضور");
            case "JOURNEE_COMPLETE" -> I18n.t("session.journee_complete", "تسجيل الحضور");
            default -> session.name();
        };
    }

    private String enrollmentStatusLabel(EnrollmentStatus status) {
        return switch (status.name()) {
            case "ACTIVE" -> I18n.t("status.active", "تسجيل الحضور");
            case "COMPLETED" -> I18n.t("status.completed", "تسجيل الحضور");
            case "DROPPED" -> I18n.t("status.dropped", "تسجيل الحضور");
            default -> status.name();
        };
    }

    /** Shows one pill per course the student is enrolled in for this inscription. */
    private TableCell<Inscription, Inscription> coursesSummaryCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Inscription i, boolean empty) {
                super.updateItem(i, empty);
                if (empty || i == null || i.getCourses() == null || i.getCourses().isEmpty()) {
                    setGraphic(null);
                    setText(empty ? null : "—");
                    return;
                }
                setText(null);
                FlowPane pills = new FlowPane(6, 6);
                pills.setPrefWrapLength(220);
                for (Course c : i.getCourses()) {
                    pills.getChildren().add(TableStyleKit.pill(c.getName(), "#F1F5F9", "#334155"));
                }
                setGraphic(pills);
            }
        };
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
                String subtitle = i.getClassroom() == null ? "—" : i.getClassroom().getName();
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
                list -> {
                    allCourses.addAll(list);
                    buildCourseChips(filterCoursesForSelectedClassroom());
                },
                err -> DialogUtil.error("Erreur", "Échec du chargement des cours : " + err.getMessage()));
        AsyncTasks.run(
                () -> new int[]{
                        settingService.getInt(EnrollmentSettingsKeys.CRECHE_MIN_AGE, EnrollmentSettingsKeys.CRECHE_MIN_AGE_DEFAULT),
                        settingService.getInt(EnrollmentSettingsKeys.CRECHE_MAX_AGE, EnrollmentSettingsKeys.CRECHE_MAX_AGE_DEFAULT),
                        settingService.getInt(EnrollmentSettingsKeys.PREPARATOIRE_MIN_AGE, EnrollmentSettingsKeys.PREPARATOIRE_MIN_AGE_DEFAULT),
                        settingService.getInt(EnrollmentSettingsKeys.PREPARATOIRE_MAX_AGE, EnrollmentSettingsKeys.PREPARATOIRE_MAX_AGE_DEFAULT),
                        settingService.getInt(EnrollmentSettingsKeys.SOUTIEN_MIN_AGE, EnrollmentSettingsKeys.SOUTIEN_MIN_AGE_DEFAULT),
                        settingService.getInt(EnrollmentSettingsKeys.SOUTIEN_MAX_AGE, EnrollmentSettingsKeys.SOUTIEN_MAX_AGE_DEFAULT),
                },
                ages -> {
                    crecheMinAge = ages[0];
                    crecheMaxAge = ages[1];
                    preparatoireMinAge = ages[2];
                    preparatoireMaxAge = ages[3];
                    soutienMinAge = ages[4];
                    soutienMaxAge = ages[5];
                },
                err -> DialogUtil.error("Erreur", "Échec du chargement des règles d'âge : " + err.getMessage()));
    }

    /** Returns only the courses belonging to the currently selected classroom, or all courses if none is selected. */
    private List<Course> filterCoursesForSelectedClassroom() {
        Classroom current = classroomField.getValue();
        if (current == null || current.getId() == null) {
            return allCourses;
        }
        return allCourses.stream()
                .filter(c -> c.getClassroom() != null && current.getId().equals(c.getClassroom().getId()))
                .toList();
    }

    /** (Re)builds the course chips for the given course list, keeping any prior selection selected by id. */
    private void buildCourseChips(List<Course> courses) {
        List<String> previouslyChecked = courseChecks.stream()
                .filter(CheckBox::isSelected)
                .map(cb -> ((Course) cb.getUserData()).getId())
                .toList();

        coursesBox.getChildren().clear();
        courseChecks.clear();

        if (courses.isEmpty()) {
            Label none = new Label(classroomField.getValue() == null
                    ? I18n.t("enrollment.no_courses_available", "تسجيل الحضور")
                    : I18n.t("ewizard.no_courses_for_classroom", "تسجيل الحضور"));
            none.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
            coursesBox.getChildren().add(none);
            updateTotalCost();
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
        FormFactory.addRow(grid, 0, I18n.t("field.student", "تسجيل الحضور"), studentField);
        FormFactory.addRow(grid, 1, I18n.t("field.classroom", "تسجيل الحضور"), classroomField);
        sessionFieldLabel.setText(I18n.t("field.session", "تسجيل الحضور"));
        grid.add(sessionFieldLabel, 0, 2);
        grid.add(sessionField, 1, 2);
        GridPane.setHgrow(sessionField, Priority.ALWAYS);
        FormFactory.addRow(grid, 3, I18n.t("field.status", "تسجيل الحضور"), statusField);

        // --- Cours: built as its own block, not through the 2-col grid row ---
        Label coursesLabel = new Label(I18n.t("enrollment.table.courses", "تسجيل الحضور"));
        coursesLabel.getStyleClass().add("field-label"); // match your other field labels' style

        ScrollPane coursesScroll = new ScrollPane(coursesBox);
        coursesScroll.setFitToWidth(true);
        coursesScroll.setPrefHeight(200);
        coursesScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox coursesBlock = new VBox(6, coursesLabel, coursesScroll);
        this.coursesBlock = coursesBlock;
        // Hidden by default; the classroomField listener reveals this only for a
        // selected دعم (Soutien) classroom.
        coursesBlock.setVisible(false);
        coursesBlock.setManaged(false);
        // -----------------------------------------------------------------

        HBox totalCostRow = new HBox(8, new Label(I18n.t("enrollment.cost", "تسجيل الحضور")), totalCostValue);
        totalCostRow.setAlignment(Pos.CENTER_LEFT);
        this.totalCostRow = totalCostRow;
        totalCostRow.setVisible(false);
        totalCostRow.setManaged(false);

        Button save = new Button(I18n.t("action.save", "تسجيل الحضور"));
        save.getStyleClass().add("primary-button");
        save.setOnAction(e -> save());

        Button cancel = new Button(I18n.t("wizard.cancel", "تسجيل الحضور"));
        cancel.getStyleClass().add("secondary-button");
        cancel.setOnAction(e -> closeForm());

        Button delete = new Button(I18n.t("action.delete", "تسجيل الحضور"));
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(e -> delete());

        HBox actions = new HBox(8, save, cancel, delete);
        VBox panel = new VBox(12, new Label(I18n.t("enrollment.details", "تسجيل الحضور")), grid, coursesBlock, totalCostRow, actions);
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
        // Setting the classroom fires the classroomField listener, which
        // rebuilds courseChecks against the courses for this classroom.
        classroomField.setValue(inscription.getClassroom());
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
        classroomField.setValue(null); // triggers listener → chips rebuild to show all/none as appropriate
        sessionField.setValue(null);
        statusField.setValue(null);
        courseChecks.forEach(cb -> cb.setSelected(false));
        updateTotalCost();
        table.getSelectionModel().clearSelection();
    }

    private void save() {
        if (studentField.getValue() == null || classroomField.getValue() == null) {
            DialogUtil.error(I18n.t("dialog.required_fields", "تسجيل الحضور"), I18n.t("enrollment.student_classroom_required", "تسجيل الحضور"));
            return;
        }
        String ageError = checkAgeMatchesCategory(studentField.getValue(), classroomField.getValue());
        if (ageError != null) {
            DialogUtil.error(I18n.t("ewizard.title", "تسجيل الحضور"), ageError);
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

    /**
     * Mirrors the per-category age rules enforced in EnrollmentWizard (step 2), so that
     * editing/creating an enrollment directly from this screen can't bypass them —
     * e.g. an infant (رضيع) cannot be enrolled in a دعم (Soutien) classroom.
     * Returns a translated error message, or null when the age is acceptable
     * (including when the student has no date of birth on file, which we don't block on).
     */
    private String checkAgeMatchesCategory(Student student, Classroom classroom) {
        if (student.getDateOfBirth() == null || classroom.getCategory() == null) {
            return null;
        }
        LocalDate dob = student.getDateOfBirth().toLocalDate();
        int age = Period.between(dob, LocalDate.now()).getYears();
        Category cat = classroom.getCategory();
        if (cat == Category.CRECHE) {
            if (crecheMinAge > 0 && age < crecheMinAge) {
                return I18n.t("ewizard.category_age_min", "تسجيل الحضور")
                        .replace("{category}", I18n.t("category.creche", "تسجيل الحضور"))
                        .replace("{min}", String.valueOf(crecheMinAge));
            }
            if (crecheMaxAge > 0 && age > crecheMaxAge) {
                return I18n.t("ewizard.category_age_max", "تسجيل الحضور")
                        .replace("{category}", I18n.t("category.creche", "تسجيل الحضور"))
                        .replace("{max}", String.valueOf(crecheMaxAge));
            }
        } else if (cat == Category.PREPARATOIRE) {
            if (preparatoireMinAge > 0 && age < preparatoireMinAge) {
                return I18n.t("ewizard.category_age_min", "تسجيل الحضور")
                        .replace("{category}", I18n.t("category.preparatoire", "تسجيل الحضور"))
                        .replace("{min}", String.valueOf(preparatoireMinAge));
            }
            if (preparatoireMaxAge > 0 && age >= preparatoireMaxAge) {
                return I18n.t("ewizard.category_age_max_exclusive", "تسجيل الحضور")
                        .replace("{category}", I18n.t("category.preparatoire", "تسجيل الحضور"))
                        .replace("{max}", String.valueOf(preparatoireMaxAge));
            }
        } else if (cat == Category.SOUTIEN) {
            if (soutienMinAge > 0 && age <= soutienMinAge) {
                return I18n.t("ewizard.category_age_min_exclusive", "تسجيل الحضور")
                        .replace("{category}", I18n.t("category.soutien", "تسجيل الحضور"))
                        .replace("{min}", String.valueOf(soutienMinAge));
            }
            if (soutienMaxAge > 0 && age > soutienMaxAge) {
                return I18n.t("ewizard.category_age_max", "تسجيل الحضور")
                        .replace("{category}", I18n.t("category.soutien", "تسجيل الحضور"))
                        .replace("{max}", String.valueOf(soutienMaxAge));
            }
        }
        return null;
    }

    private void delete() {
        if (selected == null) return;
        if (!DialogUtil.confirm(I18n.t("dialog.confirm", "تسجيل الحضور"), I18n.t("enrollment.delete_confirm", "تسجيل الحضور"))) return;
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
        countLabel.setText(rows.size() + " " + I18n.t(
                rows.size() == 1 ? "enrollment.count_singular" : "enrollment.count_plural", "تسجيل الحضور"));
    }
}