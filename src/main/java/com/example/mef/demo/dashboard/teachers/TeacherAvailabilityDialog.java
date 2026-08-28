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

    public static Optional<Result> show(Window owner, String currentSchedule) {
        return SchedulePickerDialog.show(
                owner,
                currentSchedule,
                I18n.t("availability.teacher_title"),
                I18n.t("availability.hint")
        ).map(Result::new);
    }
}
