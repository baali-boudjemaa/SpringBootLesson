package com.example.mef.demo.dashboard.courses;

import com.example.mef.demo.Model.Classroom;
import com.example.mef.demo.Model.Course;
import com.example.mef.demo.Model.CourseScheduleSlot;
import com.example.mef.demo.Model.Employee;
import com.example.mef.demo.Services.ClassroomService;
import com.example.mef.demo.Services.CourseService;
import com.example.mef.demo.Services.EmployeeService;
import com.example.mef.demo.Services.ScheduleSettingsKeys;
import com.example.mef.demo.Services.SettingService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FloatingPanel;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.dashboard.common.TableStyleKit;
import com.example.mef.demo.enums.CourseStatus;
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

    private final ObservableList<Course> rows = FXCollections.observableArrayList();
    private final TableView<Course> table = new TableView<>(rows);
    {
        TableStyleKit.applyTheme(table, "courses");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private final TextField searchField = FormFactory.textField("Rechercher un cours...");
    private final ComboBox<String> statusFilter = new ComboBox<>();

    private final TextField nameField = FormFactory.textField("Nom du cours");
    private final TextField scheduleField = FormFactory.textField("Aucun horaire choisi");
    private final Button scheduleButton = new Button("Choisir…");
    private final TextField feeField = FormFactory.textField("Frais mensuels");
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
                       ClassroomService classroomService, SettingService settingService) {
        this.courseService = courseService;
        this.employeeService = employeeService;
        this.classroomService = classroomService;
        this.settingService = settingService;

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
                return e == null ? "" : e.getFirstName() + " " + e.getLastName();
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

        ObservableList<String> statusOptions = FXCollections.observableArrayList("Tous");
        for (CourseStatus s : CourseStatus.values()) {
            statusOptions.add(statusLabel(s));
        }
        statusFilter.setItems(statusOptions);
        statusFilter.setValue("Tous");
    }

    public void render(BorderPane contentPane, Label pageTitleLabel) {
        pageTitleLabel.setText("Cours");

        buildColumns();
        wireRowDoubleClick();

        Label subtitle = new Label("Gérer les cours, enseignants et horaires");
        subtitle.getStyleClass().add("page-subtitle");

        searchField.getStyleClass().add("filter-field");
        statusFilter.getStyleClass().add("filter-field");
        statusFilter.setPrefWidth(150);

        Button addBtn = new Button("+  Nouveau Cours");
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

        TableColumn<Course, String> name = new TableColumn<>("NOM");
        name.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getName()));
        name.setPrefWidth(170);

        TableColumn<Course, String> teacher = new TableColumn<>("ENSEIGNANT");
        teacher.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getTeacher() == null ? "—" :
                        d.getValue().getTeacher().getFirstName() + " " + d.getValue().getTeacher().getLastName()));
        teacher.setPrefWidth(150);

        TableColumn<Course, String> classroom = new TableColumn<>("CLASSE");
        classroom.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getClassroom() == null ? "—" : d.getValue().getClassroom().getName()));

        TableColumn<Course, String> fee = new TableColumn<>("FRAIS/MOIS");
        fee.setCellValueFactory(d -> new ReadOnlyStringWrapper(formatFee(d.getValue().getMonthlyFee())));

        TableColumn<Course, CourseStatus> status = new TableColumn<>("STATUT");
        status.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().getStatus()));
        status.setCellFactory(col -> statusTableCell());

        TableColumn<Course, Course> schedule = new TableColumn<>("HORAIRE");
        schedule.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        schedule.setCellFactory(col -> scheduleCell());
        schedule.setPrefWidth(240);

        TableColumn<Course, Course> actions = new TableColumn<>("ACTION");
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

    private void openSchedulePicker() {
        SchedulePickerDialog.show(scheduleButton.getScene() == null ? null : scheduleButton.getScene().getWindow(),
                        scheduleField.getText())
                .ifPresent(scheduleField::setText);
    }

    private ListCell<Employee> teacherCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Employee item, boolean empty) {
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
        FormFactory.addRow(grid, 0, "Nom", nameField);
        FormFactory.addRow(grid, 1, "Enseignant", teacherField);
        FormFactory.addRow(grid, 2, "Classe", classroomField);
        HBox scheduleRow = new HBox(8, scheduleField, scheduleButton);
        HBox.setHgrow(scheduleField, Priority.ALWAYS);
        FormFactory.addRow(grid, 3, "Horaire", scheduleRow);
        FormFactory.addRow(grid, 4, "Frais/mois", feeField);
        FormFactory.addRow(grid, 5, "Statut", statusField);

        Button save = new Button("Enregistrer");
        save.getStyleClass().add("primary-button");
        save.setOnAction(e -> save());
        Button clear = new Button("+ Nouveau");
        clear.getStyleClass().add("secondary-button");
        clear.setOnAction(e -> startCreate());
        Button delete = new Button("Supprimer");
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
            floatingForm = new FloatingPanel("Détails du cours", form, this::closeForm);
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
        teacherField.setValue(course.getTeacher());
        classroomField.setValue(course.getClassroom());
        statusField.setValue(course.getStatus());
        showFormPanel();
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
                    return ScheduleValidator.validate(course, others, closedDays, enrolled);
                },
                violations -> {
                    if (!violations.isEmpty()) {
                        DialogUtil.error("Conflit d'horaire",
                                "Impossible d'enregistrer ce cours :\n\n" + String.join("\n", violations));
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
                    if (statusVal != null && !"Tous".equals(statusVal)) {
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
        footerCountLabel.setText("Total des cours : " + data.size());
        footerTotalLabel.setText("Revenu mensuel : " + formatFee(total));
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
                summaryCard("fth-book-open", String.valueOf(data.size()), "Total Cours", "#0E7490", "#CFFAFE"),
                summaryCard("fth-check-circle", String.valueOf(active.size()), "Cours Actifs", "#15803D", "#DCFCE7"),
                summaryCard("fth-dollar-sign", formatFee(totalFees), "Revenu Mensuel", "#4338CA", "#EEF2FF"),
                summaryCard("fth-alert-circle", withoutSchedule.size() + " · " + withoutTeacher.size(),
                        "Sans horaire · Sans enseignant", "#D97706", "#FEF3C7")
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
    private static String statusLabel(CourseStatus status) {
        if (status == null) return "—";
        return switch (status.name()) {
            case "ACTIVE" -> "Actif";
            case "INACTIVE" -> "Inactif";
            case "SUSPENDED" -> "Suspendu";
            case "ARCHIVED" -> "Archivé";
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