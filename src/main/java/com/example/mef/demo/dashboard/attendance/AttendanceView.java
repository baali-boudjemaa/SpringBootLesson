package com.example.mef.demo.dashboard.attendance;

import com.example.mef.demo.Model.Classroom;
import com.example.mef.demo.Services.ClassroomService;
import com.example.mef.demo.Services.ClassroomService.ClassAttendanceReport;
import com.example.mef.demo.Services.ClassroomService.ClassStudentAttendance;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.enums.AttendanceStatus;
import com.example.mef.demo.util.DialogUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class AttendanceView {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);

    private final ClassroomService classroomService;

    private LocalDate selectedDate = LocalDate.now();
    private String selectedClassroomId;
    private final Map<String, AttendanceStatus> selectedStatuses = new LinkedHashMap<>();

    public AttendanceView(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    public void render(BorderPane contentPane, Label pageTitleLabel) {
        pageTitleLabel.setText("Présence");
        contentPane.setCenter(new Label("Chargement..."));

        AsyncTasks.run(
                classroomService::findAll,
                classrooms -> {
                    if (selectedClassroomId == null && !classrooms.isEmpty()) {
                        selectedClassroomId = classrooms.get(0).getId();
                    }
                    loadReport(contentPane, classrooms);
                },
                err -> DialogUtil.error("Présence", err.getMessage())
        );
    }

    private void loadReport(BorderPane contentPane, List<Classroom> classrooms) {
        selectedStatuses.clear();
        AsyncTasks.run(
                () -> selectedClassroomId == null
                        ? classroomService.getAllStudentsAttendanceReport(selectedDate)
                        : classroomService.getClassAttendanceReport(selectedClassroomId, selectedDate),
                report -> buildUI(contentPane, classrooms, report),
                err -> DialogUtil.error("Présence", err.getMessage())
        );
    }

    private void buildUI(BorderPane contentPane, List<Classroom> classrooms, ClassAttendanceReport report) {
        report.students().forEach(row -> selectedStatuses.put(row.id(), row.status()));

        Label title = new Label("Présence");
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Label subtitle = new Label("Suivez la présence quotidienne");
        subtitle.setStyle("-fx-font-size: 15px; -fx-text-fill: #64748B;");

        Button previous = new Button("‹");
        previous.getStyleClass().add("secondary-button");
        previous.setOnAction(e -> {
            selectedDate = selectedDate.minusDays(1);
            loadReport(contentPane, classrooms);
        });

        Button next = new Button("›");
        next.getStyleClass().add("secondary-button");
        next.setOnAction(e -> {
            selectedDate = selectedDate.plusDays(1);
            loadReport(contentPane, classrooms);
        });

        Label dateLabel = new Label(formatDate(selectedDate));
        dateLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");
        HBox.setHgrow(dateLabel, Priority.ALWAYS);

        HBox datePicker = new HBox(12, previous, dateLabel, next);
        datePicker.setAlignment(Pos.CENTER);
        datePicker.setMaxWidth(360);
        datePicker.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-background-radius: 24; -fx-border-radius: 24; -fx-padding: 6 10;");

        Button save = new Button("💾  Enregistrer");
        save.getStyleClass().add("primary-button");
        save.setOnAction(e -> saveAttendance(contentPane, classrooms));

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);

        HBox header = new HBox(18, new VBox(4, title, subtitle, datePicker), titleSpacer, save);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox stats = new HBox(18,
                statCard("✓", String.valueOf(report.present()), "PRÉSENT", "#10B981", "#ECFDF5"),
                statCard("×", String.valueOf(report.absent()), "ABSENT", "#F43F5E", "#FFF1F2"),
                statCard("✉", String.valueOf(report.excused()), "EXCUSÉ", "#6366F1", "#EEF2FF"),
                statCard("◌", String.valueOf(report.unmarked()), "NON MARQUÉS", "#64748B", "#F1F5F9")
        );

        for (Node card : stats.getChildren()) {
            HBox.setHgrow(card, Priority.ALWAYS);
        }

        Label quickLabel = new Label("Remplissage rapide :");
        quickLabel.setStyle("-fx-text-fill: #64748B;");

        Button allPresent = outlineButton("✓  Tous présents", "#10B981");
        allPresent.setOnAction(e -> {
            report.students().forEach(row -> selectedStatuses.put(row.id(), AttendanceStatus.PRESENT));
            buildUI(contentPane, classrooms, reportWithSelectedStatuses(report));
        });

        Button allAbsent = outlineButton("×  Tous absents", "#F43F5E");
        allAbsent.setOnAction(e -> {
            report.students().forEach(row -> selectedStatuses.put(row.id(), AttendanceStatus.ABSENT));
            buildUI(contentPane, classrooms, reportWithSelectedStatuses(report));
        });

        HBox quickFill = new HBox(12, quickLabel, allPresent, allAbsent);
        quickFill.setAlignment(Pos.CENTER_LEFT);

        HBox classTabs = new HBox(8);
        Button allClasses = classTab("Tous (" + report.students().size() + ")", selectedClassroomId == null);
        allClasses.setOnAction(e -> {
            selectedClassroomId = null;
            loadReport(contentPane, classrooms);
        });
        classTabs.getChildren().add(allClasses);
        for (Classroom classroom : classrooms) {
            Button tab = classTab(classroom.getName(), classroom.getId().equals(selectedClassroomId));
            tab.setOnAction(e -> {
                selectedClassroomId = classroom.getId();
                loadReport(contentPane, classrooms);
            });
            classTabs.getChildren().add(tab);
        }

        GridPane table = new GridPane();
        table.setHgap(12);
        table.setVgap(0);
        table.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-background-radius: 12; -fx-border-radius: 12;");
        table.add(headerLabel("ENFANT"), 0, 0);
        table.add(headerLabel("STATUT"), 1, 0);
        table.add(headerLabel("HEURE D'ARRIVÉE"), 2, 0);

        int rowIndex = 1;
        for (ClassStudentAttendance student : report.students()) {
            table.add(studentCell(student), 0, rowIndex);
            table.add(statusButtons(contentPane, classrooms, report, student), 1, rowIndex);
            table.add(arrivalCell(student), 2, rowIndex);
            rowIndex++;
        }

        VBox root = new VBox(20, header, stats, quickFill, classTabs, table);
        root.setPadding(new Insets(28));
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        contentPane.setCenter(scroll);
    }

    private VBox statCard(String icon, String value, String label, String color, String background) {
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 22px; -fx-text-fill: " + color + "; -fx-background-color: " + background + "; -fx-background-radius: 10; -fx-padding: 10 16;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #0F172A;");

        Label caption = new Label(label);
        caption.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        VBox text = new VBox(2, valueLabel, caption);
        HBox row = new HBox(14, iconLabel, text);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(row);
        card.setStyle("-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-background-radius: 12; -fx-border-radius: 12; -fx-padding: 18;");
        return card;
    }

    private Label headerLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-padding: 16 20; -fx-font-weight: bold;");
        return label;
    }

    private HBox studentCell(ClassStudentAttendance student) {
        String initials = initials(student);
        Label avatar = new Label(initials);
        avatar.setStyle("-fx-background-color: #4F46E5; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 999; -fx-min-width: 38; -fx-min-height: 38; -fx-alignment: center;");

        Label name = new Label(student.fullName());
        name.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        HBox cell = new HBox(12, avatar, name);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setStyle("-fx-padding: 14 20; -fx-border-color: #E2E8F0 transparent transparent transparent;");
        return cell;
    }

    private HBox statusButtons(BorderPane contentPane, List<Classroom> classrooms, ClassAttendanceReport report, ClassStudentAttendance student) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-padding: 14 20; -fx-border-color: #E2E8F0 transparent transparent transparent;");
        box.getChildren().addAll(
                statusButton("✓  Présent", student, AttendanceStatus.PRESENT, contentPane, classrooms, report),
                statusButton("×  Absent", student, AttendanceStatus.ABSENT, contentPane, classrooms, report),
                statusButton("✉  Excusé", student, AttendanceStatus.EXCUSED, contentPane, classrooms, report)
        );
        return box;
    }

    private Button statusButton(String text, ClassStudentAttendance student, AttendanceStatus status,
                                BorderPane contentPane, List<Classroom> classrooms, ClassAttendanceReport report) {
        boolean selected = selectedStatuses.get(student.id()) == status;
        Button button = new Button(text);
        button.setStyle(selected
                ? "-fx-background-color: #EEF2FF; -fx-border-color: #4F46E5; -fx-text-fill: #4F46E5; -fx-background-radius: 999; -fx-border-radius: 999; -fx-padding: 6 14;"
                : "-fx-background-color: white; -fx-border-color: #CBD5E1; -fx-text-fill: #64748B; -fx-background-radius: 999; -fx-border-radius: 999; -fx-padding: 6 14;");
        button.setOnAction(e -> {
            selectedStatuses.put(student.id(), status);
            buildUI(contentPane, classrooms, reportWithSelectedStatuses(report));
        });
        return button;
    }

    private Label arrivalCell(ClassStudentAttendance student) {
        Label label = new Label(selectedStatuses.get(student.id()) == AttendanceStatus.PRESENT ? "Maintenant" : "—");
        label.setStyle("-fx-text-fill: #94A3B8; -fx-padding: 14 20; -fx-border-color: #E2E8F0 transparent transparent transparent;");
        return label;
    }

    private Button outlineButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: transparent; -fx-border-color: " + color + "; -fx-text-fill: " + color + "; -fx-background-radius: 999; -fx-border-radius: 999; -fx-padding: 7 18;");
        return button;
    }

    private Button classTab(String text, boolean selected) {
        Button button = new Button(text);
        button.setStyle(selected
                ? "-fx-background-color: #4F46E5; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 999; -fx-padding: 8 22;"
                : "-fx-background-color: white; -fx-border-color: #E2E8F0; -fx-text-fill: #64748B; -fx-background-radius: 999; -fx-border-radius: 999; -fx-padding: 8 22;");
        return button;
    }

    private ClassAttendanceReport reportWithSelectedStatuses(ClassAttendanceReport report) {
        List<ClassStudentAttendance> students = report.students().stream()
                .map(row -> new ClassStudentAttendance(
                        row.id(),
                        row.studentNumber(),
                        row.firstName(),
                        row.lastName(),
                        selectedStatuses.get(row.id())))
                .toList();

        long present = students.stream().filter(row -> row.status() == AttendanceStatus.PRESENT).count();
        long absent = students.stream().filter(row -> row.status() == AttendanceStatus.ABSENT).count();
        long excused = students.stream().filter(row -> row.status() == AttendanceStatus.EXCUSED).count();
        long unmarked = students.stream().filter(row -> row.status() == null).count();
        return new ClassAttendanceReport(report.date(), students, present, absent, excused, unmarked);
    }

    private void saveAttendance(BorderPane contentPane, List<Classroom> classrooms) {
        AsyncTasks.run(
                () -> classroomService.saveAttendance(selectedDate, selectedStatuses),
                () -> {
                    DialogUtil.info("Présence", "Présence enregistrée.");
                    loadReport(contentPane, classrooms);
                },
                err -> DialogUtil.error("Présence", err.getMessage())
        );
    }

    private String formatDate(LocalDate date) {
        String day = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        return capitalize(day) + " " + date.format(DATE_FORMAT);
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.substring(0, 1).toUpperCase(Locale.FRENCH) + text.substring(1);
    }

    private String initials(ClassStudentAttendance student) {
        String first = student.firstName() == null || student.firstName().isBlank() ? "" : student.firstName().substring(0, 1);
        String last = student.lastName() == null || student.lastName().isBlank() ? "" : student.lastName().substring(0, 1);
        String initials = first + last;
        return initials.isBlank() ? "?" : initials.toLowerCase(Locale.FRENCH);
    }
}
