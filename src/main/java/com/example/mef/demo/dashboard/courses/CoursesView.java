package com.example.mef.demo.dashboard.courses;

import com.example.mef.demo.Model.Classroom;
import com.example.mef.demo.Model.Course;
import com.example.mef.demo.Model.CourseScheduleSlot;
import com.example.mef.demo.Model.Employee;
import com.example.mef.demo.Model.Inscription;
import com.example.mef.demo.Model.Student;
import com.example.mef.demo.Services.ClassroomService;
import com.example.mef.demo.Services.CourseService;
import com.example.mef.demo.Services.EmployeeService;
import com.example.mef.demo.Services.EnrollmentService;
import com.example.mef.demo.Services.ScheduleSettingsKeys;
import com.example.mef.demo.Services.SettingService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FloatingPanel;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.dashboard.common.TableStyleKit;
import com.example.mef.demo.dashboard.common.TimeSlots;
import com.example.mef.demo.enums.CourseStatus;
import com.example.mef.demo.util.DialogUtil;
import com.example.mef.demo.util.I18n;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Typed CRUD screen for the "courses" module (Course entity), restyled to match the
 * Outcomings module: filter toolbar, summary cards, and a floating (draggable) details panel
 * instead of a fixed side form.
 */
@Component
public class CoursesView {

    private final CourseService courseService;
    private final EmployeeService employeeService;
    private final ClassroomService classroomService;
    private final SettingService settingService;
    private final EnrollmentService enrollmentService;

