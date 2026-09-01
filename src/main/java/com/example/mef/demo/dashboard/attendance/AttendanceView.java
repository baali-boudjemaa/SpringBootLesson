package com.example.mef.demo.dashboard.attendance;

import com.example.mef.demo.Model.Classroom;
import com.example.mef.demo.Services.ClassroomService;
import com.example.mef.demo.Services.ClassroomService.ClassAttendanceReport;
import com.example.mef.demo.Services.ClassroomService.ClassStudentAttendance;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.enums.AttendanceStatus;
import com.example.mef.demo.enums.Category;
import com.example.mef.demo.util.DialogUtil;
import com.example.mef.demo.util.I18n;
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
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class AttendanceView {

    private final ClassroomService classroomService;

    private LocalDate selectedDate = LocalDate.now();
    private Category selectedCategory;
    private String selectedClassroomId;
    private final Map<String, AttendanceStatus> selectedStatuses = new LinkedHashMap<>();

    public AttendanceView(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    public void render(BorderPane contentPane, Label pageTitleLabel) {
        pageTitleLabel.setText(I18n.t("attendance.title", "تسجيل الحضور"));
        contentPane.setCenter(new Label("Chargement..."));

        AsyncTasks.run(
                classroomService::findAll,
                classrooms -> {
                    if (selectedCategory == null && !classrooms.isEmpty()) {
                        selectedCategory = classrooms.stream()
                                .map(Classroom::getCategory)
                                .filter(java.util.Objects::nonNull)
                                .findFirst()
                                .orElse(null);
                    }
                    loadReport(contentPane, classrooms);
                },
                err -> DialogUtil.error("Présence", err.getMessage())
        );
    }

    private void loadReport(BorderPane contentPane, List<Classroom> classrooms) {
        selectedStatuses.clear();
        AsyncTasks.run(
                () -> {
                    if (selectedClassroomId != null) {
                        return classroomService.getClassAttendanceReport(selectedClassroomId, selectedDate);
                    }
                    if (selectedCategory != null) {
                        return classroomService.getCategoryAttendanceReport(selectedCategory, selectedDate);
                    }
                    return classroomService.getAllStudentsAttendanceReport(selectedDate);
                },
                report -> buildUI(contentPane, classrooms, report),
                err -> DialogUtil.error("Présence", err.getMessage())
        );
    }

    private void buildUI(BorderPane contentPane, List<Classroom> classrooms, ClassAttendanceReport report) {
        report.students().forEach(row -> selectedStatuses.put(row.id(), row.status()));

        Label title = new Label(I18n.t("attendance.title", "تسجيل الحضور"));
        title.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: #0F172A;");

        Label subtitle = new Label(I18n.t("attendance.subtitle", "تسجيل الحضور"));
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

        Button save = new Button("💾  " + I18n.t("attendance.save", "تسجيل الحضور"));
        save.getStyleClass().add("primary-button");
        save.setOnAction(e -> saveAttendance(contentPane, classrooms));

        Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);

        HBox header = new HBox(18, new VBox(4, title, subtitle, datePicker), titleSpacer, save);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox stats = new HBox(18,
                statCard("✓", String.valueOf(report.present()), I18n.t("attendance.present", "تسجيل الحضور"), "#10B981", "#ECFDF5"),
                statCard("×", String.valueOf(report.absent()), I18n.t("attendance.absent", "تسجيل الحضور"), "#F43F5E", "#FFF1F2"),
                statCard("✉", String.valueOf(report.excused()), I18n.t("attendance.excused", "تسجيل الحضور"), "#6366F1", "#EEF2FF"),
                statCard("◌", String.valueOf(report.unmarked()), I18n.t("attendance.unmarked", "تسجيل الحضور"), "#64748B", "#F1F5F9")
        );

        for (Node card : stats.getChildren()) {
            HBox.setHgrow(card, Priority.ALWAYS);
        }

        Label quickLabel = new Label(I18n.t("attendance.quick_fill", "تسجيل الحضور"));
        quickLabel.setStyle("-fx-text-fill: #64748B;");

        Button allPresent = outlineButton("✓  " + I18n.t("attendance.all_present", "تسجيل الحضور"), "#10B981");
        allPresent.setOnAction(e -> {
            report.students().forEach(row -> selectedStatuses.put(row.id(), AttendanceStatus.PRESENT));
            buildUI(contentPane, classrooms, reportWithSelectedStatuses(report));
        });

        Button allAbsent = outlineButton("×  " + I18n.t("attendance.all_absent", "تسجيل الحضور"), "#F43F5E");
        allAbsent.setOnAction(e -> {
            report.students().forEach(row -> selectedStatuses.put(row.id(), AttendanceStatus.ABSENT));
            buildUI(contentPane, classrooms, reportWithSelectedStatuses(report));
        });

        HBox quickFill = new HBox(12, quickLabel, allPresent, allAbsent);
        quickFill.setAlignment(Pos.CENTER_LEFT);

        HBox categoryTabs = new HBox(8);
        Label categoryLabel = new Label(I18n.t("attendance.choose_category", "تسجيل الحضور"));
        categoryLabel.setStyle("-fx-text-fill: #64748B; -fx-padding: 8 4;");
        categoryTabs.getChildren().add(categoryLabel);
        for (Category category : Category.values()) {
            boolean exists = classrooms.stream().anyMatch(room -> room.getCategory() == category);
            if (!exists) continue;
            Button tab = classTab(categoryLabel(category), category == selectedCategory);
            tab.setOnAction(e -> {
                selectedCategory = category;
                selectedClassroomId = null;
                loadReport(contentPane, classrooms);
            });
            categoryTabs.getChildren().add(tab);
        }

        List<Classroom> categoryClassrooms = classrooms.stream()
                .filter(classroom -> classroom.getCategory() == selectedCategory)
                .toList();
        HBox classTabs = new HBox(8);
        Label classLabel = new Label(I18n.t("attendance.choose_classroom", "تسجيل الحضور"));
        classLabel.setStyle("-fx-text-fill: #64748B; -fx-padding: 8 4;");
        classTabs.getChildren().add(classLabel);
        Button allClasses = classTab(I18n.t("attendance.all_category_classes", "تسجيل الحضور").replace("{0}", String.valueOf(report.students().size())), selectedClassroomId == null);
        allClasses.setOnAction(e -> {
            selectedClassroomId = null;
            loadReport(contentPane, classrooms);
        });
        classTabs.getChildren().add(allClasses);
        for (Classroom classroom : categoryClassrooms) {
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
        table.add(headerLabel(I18n.t("attendance.table.child", "تسجيل الحضور")), 0, 0);
        table.add(headerLabel(I18n.t("attendance.table.status", "تسجيل الحضور")), 1, 0);
        table.add(headerLabel(I18n.t("attendance.table.arrival_time", "تسجيل الحضور")), 2, 0);

        int rowIndex = 1;
        for (ClassStudentAttendance student : report.students()) {
            table.add(studentCell(student), 0, rowIndex);
            table.add(statusButtons(contentPane, classrooms, report, student), 1, rowIndex);
            table.add(arrivalCell(student), 2, rowIndex);
            rowIndex++;
        }

        VBox root = new VBox(20, header, stats, quickFill, categoryTabs, classTabs, table);
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

    private String categoryLabel(Category category) {
        if (category == null) return "—";
        return switch (category) {
            case CRECHE -> I18n.t("category.creche", "تسجيل الحضور");
            case PREPARATOIRE -> I18n.t("category.preparatoire", "تسجيل الحضور");
            case SOUTIEN -> I18n.t("category.soutien", "تسجيل الحضور");
        };
    }

    private HBox statusButtons(BorderPane contentPane, List<Classroom> classrooms, ClassAttendanceReport report, ClassStudentAttendance student) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-padding: 14 20; -fx-border-color: #E2E8F0 transparent transparent transparent;");
        box.getChildren().addAll(
                statusButton("✓  " + I18n.t("attendance.present", "تسجيل الحضور"), student, AttendanceStatus.PRESENT, contentPane, classrooms, report),
                statusButton("×  " + I18n.t("attendance.absent", "تسجيل الحضور"), student, AttendanceStatus.ABSENT, contentPane, classrooms, report),
                statusButton("✉  " + I18n.t("attendance.excused", "تسجيل الحضور"), student, AttendanceStatus.EXCUSED, contentPane, classrooms, report)
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
        Label label = new Label(selectedStatuses.get(student.id()) == AttendanceStatus.PRESENT ? I18n.t("attendance.now", "تسجيل الحضور") : "—");
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
                        row.classroomName(),
                        row.category(),
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
                    DialogUtil.info(I18n.t("attendance.title", "تسجيل الحضور"), I18n.t("attendance.saved", "تسجيل الحضور"));
                    loadReport(contentPane, classrooms);
                },
                err -> DialogUtil.error("Présence", err.getMessage())
        );
    }

    private String formatDate(LocalDate date) {
        Locale locale = I18n.getLocale();
        String day = date.getDayOfWeek().getDisplayName(TextStyle.FULL, locale);
        return capitalize(day) + " " + date.format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", locale));
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.substring(0, 1).toUpperCase(I18n.getLocale()) + text.substring(1);
    }

    private String initials(ClassStudentAttendance student) {
        String first = student.firstName() == null || student.firstName().isBlank() ? "" : student.firstName().substring(0, 1);
        String last = student.lastName() == null || student.lastName().isBlank() ? "" : student.lastName().substring(0, 1);
        String initials = first + last;
        return initials.isBlank() ? "?" : initials.toLowerCase(I18n.getLocale());
    }
}
