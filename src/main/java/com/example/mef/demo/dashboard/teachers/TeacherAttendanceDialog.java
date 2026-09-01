package com.example.mef.demo.dashboard.teachers;

import com.example.mef.demo.Model.Employee;
import com.example.mef.demo.Model.TeacherAttendance;
import com.example.mef.demo.Services.EmployeeService;
import com.example.mef.demo.Services.TeacherPayrollService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.enums.TeacherAttendanceStatus;
import com.example.mef.demo.util.DialogUtil;
import com.example.mef.demo.util.I18n;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.mef.demo.enums.AttendanceStatus.EXCUSED;

public class TeacherAttendanceDialog extends Dialog<Void> {

    private final EmployeeService employeeService;
    private final TeacherPayrollService payrollService;
    private LocalDate selectedDate = LocalDate.now();
    private final Map<String, AttendanceRecordRow> rows = new HashMap<>();
    private final VBox listContainer = new VBox(8);

    public static void show(Window owner, EmployeeService employeeService, TeacherPayrollService payrollService) {
        TeacherAttendanceDialog dialog = new TeacherAttendanceDialog(employeeService, payrollService);
        dialog.initOwner(owner);
        dialog.showAndWait();
    }

    private TeacherAttendanceDialog(EmployeeService employeeService, TeacherPayrollService payrollService) {
        this.employeeService = employeeService;
        this.payrollService = payrollService;

        setTitle(I18n.t("teachers.attendance.title", "تسجيل حضور وغياب الأساتذة"));
        getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        
        Node closeBtn = getDialogPane().lookupButton(ButtonType.CLOSE);
        if (closeBtn != null) {
            closeBtn.setVisible(false);
            closeBtn.setManaged(false);
        }

        DatePicker datePicker = new DatePicker(selectedDate);
        datePicker.valueProperty().addListener((obs, old, val) -> {
            if (val != null) {
                selectedDate = val;
                loadData();
            }
        });

        HBox topBar = new HBox(12, new Label("التاريخ:"), datePicker);
        topBar.setAlignment(Pos.CENTER_LEFT);

        listContainer.setPadding(new Insets(12));
        ScrollPane scroll = new ScrollPane(listContainer);
        scroll.setFitToWidth(true);
        scroll.setPrefSize(500, 400);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: white;");

        Button saveBtn = new Button("حفظ");
        saveBtn.getStyleClass().add("primary-button");
        saveBtn.setOnAction(e -> saveAndClose());

        Button cancelBtn = new Button("إلغاء");
        cancelBtn.getStyleClass().add("secondary-button");
        cancelBtn.setOnAction(e -> close());

        HBox actions = new HBox(12, saveBtn, cancelBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(16, topBar, scroll, actions);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: white;");

        getDialogPane().setContent(root);
        loadData();
    }

    private void loadData() {
        listContainer.getChildren().clear();
        listContainer.getChildren().add(new Label("جاري التحميل..."));
        
        AsyncTasks.run(
                () -> {
                    List<Employee> teachers = employeeService.findTeachers();
                    Map<String, TeacherAttendance> records = new HashMap<>();
                    for (Employee t : teachers) {
                        TeacherAttendance att = payrollService.attendanceFor(t.getId(), selectedDate);
                        records.put(t.getId(), att);
                    }
                    return new Data(teachers, records);
                },
                data -> buildList(data.teachers(), data.records()),
                err -> DialogUtil.error("خطأ", err.getMessage())
        );
    }

    private void buildList(List<Employee> teachers, Map<String, TeacherAttendance> records) {
        listContainer.getChildren().clear();
        rows.clear();

        if (teachers.isEmpty()) {
            listContainer.getChildren().add(new Label("لا يوجد أساتذة"));
            return;
        }

        GridPane header = new GridPane();
        header.setHgap(12);
        header.add(new Label("الأستاذ"), 0, 0);
        header.add(new Label("الحالة"), 1, 0);
        header.add(new Label("ساعات الغياب"), 2, 0);
        
        ColumnConstraints c1 = new ColumnConstraints(); c1.setHgrow(Priority.ALWAYS);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPrefWidth(120);
        ColumnConstraints c3 = new ColumnConstraints(); c3.setPrefWidth(100);
        header.getColumnConstraints().addAll(c1, c2, c3);
        header.setStyle("-fx-font-weight: bold; -fx-padding: 0 0 8 0; -fx-border-color: transparent transparent #E2E8F0 transparent; -fx-border-width: 0 0 1 0;");
        listContainer.getChildren().add(header);

        for (Employee t : teachers) {
            TeacherAttendance att = records.get(t.getId());
            TeacherAttendanceStatus status = att != null ? att.getStatus() : TeacherAttendanceStatus.PRESENT;
            double hours = att != null && att.getAbsentHours() != null ? att.getAbsentHours() : 0.0;

            Label nameLbl = new Label(t.getFirstName() + " " + t.getLastName());
            
            ComboBox<TeacherAttendanceStatus> statusCombo = new ComboBox<>();
            statusCombo.getItems().addAll(TeacherAttendanceStatus.values());
            statusCombo.setValue(status);
            statusCombo.setMaxWidth(Double.MAX_VALUE);
            statusCombo.setConverter(new StringConverter<>() {
                @Override public String toString(TeacherAttendanceStatus st) {
                    if (st == null) return "";
                    return switch (st) {
                        case PRESENT -> "حاضر ✓";
                        case ABSENT -> "غائب ×";
                        case EXCUSED -> "مبرر ✉";
                    };
                }
                @Override public TeacherAttendanceStatus fromString(String string) { return null; }
            });

            TextField hoursField = new TextField(hours > 0 ? String.valueOf(hours) : "");
            hoursField.setPromptText("ساعات");
            
            statusCombo.valueProperty().addListener((obs, old, val) -> {
                if (val == TeacherAttendanceStatus.ABSENT) {
                    hoursField.setDisable(true);
                    hoursField.setText("");
                } else if (val == TeacherAttendanceStatus.PRESENT || val == TeacherAttendanceStatus.EXCUSED) {
                    hoursField.setDisable(false);
                }
            });

            if (status == TeacherAttendanceStatus.ABSENT) {
                hoursField.setDisable(true);
            }

            GridPane row = new GridPane();
            row.setHgap(12);
            row.add(nameLbl, 0, 0);
            row.add(statusCombo, 1, 0);
            row.add(hoursField, 2, 0);
            row.getColumnConstraints().addAll(c1, c2, c3);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(4, 0, 4, 0));
            
            listContainer.getChildren().add(row);
            rows.put(t.getId(), new AttendanceRecordRow(t.getId(), statusCombo, hoursField));
        }
    }

    private void saveAndClose() {
        AsyncTasks.run(
                () -> {
                    for (AttendanceRecordRow row : rows.values()) {
                        TeacherAttendanceStatus status = row.statusCombo().getValue();
                        double hours = 0.0;
                        if (status != TeacherAttendanceStatus.ABSENT) {
                            try {
                                String text = row.hoursField().getText().trim();
                                if (!text.isEmpty()) {
                                    hours = Double.parseDouble(text);
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                        payrollService.saveAttendance(row.teacherId(), selectedDate, status, hours);
                    }
                },
                () -> {
                    Platform.runLater(this::close);
                },
                err -> DialogUtil.error("خطأ", "حدث خطأ أثناء الحفظ: " + err.getMessage())
        );
    }

    private record Data(List<Employee> teachers, Map<String, TeacherAttendance> records) {}
    private record AttendanceRecordRow(String teacherId, ComboBox<TeacherAttendanceStatus> statusCombo, TextField hoursField) {}
}
