package com.example.mef.demo.dashboard.teachers;

import com.example.mef.demo.dashboard.courses.SchedulePickerDialog;
import com.example.mef.demo.util.I18n;
import javafx.stage.Window;

import java.util.Optional;

/** Per-day, per-hour availability picker for a teacher. */
public final class TeacherAvailabilityDialog {

    private TeacherAvailabilityDialog() {
    }

    public record Result(String schedule) {
    }

    public static Optional<Result> show(Window owner, String currentSchedule, java.util.List<com.example.mef.demo.dashboard.common.TimeSlots.TimeBlock> blocks) {
        return SchedulePickerDialog.show(
                owner,
                currentSchedule,
                I18n.t("availability.teacher_title", "تسجيل الحضور"),
                I18n.t("availability.hint", "تسجيل الحضور"),
                blocks
        ).map(Result::new);
    }
}
