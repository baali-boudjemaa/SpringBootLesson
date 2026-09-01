package com.example.mef.demo.dashboard.teachers;

import com.example.mef.demo.Model.Employee;
import com.example.mef.demo.Model.TeacherAttendance;
import com.example.mef.demo.Services.TeacherPayrollService;
import com.example.mef.demo.Services.TeacherPayrollService.PayrollSummary;
import com.example.mef.demo.enums.TeacherAttendanceStatus;
import com.example.mef.demo.util.DialogUtil;
import com.example.mef.demo.util.I18n;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.YearMonth;

/** Records a teacher's daily attendance and displays the live monthly payroll result. */
public final class TeacherPayrollDialog {
    private TeacherPayrollDialog() { }

    public static void show(Window owner, Employee teacher, TeacherPayrollService payrollService) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(I18n.t("teachers.payroll.title", "تسجيل الحضور"));
        dialog.setHeaderText(teacher.getFirstName() + " " + teacher.getLastName());
        if (owner != null) dialog.initOwner(owner);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        DatePicker date = new DatePicker(LocalDate.now());
        ComboBox<TeacherAttendanceStatus> status = new ComboBox<>(
                FXCollections.observableArrayList(TeacherAttendanceStatus.values()));
        status.setValue(TeacherAttendanceStatus.PRESENT);
        TextField absentHours = new TextField("0");
        Label summary = new Label();
        summary.setWrapText(true);
        summary.setStyle("-fx-background-color: #F1F5F9; -fx-padding: 12; -fx-background-radius: 8;");

        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(10);
        form.addRow(0, new Label(I18n.t("teachers.payroll.date", "تسجيل الحضور")), date);
        form.addRow(1, new Label(I18n.t("teachers.payroll.status", "تسجيل الحضور")), status);
        form.addRow(2, new Label(I18n.t("teachers.payroll.absent_hours", "تسجيل الحضور")), absentHours);
        Button save = new Button(I18n.t("attendance.save", "تسجيل الحضور"));
        save.getStyleClass().add("primary-button");
        
        Button recordExpense = new Button(I18n.t("teachers.payroll.record_expense", "تسجيل الحضور"));
        recordExpense.getStyleClass().add("primary-button");
        
        VBox root = new VBox(14, form, save, new Label(I18n.t("teachers.payroll.monthly_summary", "تسجيل الحضور")), summary, recordExpense);
        root.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(root);

        Runnable refresh = () -> {
            TeacherAttendance record = payrollService.attendanceFor(teacher.getId(), date.getValue());
            status.setValue(record == null ? TeacherAttendanceStatus.PRESENT : record.getStatus());
            absentHours.setText(String.valueOf(record == null || record.getAbsentHours() == null ? 0 : record.getAbsentHours()));
            PayrollSummary payroll = payrollService.calculate(teacher.getId(), YearMonth.from(date.getValue()));
            summary.setText(format(payroll));
            recordExpense.setDisable(payroll.net() <= 0);
        };
        date.valueProperty().addListener((obs, old, value) -> refresh.run());
        
        recordExpense.setOnAction(event -> {
            try {
                payrollService.recordSalaryExpense(teacher.getId(), YearMonth.from(date.getValue()));
                DialogUtil.info(I18n.t("teachers.payroll.title", "تسجيل الحضور"), I18n.t("teachers.payroll.expense_recorded", "تسجيل الحضور"));
                refresh.run();
            } catch (Exception ex) {
                DialogUtil.error(I18n.t("teachers.payroll.title", "تسجيل الحضور"), ex.getMessage());
            }
        });
        
        save.setOnAction(event -> {
            try {
                double hours = Double.parseDouble(absentHours.getText().trim().replace(',', '.'));
                payrollService.saveAttendance(teacher.getId(), date.getValue(), status.getValue(), hours);
                refresh.run();
            } catch (NumberFormatException ex) {
                DialogUtil.error(I18n.t("teachers.payroll.title", "تسجيل الحضور"), I18n.t("teachers.payroll.invalid_hours", "تسجيل الحضور"));
            } catch (Exception ex) {
                DialogUtil.error(I18n.t("teachers.payroll.title", "تسجيل الحضور"), ex.getMessage());
            }
        });
        refresh.run();
        dialog.showAndWait();
    }

    private static String format(PayrollSummary payroll) {
        String type = payroll.compensationType().name().equals("MONTHLY")
                ? I18n.t("teachers.payroll.monthly", "تسجيل الحضور") : I18n.t("teachers.payroll.per_lesson", "تسجيل الحضور");
        return I18n.t("teachers.payroll.summary", "تسجيل الحضور")
                .replace("{0}", type)
                .replace("{1}", String.valueOf(payroll.payableLessons()))
                .replace("{2}", String.valueOf(payroll.absentDays()))
                .replace("{3}", String.valueOf(payroll.absentHours()))
                .replace("{4}", money(payroll.gross()))
                .replace("{5}", money(payroll.deductions()))
                .replace("{6}", money(payroll.net()));
    }

    private static String money(double value) { return String.format("%.2f", value); }
}
