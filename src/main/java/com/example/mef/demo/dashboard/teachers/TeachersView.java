package com.example.mef.demo.dashboard.teachers;

import com.example.mef.demo.Model.Course;
import com.example.mef.demo.Model.Employee;
import com.example.mef.demo.Model.TeacherAvailabilitySlot;
import com.example.mef.demo.Services.CourseService;
import com.example.mef.demo.Services.EmployeeService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.DaysPicker;
import com.example.mef.demo.dashboard.common.FloatingPanel;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.dashboard.common.TableStyleKit;
import com.example.mef.demo.dashboard.courses.ScheduleValidator;
import com.example.mef.demo.enums.EmployeeRole;
import com.example.mef.demo.util.DialogUtil;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
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

import org.kordamp.ikonli.javafx.FontIcon;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;


/**
 * Teachers / Personnel screen, restyled to match the Outcomings/Courses/Students pattern:
 * filter toolbar, summary cards, and a floating (draggable) details panel instead of a
 * fixed side panel. Availability picking, the weekly timetable dialog and printing are
 * unchanged from the previous version.
 */
@Component
public class TeachersView {

    // =========================================================
    // SERVICES
    // =========================================================

    private final EmployeeService employeeService;
    private final CourseService courseService;

    // =========================================================
    // STATE
    // =========================================================

    private Employee selected;
    private List<Employee> allEmployees = List.of();
    private boolean tableInitialized = false;
    private VBox form;

    /** Overlay Pane that the floating panel lives in, stacked on top of the normal screen content. */
    private Pane overlay;
    private FloatingPanel floatingForm;

    // =========================================================
    // TABLE
    // =========================================================

    private final ObservableList<Employee> rows =
            FXCollections.observableArrayList();

    private final TableView<Employee> table =
            new TableView<>(rows);

    // =========================================================
    // FILTERS
    // =========================================================

    private final TextField searchField =
            FormFactory.textField("Rechercher un employé...");

    private final ComboBox<String> roleFilter = new ComboBox<>();

    private final Label footerCountLabel = new Label();
    private final HBox summaryCards = new HBox(14);

    // =========================================================
    // FORM FIELDS
    // =========================================================

    private final TextField firstNameField =
            FormFactory.textField("Prénom");

    private final TextField lastNameField =
            FormFactory.textField("Nom");

    private final TextField emailField =
            FormFactory.textField("Email");

    private final TextField phoneField =
            FormFactory.textField("Téléphone");

    private final ComboBox<EmployeeRole> roleField =
            new ComboBox<>(
                    FXCollections.observableArrayList(
                            EmployeeRole.values()
                    )
            );

    private final TextArea certificationsField =
            new TextArea();

    private final TextField availabilityField =
            FormFactory.textField("Aucune disponibilité");

    private final Button availabilityButton =
            new Button("Choisir…");

    private final Button timetableButton =
            new Button("📅 Emploi du temps");

    // =========================================================
    // AVAILABILITY STATE
    // =========================================================

    private String currentWorkingDays = "";

    private String currentWorkStart = "";

    private String currentWorkEnd = "";

