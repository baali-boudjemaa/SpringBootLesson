package com.example.mef.demo.dashboard.classrooms;

import com.example.mef.demo.Model.Classroom;
import com.example.mef.demo.Model.Course;
import com.example.mef.demo.Model.CourseScheduleSlot;
import com.example.mef.demo.Model.Employee;
import com.example.mef.demo.Model.Room;
import com.example.mef.demo.Services.ClassroomService;
import com.example.mef.demo.Services.ClassroomService.ClassAttendanceReport;
import com.example.mef.demo.Services.ClassroomService.ClassStudentAttendance;
import com.example.mef.demo.Services.ClassroomService.RoomConflict;
import com.example.mef.demo.Services.CourseService;
import com.example.mef.demo.Services.RoomService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.enums.Category;
import com.example.mef.demo.util.DialogUtil;
import com.example.mef.demo.util.I18n;
import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * The classrooms screen: a card grid of sections with a create/edit form,
 * plus the "students in this class" dialog and report.
 */
@Component
public class ClassroomsView {

    @Autowired
    private ClassroomService classroomService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private CourseService courseService;

    private final List<Classroom> allClassrooms = new ArrayList<>();

    /** Stored so card-level actions (e.g. "Voir les élèves") can open dialogs owned by the main window. */
    private BorderPane rootContentPane;

    /** One hour-row of the weekly timetable dialog: a time slot, or the lunch-break band. */
    private record TimetableRow(int startMinutes, int endMinutes, String startLabel, String endLabel, boolean isBreak) {}

    /** One course period placed on the timetable: day + time range + course/teacher labels. */
    private record CourseSlotEntry(int dayIndex, int startMinutes, int endMinutes, String courseName, String teacherName) {}

    /** Days shown as timetable columns, in display order. */
    private static final List<String> TIMETABLE_DAYS = List.of(
            "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche");

    /** Fixed hourly rows 8h–18h with a lunch-break band 12h–14h, matching the standard school timetable layout. */
    private static final List<TimetableRow> TIMETABLE_ROWS = List.of(
            new TimetableRow(8 * 60, 9 * 60, "8h", "9h", false),
            new TimetableRow(9 * 60, 10 * 60, "9h", "10h", false),
            new TimetableRow(10 * 60, 11 * 60, "10h", "11h", false),
            new TimetableRow(11 * 60, 12 * 60, "11h", "12h", false),
            new TimetableRow(12 * 60, 14 * 60, "", "", true),
            new TimetableRow(14 * 60, 15 * 60, "14h", "15h", false),
            new TimetableRow(15 * 60, 16 * 60, "15h", "16h", false),
            new TimetableRow(16 * 60, 17 * 60, "16h", "17h", false),
            new TimetableRow(17 * 60, 18 * 60, "17h", "18h", false));