    private final ObservableList<Course> rows = FXCollections.observableArrayList();
    private final TableView<Course> table = new TableView<>(rows);
    {
        TableStyleKit.applyTheme(table, "courses");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private final TextField searchField = FormFactory.textField("");
    private final ComboBox<String> statusFilter = new ComboBox<>();

    private final TextField nameField = FormFactory.textField("");
    private final TextField scheduleField = FormFactory.textField("");
    private final Button scheduleButton = new Button();
    private final TextField feeField = FormFactory.textField("");
    private final ComboBox<Employee> teacherField = new ComboBox<>();
    private final ComboBox<Classroom> classroomField = new ComboBox<>();
    private final ComboBox<CourseStatus> statusField = new ComboBox<>(FXCollections.observableArrayList(CourseStatus.values()));

    private final Label footerCountLabel = new Label();
    private final Label footerTotalLabel = new Label();
    private final HBox summaryCards = new HBox(14);

    private List<Course> allCourses = List.of();
    private Course selected;
    private VBox form;

    /** Overlay Pane that the floating panel lives in, stacked on top of the normal screen content. */
    private Pane overlay;
    private FloatingPanel floatingForm;

    public CoursesView(CourseService courseService, EmployeeService employeeService,
                       ClassroomService classroomService, SettingService settingService,
                       EnrollmentService enrollmentService) {
        this.courseService = courseService;
        this.employeeService = employeeService;
        this.classroomService = classroomService;
        this.settingService = settingService;
        this.enrollmentService = enrollmentService;

        teacherField.setMaxWidth(Double.MAX_VALUE);
        classroomField.setMaxWidth(Double.MAX_VALUE);
        statusField.setMaxWidth(Double.MAX_VALUE);

        teacherField.setCellFactory(cb -> teacherCell());
        teacherField.setButtonCell(teacherCell());
        // Explicit converter: without this, the button cell can fall back to
        // Employee#toString() (e.g. "com.example...@1a2b3c") instead of the
        // name, particularly when the selected value isn't reference-equal
        // to an item already loaded into the combo's items list.
        teacherField.setConverter(new StringConverter<Employee>() {
            @Override
            public String toString(Employee e) {
                return e == null ? "" : teacherLabel(e);
            }

            @Override
            public Employee fromString(String s) {
                return teacherField.getValue();
            }
        });

        classroomField.setCellFactory(cb -> classroomCell());
        classroomField.setButtonCell(classroomCell());
        classroomField.setConverter(new StringConverter<Classroom>() {
            @Override
            public String toString(Classroom c) {
                return c == null ? "" : c.getName();
            }

            @Override
            public Classroom fromString(String s) {
                return classroomField.getValue();
            }
        });

        statusField.setCellFactory(cb -> statusListCell());
        statusField.setButtonCell(statusListCell());

        scheduleField.setEditable(false);
        scheduleField.setFocusTraversable(false);
        scheduleButton.getStyleClass().add("secondary-button");
        scheduleButton.setOnAction(e -> openSchedulePicker());

        refreshLocalizedControls();
    }

    private void refreshLocalizedControls() {
        searchField.setPromptText(I18n.t("course.search", "تسجيل الحضور"));
        nameField.setPromptText(I18n.t("course.name_hint", "تسجيل الحضور"));
        scheduleField.setPromptText(I18n.t("course.schedule_none", "تسجيل الحضور"));
        scheduleButton.setText(I18n.t("course.choose_schedule", "تسجيل الحضور"));
        feeField.setPromptText(I18n.t("course.fee_hint", "تسجيل الحضور"));

        String selectedStatus = statusFilter.getValue();
        ObservableList<String> statusOptions = FXCollections.observableArrayList(I18n.t("course.filter_all", "تسجيل الحضور"));
        for (CourseStatus status : CourseStatus.values()) {
            statusOptions.add(statusLabel(status));
        }
        statusFilter.setItems(statusOptions);
        statusFilter.setValue(statusOptions.contains(selectedStatus) ? selectedStatus : I18n.t("course.filter_all", "تسجيل الحضور"));
    }

    public void render(BorderPane contentPane, Label pageTitleLabel) {
        refreshLocalizedControls();
        pageTitleLabel.setText(I18n.t("course.title", "تسجيل الحضور"));

        buildColumns();
        wireRowDoubleClick();

        Label subtitle = new Label(I18n.t("course.subtitle", "تسجيل الحضور"));
        subtitle.getStyleClass().add("page-subtitle");

        searchField.getStyleClass().add("filter-field");
        statusFilter.getStyleClass().add("filter-field");
        statusFilter.setPrefWidth(150);

        Button addBtn = new Button("+  " + I18n.t("course.add", "تسجيل الحضور"));
        addBtn.getStyleClass().add("primary-button");
        addBtn.setOnAction(e -> startCreate());

        HBox filters = new HBox(10, statusFilter, searchField);
        filters.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        HBox toolbar = new HBox(12, filters, addBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("module-toolbar");

        footerCountLabel.getStyleClass().add("footer-stat");
        footerTotalLabel.getStyleClass().add("footer-stat-bold");
        HBox footer = new HBox(20, footerCountLabel, new Region(), footerTotalLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(footer.getChildren().get(1), Priority.ALWAYS);
        footer.getStyleClass().add("table-footer");

        for (Node n : summaryCards.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        VBox tableBlock = new VBox(0, table, footer);
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox center = new VBox(16, subtitle, toolbar, tableBlock, summaryCards);
        center.setPadding(new Insets(24));
        VBox.setVgrow(tableBlock, Priority.ALWAYS);

        // Rebuild the panel so its labels and buttons follow the selected language.
        form = buildForm();
        floatingForm = null;

        // Overlay hosts the floating panel; pickOnBounds(false) lets clicks pass through
        // to the table/buttons underneath wherever the overlay itself has no floating panel.
        overlay = new Pane();
        overlay.setPickOnBounds(false);

        StackPane root = new StackPane(center, overlay);
        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.getStyleClass().add("details-scroll");
        scrollPane.setFitToWidth(true);
        contentPane.setCenter(scrollPane);

        wireFilters();
        loadPickers();
        reload();
    }

    private void wireFilters() {
        searchField.textProperty().addListener((o, a, b) -> applyFilters());
        statusFilter.valueProperty().addListener((o, a, b) -> applyFilters());
    }

    /** Opens the floating details panel for a row when the user double-clicks it. */
    private void wireRowDoubleClick() {
        table.setRowFactory(tv -> {
            TableRow<Course> row = new TableRow<>();
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

        TableColumn<Course, String> name = new TableColumn<>(I18n.t("course.table.name", "تسجيل الحضور"));
        name.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getName()));
        name.setPrefWidth(170);

        TableColumn<Course, String> teacher = new TableColumn<>(I18n.t("course.table.teacher", "تسجيل الحضور"));
        teacher.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getTeacher() == null ? "—" : teacherLabel(d.getValue().getTeacher())));
        teacher.setPrefWidth(150);

        TableColumn<Course, String> classroom = new TableColumn<>(I18n.t("course.table.classroom", "تسجيل الحضور"));
        classroom.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getClassroom() == null ? "—" : d.getValue().getClassroom().getName()));

        TableColumn<Course, String> fee = new TableColumn<>(I18n.t("course.table.fee", "تسجيل الحضور"));
        fee.setCellValueFactory(d -> new ReadOnlyStringWrapper(formatFee(d.getValue().getMonthlyFee())));

        TableColumn<Course, CourseStatus> status = new TableColumn<>(I18n.t("course.table.status", "تسجيل الحضور"));
        status.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getStatus()));
        status.setCellFactory(col -> statusTableCell());

        TableColumn<Course, Course> schedule = new TableColumn<>(I18n.t("course.table.schedule", "تسجيل الحضور"));
        schedule.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        schedule.setCellFactory(col -> scheduleCell());
        schedule.setPrefWidth(240);

        TableColumn<Course, Course> actions = new TableColumn<>(I18n.t("course.table.actions", "تسجيل الحضور"));
        actions.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        actions.setCellFactory(col -> actionCell());
        actions.setPrefWidth(110);
        actions.setMaxWidth(120);

        table.getColumns().addAll(List.of(name, teacher, classroom, fee, status, schedule, actions));
    }

    private TableCell<Course, CourseStatus> statusTableCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(CourseStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Label badge = new Label(statusLabel(item));
                badge.getStyleClass().add("status-badge");
                badge.setStyle(statusBadgeStyle(item));
                setGraphic(badge);
            }
        };
    }

    private TableCell<Course, Course> actionCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Course item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                Button view = iconBtn("fth-eye", I18n.t("course.view_students", "تسجيل الحضور"));
                Button edit = iconBtn("fth-edit-2", I18n.t("action.edit", "تسجيل الحضور"));
                Button del = iconBtn("fth-trash-2", I18n.t("action.delete", "تسجيل الحضور"));
                del.getStyleClass().add("icon-action-danger");

                // "Voir" now opens the enrolled-students dialog instead of the edit form —
                // use the pencil icon to edit the course itself.
                view.setOnAction(e -> showEnrolledStudents(item));
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

    /**
     * Loads every enrollment, filters down to the ones that include the given course, and
     * shows the matching students in a small modal dialog. Filtering happens client-side
     * since {@link EnrollmentService#findAll()} already returns each {@link Inscription}
     * with its {@code courses} collection populated — no new repository query needed.
     */
    private void showEnrolledStudents(Course course) {
        AsyncTasks.run(
                enrollmentService::findAll,
                inscriptions -> {
                    List<Inscription> matching = inscriptions.stream()
                            .filter(i -> i.getCourses() != null && i.getCourses().stream()
                                    .anyMatch(c -> c.getId() != null && c.getId().equals(course.getId())))
                            .toList();
                    openEnrolledStudentsDialog(course, matching);
                },
                err -> DialogUtil.error("Erreur", "Échec du chargement des inscriptions : " + err.getMessage())
        );
    }

    private void openEnrolledStudentsDialog(Course course, List<Inscription> matching) {
        Label title = new Label("Élèves inscrits — " + course.getName());
        title.getStyleClass().add("workflow-title");

        Label count = new Label(matching.size() + (matching.size() > 1 ? " élèves" : " élève"));
        count.getStyleClass().add("stat-caption");

        VBox listBox = new VBox(8);
        if (matching.isEmpty()) {
            Label none = new Label("Aucun élève inscrit à ce cours pour le moment.");
            none.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px;");
            listBox.getChildren().add(none);
        } else {
            for (Inscription inscription : matching) {
                listBox.getChildren().add(enrolledStudentRow(inscription));
            }
        }

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(320);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        Button close = new Button("Fermer");
        close.getStyleClass().add("secondary-button");

        VBox root = new VBox(14, title, count, scroll, close);
        root.getStyleClass().add("workflow-card");
        root.setPadding(new Insets(20));
        root.setPrefWidth(420);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        Window owner = table.getScene() == null ? null : table.getScene().getWindow();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle("Élèves inscrits");
        dialog.setScene(new Scene(root));
        close.setOnAction(e -> dialog.close());
        dialog.showAndWait();
    }

    /** One row: avatar-style initials, student name, and their classroom/status for context. */
    /** One row: avatar-style initials, student name, and their classroom/status for context. */
    private HBox enrolledStudentRow(Inscription inscription) {
        Student s = inscription.getStudent();
        String initials = s == null ? "?" : TableStyleKit.initialsOf(s.getFirstName(), s.getLastName());
        String color = TableStyleKit.colorFor(s == null || s.getGender() == null ? "" : s.getGender().name());
        String fullName = s == null ? "—"
                : ((s.getFirstName() == null ? "" : s.getFirstName()) + " " + (s.getLastName() == null ? "" : s.getLastName())).trim();
        String classroomName = inscription.getClassroom() == null ? "—" : inscription.getClassroom().getName();
        String statusName = inscription.getStatus() == null ? "—" : inscription.getStatus().name();

        Label avatar = new Label(initials);
        avatar.setMinSize(36, 36);
        avatar.setMaxSize(36, 36);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 18; "
                + "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label nameLbl = new Label(fullName);
        nameLbl.setStyle("-fx-text-fill: #0F172A; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label classLbl = new Label(classroomName);
        classLbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
        VBox nameBox = new VBox(2, nameLbl, classLbl);

        Label statusBadge = new Label(statusName);
        statusBadge.getStyleClass().add("status-badge");
        boolean active = "ACTIVE".equalsIgnoreCase(statusName);
        statusBadge.setStyle((active
                ? "-fx-background-color: #DCFCE7; -fx-text-fill: #15803D;"
                : "-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C;")
                + " -fx-padding: 3 10; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;");

        HBox row = new HBox(12, avatar, nameBox, new Region(), statusBadge);
        HBox.setHgrow(row.getChildren().get(2), Priority.ALWAYS);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 8;");
        row.setPadding(new Insets(8, 12, 8, 12));
        return row;
    }
    private void openSchedulePicker() {
        Employee teacher = teacherField.getValue();
        AsyncTasks.run(
                courseService::findAll,
                allCourses -> {
                    List<Course> others = allCourses.stream()
                            .filter(c -> selected == null || selected.getId() == null
                                    || !selected.getId().equals(c.getId()))
                            .toList();
                    String dayStart = settingService.get(ScheduleSettingsKeys.DAY_START, ScheduleSettingsKeys.DAY_START_DEFAULT);
                    String dayEnd = settingService.get(ScheduleSettingsKeys.DAY_END, ScheduleSettingsKeys.DAY_END_DEFAULT);
                    String breakStart = settingService.get(ScheduleSettingsKeys.REST_START, ScheduleSettingsKeys.REST_START_DEFAULT);
                    String breakEnd = settingService.get(ScheduleSettingsKeys.REST_END, ScheduleSettingsKeys.REST_END_DEFAULT);
                    List<TimeSlots.TimeBlock> blocks = TimeSlots.generateBlocks(dayStart, breakStart, breakEnd, dayEnd);
                    
                    SchedulePickerDialog.show(
                                    scheduleButton.getScene() == null ? null : scheduleButton.getScene().getWindow(),
                                    scheduleField.getText(),
                                    I18n.t("schedule.title", "تسجيل الحضور"),
                                    I18n.t("schedule.hint", "تسجيل الحضور"),
                                    teacher, others, blocks)
                            .ifPresent(scheduleField::setText);
                },
                err -> DialogUtil.error("Erreur", "Échec du chargement des cours : " + err.getMessage())
        );
    }

    private ListCell<Employee> teacherCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Employee item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : teacherLabel(item));
            }
        };
    }

    /**
     * "Prénom Nom", plus the employee number when another teacher in the list shares the
     * same name — otherwise two different Employee records with identical names are
     * indistinguishable in the dropdown, which can lead to picking the wrong one (e.g. when
     * checking whether a course is really being double-booked on the same teacher).
     */
    private String teacherLabel(Employee e) {
        if (e == null) return "";
        String name = safeTeacherName(e);
        boolean hasDuplicate = teacherField.getItems().stream()
                .filter(other -> other != e)
                .anyMatch(other -> safeTeacherName(other).equalsIgnoreCase(name));
        if (hasDuplicate && e.getEmployeeNumber() != null && !e.getEmployeeNumber().isBlank()) {
            return name + "  ·  N° " + e.getEmployeeNumber();
        }
        return name;
    }

    private static String safeTeacherName(Employee e) {
        String first = e.getFirstName() == null ? "" : e.getFirstName();
        String last = e.getLastName() == null ? "" : e.getLastName();
        return (first + " " + last).trim();
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

    private ListCell<CourseStatus> statusListCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(CourseStatus item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : statusLabel(item));
            }
        };
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
        FormFactory.addRow(grid, 0, I18n.t("field.name", "تسجيل الحضور"), nameField);
        FormFactory.addRow(grid, 1, I18n.t("field.teacher", "تسجيل الحضور"), teacherField);
        FormFactory.addRow(grid, 2, I18n.t("field.classroom", "تسجيل الحضور"), classroomField);
        HBox scheduleRow = new HBox(8, scheduleField, scheduleButton);
        HBox.setHgrow(scheduleField, Priority.ALWAYS);
        FormFactory.addRow(grid, 3, I18n.t("field.schedule", "تسجيل الحضور"), scheduleRow);
        FormFactory.addRow(grid, 4, I18n.t("course.table.fee", "تسجيل الحضور"), feeField);
        FormFactory.addRow(grid, 5, I18n.t("field.status", "تسجيل الحضور"), statusField);

        Button save = new Button(I18n.t("action.save", "تسجيل الحضور"));
        save.getStyleClass().add("primary-button");
        save.setOnAction(e -> save());
        Button clear = new Button("+ " + I18n.t("action.new", "تسجيل الحضور"));
        clear.getStyleClass().add("secondary-button");
        clear.setOnAction(e -> startCreate());
        Button delete = new Button(I18n.t("action.delete", "تسجيل الحضور"));
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(e -> delete());

        // No title label here — the FloatingPanel header already shows "Détails du cours".
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
            floatingForm = new FloatingPanel(I18n.t("course.details", "تسجيل الحضور"), form, this::closeForm);
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

    /** Compact day/time badges showing the course's selected timetable slots. */
    private TableCell<Course, Course> scheduleCell() {
        return new TableCell<>() {
            {
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Course course, boolean empty) {
                super.updateItem(course, empty);
                setText(null);
                if (empty || course == null) {
                    setGraphic(null);
                    return;
                }
                List<ScheduleValidator.Slot> slots = ScheduleValidator.parse(course.getSchedule());
                if (slots.isEmpty()) {
                    setGraphic(new Label("—"));
                    return;
                }
                FlowPane badges = new FlowPane(6, 4);
                badges.setAlignment(Pos.CENTER);
                int visible = Math.min(3, slots.size());
                for (int i = 0; i < visible; i++) {
                    ScheduleValidator.Slot slot = slots.get(i);
                    String label = shortDay(slot.day()) + " " + formatTime(slot.startMinutes())
                            + "–" + formatTime(slot.endMinutes());
                    badges.getChildren().add(TableStyleKit.pill(label, "#CFFAFE", "#0E7490"));
                }
                if (slots.size() > visible) {
                    badges.getChildren().add(TableStyleKit.pill("+" + (slots.size() - visible), "#E0F2FE", "#0369A1"));
                }
                setGraphic(badges);
            }
        };
    }

    private void selectRow(Course course) {
        if (course == null) {
            return;
        }
        selected = course;
        nameField.setText(course.getName());
        scheduleField.setText(course.getSchedule());
        feeField.setText(course.getMonthlyFee() == null ? "" : String.valueOf(course.getMonthlyFee()));
        teacherField.setValue(matchById(teacherField.getItems(), course.getTeacher(), Employee::getId));
        classroomField.setValue(matchById(classroomField.getItems(), course.getClassroom(), Classroom::getId));
        statusField.setValue(course.getStatus());
        showFormPanel();
    }

    /** Resolves to the item already loaded in a ComboBox's list that shares the same id as
     *  {@code target}, so the ComboBox's selection model can actually match it (avoids blank
     *  display when {@code target} came from a different service call / object instance than
     *  the combo's own items, and the entity has no id-based equals()). */
    private static <T> T matchById(List<T> items, T target, java.util.function.Function<T, String> idFn) {
        if (target == null || idFn.apply(target) == null) return target;
        String targetId = idFn.apply(target);
        return items.stream()
                .filter(item -> targetId.equals(idFn.apply(item)))
                .findFirst()
                .orElse(target);
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
        course.replaceScheduleSlots(toScheduleSlots(scheduleField.getText()));
        course.setMonthlyFee(fee);
        course.setStatus(statusField.getValue() == null ? CourseStatus.ACTIVE : statusField.getValue());
        course.setTeacher(teacherField.getValue());
        course.setClassroom(classroomField.getValue());
        String teacherId = teacherField.getValue() == null ? null : teacherField.getValue().getId();
        String classroomId = classroomField.getValue() == null ? null : classroomField.getValue().getId();

        AsyncTasks.run(
                () -> {
                    List<Course> others = courseService.findAll();
                    Set<String> closedDays = ScheduleValidator.daysOf(
                            settingService.get(ScheduleSettingsKeys.CLOSED_DAYS, ScheduleSettingsKeys.CLOSED_DAYS_DEFAULT));
                    int enrolled = classroomId == null ? 0 : classroomService.countStudentsInClassroom(classroomId);
                    int restStart = TimeSlots.toMinutes(
                            settingService.get(ScheduleSettingsKeys.REST_START, ScheduleSettingsKeys.REST_START_DEFAULT));
                    int restEnd = TimeSlots.toMinutes(
                            settingService.get(ScheduleSettingsKeys.REST_END, ScheduleSettingsKeys.REST_END_DEFAULT));
                    return ScheduleValidator.validate(course, others, closedDays, enrolled, restStart, restEnd);
                },
                violations -> {
                    if (!violations.isEmpty()) {
                        DialogUtil.error(I18n.t("schedule.validation.title", "تسجيل الحضور"),
                                I18n.t("schedule.validation.save_failed", "تسجيل الحضور") + "\n\n" + String.join("\n", violations));
                        return;
                    }
                    AsyncTasks.run(
                            () -> courseService.save(course, teacherId, classroomId),
                            saved -> { clearForm(); closeForm(); reload(); },
                            err -> DialogUtil.error("Erreur", "Échec de l'enregistrement : " + err.getMessage())
                    );
                },
                err -> DialogUtil.error("Erreur", "Échec de la validation de l'horaire : " + err.getMessage())
        );
    }

    private void delete() {
        if (selected == null) return;
        if (!DialogUtil.confirm("Confirmer", "Supprimer ce cours ?")) return;
        String id = selected.getId();
        AsyncTasks.run(
                () -> courseService.delete(id),
                () -> { clearForm(); closeForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de la suppression : " + err.getMessage())
        );
    }

    private void reload() {
        AsyncTasks.run(
                courseService::findAll,
                list -> {
                    allCourses = list;
                    applyFilters();
                },
                err -> DialogUtil.error("Erreur", "Échec du chargement : " + err.getMessage())
        );
    }

    private void applyFilters() {
        String needle = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String statusVal = statusFilter.getValue();

        List<Course> filtered = allCourses.stream()
                .filter(c -> {
                    if (!needle.isBlank()) {
                        String name = c.getName() == null ? "" : c.getName().toLowerCase();
                        String teacherName = c.getTeacher() == null ? "" :
                                (c.getTeacher().getFirstName() + " " + c.getTeacher().getLastName()).toLowerCase();
                        String classroomName = c.getClassroom() == null ? "" : c.getClassroom().getName().toLowerCase();
                        if (!name.contains(needle) && !teacherName.contains(needle) && !classroomName.contains(needle)) {
                            return false;
                        }
                    }
                    if (statusVal != null && !I18n.t("course.filter_all", "تسجيل الحضور").equals(statusVal)) {
                        if (!statusLabel(c.getStatus()).equals(statusVal)) return false;
                    }
                    return true;
                })
                .toList();

        rows.setAll(filtered);
        updateFooter(filtered);
        updateSummaryCards(allCourses);
    }

    private void updateFooter(List<Course> data) {
        double total = data.stream().mapToDouble(c -> c.getMonthlyFee() == null ? 0 : c.getMonthlyFee()).sum();
        footerCountLabel.setText(I18n.t("course.total", "تسجيل الحضور").replace("{0}", String.valueOf(data.size())));
        footerTotalLabel.setText(I18n.t("course.monthly_income", "تسجيل الحضور").replace("{0}", formatFee(total)));
    }

    private void updateSummaryCards(List<Course> data) {
        summaryCards.getChildren().clear();
        double totalFees = data.stream().mapToDouble(c -> c.getMonthlyFee() == null ? 0 : c.getMonthlyFee()).sum();

        List<Course> active = data.stream().filter(c -> c.getStatus() == CourseStatus.ACTIVE).toList();
        List<Course> withoutSchedule = data.stream()
                .filter(c -> ScheduleValidator.parse(c.getSchedule()).isEmpty())
                .toList();
        List<Course> withoutTeacher = data.stream().filter(c -> c.getTeacher() == null).toList();

        summaryCards.getChildren().addAll(
                summaryCard("fth-book-open", String.valueOf(data.size()), I18n.t("course.total_summary", "تسجيل الحضور"), "#0E7490", "#CFFAFE"),
                summaryCard("fth-check-circle", String.valueOf(active.size()), I18n.t("course.active_summary", "تسجيل الحضور"), "#15803D", "#DCFCE7"),
                summaryCard("fth-dollar-sign", formatFee(totalFees), I18n.t("course.income_summary", "تسجيل الحضور"), "#4338CA", "#EEF2FF"),
                summaryCard("fth-alert-circle", withoutSchedule.size() + " · " + withoutTeacher.size(),
                        I18n.t("course.missing_summary", "تسجيل الحضور"), "#D97706", "#FEF3C7")
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

    private static List<CourseScheduleSlot> toScheduleSlots(String schedule) {
        return ScheduleValidator.parse(schedule).stream()
                .map(slot -> new CourseScheduleSlot(
                        slot.day(), formatTime(slot.startMinutes()), formatTime(slot.endMinutes())))
                .toList();
    }

    private static String formatTime(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }

    private static String formatFee(Double fee) {
        if (fee == null) return "—";
        return String.format(java.util.Locale.FRENCH, "%,.2f DA", fee);
    }

    private static String shortDay(String day) {
        return switch (day) {
            case "Lundi" -> "Lun";
            case "Mardi" -> "Mar";
            case "Mercredi" -> "Mer";
            case "Jeudi" -> "Jeu";
            case "Vendredi" -> "Ven";
            case "Samedi" -> "Sam";
            case "Dimanche" -> "Dim";
            default -> day;
        };
    }

    /** Human-readable label for a CourseStatus value, independent of its exact enum spelling. */
    private String statusLabel(CourseStatus status) {
        if (status == null) return "—";
        return switch (status.name()) {
            case "ACTIVE" -> I18n.t("course.status.active", "تسجيل الحضور");
            case "INACTIVE" -> I18n.t("course.status.inactive", "تسجيل الحضور");
            case "SUSPENDED" -> I18n.t("course.status.suspended", "تسجيل الحضور");
            case "ARCHIVED" -> I18n.t("course.status.archived", "تسجيل الحضور");
            default -> status.name();
        };
    }

    /** vBadge color for a CourseStatus value; unmatched values fall back to a neutral style. */
    private static String statusBadgeStyle(CourseStatus status) {
        if (status == null) return "";
        return switch (status.name()) {
            case "ACTIVE" -> "-fx-background-color: #DCFCE7; -fx-text-fill: #15803D;";
            case "INACTIVE" -> "-fx-background-color: #F1F5F9; -fx-text-fill: #475569;";
            case "SUSPENDED" -> "-fx-background-color: #FEF3C7; -fx-text-fill: #92400E;";
            case "ARCHIVED" -> "-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B;";
            default -> "-fx-background-color: #EEF2FF; -fx-text-fill: #4338CA;";
        };
    }
}
