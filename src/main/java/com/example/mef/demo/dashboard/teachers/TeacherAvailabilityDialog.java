package com.example.mef.demo.dashboard.teachers;

import com.example.mef.demo.dashboard.courses.SchedulePickerDialog;
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
                "Disponibilité de l'enseignant",
                "Cliquez sur un créneau pour le sélectionner ou le désélectionner. Chaque jour peut avoir ses propres heures."
        ).map(Result::new);
    }
}