    public void render(BorderPane contentPane) {
        this.rootContentPane = contentPane;

        FlowPane cardGrid = new FlowPane(16, 16);
        cardGrid.setPadding(new Insets(4));

        TextField searchField = FormFactory.textField(I18n.t("classroom.search"));
        searchField.getStyleClass().add("filter-field");
        Label countLabel = new Label();
        countLabel.getStyleClass().add("stat-caption");

        TextField nameField = FormFactory.textField(I18n.t("classroom.name"));
        TextField ageGroupField = FormFactory.textField(I18n.t("classroom.age_group"));
        TextField capacityField = FormFactory.textField(I18n.t("classroom.max_capacity"));
        ComboBox<Category> categoryField = new ComboBox<>(FXCollections.observableArrayList(Category.values()));
        categoryField.setMaxWidth(Double.MAX_VALUE);
        categoryField.setValue(Category.CRECHE);
        categoryField.setCellFactory(cb -> categoryCell());
        categoryField.setButtonCell(categoryCell());

        // Room picker: horizontal chip row that scrolls sideways (many-to-many).
        HBox roomsBox = new HBox(8);
        roomsBox.setAlignment(Pos.CENTER_LEFT);
        roomsBox.setPadding(new Insets(4, 2, 4, 2));
        ScrollPane roomsScroll = new ScrollPane(roomsBox);
        roomsScroll.setFitToHeight(true);
        roomsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        roomsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        roomsScroll.setPrefHeight(52);
        roomsScroll.setMinHeight(52);
        roomsScroll.setMinWidth(200);
        roomsScroll.getStyleClass().add("rooms-scroll");

        roomsScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        List<CheckBox> roomChecks = new ArrayList<>();

        GridPane form = FormFactory.sectionGrid();
        FormFactory.addRow(form, 0, I18n.t("field.name"), nameField);
        FormFactory.addRow(form, 1, I18n.t("classroom.age_group"), ageGroupField);
        FormFactory.addRow(form, 2, I18n.t("field.capacity"), capacityField);
        FormFactory.addRow(form, 3, I18n.t("classroom.category"), categoryField);

        // A section's room-use times come from its course schedules.  They are
        // displayed through «Voir l'emploi du temps», not entered manually here.
        FormFactory.addRow(form, 5, I18n.t("classroom.rooms"));
        FormFactory.addRow(form, 6, roomsScroll);

        Button save   = new Button(I18n.t("action.save"));   save.getStyleClass().add("primary-button");
        Button cancel = new Button(I18n.t("action.clear"));  cancel.getStyleClass().add("secondary-button");
        Button delete = new Button(I18n.t("action.delete")); delete.getStyleClass().add("danger-button");
        HBox actions = new HBox(10, save, cancel, delete);

        Classroom[] selected = new Classroom[]{null};
        Runnable[] reload = new Runnable[1];
        Runnable[] applyFilter = new Runnable[1];
        VBox formPanelcor = new VBox(14, form, actions);

        ScrollPane formScroll = new ScrollPane(formPanelcor);
        formScroll.getStyleClass().add("details-scroll");
        formScroll.setFitToWidth(true);
        formScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        formScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        formScroll.setPrefWidth(380);
        formScroll.setMinWidth(320);
        formScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        Label detailsTitle = new Label(I18n.t("table.details"));
        detailsTitle.getStyleClass().add("side-panel-title");
        detailsTitle.setMaxWidth(Double.MAX_VALUE);
        detailsTitle.setAlignment(Pos.CENTER);

        VBox formPanel = new VBox(14, detailsTitle, formScroll);
        formPanel.getStyleClass().add("class-side-panel");
        formPanel.setPadding(new Insets(20, 10, 10, 10));

        // Details panel scrolls vertically when the form is taller than the window.
        BorderPane layout = new BorderPane();

        Runnable showFormPanel = () -> {
            layout.setRight(formPanel);
            BorderPane.setMargin(formPanel, new Insets(20, 24, 0, 16));
        };
        Runnable closeForm = () -> layout.setRight(null);

        // Loads every room as a chip; re-run whenever the form is opened fresh
        // so rooms created in the Rooms module show up here too.
        Runnable loadRooms = () -> AsyncTasks.run(
                () -> roomService.findAll(),
                rooms -> {
                    roomsBox.getChildren().clear();
                    roomChecks.clear();
                    if (rooms.isEmpty()) {
                        Label none = new Label(I18n.t("classroom.rooms_none"));
                        none.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
                        roomsBox.getChildren().add(none);
                        return;
                    }
                    for (Room r : rooms) {
                        CheckBox cb = new CheckBox(r.getName());
                        cb.setUserData(r);
                        cb.getStyleClass().add("room-chip");
                        cb.setMinWidth(Region.USE_PREF_SIZE);
                        roomChecks.add(cb);
                        roomsBox.getChildren().add(cb);
                    }
                },
                err -> DialogUtil.error(I18n.t("classroom.rooms"), err.getMessage())
        );

        Runnable clearForm = () -> {
            selected[0] = null;
            nameField.setText("");
            ageGroupField.setText("");
            capacityField.setText("");
            categoryField.setValue(Category.CRECHE);
            roomChecks.forEach(cb -> cb.setSelected(false));
            cardGrid.getChildren().forEach(n -> n.getStyleClass().remove("class-card-selected"));
        };

        applyFilter[0] = () -> {
            String needle = searchField.getText();
            List<Classroom> filtered = (needle == null || needle.isBlank())
                    ? allClassrooms
                    : allClassrooms.stream()
                    .filter(c -> c.getName() != null && c.getName().toLowerCase().contains(needle.trim().toLowerCase()))
                    .toList();

            cardGrid.getChildren().clear();
            for (Classroom c : filtered) {
                VBox card = buildClassroomCard(c);
                card.setOnMouseClicked(ev -> {
                    if (ev.getClickCount() == 2) {
                        showClassStudentsDialog(contentPane, c);
                        return;
                    }
                    selected[0] = c;
                    cardGrid.getChildren().forEach(n -> n.getStyleClass().remove("class-card-selected"));
                    card.getStyleClass().add("class-card-selected");
                    nameField.setText(c.getName());
                    ageGroupField.setText(c.getAgeGroup() == null ? "" : c.getAgeGroup());
                    capacityField.setText(String.valueOf(c.getCapacity()));
                    categoryField.setValue(c.getCategory() == null ? Category.CRECHE : c.getCategory());
                    List<String> linkedRoomIds = c.getRooms() == null ? List.of()
                            : c.getRooms().stream().map(Room::getId).toList();
                    roomChecks.forEach(cb -> cb.setSelected(
                            linkedRoomIds.contains(((Room) cb.getUserData()).getId())));
                    showFormPanel.run();
                });
                cardGrid.getChildren().add(card);
            }
            countLabel.setText(filtered.size() + " " + I18n.t(filtered.size() > 1 ? "classroom.section_plural" : "classroom.section_singular"));
        };

        reload[0] = () -> AsyncTasks.run(
                () -> classroomService.findAll(),
                classrooms -> {
                    allClassrooms.clear();
                    allClassrooms.addAll(classrooms);
                    applyFilter[0].run();
                },
                err -> DialogUtil.error("Chargement échoué", err.getMessage())
        );

        searchField.textProperty().addListener((obs, old, val) -> applyFilter[0].run());

        cancel.setOnAction(e -> { clearForm.run(); closeForm.run(); });

        save.setOnAction(e -> {
            try {
                if (nameField.getText().isBlank()) throw new IllegalArgumentException(I18n.t("classroom.name_required"));
                int capacity;
                try {
                    capacity = Integer.parseInt(capacityField.getText().trim());
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(I18n.t("classroom.capacity_number"));
                }

                Classroom c = selected[0] != null ? selected[0] : Classroom.builder().build();
                c.setName(nameField.getText().trim());
                c.setAgeGroup(ageGroupField.getText().trim());
                c.setCapacity(capacity);
                c.setCategory(categoryField.getValue() == null ? Category.CRECHE : categoryField.getValue());
                c.setRooms(roomChecks.stream()
                        .filter(CheckBox::isSelected)
                        .map(cb -> (Room) cb.getUserData())
                        .collect(Collectors.toList()));

                Runnable doSave = () -> {
                    save.setDisable(true);
                    AsyncTasks.run(
                            () -> classroomService.save(c),
                            () -> { save.setDisable(false); reload[0].run(); clearForm.run(); closeForm.run(); },
                            err -> { save.setDisable(false); DialogUtil.error(I18n.t("action.save"), err.getMessage()); }
                    );
                };

                save.setDisable(true);
                AsyncTasks.run(
                        () -> classroomService.findRoomConflicts(c),
                        (List<RoomConflict> conflicts) -> {
                            save.setDisable(false);
                            if (conflicts.isEmpty()) {
                                doSave.run();
                                return;
                            }
                            String details = conflicts.stream()
                                    .map(rc -> rc.roomName() + " — " + rc.otherClassroomName()
                                            + " (" + rc.day() + " " + rc.timeRange() + ")")
                                    .collect(Collectors.joining("\n"));
                            if (DialogUtil.confirm(I18n.t("classroom.rooms_conflict_title"),
                                    I18n.t("classroom.rooms_conflict_confirm") + "\n\n" + details)) {
                                doSave.run();
                            }
                        },
                        err -> { save.setDisable(false); DialogUtil.error(I18n.t("action.save"), err.getMessage()); }
                );
            } catch (RuntimeException ex) {
                DialogUtil.error(I18n.t("action.save"), ex.getMessage());
            }
        });

        delete.setOnAction(e -> {
            if (selected[0] == null) {
                DialogUtil.info(I18n.t("action.delete"), I18n.t("classroom.select_before_delete"));
                return;
            }
            if (DialogUtil.confirm(I18n.t("action.delete"), I18n.t("classroom.delete_confirm"))) {
                String id = selected[0].getId();
                delete.setDisable(true);
                AsyncTasks.run(
                        () -> classroomService.delete(id),
                        () -> { delete.setDisable(false); reload[0].run(); clearForm.run(); closeForm.run(); },
                        err -> { delete.setDisable(false); DialogUtil.error(I18n.t("action.delete"), err.getMessage()); }
                );
            }
        });

        Button addNew = new Button("+  " + I18n.t("classroom.new_section"));
        addNew.getStyleClass().add("primary-button");
        addNew.setOnAction(e -> {
            clearForm.run();
            showFormPanel.run();
            nameField.requestFocus();
        });

        loadRooms.run();
        reload[0].run();

        Label title = new Label(I18n.t("classroom.title"));
        title.getStyleClass().add("page-title");
        HBox headerRow = new HBox(12, title);
        HBox.setHgrow(title, Priority.ALWAYS);
        headerRow.getChildren().add(addNew);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        VBox headerBlock = new VBox(4, headerRow, countLabel);

        ScrollPane cardScroll = new ScrollPane(cardGrid);
        cardScroll.setFitToWidth(true);
        cardScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        cardScroll.getStyleClass().add("details-scroll");
        VBox cardPanel = new VBox(14, headerBlock, searchField, cardScroll);
        VBox.setVgrow(cardScroll, Priority.ALWAYS);
        cardPanel.setPadding(new Insets(24));

        layout.setCenter(cardPanel);

        contentPane.setCenter(layout);
        contentPane.setPadding(new Insets(0));
    }