    private String currentAvailabilitySchedule = "";

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public TeachersView(
            EmployeeService employeeService,
            CourseService courseService
    ) {

        this.employeeService = employeeService;
        this.courseService = courseService;

        TableStyleKit.applyTheme(
                table,
                "teachers"
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        roleField.setMaxWidth(
                Double.MAX_VALUE
        );

        certificationsField.setPromptText(
                "Certifications"
        );

        certificationsField.setPrefRowCount(3);

        availabilityField.setEditable(false);

        availabilityField.setFocusTraversable(false);

        availabilityButton
                .getStyleClass()
                .add("secondary-button");

        availabilityButton.setOnAction(
                e -> openAvailabilityPicker()
        );

        timetableButton
                .getStyleClass()
                .add("secondary-button");

        timetableButton.setOnAction(
                e -> showTeacherTimetable()
        );

        updateAvailabilitySummary();

        ObservableList<String> roleOptions = FXCollections.observableArrayList("Tous");
        for (EmployeeRole r : EmployeeRole.values()) {
            roleOptions.add(r.name());
        }
        roleFilter.setItems(roleOptions);
        roleFilter.setValue("Tous");
    }

    // =========================================================
    // AVAILABILITY PICKER
    // =========================================================

    private void openAvailabilityPicker() {

        String originalSchedule =
                currentAvailabilitySchedule;

        TeacherAvailabilityDialog.show(

                availabilityButton.getScene() == null
                        ? null
                        : availabilityButton
                        .getScene()
                        .getWindow(),

                originalSchedule

        ).ifPresent(result -> {

            currentAvailabilitySchedule =
                    ScheduleValidator.parse(
                                    result.schedule()
                            )
                            .stream()
                            .map(
                                    TeachersView::slotToScheduleString
                            )
                            .distinct()
                            .collect(
                                    Collectors.joining("; ")
                            );

            currentWorkingDays = "";
            currentWorkStart = "";
            currentWorkEnd = "";

            updateAvailabilitySummary();
        });
    }

    // =========================================================
    // AVAILABILITY SUMMARY
    // =========================================================

    private void updateAvailabilitySummary() {

        if (currentAvailabilitySchedule != null
                && !currentAvailabilitySchedule.isBlank()) {

            List<ScheduleValidator.Slot> slots =
                    ScheduleValidator.parse(
                            currentAvailabilitySchedule
                    );

            if (!slots.isEmpty()) {

                int visible =
                        Math.min(2, slots.size());

                StringBuilder summary =
                        new StringBuilder();

                for (int i = 0; i < visible; i++) {

                    ScheduleValidator.Slot slot =
                            slots.get(i);

                    if (i > 0) {
                        summary.append(" · ");
                    }

                    summary.append(shortDay(slot.day()));
                    summary.append(" ");
                    summary.append(formatTime(slot.startMinutes()));
                    summary.append("–");
                    summary.append(formatTime(slot.endMinutes()));
                }

                if (slots.size() > visible) {

                    summary.append(" +");
                    summary.append(slots.size() - visible);
                }

                availabilityField.setText(summary.toString());

                return;
            }
        }

        // Legacy fallback

        if (currentWorkingDays == null
                || currentWorkingDays.isBlank()) {

            availabilityField.setText("Aucune disponibilité");

            return;
        }

        String days =
                String.join(", ", currentWorkingDays.split(","));

        String hours = "";

        if (currentWorkStart != null
                && !currentWorkStart.isBlank()
                && currentWorkEnd != null
                && !currentWorkEnd.isBlank()) {

            hours = " · " + currentWorkStart + "–" + currentWorkEnd;
        }

        availabilityField.setText(days + hours);
    }

    // =========================================================
    // RENDER
    // =========================================================

    public void render(
            BorderPane contentPane,
            Label pageTitleLabel
    ) {

        pageTitleLabel.setText("Personnel");

        if (!tableInitialized) {

            initializeTeacherTable();

            tableInitialized = true;
        }

        Label subtitle = new Label("Gérer les enseignants et le personnel");
        subtitle.getStyleClass().add("page-subtitle");

        searchField.getStyleClass().add("filter-field");
        roleFilter.getStyleClass().add("filter-field");
        roleFilter.setPrefWidth(150);

        Button add =
                new Button("+  Ajouter un employé");

        add.getStyleClass().add("primary-button");

        add.setOnAction(e -> startCreate());

        HBox filters = new HBox(10, roleFilter, searchField);
        filters.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        HBox toolbar = new HBox(12, filters, add);
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

        table.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, oldValue, newValue) -> {
                            if (newValue != null) {
                                selectRow(newValue);
                            }
                        }
                );

