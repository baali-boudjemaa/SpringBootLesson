package com.example.mef.demo.dashboard.classrooms;

import com.example.mef.demo.Model.Classroom;
import com.example.mef.demo.Model.Room;
import com.example.mef.demo.Model.Student;
import com.example.mef.demo.Services.ClassroomService;
import com.example.mef.demo.Services.ClassroomService.ClassAttendanceReport;
import com.example.mef.demo.Services.ClassroomService.ClassStudentAttendance;
import com.example.mef.demo.Services.ClassroomService.RoomConflict;
import com.example.mef.demo.Services.RoomService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.dashboard.common.WeeklyOccupancyGrid;
import com.example.mef.demo.enums.Category;
import com.example.mef.demo.util.DialogUtil;
import com.example.mef.demo.util.I18n;
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
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

    public void render(BorderPane contentPane) {
        FlowPane cardGrid = new FlowPane(16, 16);
        cardGrid.setPadding(new Insets(4));

        TextField nameField = FormFactory.textField("Nom de la section");
        TextField ageGroupField = FormFactory.textField("Tranche d'âge");
        TextField capacityField = FormFactory.textField("Capacité max");
        ComboBox<Category> categoryField = new ComboBox<>(FXCollections.observableArrayList(Category.values()));
        categoryField.setMaxWidth(Double.MAX_VALUE);
        categoryField.setValue(Category.CRECHE);
        categoryField.setCellFactory(cb -> categoryCell());
        categoryField.setButtonCell(categoryCell());

        WeeklyOccupancyGrid occupancyGrid = new WeeklyOccupancyGrid();

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
        FormFactory.addRow(form, 0, "Nom", nameField);
        FormFactory.addRow(form, 1, "Tranche d'âge", ageGroupField);
        FormFactory.addRow(form, 2, "Capacité", capacityField);
        FormFactory.addRow(form, 3, I18n.t("classroom.category"), categoryField);
        FormFactory.addRow(form, 4, "Occupation hebdomadaire");
        FormFactory.addRow(form, 5,  occupancyGrid.getNode());
        FormFactory.addRow(form, 6, I18n.t("classroom.rooms"));
        FormFactory.addRow(form, 7, roomsScroll);

        Button save   = new Button(I18n.t("action.save"));   save.getStyleClass().add("primary-button");
        Button clear  = new Button(I18n.t("action.clear"));  clear.getStyleClass().add("secondary-button");
        Button delete = new Button(I18n.t("action.delete")); delete.getStyleClass().add("danger-button");
        HBox actions = new HBox(10, save, clear, delete);

        Classroom[] selected = new Classroom[]{null};
        Runnable[] reload = new Runnable[1];

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
            occupancyGrid.clear();
            roomChecks.forEach(cb -> cb.setSelected(false));
            cardGrid.getChildren().forEach(n -> n.getStyleClass().remove("class-card-selected"));
        };

        reload[0] = () -> AsyncTasks.run(
                () -> classroomService.findAll(),
                classrooms -> {
                    cardGrid.getChildren().clear();
                    for (Classroom c : classrooms) {
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
                            occupancyGrid.setValue(c.getOccupancySchedule(), c.getAttendanceDays(),
                                    c.getPeriodStartTime(), c.getPeriodEndTime());
                            List<String> linkedRoomIds = c.getRooms() == null ? List.of()
                                    : c.getRooms().stream().map(Room::getId).toList();
                            roomChecks.forEach(cb -> cb.setSelected(
                                    linkedRoomIds.contains(((Room) cb.getUserData()).getId())));
                        });
                        cardGrid.getChildren().add(card);
                    }
                },
                err -> DialogUtil.error("Chargement échoué", err.getMessage())
        );

        clear.setOnAction(e -> clearForm.run());

        save.setOnAction(e -> {
            try {
                if (nameField.getText().isBlank()) throw new IllegalArgumentException("Le nom est requis.");
                int capacity;
                try {
                    capacity = Integer.parseInt(capacityField.getText().trim());
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Capacité doit être un nombre.");
                }

                Classroom c = selected[0] != null ? selected[0] : Classroom.builder().build();
                c.setName(nameField.getText().trim());
                c.setAgeGroup(ageGroupField.getText().trim());
                c.setCapacity(capacity);
                c.setCategory(categoryField.getValue() == null ? Category.CRECHE : categoryField.getValue());
                c.setOccupancySchedule(occupancyGrid.getValue());
                c.setAttendanceDays(occupancyGrid.getDays());
                c.setPeriodStartTime(occupancyGrid.getEarliestStart());
                c.setPeriodEndTime(occupancyGrid.getLatestEnd());
                c.setRooms(roomChecks.stream()
                        .filter(CheckBox::isSelected)
                        .map(cb -> (Room) cb.getUserData())
                        .collect(Collectors.toList()));

                Runnable doSave = () -> {
                    save.setDisable(true);
                    AsyncTasks.run(
                            () -> classroomService.save(c),
                            () -> { save.setDisable(false); reload[0].run(); clearForm.run(); },
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
                DialogUtil.info(I18n.t("action.delete"), "Sélectionnez une classe avant de supprimer.");
                return;
            }
            if (DialogUtil.confirm(I18n.t("action.delete"), "Supprimer cette classe ?")) {
                String id = selected[0].getId();
                delete.setDisable(true);
                AsyncTasks.run(
                        () -> classroomService.delete(id),
                        () -> { delete.setDisable(false); reload[0].run(); clearForm.run(); },
                        err -> { delete.setDisable(false); DialogUtil.error(I18n.t("action.delete"), err.getMessage()); }
                );
            }
        });

        Button addNew = new Button("➕  Nouvelle Section");
        addNew.getStyleClass().add("primary-button");
        addNew.setOnAction(e -> {
            clearForm.run();
            nameField.requestFocus();
        });

        loadRooms.run();
        reload[0].run();

        VBox formPanel = new VBox(14, new Label(I18n.t("table.details")), form, actions);
        formPanel.getStyleClass().add("side-panel");

        // Details panel scrolls vertically when the form is taller than the window.
        ScrollPane formScroll = new ScrollPane(formPanel);
        formScroll.setFitToWidth(true);
        formScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        formScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        formScroll.setPrefWidth(380);
        formScroll.setMinWidth(340);
        formScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        ScrollPane cardScroll = new ScrollPane(cardGrid);
        cardScroll.setFitToWidth(true);
        cardScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox cardPanel = new VBox(10, addNew, cardScroll);
        VBox.setVgrow(cardScroll, Priority.ALWAYS);

        HBox workspace = new HBox(18, cardPanel, formScroll);
        HBox.setHgrow(cardPanel, Priority.ALWAYS);
        workspace.setPadding(new Insets(24));
        contentPane.setCenter(workspace);
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
        Label capacity = new Label(enrolled + "/" + c.getCapacity() + " places");
        capacity.setStyle("-fx-font-size: 12px; -fx-text-fill: #15803D; -fx-font-weight: bold;");

        Label hint = new Label("Double-clic pour voir les élèves");
        hint.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");

        VBox card = new VBox(6, name, meta, capacity);
        if (c.getRooms() != null && !c.getRooms().isEmpty()) {
            String roomNames = c.getRooms().stream().map(Room::getName).collect(Collectors.joining(", "));
            Label rooms = new Label("🏠 " + roomNames);
            rooms.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
            card.getChildren().add(rooms);
        }
        card.getChildren().add(hint);
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
        dialog.setTitle("Élèves — " + classroom.getName());
        dialog.setMinWidth(460);
        dialog.setMinHeight(420);

        ListView<ClassStudentAttendance> listView = new ListView<>();
        listView.setPrefSize(420, 280);
        listView.setPlaceholder(new Label("Aucun élève inscrit dans cette classe."));
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(ClassStudentAttendance row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setText(null);
                    return;
                }

                setText(row.fullName() + "  (" + row.studentNumber() + ")  -  " + row.statusLabel());
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

                    Button reportBtn = new Button("📋  Générer le rapport");
                    reportBtn.getStyleClass().add("primary-button");

                    Button copyBtn = new Button("📋  Copier");
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

                    Button closeBtn = new Button("Fermer");
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