    private ListCell<Category> categoryCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : categoryLabel(item));
            }
        };
    }

    private String categoryLabel(Category category) {
        return switch (category) {
            case CRECHE -> I18n.t("category.creche");
            case PREPARATOIRE -> I18n.t("category.preparatoire");
            case SOUTIEN -> I18n.t("category.soutien");
        };
    }

    private String attendanceStatusLabel(ClassStudentAttendance row) {
        if (row.status() == null) return I18n.t("attendance.unmarked");
        return switch (row.status()) {
            case PRESENT -> I18n.t("status.present");
            case ABSENT -> I18n.t("status.absent");
            case LATE -> I18n.t("status.late");
            case EXCUSED -> I18n.t("attendance.excused");
        };
    }

    private VBox buildClassroomCard(Classroom c) {
        Label name = new Label(c.getName());
        name.getStyleClass().add("section-title");

        String ageGroupText = c.getAgeGroup() == null ? "" : c.getAgeGroup();
        String categoryText = c.getCategory() == null ? "" : categoryLabel(c.getCategory());
        String metaText = ageGroupText.isBlank() ? categoryText
                : categoryText.isBlank() ? ageGroupText
                : ageGroupText + " · " + categoryText;
        Label meta = new Label(metaText);
        meta.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");

        int enrolled = classroomService.countStudentsInClassroom(c.getId());
        Label capacity = new Label(enrolled + "/" + c.getCapacity() + " " + I18n.t("classroom.places"));
        capacity.setStyle("-fx-font-size: 12px; -fx-text-fill: #15803D; -fx-font-weight: bold;");

        // Explicit action, in addition to the double-click-to-open behavior on the card itself.
        Button viewStudentsBtn = new Button("👁  " + I18n.t("classroom.view_students"));
        viewStudentsBtn.getStyleClass().add("link-button");
        viewStudentsBtn.setOnAction(e -> showClassStudentsDialog(rootContentPane, c));
        // Consume the click so it doesn't also bubble up to the card's own
        // setOnMouseClicked handler (which would select the card / open the edit panel).
        viewStudentsBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, javafx.event.Event::consume);

        // Same pattern, for the courses taught in this classroom.
        Button viewCoursesBtn = new Button("📚  " + I18n.t("classroom.view_courses"));
        viewCoursesBtn.getStyleClass().add("link-button");
        viewCoursesBtn.setOnAction(e -> showClassCoursesDialog(c));
        viewCoursesBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, javafx.event.Event::consume);

        // Same pattern, for the weekly time schedule of this classroom.
        Button viewScheduleBtn = new Button("🗓  " + I18n.t("classroom.view_schedule"));
        viewScheduleBtn.getStyleClass().add("link-button");
        viewScheduleBtn.setOnAction(e -> showClassScheduleDialog(c));
        viewScheduleBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, javafx.event.Event::consume);

        Label hint = new Label(I18n.t("classroom.double_click_hint"));
        hint.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");

        VBox card = new VBox(6, name, meta, capacity);
        if (c.getRooms() != null && !c.getRooms().isEmpty()) {
            String roomNames = c.getRooms().stream().map(Room::getName).collect(Collectors.joining(", "));
            Label rooms = new Label("🏠 " + roomNames);
            rooms.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
            card.getChildren().add(rooms);
        }
        card.getChildren().addAll(viewStudentsBtn, viewCoursesBtn, viewScheduleBtn, hint);
        card.getStyleClass().add("class-card");
        card.setPadding(new Insets(16));
        card.setPrefWidth(220);
        return card;
    }

    /** Modal dialog listing all students enrolled in this classroom. */
    private void showClassStudentsDialog(BorderPane contentPane, Classroom classroom) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(contentPane.getScene().getWindow());
        dialog.setTitle(I18n.t("classroom.students_title").replace("{0}", classroom.getName()));
        dialog.setMinWidth(460);
        dialog.setMinHeight(420);

        ListView<ClassStudentAttendance> listView = new ListView<>();
        listView.setPrefSize(420, 280);
        listView.setPlaceholder(new Label(I18n.t("classroom.students_empty")));
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(ClassStudentAttendance row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setText(null);
                    return;
                }

                setText(row.fullName() + "  (" + row.studentNumber() + ")  -  " + attendanceStatusLabel(row));
            }
        });

        Label loading = new Label("Chargement...");
        VBox root = new VBox(12, loading);
        root.setPadding(new Insets(20));
        root.setMinSize(420, 340);

        AsyncTasks.run(
                () -> classroomService.getClassAttendanceReport(classroom.getId(), LocalDate.now()),
                report -> {
                    root.getChildren().remove(loading);

                    listView.getItems().setAll(report.students());

                    TextArea reportArea = new TextArea();
                    reportArea.setEditable(false);
                    reportArea.setWrapText(false);
                    reportArea.getStyleClass().add("monthly-report-area");
                    reportArea.setPrefRowCount(10);
                    reportArea.setVisible(false);
                    reportArea.setManaged(false);

                    Button reportBtn = new Button("📋  " + I18n.t("classroom.generate_report"));
                    reportBtn.getStyleClass().add("primary-button");

                    Button copyBtn = new Button("📋  " + I18n.t("action.copy"));
                    copyBtn.getStyleClass().add("secondary-button");
                    copyBtn.setVisible(false);
                    copyBtn.setManaged(false);
                    copyBtn.setOnAction(ev -> {
                        javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
                        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                        content.putString(reportArea.getText());
                        cb.setContent(content);
                    });

                    reportBtn.setOnAction(ev -> {
                        listView.setVisible(false);
                        listView.setManaged(false);
                        reportArea.setVisible(true);
                        reportArea.setManaged(true);
                        copyBtn.setVisible(true);
                        copyBtn.setManaged(true);
                        reportArea.setText(buildClassReportText(classroom, report));
                    });

                    Button closeBtn = new Button(I18n.t("action.close"));
                    closeBtn.getStyleClass().add("secondary-button");
                    closeBtn.setOnAction(ev -> dialog.close());

                    HBox buttons = new HBox(10, reportBtn, copyBtn, closeBtn);
                    root.getChildren().addAll(listView, reportArea, buttons);
                },
                err -> {
                    root.getChildren().remove(loading);
                    root.getChildren().add(new Label("Erreur : " + err.getMessage()));
                }
        );

        dialog.setScene(new Scene(root, 460, 420));
        dialog.showAndWait();
    }

    /**
     * Modal dialog listing every {@link Course} taught in this classroom. Loads the full
     * course list and filters client-side by {@code course.getClassroom().getId()} — same
     * pattern used for the enrolled-students dialog in {@code CoursesView}.
     */
    private void showClassCoursesDialog(Classroom classroom) {
        Label title = new Label(I18n.t("classroom.courses_title").replace("{0}", classroom.getName()));
        title.getStyleClass().add("workflow-title");

        Label loading = new Label("Chargement...");
        VBox root = new VBox(14, title, loading);
        root.getStyleClass().add("workflow-card");
        root.setPadding(new Insets(20));
        root.setPrefWidth(420);
        root.setPrefHeight(300);
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        Window owner = rootContentPane == null || rootContentPane.getScene() == null
                ? null : rootContentPane.getScene().getWindow();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle(I18n.t("classroom.courses_dialog_title"));
        dialog.setScene(new Scene(root));

        AsyncTasks.run(
                () -> courseService.findAll(),
                allCourses -> {
                    List<Course> matching = allCourses.stream()
                            .filter(c -> c.getClassroom() != null
                                    && classroom.getId() != null
                                    && classroom.getId().equals(c.getClassroom().getId()))
                            .toList();

                    root.getChildren().remove(loading);

                    Label count = new Label(I18n.t("classroom.course_count").replace("{0}", String.valueOf(matching.size())));
                    count.getStyleClass().add("stat-caption");

                    VBox listBox = new VBox(8);
                    if (matching.isEmpty()) {
                        Label none = new Label(I18n.t("classroom.courses_empty"));
                        none.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px;");
                        listBox.getChildren().add(none);
                    } else {
                        for (Course course : matching) {
                            listBox.getChildren().add(courseRow(course));
                        }
                    }

                    ScrollPane scroll = new ScrollPane(listBox);
                    scroll.setFitToWidth(true);
                    scroll.setPrefViewportHeight(320);
                    scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

                    Button close = new Button(I18n.t("action.close"));
                    close.getStyleClass().add("secondary-button");
                    close.setOnAction(ev -> dialog.close());

                    root.getChildren().addAll(count, scroll, close);
                },
                err -> {
                    root.getChildren().remove(loading);
                    Label errLabel = new Label("Erreur : " + err.getMessage());
                    errLabel.setStyle("-fx-text-fill: #B91C1C;");
                    Button close = new Button(I18n.t("action.close"));
                    close.getStyleClass().add("secondary-button");
                    close.setOnAction(ev -> dialog.close());
                    root.getChildren().addAll(errLabel, close);
                }
        );

        dialog.showAndWait();
    }

    /**
     * Modal dialog showing this classroom's weekly timetable — days as columns, hourly
     * rows, a lunch-break band between 12h and 14h — built from every {@link Course}
     * taught in this classroom and their {@link CourseScheduleSlot}s, so each block shows
     * the course name and teacher.
     *
     * FIXED: the dialog was fixed at 900x640 while the grid's minimum width (hour column +
     * 7 day columns with a 96px floor each) could exceed that once padding, borders and the
     * vertical scrollbar were accounted for — the last ("Dimanche") column ended up squeezed
     * against the vertical scrollbar instead of being reachable through a clean horizontal
     * scroll. The dialog is now wider, the grid's column minimums are smaller so the whole
     * week reliably fits, and the ScrollPane fits the grid to the available width so no
     * horizontal scrollbar/column-clipping can happen at all.
     */
    private void showClassScheduleDialog(Classroom classroom) {
        Label title = new Label(I18n.t("classroom.schedule_heading").replace("{0}", classroom.getName()));
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label loading = new Label("Chargement...");
        VBox root = new VBox(16, title, loading);
        root.getStyleClass().add("workflow-card");
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: white;");
        VBox.setVgrow(root, Priority.ALWAYS);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);

        Window owner = rootContentPane == null || rootContentPane.getScene() == null
                ? null
                : rootContentPane.getScene().getWindow();

        if (owner != null) {
            dialog.initOwner(owner);
        }

        dialog.setTitle(I18n.t("classroom.schedule_title").replace("{0}", classroom.getName()));
        dialog.setMinWidth(820);
        dialog.setMinHeight(480);
        dialog.setResizable(true);
        dialog.setScene(new Scene(root, 1050, 640));

        AsyncTasks.run(
                () -> courseService.findAll(),
                allCourses -> {
                    List<Course> matching = allCourses.stream()
                            .filter(course -> course.getClassroom() != null
                                    && classroom.getId() != null
                                    && classroom.getId().equals(course.getClassroom().getId()))
                            .toList();

                    root.getChildren().remove(loading);

                    GridPane grid = buildTimetableGrid(matching);

                    ScrollPane scroll = new ScrollPane(grid);
                    scroll.setFitToWidth(false);
                    scroll.setFitToHeight(false);
                    scroll.setPannable(true);
                    scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                    scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                    scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

                    VBox.setVgrow(scroll, Priority.ALWAYS);

                    Button print = new Button("🖨  " + I18n.t("classroom.generate_pdf"));
                    print.getStyleClass().add("primary-button");
                    print.setOnAction(event -> {
                        print.setDisable(true);

                        AsyncTasks.run(
                                () -> {
                                    try {
                                        return exportScheduleToPdf(classroom, matching);
                                    } catch (IOException exception) {
                                        throw new RuntimeException(exception);
                                    }
                                },
                                file -> {
                                    print.setDisable(false);
                                    openFile(file);
                                },
                                error -> {
                                    print.setDisable(false);
                                    DialogUtil.error("Générer le PDF", error.getMessage());
                                }
                        );
                    });

                    Button close = new Button(I18n.t("action.close"));
                    close.getStyleClass().add("secondary-button");
                    close.setOnAction(event -> dialog.close());

                    HBox buttons = new HBox(10, print, close);
                    root.getChildren().addAll(scroll, buttons);
                },
                error -> {
                    root.getChildren().remove(loading);

                    Label errorLabel = new Label("Erreur : " + error.getMessage());
                    errorLabel.setStyle("-fx-text-fill: #B91C1C;");

                    Button close = new Button(I18n.t("action.close"));
                    close.getStyleClass().add("secondary-button");
                    close.setOnAction(event -> dialog.close());

                    root.getChildren().addAll(errorLabel, close);
                }
        );

        dialog.showAndWait();
    }

    private GridPane buildTimetableGrid(List<Course> courses) {
        List<CourseSlotEntry> entries = collectSlotEntries(courses);

        GridPane grid = new GridPane();
        grid.setStyle("-fx-background-color: white; "
                + "-fx-border-color: #CBD5E1; "
                + "-fx-border-width: 1;");

        double hourColumnWidth = 56;
        double dayColumnWidth = 150;
        double timetableWidth = hourColumnWidth + TIMETABLE_DAYS.size() * dayColumnWidth;

        // The grid keeps its width. The ScrollPane displays a horizontal scrollbar if needed.
        grid.setMinWidth(timetableWidth);
        grid.setPrefWidth(timetableWidth);
        grid.setMaxWidth(Region.USE_PREF_SIZE);

        ColumnConstraints hourCol = new ColumnConstraints();
        hourCol.setMinWidth(hourColumnWidth);
        hourCol.setPrefWidth(hourColumnWidth);
        hourCol.setMaxWidth(hourColumnWidth);
        grid.getColumnConstraints().add(hourCol);

        for (int i = 0; i < TIMETABLE_DAYS.size(); i++) {
            ColumnConstraints dayCol = new ColumnConstraints();
            dayCol.setMinWidth(dayColumnWidth);
            dayCol.setPrefWidth(dayColumnWidth);
            dayCol.setMaxWidth(dayColumnWidth);
            grid.getColumnConstraints().add(dayCol);
        }

        RowConstraints headerRow = new RowConstraints();
        headerRow.setPrefHeight(42);
        grid.getRowConstraints().add(headerRow);

        grid.add(new Region(), 0, 0);

        for (int d = 0; d < TIMETABLE_DAYS.size(); d++) {
            Label dayLabel = new Label(localizedDay(TIMETABLE_DAYS.get(d)).toUpperCase(I18n.getLocale()));
            dayLabel.setStyle("-fx-background-color: #CFE8E4; "
                    + "-fx-background-radius: 14; "
                    + "-fx-text-fill: #0F172A; "
                    + "-fx-font-weight: bold; "
                    + "-fx-font-size: 11px; "
                    + "-fx-padding: 6 4; "
                    + "-fx-alignment: center;");
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            dayLabel.setAlignment(Pos.CENTER);

            StackPane cell = new StackPane(dayLabel);
            cell.setPadding(new Insets(4));
            grid.add(cell, d + 1, 0);
        }

        for (int r = 0; r < TIMETABLE_ROWS.size(); r++) {
            TimetableRow row = TIMETABLE_ROWS.get(r);
            int gridRow = r + 1;

            RowConstraints rowConstraints = new RowConstraints();
            rowConstraints.setPrefHeight(row.isBreak() ? 28 : 70);
            grid.getRowConstraints().add(rowConstraints);

            if (row.isBreak()) {
                Region breakBand = new Region();
                breakBand.setStyle("-fx-background-color: #CFE8E4;");
                grid.add(breakBand, 0, gridRow, TIMETABLE_DAYS.size() + 1, 1);
                continue;
            }

            Label hourLabel = new Label(row.startLabel() + "\n–\n" + row.endLabel());
            hourLabel.setStyle("-fx-font-weight: bold; "
                    + "-fx-font-size: 11px; "
                    + "-fx-text-fill: #334155; "
                    + "-fx-text-alignment: center;");

            StackPane hourCell = new StackPane(hourLabel);
            hourCell.setStyle("-fx-border-color: #CBD5E1; -fx-border-width: 0 1 1 0;");
            grid.add(hourCell, 0, gridRow);
        }

        for (int d = 0; d < TIMETABLE_DAYS.size(); d++) {
            for (int r = 0; r < TIMETABLE_ROWS.size(); r++) {
                if (TIMETABLE_ROWS.get(r).isBreak()) {
                    continue;
                }

                Region empty = new Region();
                empty.setStyle("-fx-border-color: #E2E8F0; -fx-border-width: 0 1 1 0;");
                grid.add(empty, d + 1, r + 1);
            }
        }

        for (int d = 0; d < TIMETABLE_DAYS.size(); d++) {
            int dayIndex = d;

            List<CourseSlotEntry> daySlots = entries.stream()
                    .filter(entry -> entry.dayIndex() == dayIndex)
                    .toList();

            for (CourseSlotEntry entry : daySlots) {
                int firstRow = -1;
                int rowSpan = 0;

                for (int r = 0; r < TIMETABLE_ROWS.size(); r++) {
                    TimetableRow row = TIMETABLE_ROWS.get(r);

                    if (row.isBreak()) {
                        continue;
                    }

                    boolean overlaps = row.startMinutes() < entry.endMinutes()
                            && entry.startMinutes() < row.endMinutes();

                    if (overlaps) {
                        if (firstRow == -1) {
                            firstRow = r;
                        }
                        rowSpan++;
                    }
                }

                if (firstRow == -1) {
                    continue;
                }

                Label courseLabel = new Label(entry.courseName());
                courseLabel.setStyle("-fx-text-fill: #0F172A; "
                        + "-fx-font-size: 11px; "
                        + "-fx-font-weight: bold;");
                courseLabel.setWrapText(true);

                Label teacherLabel = new Label(entry.teacherName());
                teacherLabel.setStyle("-fx-text-fill: #334155; -fx-font-size: 10px;");
                teacherLabel.setWrapText(true);

                Label timeLabel = new Label(
                        formatMinutes(entry.startMinutes()) + "–" + formatMinutes(entry.endMinutes())
                );
                timeLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 9px;");

                VBox slotBox = new VBox(2, courseLabel, teacherLabel, timeLabel);
                slotBox.setAlignment(Pos.CENTER);

                StackPane slotCell = new StackPane(slotBox);
                slotCell.setStyle("-fx-background-color: #A7D8CF; -fx-background-radius: 6;");
                slotCell.setPadding(new Insets(4));

                GridPane.setMargin(slotCell, new Insets(2));
                grid.add(slotCell, d + 1, firstRow + 1, 1, rowSpan);
            }
        }

        return grid;
    }
    /** Builds the day-columns × hour-rows timetable grid from the given classroom's courses. */

    private List<CourseSlotEntry> collectSlotEntries(List<Course> courses) {
        List<CourseSlotEntry> entries = new ArrayList<>();
        for (Course course : courses) {
            String courseName = course.getName() == null ? "—" : course.getName();
            Employee teacher = course.getTeacher();
            String teacherName = teacher == null ? "—"
                    : ((teacher.getFirstName() == null ? "" : teacher.getFirstName()) + " "
                    + (teacher.getLastName() == null ? "" : teacher.getLastName())).trim();
            if (teacherName.isBlank()) teacherName = "—";

            if (course.getScheduleSlots() == null) continue;
            for (CourseScheduleSlot slot : course.getScheduleSlots()) {
                int dayIndex = dayIndexOf(slot.getDayOfWeek());
                Integer start = toMinutes(slot.getStartTime());
                Integer end = toMinutes(slot.getEndTime());
                if (dayIndex == -1 || start == null || end == null || end <= start) continue;
                entries.add(new CourseSlotEntry(dayIndex, start, end, courseName, teacherName));
            }
        }
        return entries;
    }

    private String localizedDay(String raw) {
        return switch (dayIndexOf(raw)) {
            case 0 -> I18n.t("day.mon");
            case 1 -> I18n.t("day.tue");
            case 2 -> I18n.t("day.wed");
            case 3 -> I18n.t("day.thu");
            case 4 -> I18n.t("day.fri");
            case 5 -> I18n.t("day.sat");
            case 6 -> I18n.t("day.sun");
            default -> raw == null ? "" : raw;
        };
    }

    private int dayIndexOf(String raw) {
        if (raw == null) return -1;
        String normalized = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "LUNDI", "MONDAY" -> 0;
            case "MARDI", "TUESDAY" -> 1;
            case "MERCREDI", "WEDNESDAY" -> 2;
            case "JEUDI", "THURSDAY" -> 3;
            case "VENDREDI", "FRIDAY" -> 4;
            case "SAMEDI", "SATURDAY" -> 5;
            case "DIMANCHE", "SUNDAY" -> 6;
            default -> -1;
        };
    }

    private Integer toMinutes(String hhmm) {
        try {
            String[] parts = hhmm.trim().split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return null;
        }
    }

    private String formatMinutes(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }

    private void setColorFromHex(PDPageContentStream cs, int hexColor, boolean isNonStroking) throws IOException {
        int r = (hexColor >> 16) & 0xFF;
        int g = (hexColor >> 8) & 0xFF;
        int b = hexColor & 0xFF;

        float rf = r / 255f;
        float gf = g / 255f;
        float bf = b / 255f;

        if (isNonStroking) {
            cs.setNonStrokingColor(rf, gf, bf);
        } else {
            cs.setStrokingColor(rf, gf, bf);
        }
    }

    // ---------------------------------------------------------------------
    // NEW: Arabic-text support helpers (font-missing-glyph fix).
    // ---------------------------------------------------------------------

    /** True if the string contains any character in the Arabic Unicode block. */
    private boolean containsArabic(String s) {
        if (s == null) return false;
        return s.chars().anyMatch(c -> c >= 0x0600 && c <= 0x06FF);
    }

    /**
     * Shapes Arabic text (joins letters into their contextual forms) and reorders it into
     * visual (left-to-right storage, right-to-left reading) order so PDFBox — which only
     * draws glyphs left-to-right in the order given — renders it correctly.
     */
    private String shapeArabic(String s) {
        try {
            ArabicShaping shaper = new ArabicShaping(
                    ArabicShaping.LETTERS_SHAPE | ArabicShaping.TEXT_DIRECTION_LOGICAL);
            String shaped = shaper.shape(s);
            Bidi bidi = new Bidi(shaped, Bidi.DIRECTION_RIGHT_TO_LEFT);
            return bidi.writeReordered(Bidi.KEEP_BASE_COMBINING);
        } catch (ArabicShapingException e) {
            return s; // fallback: unlinked letters, but at least it won't crash
        }
    }

    /** Picks the Latin or Arabic font depending on the text's content, and shapes Arabic text. */
    private PDFont fontFor(String text, PDFont latinFont, PDFont arabicFont) {
        return containsArabic(text) ? arabicFont : latinFont;
    }

    private String renderableText(String text) {
        return containsArabic(text) ? shapeArabic(text) : text;
    }

    /** Loads the embedded Arabic font (Type0, full Unicode support) from the classpath. */
    private PDFont loadArabicFont(PDDocument doc) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/fonts/NotoNaskhArabic-Regular.ttf")) {
            if (in == null) {
                throw new IOException("Police arabe introuvable : /fonts/NotoNaskhArabic-Regular.ttf "
                        + "(placez le fichier .ttf dans src/main/resources/fonts/)");
            }
            return PDType0Font.load(doc, in);
        }
    }

    /**
     * Renders text at (x, y), choosing the Latin or Arabic font automatically. For Arabic
     * text, right-aligns within [x, x+maxWidth] since Arabic reads right-to-left.
     */
    private void drawAutoText(PDPageContentStream cs, String text, float x, float y, float maxWidth,
                              PDFont latinFont, PDFont arabicFont, float size) throws IOException {
        boolean arabic = containsArabic(text);
        PDFont font = arabic ? arabicFont : latinFont;
        String rendered = renderableText(text);

        float drawX = x;
        if (arabic) {
            float textWidth = font.getStringWidth(rendered) / 1000 * size;
            drawX = x + Math.max(0, maxWidth - textWidth);
        }

        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(drawX, y);
        cs.showText(rendered);
        cs.endText();
    }

    /**
     * Renders this classroom's weekly timetable as a landscape A4 PDF (title, day header
     * band, hourly grid, lunch-break band, colored course blocks) using Apache PDFBox, and
     * returns the temp file it was written to. Runs on a background thread via
     * {@link AsyncTasks#run}.
     *
     * CHANGED: course/teacher labels now go through {@link #drawAutoText}, which switches
     * to an embedded Unicode font and shapes/reorders the text whenever it contains Arabic
     * characters — Helvetica (a Standard-14 font, WinAnsiEncoding only) cannot render Arabic
     * at all and previously crashed with "U+0627 is not available in the font Helvetica".
     */
    private File exportScheduleToPdf(Classroom classroom, List<Course> courses) throws IOException {
        List<CourseSlotEntry> entries = collectSlotEntries(courses);

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(
                    new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
            doc.addPage(page);

            PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont small = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont arabicFont = loadArabicFont(doc);

            float margin = 28f;
            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            float usableWidth = pageWidth - 2 * margin;

            float titleHeight = 32f;
            float headerHeight = 26f;
            float hourColWidth = 60f;
            float dayColWidth = (usableWidth - hourColWidth) / TIMETABLE_DAYS.size();

            float gridTop = pageHeight - margin - titleHeight - headerHeight;
            float gridBottom = margin;
            float gridHeight = gridTop - gridBottom;

            // The header now touches the timetable grid.
            float headerY = gridTop;

            int hourRowCount = (int) TIMETABLE_ROWS.stream()
                    .filter(row -> !row.isBreak())
                    .count();
            int breakRowCount = TIMETABLE_ROWS.size() - hourRowCount;
            float breakRowHeight = gridHeight
                    / (hourRowCount + breakRowCount * 0.4f) * 0.4f;
            float hourRowHeight = (gridHeight - breakRowHeight * breakRowCount) / hourRowCount;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                // Title is explicitly above the day-header band, so it cannot be covered.
                setColorFromHex(cs, 0x0F172A, true);
                drawAutoText(cs, "Emploi du temps de : " + classroom.getName(),
                        margin, headerY + headerHeight + 6f,
                        usableWidth, bold, arabicFont, 16);

                // Day header band.
                setColorFromHex(cs, 0xCFE8E4, true);
                cs.addRect(margin, headerY, usableWidth, headerHeight);
                cs.fill();

                setColorFromHex(cs, 0x0F172A, true);
                for (int d = 0; d < TIMETABLE_DAYS.size(); d++) {
                    float x = margin + hourColWidth + d * dayColWidth;
                    String label = TIMETABLE_DAYS.get(d).toUpperCase(Locale.FRENCH);
                    float textWidth = bold.getStringWidth(label) / 1000 * 10;

                    cs.beginText();
                    cs.setFont(bold, 10);
                    cs.newLineAtOffset(x + (dayColWidth - textWidth) / 2, headerY + 8);
                    cs.showText(label);
                    cs.endText();
                }

                // Hour rows, lunch-break band, and horizontal grid lines.
                float y = gridTop;
                List<float[]> rowBounds = new ArrayList<>();

                for (TimetableRow row : TIMETABLE_ROWS) {
                    float rowHeight = row.isBreak() ? breakRowHeight : hourRowHeight;
                    float top = y;
                    float bottom = y - rowHeight;
                    rowBounds.add(new float[]{top, bottom});

                    if (row.isBreak()) {
                        setColorFromHex(cs, 0xCFE8E4, true);
                        cs.addRect(margin, bottom, usableWidth, rowHeight);
                        cs.fill();
                    } else {
                        setColorFromHex(cs, 0x334155, true);
                        cs.beginText();
                        cs.setFont(regular, 9);
                        cs.newLineAtOffset(margin + 6, top - rowHeight / 2 + 3);
                        cs.showText(row.startLabel() + " - " + row.endLabel());
                        cs.endText();
                    }

                    setColorFromHex(cs, 0xE2E8F0, false);
                    cs.setLineWidth(0.75f);
                    cs.moveTo(margin, bottom);
                    cs.lineTo(margin + usableWidth, bottom);
                    cs.stroke();

                    y = bottom;
                }

                // Vertical separators.
                setColorFromHex(cs, 0xE2E8F0, false);
                for (int d = 0; d <= TIMETABLE_DAYS.size(); d++) {
                    float x = margin + hourColWidth + d * dayColWidth;
                    cs.moveTo(x, gridTop);
                    cs.lineTo(x, gridBottom);
                    cs.stroke();
                }

                // Outer border.
                setColorFromHex(cs, 0xCBD5E1, false);
                cs.setLineWidth(1f);
                cs.addRect(margin, gridBottom, usableWidth, gridTop - gridBottom);
                cs.stroke();

                // Course blocks.
                for (int d = 0; d < TIMETABLE_DAYS.size(); d++) {
                    int dayIndex = d;
                    List<CourseSlotEntry> daySlots = entries.stream()
                            .filter(entry -> entry.dayIndex() == dayIndex)
                            .toList();

                    for (CourseSlotEntry entry : daySlots) {
                        int firstRow = -1;
                        int lastRow = -1;

                        for (int r = 0; r < TIMETABLE_ROWS.size(); r++) {
                            TimetableRow row = TIMETABLE_ROWS.get(r);
                            if (row.isBreak()) {
                                continue;
                            }

                            boolean overlaps = row.startMinutes() < entry.endMinutes()
                                    && entry.startMinutes() < row.endMinutes();

                            if (overlaps) {
                                if (firstRow == -1) {
                                    firstRow = r;
                                }
                                lastRow = r;
                            }
                        }

                        if (firstRow == -1) {
                            continue;
                        }

                        float blockTop = rowBounds.get(firstRow)[0];
                        float blockBottom = rowBounds.get(lastRow)[1];
                        float x = margin + hourColWidth + d * dayColWidth;
                        float innerWidth = dayColWidth - 12;

                        setColorFromHex(cs, 0xA7D8CF, true);
                        cs.addRect(x + 2, blockBottom + 2,
                                dayColWidth - 4, blockTop - blockBottom - 4);
                        cs.fill();

                        PDFont courseMeasureFont = fontFor(entry.courseName(), bold, arabicFont);
                        String courseTxt = truncate(
                                entry.courseName(), courseMeasureFont, 8.5f, innerWidth);

                        setColorFromHex(cs, 0x0F172A, true);
                        drawAutoText(cs, courseTxt, x + 6, blockTop - 12,
                                innerWidth, bold, arabicFont, 8.5f);

                        PDFont teacherMeasureFont = fontFor(entry.teacherName(), small, arabicFont);
                        String teacherTxt = truncate(
                                entry.teacherName(), teacherMeasureFont, 7.5f, innerWidth);

                        setColorFromHex(cs, 0x334155, true);
                        drawAutoText(cs, teacherTxt, x + 6, blockTop - 23,
                                innerWidth, small, arabicFont, 7.5f);

                        setColorFromHex(cs, 0x475569, true);
                        cs.beginText();
                        cs.setFont(small, 7f);
                        cs.newLineAtOffset(x + 6, blockTop - 33);
                        cs.showText(formatMinutes(entry.startMinutes())
                                + "-" + formatMinutes(entry.endMinutes()));
                        cs.endText();
                    }
                }
            }

            File out = Files.createTempFile(
                    "emploi_du_temps_" + safeFileName(classroom.getName()) + "_",
                    ".pdf"
            ).toFile();

            doc.save(out);
            return out;
        }
    }
    /** Truncates text with an ellipsis so it doesn't overflow the given width at the given font/size. */
    private String truncate(String text, PDFont font, float size, float maxWidth) throws IOException {
        if (font.getStringWidth(renderableText(text)) / 1000 * size <= maxWidth) return text;
        String ellipsis = "…";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            String candidate = sb.toString() + c + ellipsis;
            if (font.getStringWidth(renderableText(candidate)) / 1000 * size > maxWidth) break;
            sb.append(c);
        }
        return sb + ellipsis;
    }

    private String safeFileName(String name) {
        return name == null ? "classe" : name.replaceAll("[^a-zA-Z0-9-_]", "_");
    }

    /** Opens a generated file with the OS's default associated application (PDF viewer). */
    private void openFile(File file) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file);
            } else {
                DialogUtil.info("PDF généré", "Fichier enregistré : " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            DialogUtil.error("Ouvrir le PDF", e.getMessage());
        }
    }

    /** One row: course name, teacher, fee and status — styled with explicit colors so it
     * reads correctly regardless of the dialog's plain background. */
    private HBox courseRow(Course course) {
        String teacherName = course.getTeacher() == null ? "—"
                : ((course.getTeacher().getFirstName() == null ? "" : course.getTeacher().getFirstName()) + " "
                + (course.getTeacher().getLastName() == null ? "" : course.getTeacher().getLastName())).trim();
        String feeText = course.getMonthlyFee() == null ? "—"
                : String.format(java.util.Locale.FRENCH, "%,.2f DA", course.getMonthlyFee());
        String statusText = course.getStatus() == null ? "—" : course.getStatus().name();

        Label nameLbl = new Label(course.getName() == null ? "—" : course.getName());
        nameLbl.setStyle("-fx-text-fill: #0F172A; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label subLbl = new Label(teacherName + "  ·  " + feeText);
        subLbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
        VBox textBox = new VBox(2, nameLbl, subLbl);

        Label statusBadge = new Label(statusText);
        boolean active = "ACTIVE".equalsIgnoreCase(statusText);
        statusBadge.setStyle((active
                ? "-fx-background-color: #DCFCE7; -fx-text-fill: #15803D;"
                : "-fx-background-color: #F1F5F9; -fx-text-fill: #475569;")
                + " -fx-padding: 3 10; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;");

        HBox row = new HBox(12, textBox, new Region(), statusBadge);
        HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 8;");
        row.setPadding(new Insets(8, 12, 8, 12));
        return row;
    }

    private String buildClassReportText(Classroom classroom, ClassAttendanceReport report) {
        String line = "═".repeat(48);
        StringBuilder studentLines = new StringBuilder();
        List<ClassStudentAttendance> students = report.students();
        for (int i = 0; i < students.size(); i++) {
            ClassStudentAttendance student = students.get(i);
            studentLines.append(String.format("%3d. %-25s %-15s %-10s%n",
                    i + 1,
                    student.lastName() + " " + student.firstName(),
                    student.studentNumber(),
                    student.statusLabel()));
        }

        return """
           %s
           RAPPORT DE CLASSE — %s
           %s

           Date          : %s
           Tranche d'âge : %s
           Effectif      : %d / %d places
           Présents      : %d
           Absents       : %d
           Excusés       : %d

           ── LISTE DES ÉLÈVES ──
           %s
           %s
           """.formatted(
                line,
                classroom.getName().toUpperCase(),
                line,
                report.date(),
                classroom.getAgeGroup() == null ? "—" : classroom.getAgeGroup(),
                students.size(), classroom.getCapacity(),
                report.present(),
                report.absent(),
                report.excused(),
                studentLines,
                line
        );
    }
}