        // -----------------------------------------------------
        // FORM (built once, panel shown/hidden on demand)
        // -----------------------------------------------------

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
        roleFilter.valueProperty().addListener((o, a, b) -> applyFilters());
    }

    // =========================================================
    // SHOW / HIDE FORM PANEL
    // =========================================================

    private void startCreate() {

        clearForm();

        showFormPanel();
    }

    private void showFormPanel() {
        if (floatingForm == null) {
            floatingForm = new FloatingPanel("Détails de l'employé", form, this::closeForm);
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

    // =========================================================
    // FORM
    // =========================================================

    private VBox buildForm() {

        GridPane grid =
                FormFactory.sectionGrid();

        FormFactory.addRow(grid, 0, "Prénom", firstNameField);
        FormFactory.addRow(grid, 1, "Nom", lastNameField);
        FormFactory.addRow(grid, 2, "Email", emailField);
        FormFactory.addRow(grid, 3, "Téléphone", phoneField);
        FormFactory.addRow(grid, 4, "Rôle", roleField);
        FormFactory.addRow(grid, 5, "Certifications", certificationsField);

        HBox availabilityRow =
                new HBox(6, availabilityField, availabilityButton);

        availabilityRow.setAlignment(Pos.CENTER_LEFT);

        HBox.setHgrow(availabilityField, Priority.ALWAYS);

        FormFactory.addRow(grid, 6, "Disponibilité", availabilityRow);

        // -----------------------------------------------------
        // BUTTONS
        // -----------------------------------------------------

        Button save =
                new Button("Enregistrer");

        save.getStyleClass().add("primary-button");

        save.setOnAction(e -> save());

        Button clear =
                new Button("+ Nouveau");

        clear.getStyleClass().add("secondary-button");

        clear.setOnAction(e -> startCreate());

        Button delete =
                new Button("Supprimer");

        delete.getStyleClass().add("danger-button");

        delete.setOnAction(e -> delete());

        HBox actions =
                new HBox(8, save, clear, delete);

        // No title label here — the FloatingPanel header already shows "Détails de l'employé".
        // NOTE: no setPrefWidth() on this VBox — the panel sits inside a ScrollPane with
        // fitToWidth(true) (see FloatingPanel), and forcing a fixed prefWidth here fought
        // that constraint on the very first layout pass, which could resolve the GridPane's
        // input column to 0 width and make every field render invisible.
        return new VBox(12, grid, actions, timetableButton);
    }

    // =========================================================
    // TABLE CELLS
    // =========================================================

    private TableCell<Employee, Employee> availabilityCell() {

        return new TableCell<>() {

            {
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Employee employee, boolean empty) {

                super.updateItem(employee, empty);

                setText(null);

                if (empty || employee == null) {

                    setGraphic(null);

                    return;
                }

                String schedule =
                        employee.getAvailabilitySchedule();

                if (schedule == null || schedule.isBlank()) {

                    schedule =
                            legacyAvailabilitySchedule(
                                    employee.getWorkingDays(),
                                    employee.getWorkStartTime(),
                                    employee.getWorkEndTime()
                            );
                }

                List<ScheduleValidator.Slot> slots =
                        ScheduleValidator.parse(schedule);

                if (slots.isEmpty()) {

                    setGraphic(new Label("—"));

                    return;
                }

                FlowPane badges =
                        new FlowPane(5, 4);

                badges.setAlignment(Pos.CENTER);

                int visible =
                        Math.min(2, slots.size());

                for (int i = 0; i < visible; i++) {

                    ScheduleValidator.Slot slot =
                            slots.get(i);

                    String label =
                            shortDay(slot.day())
                                    + " "
                                    + formatTime(slot.startMinutes())
                                    + "–"
                                    + formatTime(slot.endMinutes());

                    badges.getChildren().add(
                            TableStyleKit.pill(label, "#FCE7F3", "#9D174D")
                    );
                }

                if (slots.size() > visible) {

                    badges.getChildren().add(
                            TableStyleKit.pill(
                                    "+" + (slots.size() - visible),
                                    "#F3E8FF",
                                    "#6B21A8"
                            )
                    );
                }

                setGraphic(badges);
            }
        };
    }

    private TableCell<Employee, String> rolePillCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setGraphic(null);
                } else {
                    setText(null);
                    setGraphic(TableStyleKit.pill(item, "#EEF2FF", "#4338CA"));
                }
            }
        };
    }

    private TableCell<Employee, Employee> actionCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Employee item, boolean empty) {
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

    // =========================================================
    // INITIALIZE TABLE
    // =========================================================

    private void initializeTeacherTable() {

        TableColumn<Employee, String> number =
                new TableColumn<>("N°");

        number.setCellValueFactory(
                d -> new ReadOnlyStringWrapper(
                        safe(d.getValue().getEmployeeNumber())
                )
        );

        number.setPrefWidth(80);

        TableColumn<Employee, String> name =
                new TableColumn<>("NOM");

        name.setCellValueFactory(
                d -> new ReadOnlyStringWrapper(
                        safe(d.getValue().getFirstName())
                                + " "
                                + safe(d.getValue().getLastName())
                )
        );

        name.setPrefWidth(160);

        TableColumn<Employee, String> role =
                new TableColumn<>("RÔLE");

        role.setCellValueFactory(
                d -> new ReadOnlyStringWrapper(
                        d.getValue().getRole() == null
                                ? ""
                                : d.getValue().getRole().name()
                )
        );

        role.setCellFactory(col -> rolePillCell());

        role.setPrefWidth(120);

        TableColumn<Employee, String> email =
                new TableColumn<>("EMAIL");

        email.setCellValueFactory(
                d -> new ReadOnlyStringWrapper(
                        safe(d.getValue().getEmail())
                )
        );

        email.setPrefWidth(180);

        TableColumn<Employee, Employee> availability =
                new TableColumn<>("DISPONIBILITÉ");

        availability.setCellValueFactory(
                d -> new ReadOnlyObjectWrapper<>(d.getValue())
        );

        availability.setCellFactory(column -> availabilityCell());

        availability.setPrefWidth(230);

        TableColumn<Employee, Employee> actions =
                new TableColumn<>("ACTION");

        actions.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        actions.setCellFactory(col -> actionCell());
        actions.setPrefWidth(110);
        actions.setMaxWidth(120);

        table.getColumns().setAll(
                number, name, role, email, availability, actions
        );
    }

    // =========================================================
    // SELECT EMPLOYEE
    // =========================================================

    private void selectRow(Employee employee) {

        selected = employee;

        if (employee == null) {
            return;
        }

        firstNameField.setText(safe(employee.getFirstName()));
        lastNameField.setText(safe(employee.getLastName()));
        emailField.setText(safe(employee.getEmail()));
        phoneField.setText(safe(employee.getPhoneNumber()));
        roleField.setValue(employee.getRole());
        certificationsField.setText(safe(employee.getCertifications()));

        currentWorkingDays = safe(employee.getWorkingDays());
        currentWorkStart = safe(employee.getWorkStartTime());
        currentWorkEnd = safe(employee.getWorkEndTime());

        currentAvailabilitySchedule =
                employee.getAvailabilitySchedule() == null
                        ? legacyAvailabilitySchedule(
                        currentWorkingDays,
                        currentWorkStart,
                        currentWorkEnd
                )
                        : employee.getAvailabilitySchedule();

        updateAvailabilitySummary();

        showFormPanel();
    }

    // =========================================================
    // CLEAR FORM
    // =========================================================

    private void clearForm() {

        selected = null;

        firstNameField.clear();
        lastNameField.clear();
        emailField.clear();
        phoneField.clear();

        roleField.setValue(null);

        certificationsField.clear();

        currentWorkingDays = "";
        currentWorkStart = "";
        currentWorkEnd = "";
        currentAvailabilitySchedule = "";

        updateAvailabilitySummary();

        table.getSelectionModel().clearSelection();
    }

    // =========================================================
    // SAVE
    // =========================================================

    private void save() {

        if (firstNameField.getText().isBlank()
                || lastNameField.getText().isBlank()
                || emailField.getText().isBlank()) {

            DialogUtil.error(
                    "Champs requis",
                    "Le prénom, le nom et l'email sont obligatoires."
            );

            return;
        }

        String phone =
                phoneField.getText().trim();

        if (!phone.matches("^(05|06|07)\\d{8}$")) {

            DialogUtil.error(
                    "Téléphone invalide",
                    "Le numéro doit contenir 10 chiffres et commencer par 05, 06 ou 07."
            );

            return;
        }

        Employee employee =
                selected != null ? selected : new Employee();

        employee.setFirstName(firstNameField.getText().trim());
        employee.setLastName(lastNameField.getText().trim());
        employee.setEmail(emailField.getText().trim());
        employee.setPhoneNumber(phone);
        employee.setRole(EmployeeRole.TEACHER);
        employee.setCertifications(certificationsField.getText().trim());

        String normalizedSchedule =
                ScheduleValidator.parse(currentAvailabilitySchedule)
                        .stream()
                        .map(TeachersView::slotToScheduleString)
                        .distinct()
                        .collect(Collectors.joining("; "));

        employee.setAvailabilitySchedule(
                normalizedSchedule.isBlank() ? null : normalizedSchedule
        );

        employee.setWorkingDays(null);
        employee.setWorkStartTime(null);
        employee.setWorkEndTime(null);

        employee.replaceAvailabilitySlots(
                toAvailabilitySlots(normalizedSchedule)
        );

        AsyncTasks.run(

                () -> employeeService.save(employee),

                saved -> { clearForm(); closeForm(); reload(); },

                err -> DialogUtil.error(
                        "Erreur",
                        "Échec de l'enregistrement : " + err.getMessage()
                )
        );
    }

    // =========================================================
    // DELETE
    // =========================================================

    private void delete() {

        if (selected == null) {
            return;
        }

        if (!DialogUtil.confirm("Confirmer", "Supprimer cet employé ?")) {
            return;
        }

        String id = selected.getId();

        AsyncTasks.run(

                () -> employeeService.delete(id),

                () -> { clearForm(); closeForm(); reload(); },

                err -> DialogUtil.error(
                        "Erreur",
                        "Échec de la suppression : " + err.getMessage()
                )
        );
    }

    // =========================================================
    // RELOAD / FILTER
    // =========================================================

    private void reload() {

        AsyncTasks.run(

                () -> employeeService.search(""),

                list -> {
                    allEmployees = list;
                    applyFilters();
                },

                err -> DialogUtil.error(
                        "Erreur",
                        "Échec du chargement : " + err.getMessage()
                )
        );
    }

    private void applyFilters() {
        String needle = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String roleVal = roleFilter.getValue();

        List<Employee> filtered = allEmployees.stream()
                .filter(emp -> {
                    if (!needle.isBlank()) {
                        String first = safe(emp.getFirstName()).toLowerCase();
                        String last = safe(emp.getLastName()).toLowerCase();
                        String email = safe(emp.getEmail()).toLowerCase();
                        if (!first.contains(needle) && !last.contains(needle) && !email.contains(needle)) {
                            return false;
                        }
                    }
                    if (roleVal != null && !"Tous".equals(roleVal)) {
                        String role = emp.getRole() == null ? "" : emp.getRole().name();
                        if (!role.equals(roleVal)) return false;
                    }
                    return true;
                })
                .toList();

        rows.setAll(filtered);
        updateFooter(filtered);
        updateSummaryCards(allEmployees);
    }

    private void updateFooter(List<Employee> data) {
        footerCountLabel.setText(data.size() + (data.size() > 1 ? " employés" : " employé"));
    }

    private void updateSummaryCards(List<Employee> data) {
        summaryCards.getChildren().clear();

        long withAvailability = data.stream()
                .filter(e -> {
                    String sched = e.getAvailabilitySchedule();
                    if (sched == null || sched.isBlank()) {
                        sched = legacyAvailabilitySchedule(e.getWorkingDays(), e.getWorkStartTime(), e.getWorkEndTime());
                    }
                    return !ScheduleValidator.parse(sched).isEmpty();
                })
                .count();

        long roleCount = data.stream().map(e -> e.getRole() == null ? "—" : e.getRole().name()).distinct().count();

        summaryCards.getChildren().addAll(
                summaryCard("fth-users", String.valueOf(data.size()), "Total Employés", "#4338CA", "#EEF2FF"),
                summaryCard("fth-calendar", String.valueOf(withAvailability), "Avec Disponibilité", "#9D174D", "#FCE7F3"),
                summaryCard("fth-briefcase", String.valueOf(roleCount), "Rôles Distincts", "#0E7490", "#CFFAFE")
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

    // =========================================================
    // TIMETABLE
    // =========================================================

    private void showTeacherTimetable() {

        if (selected == null) {

            DialogUtil.info(
                    "Emploi du temps",
                    "Sélectionnez d'abord un enseignant."
            );

            return;
        }

        Employee teacher = selected;

        Stage dialog = new Stage();

        dialog.initModality(Modality.APPLICATION_MODAL);

        if (timetableButton.getScene() != null) {

            dialog.initOwner(timetableButton.getScene().getWindow());
        }

        dialog.setTitle(
                "Emploi du temps — "
                        + safe(teacher.getFirstName())
                        + " "
                        + safe(teacher.getLastName())
        );

        dialog.setWidth(900);
        dialog.setHeight(600);

        dialog.setMinWidth(800);
        dialog.setMinHeight(500);

        VBox root = new VBox(10);

        root.setPadding(new Insets(12));

        root.setStyle("-fx-background-color: #F8FAFC;");

        Label title =
                new Label(
                        "Emploi du temps : "
                                + safe(teacher.getFirstName())
                                + " "
                                + safe(teacher.getLastName())
                );

        title.setStyle(
                "-fx-font-size: 17px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #172554;"
        );

        Label subtitle =
                new Label("Planning hebdomadaire");

        subtitle.setStyle(
                "-fx-font-size: 11px;" +
                        "-fx-text-fill: #64748B;"
        );

        VBox header = new VBox(2, title, subtitle);

        BorderPane timetableContainer = new BorderPane();

        Label loading = new Label("Chargement...");

        loading.setStyle("-fx-text-fill: #64748B;");

        timetableContainer.setCenter(loading);

        Button printButton = new Button("🖨 Imprimer");

        printButton.getStyleClass().add("primary-button");

        printButton.setDisable(true);

        Button closeButton = new Button("Fermer");

        closeButton.getStyleClass().add("secondary-button");

        closeButton.setOnAction(e -> dialog.close());

        HBox buttons = new HBox(8, printButton, closeButton);

        buttons.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(header, timetableContainer, buttons);

        VBox.setVgrow(timetableContainer, Priority.ALWAYS);

        AsyncTasks.run(

                courseService::findAll,

                allCourses -> {

                    GridPane grid =
                            buildTeacherTimetableGrid(teacher, allCourses);

                    ScrollPane scroll = new ScrollPane(grid);

                    scroll.setFitToWidth(true);
                    scroll.setFitToHeight(true);
                    scroll.setPannable(true);

                    scroll.setStyle(
                            "-fx-background-color: transparent;" +
                                    "-fx-background: transparent;"
                    );

                    timetableContainer.setCenter(scroll);

                    printButton.setDisable(false);

                    printButton.setOnAction(
                            e -> printTeacherTimetable(teacher, grid)
                    );
                },

                err -> {

                    Label error =
                            new Label("Erreur : " + safe(err.getMessage()));

                    error.setWrapText(true);

                    error.setStyle("-fx-text-fill: #DC2626;");

                    timetableContainer.setCenter(error);
                }
        );

        Scene scene = new Scene(root, 900, 600);

        dialog.setScene(scene);

        dialog.showAndWait();
    }

    // =========================================================
    // BUILD TIMETABLE GRID
    // =========================================================

    private GridPane buildTeacherTimetableGrid(
            Employee teacher,
            List<Course> allCourses
    ) {

        GridPane grid = new GridPane();

        grid.setHgap(0);
        grid.setVgap(0);

        grid.setPadding(new Insets(6));

        grid.setStyle("-fx-background-color: #CBD5E1;");

        ColumnConstraints timeColumn = new ColumnConstraints();

        timeColumn.setPrefWidth(65);
        timeColumn.setMinWidth(65);
        timeColumn.setMaxWidth(65);

        grid.getColumnConstraints().add(timeColumn);

        for (int i = 0; i < 7; i++) {

            ColumnConstraints dayColumn = new ColumnConstraints();

            dayColumn.setPrefWidth(115);
            dayColumn.setMinWidth(95);

            dayColumn.setHgrow(Priority.ALWAYS);

            grid.getColumnConstraints().add(dayColumn);
        }

        addTimetableCell(grid, 0, 0, "Heure", "#E2E8F0", "#1E293B", true);

        for (int i = 0; i < DaysPicker.DAYS.size(); i++) {

            String day = DaysPicker.DAYS.get(i);

            addTimetableCell(
                    grid, i + 1, 0, day.toUpperCase(), "#DBEAFE", "#1E3A8A", true
            );
        }

        int firstHour = 8;
        int lastHour = 18;

        int row = 1;

        for (int hour = firstHour; hour < lastHour; hour++) {

            if (hour == 12) {

                for (int col = 0; col <= 7; col++) {

                    addTimetableCell(
                            grid,
                            col,
                            row,
                            col == 0 ? "12:00" : "PAUSE",
                            "#DBEAFE",
                            "#1E40AF",
                            true
                    );
                }

                row++;

                continue;
            }

            String time = String.format("%02d:00", hour);

            addTimetableCell(grid, 0, row, time, "#F1F5F9", "#334155", true);

            for (int dayIndex = 0; dayIndex < 7; dayIndex++) {

                String day = DaysPicker.DAYS.get(dayIndex);

                Course course =
                        findCourseAt(teacher, allCourses, day, hour * 60);

                if (course != null) {

                    String classroom = "";

                    if (course.getClassroom() != null) {

                        classroom = safe(course.getClassroom().getName());
                    }

                    String text = safe(course.getName());

                    if (!classroom.isBlank()) {

                        text += "\n📍 " + classroom;
                    }

                    addTimetableCell(
                            grid, dayIndex + 1, row, text, "#BFDBFE", "#1E3A8A", false
                    );

                } else {

                    addTimetableCell(
                            grid, dayIndex + 1, row, "", "#FFFFFF", "#334155", false
                    );
                }
            }

            row++;
        }

        return grid;
    }

    // =========================================================
    // FIND COURSE
    // =========================================================

    private Course findCourseAt(
            Employee teacher,
            List<Course> courses,
            String day,
            int minute
    ) {

        for (Course course : courses) {

            if (course.getTeacher() == null) {
                continue;
            }

            if (!teacher.getId().equals(course.getTeacher().getId())) {
                continue;
            }

            String schedule = course.getSchedule();

            if (schedule == null || schedule.isBlank()) {
                continue;
            }

            for (ScheduleValidator.Slot slot : ScheduleValidator.parse(schedule)) {

                if (!slot.day().equals(day)) {
                    continue;
                }

                if (minute >= slot.startMinutes() && minute < slot.endMinutes()) {

                    return course;
                }
            }
        }

        return null;
    }

    // =========================================================
    // TIMETABLE CELL
    // =========================================================

    private void addTimetableCell(
            GridPane grid,
            int column,
            int row,
            String text,
            String background,
            String textColor,
            boolean bold
    ) {

        Label label = new Label(text);

        label.setAlignment(Pos.CENTER);

        label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        label.setMinHeight(42);

        label.setWrapText(true);

        String weight = bold ? "bold" : "normal";

        label.setStyle(
                "-fx-background-color: " + background + ";"
                        + "-fx-text-fill: " + textColor + ";"
                        + "-fx-font-size: " + (bold ? "10px" : "9px") + ";"
                        + "-fx-font-weight: " + weight + ";"
                        + "-fx-alignment: center;"
                        + "-fx-padding: 5px;"
                        + "-fx-border-color: #CBD5E1;"
                        + "-fx-border-width: 0.5px;"
        );

        GridPane.setHgrow(label, Priority.ALWAYS);
        GridPane.setVgrow(label, Priority.ALWAYS);

        grid.add(label, column, row);
    }

    // =========================================================
    // PRINT TIMETABLE
    // =========================================================

    private void printTeacherTimetable(Employee teacher, GridPane timetable) {

        PrinterJob job = PrinterJob.createPrinterJob();

        if (job == null) {

            DialogUtil.error("Impression", "Aucune imprimante disponible.");

            return;
        }

        boolean accepted = job.showPrintDialog(
                timetable.getScene() == null
                        ? null
                        : timetable.getScene().getWindow()
        );

        if (!accepted) {
            return;
        }

        javafx.print.Printer printer = job.getPrinter();

        PageLayout pageLayout = printer.createPageLayout(
                Paper.A4,
                PageOrientation.LANDSCAPE,
                javafx.print.Printer.MarginType.DEFAULT
        );

        timetable.applyCss();
        timetable.layout();

        double width = timetable.getLayoutBounds().getWidth();
        double height = timetable.getLayoutBounds().getHeight();

        if (width <= 0 || height <= 0) {

            DialogUtil.error(
                    "Impression",
                    "Impossible de déterminer la taille de l'emploi du temps."
            );

            job.endJob();

            return;
        }

        double printableWidth = pageLayout.getPrintableWidth();
        double printableHeight = pageLayout.getPrintableHeight();

        double scaleX = printableWidth / width;
        double scaleY = printableHeight / height;

        double scale = Math.min(scaleX, scaleY);

        scale = Math.min(scale, 1.0);

        double oldScaleX = timetable.getScaleX();
        double oldScaleY = timetable.getScaleY();

        try {

            timetable.setScaleX(scale);
            timetable.setScaleY(scale);

            boolean success = job.printPage(pageLayout, timetable);

            if (success) {

                job.endJob();

                DialogUtil.info(
                        "Impression",
                        "L'emploi du temps a été envoyé à l'imprimante."
                );

            } else {

                DialogUtil.error("Impression", "L'impression a échoué.");
            }

        } finally {

            timetable.setScaleX(oldScaleX);
            timetable.setScaleY(oldScaleY);
        }
    }

    // =========================================================
    // LEGACY AVAILABILITY
    // =========================================================

    private static String legacyAvailabilitySchedule(
            String days,
            String start,
            String end
    ) {

        if (days == null || days.isBlank()
                || start == null || start.isBlank()
                || end == null || end.isBlank()) {

            return "";
        }

        return java.util.Arrays.stream(days.split(","))
                .map(String::trim)
                .filter(day -> !day.isEmpty())
                .map(day -> day + " " + start + "-" + end)
                .collect(Collectors.joining("; "));
    }

    // =========================================================
    // AVAILABILITY SLOTS
    // =========================================================

    private static List<TeacherAvailabilitySlot> toAvailabilitySlots(String schedule) {

        return ScheduleValidator.parse(schedule)
                .stream()
                .map(slot -> new TeacherAvailabilitySlot(
                        slot.day(),
                        formatTime(slot.startMinutes()),
                        formatTime(slot.endMinutes())
                ))
                .toList();
    }

    // =========================================================
    // SLOT TO STRING
    // =========================================================

    private static String slotToScheduleString(ScheduleValidator.Slot slot) {

        return slot.day()
                + " "
                + formatTime(slot.startMinutes())
                + "-"
                + formatTime(slot.endMinutes());
    }

    // =========================================================
    // FORMAT TIME
    // =========================================================

    private static String formatTime(int minutes) {

        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }

    // =========================================================
    // SHORT DAY
    // =========================================================

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

    // =========================================================
    // SAFE
    // =========================================================

    private static String safe(String value) {

        return value == null ? "" : value;
    }
}