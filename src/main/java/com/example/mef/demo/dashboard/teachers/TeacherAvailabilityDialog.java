package com.example.mef.demo.dashboard.teachers;

import com.example.mef.demo.dashboard.common.DaysPicker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Modal "weekly timetable" picker for a teacher's availability — same grid
 * shape as {@link com.example.mef.demo.dashboard.courses.SchedulePickerDialog}
 * (days across the top, one-hour rows down the side, lunch break shown as a
 * non-clickable band), but adapted to what {@code Employee} can actually
 * store: a set of working days that all share the *same* daily start/end
 * window (there's no per-day-different-hours column in the data model).
 *
 * Click a day header to toggle that day on/off. Click any cell to turn its
 * day on and set the shared hour window (first click starts the range,
 * second click on another row completes it) — every ticked day always shows
 * that same window, which is exactly what gets saved.
 */
public final class TeacherAvailabilityDialog {

    /** [startHour, endHour] blocks shown as rows (lunch break inserted between index 3 and 4). */
    private static final int[][] HOUR_BLOCKS = {
            {8, 9}, {9, 10}, {10, 11}, {11, 12}, {14, 15}, {15, 16}, {16, 17}, {17, 18}
    };
    private static final int LUNCH_BREAK_ROW_INDEX = 4;
    private static final List<String> DAYS = DaysPicker.DAYS;

    private TeacherAvailabilityDialog() {}

    /** What the dialog produces: comma-separated days plus a shared "HH:mm" start/end (blank = not set). */
    public record Result(String workingDays, String workStartTime, String workEndTime) {}

    public static Optional<Result> show(Window owner, String currentDaysCsv, String currentStart, String currentEnd) {
        Set<String> workingDays = new LinkedHashSet<>();
        if (currentDaysCsv != null) {
            for (String d : currentDaysCsv.split(",")) {
                String trimmed = d.trim();
                if (DAYS.contains(trimmed)) workingDays.add(trimmed);
            }
        }
        int startMinutes = toMinutes(currentStart);
        int endMinutes = toMinutes(currentEnd);
        Integer[] range = new Integer[2]; // [startBlock, endBlock], both null = no window set
        if (startMinutes >= 0 && endMinutes >= 0 && endMinutes > startMinutes) {
            range[0] = blockIndexForStart(startMinutes);
            range[1] = blockIndexForEnd(endMinutes);
        }
        boolean[] rangeComplete = {true}; // a loaded range is already "complete"

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle("Jours et horaires de travail");
        dialog.setResizable(false);

        Label subtitle = new Label("Activez les jours travaillés, puis cliquez sur le créneau de début et celui de fin. L'horaire est identique pour chaque jour sélectionné.");
        subtitle.getStyleClass().add("timetable-subtitle");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(680);

        GridPane grid = new GridPane();
        grid.getStyleClass().add("timetable-grid");

        ColumnConstraints hourCol = new ColumnConstraints(72);
        grid.getColumnConstraints().add(hourCol);
        for (int i = 0; i < DAYS.size(); i++) {
            ColumnConstraints dayCol = new ColumnConstraints();
            dayCol.setHgrow(Priority.ALWAYS);
            dayCol.setMinWidth(88);
            grid.getColumnConstraints().add(dayCol);
        }

        Label[] dayHeaders = new Label[DAYS.size()];
        StackPane[][] cells = new StackPane[DAYS.size()][HOUR_BLOCKS.length];
        Label[][] checks = new Label[DAYS.size()][HOUR_BLOCKS.length];

        Runnable[] repaint = new Runnable[1];

        // Row 0: day header pills — click toggles that day on/off.
        grid.add(new StackPane(), 0, 0);
        for (int c = 0; c < DAYS.size(); c++) {
            String day = DAYS.get(c);
            Label header = new Label(day.toUpperCase());
            header.getStyleClass().add("timetable-day-header");
            header.setMaxWidth(Double.MAX_VALUE);
            header.setCursor(Cursor.HAND);
            dayHeaders[c] = header;
            header.setOnMouseClicked(ev -> {
                if (workingDays.contains(day)) {
                    workingDays.remove(day);
                } else {
                    workingDays.add(day);
                }
                repaint[0].run();
            });
            StackPane wrap = new StackPane(header);
            wrap.setPadding(new Insets(0, 4, 8, 4));
            GridPane.setHgrow(wrap, Priority.ALWAYS);
            grid.add(wrap, c + 1, 0);
        }

        int row = 1;
        for (int b = 0; b < HOUR_BLOCKS.length; b++) {
            if (b == LUNCH_BREAK_ROW_INDEX) {
                addLunchBreakRow(grid, row++);
            }
            int start = HOUR_BLOCKS[b][0];
            int end = HOUR_BLOCKS[b][1];
            int blockIndex = b;
            grid.add(hourLabel(start, end), 0, row);

            for (int c = 0; c < DAYS.size(); c++) {
                String day = DAYS.get(c);
                Label check = new Label("✓");
                check.getStyleClass().add("timetable-cell-check");
                check.setVisible(false);
                checks[c][blockIndex] = check;

                StackPane cell = new StackPane(check);
                cell.getStyleClass().add("timetable-cell");
                cell.setPrefSize(88, 40);
                cell.setCursor(Cursor.HAND);
                cells[c][blockIndex] = cell;

                cell.setOnMouseClicked(ev -> {
                    workingDays.add(day);
                    if (range[0] == null || rangeComplete[0]) {
                        range[0] = blockIndex;
                        range[1] = blockIndex;
                        rangeComplete[0] = false;
                    } else {
                        int a = range[0];
                        range[0] = Math.min(a, blockIndex);
                        range[1] = Math.max(a, blockIndex);
                        rangeComplete[0] = true;
                    }
                    repaint[0].run();
                });
                grid.add(cell, c + 1, row);
            }
            row++;
        }

        repaint[0] = () -> {
            for (int c = 0; c < DAYS.size(); c++) {
                boolean dayOn = workingDays.contains(DAYS.get(c));
                dayHeaders[c].getStyleClass().remove("timetable-day-header-active");
                if (dayOn) dayHeaders[c].getStyleClass().add("timetable-day-header-active");
                for (int b = 0; b < HOUR_BLOCKS.length; b++) {
                    boolean on = dayOn && range[0] != null
                            && b >= Math.min(range[0], range[1]) && b <= Math.max(range[0], range[1]);
                    cells[c][b].getStyleClass().remove("timetable-cell-selected");
                    if (on) cells[c][b].getStyleClass().add("timetable-cell-selected");
                    checks[c][b].setVisible(on);
                }
            }
        };
        repaint[0].run();

        VBox card = new VBox(grid);
        card.getStyleClass().add("timetable-card");

        Button clearAll = new Button("Tout effacer");
        clearAll.getStyleClass().add("secondary-button");
        clearAll.setOnAction(e -> {
            workingDays.clear();
            range[0] = null;
            range[1] = null;
            repaint[0].run();
        });

        Button cancel = new Button("Annuler");
        cancel.getStyleClass().add("secondary-button");

        Button ok = new Button("Valider");
        ok.getStyleClass().add("primary-button");

        Result[] result = new Result[1];
        ok.setOnAction(e -> {
            String daysCsv = String.join(",", DAYS.stream().filter(workingDays::contains).toList());
            String startStr = range[0] == null ? "" : fmt(HOUR_BLOCKS[Math.min(range[0], range[1])][0]);
            String endStr = range[0] == null ? "" : fmt(HOUR_BLOCKS[Math.max(range[0], range[1])][1]);
            result[0] = new Result(daysCsv, startStr, endStr);
            dialog.close();
        });
        cancel.setOnAction(e -> dialog.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox(8, clearAll, spacer, cancel, ok);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(16, subtitle, card, buttons);
        root.setPadding(new Insets(22));
        root.getStyleClass().add("timetable-root");

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                Objects.requireNonNull(TeacherAvailabilityDialog.class.getResource("/css/style.css")).toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();

        return Optional.ofNullable(result[0]);
    }

    private static void addLunchBreakRow(GridPane grid, int row) {
        Label label = new Label("Pause déjeuner");
        label.getStyleClass().add("timetable-break-label");
        StackPane band = new StackPane(label);
        band.getStyleClass().add("timetable-break-row");
        band.setPrefHeight(32);
        band.setMaxWidth(Double.MAX_VALUE);
        grid.add(band, 0, row, DAYS.size() + 1, 1);
        while (grid.getRowConstraints().size() <= row) {
            grid.getRowConstraints().add(new RowConstraints());
        }
        RowConstraints rc = new RowConstraints();
        rc.setMinHeight(32);
        rc.setPrefHeight(32);
        grid.getRowConstraints().set(row, rc);
    }

    private static VBox hourLabel(int startHour, int endHour) {
        Label start = new Label(startHour + "h");
        Label dash = new Label("—");
        Label end = new Label(endHour + "h");
        start.getStyleClass().add("timetable-hour-label");
        end.getStyleClass().add("timetable-hour-label");
        dash.getStyleClass().add("timetable-hour-dash");
        VBox box = new VBox(0, start, dash, end);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private static String fmt(int hour) {
        return String.format("%02d:00", hour);
    }

    private static int toMinutes(String hhmm) {
        if (hhmm == null || hhmm.isBlank()) return -1;
        try {
            String[] parts = hhmm.trim().split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return -1;
        }
    }

    private static int blockIndexForStart(int minutes) {
        for (int i = 0; i < HOUR_BLOCKS.length; i++) {
            if (minutes <= HOUR_BLOCKS[i][0] * 60) return i;
        }
        return HOUR_BLOCKS.length - 1;
    }

    private static int blockIndexForEnd(int minutes) {
        for (int i = HOUR_BLOCKS.length - 1; i >= 0; i--) {
            if (minutes >= HOUR_BLOCKS[i][1] * 60) return i;
        }
        return 0;
    }
}
