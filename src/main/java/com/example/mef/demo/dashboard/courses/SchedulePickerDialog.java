package com.example.mef.demo.dashboard.courses;

import com.example.mef.demo.Model.Course;
import com.example.mef.demo.Model.Employee;
import com.example.mef.demo.dashboard.common.TimeSlots.TimeBlock;
import com.example.mef.demo.util.DialogUtil;
import com.example.mef.demo.util.I18n;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Modal "weekly timetable" picker: days across the top, dynamic rows down
 * the side. The user clicks cells to toggle them on/off; contiguous ticked 
 * cells on the same day are merged into a single time range on save.
 *
 * Produces (and parses back) the same compact string other pickers used,
 * e.g. "Lundi 08:00-10:00; Mercredi 14:00-16:00", stored verbatim in
 * {@link com.example.mef.demo.Model.Course#getSchedule()}.
 */
public final class SchedulePickerDialog {

    private static final String[] DAYS = {
            "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"
    };

    private static final String ENTRY_SEPARATOR = "; ";
    private static final String RANGE_SEPARATOR = "-";

    private SchedulePickerDialog() {}

    /** Opens the picker pre-filled from {@code currentSchedule} and blocks until closed. */
    public static Optional<String> show(Window owner, String currentSchedule, List<TimeBlock> blocks) {
        return show(owner, currentSchedule, I18n.t("schedule.title", "تسجيل الحضور"), I18n.t("schedule.hint", "تسجيل الحضور"), blocks);
    }

    /** Opens the same individual-cell picker with caller-provided teacher/course wording. */
    public static Optional<String> show(Window owner, String currentSchedule, String title, String subtitleText, List<TimeBlock> blocks) {
        return show(owner, currentSchedule, title, subtitleText, null, List.of(), blocks);
    }

    /**
     * Same picker, additionally highlighting the hours unavailable for {@code teacher}: red for
     * hours outside their declared availability, yellow for hours already booked on another of
     * their courses ({@code otherCourses}, which the caller should exclude the course being
     * edited from). Purely a visual aid — cells stay clickable; {@link ScheduleValidator#validate}
     * on save is still the authoritative check.
     */
    public static Optional<String> show(Window owner, String currentSchedule, String title, String subtitleText,
                                        Employee teacher, List<Course> otherCourses, List<TimeBlock> blocks) {
        Map<String, Set<Integer>> selected = initialSelection(currentSchedule, blocks);
        Map<String, Set<Integer>> unavailable = computeUnavailableBlocks(teacher, blocks);
        Map<String, Set<Integer>> occupied = computeOccupiedBlocks(teacher, otherCourses, unavailable, blocks);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle(title);
        dialog.setResizable(false);

        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("timetable-subtitle");
        subtitle.setWrapText(true);

        GridPane grid = buildGrid(selected, unavailable, occupied, blocks);
        VBox card = new VBox(grid);
        card.getStyleClass().add("timetable-card");

        Button clearAll = new Button(I18n.t("action.clear", "تسجيل الحضور"));
        clearAll.getStyleClass().add("secondary-button");
        clearAll.setOnAction(e -> {
            selected.values().forEach(Set::clear);
            grid.lookupAll(".timetable-cell").forEach(n -> n.getStyleClass().remove("timetable-cell-selected"));
            grid.lookupAll(".timetable-cell-check").forEach(n -> n.setVisible(false));
        });

        Button cancel = new Button(I18n.t("action.cancel", "تسجيل الحضور"));
        cancel.getStyleClass().add("secondary-button");

        Button ok = new Button(I18n.t("action.save", "تسجيل الحضور"));
        ok.getStyleClass().add("primary-button");

        final String[] result = new String[1];

        ok.setOnAction(e -> {
            result[0] = buildScheduleString(selected, blocks);
            dialog.close();
        });
        cancel.setOnAction(e -> dialog.close());

        HBox buttons = new HBox(8, clearAll, spacer(), cancel, ok);
        buttons.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(16, subtitle, card, buttons);
        root.setPadding(new Insets(22));
        root.getStyleClass().add("timetable-root");

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                Objects.requireNonNull(SchedulePickerDialog.class.getResource("/css/style.css")).toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();

        return Optional.ofNullable(result[0]);
    }

    /** Red cells: outside the teacher's declared availability. */
    private static Map<String, Set<Integer>> computeUnavailableBlocks(Employee teacher, List<TimeBlock> blocks) {
        Map<String, Set<Integer>> unavailable = new LinkedHashMap<>();
        for (String day : DAYS) {
            unavailable.put(day, new LinkedHashSet<>());
        }
        if (teacher == null) {
            return unavailable;
        }

        for (String day : DAYS) {
            for (int b = 0; b < blocks.size(); b++) {
                if (blocks.get(b).isBreak()) continue;
                int blockStart = blocks.get(b).startMinutes();
                int blockEnd = blocks.get(b).endMinutes();
                ScheduleValidator.Slot blockSlot = new ScheduleValidator.Slot(day, blockStart, blockEnd);
                if (ScheduleValidator.isOutsideAvailability(teacher, blockSlot)) {
                    unavailable.get(day).add(b);
                }
            }
        }
        return unavailable;
    }

    /**
     * Yellow cells: already booked on another of the teacher's courses. Skips any block already
     * flagged red by {@code unavailable}, so a cell only ever shows one color.
     */
    private static Map<String, Set<Integer>> computeOccupiedBlocks(Employee teacher, List<Course> otherCourses,
                                                                   Map<String, Set<Integer>> unavailable, List<TimeBlock> blocks) {
        Map<String, Set<Integer>> occupied = new LinkedHashMap<>();
        for (String day : DAYS) {
            occupied.put(day, new LinkedHashSet<>());
        }
        if (teacher == null) {
            return occupied;
        }

        for (String day : DAYS) {
            for (int b = 0; b < blocks.size(); b++) {
                if (blocks.get(b).isBreak()) continue;
                if (unavailable.getOrDefault(day, Set.of()).contains(b)) {
                    continue;
                }
                int blockStart = blocks.get(b).startMinutes();
                int blockEnd = blocks.get(b).endMinutes();

                for (Course other : otherCourses) {
                    if (other.getTeacher() == null || teacher.getId() == null
                            || !teacher.getId().equals(other.getTeacher().getId())) {
                        continue;
                    }
                    boolean overlaps = ScheduleValidator.slotsOf(other).stream()
                            .anyMatch(os -> os.day().equals(day)
                                    && blockStart < os.endMinutes() && os.startMinutes() < blockEnd);
                    if (overlaps) {
                        occupied.get(day).add(b);
                        break;
                    }
                }
            }
        }
        return occupied;
    }

    private static javafx.scene.layout.Region spacer() {
        javafx.scene.layout.Region r = new javafx.scene.layout.Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    /** Builds the day-header + hour-row grid, wiring click handlers that flip {@code selected}. */
    private static GridPane buildGrid(Map<String, Set<Integer>> selected,
                                      Map<String, Set<Integer>> unavailable,
                                      Map<String, Set<Integer>> occupied,
                                      List<TimeBlock> blocks) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("timetable-grid");

        javafx.scene.layout.ColumnConstraints hourCol = new javafx.scene.layout.ColumnConstraints(72);
        grid.getColumnConstraints().add(hourCol);
        for (int i = 0; i < DAYS.length; i++) {
            javafx.scene.layout.ColumnConstraints dayCol = new javafx.scene.layout.ColumnConstraints();
            dayCol.setHgrow(Priority.ALWAYS);
            dayCol.setMinWidth(88);
            grid.getColumnConstraints().add(dayCol);
        }

        // Row 0: day headers.
        grid.add(new StackPane(), 0, 0);
        for (int c = 0; c < DAYS.length; c++) {
            Label header = new Label(dayLabel(DAYS[c]).toUpperCase());
            header.getStyleClass().add("timetable-day-header");
            header.setMaxWidth(Double.MAX_VALUE);
            StackPane wrap = new StackPane(header);
            wrap.setPadding(new Insets(0, 4, 8, 4));
            GridPane.setHgrow(wrap, Priority.ALWAYS);
            grid.add(wrap, c + 1, 0);
        }

        int row = 1;
        for (int b = 0; b < blocks.size(); b++) {
            TimeBlock block = blocks.get(b);
            if (block.isBreak()) {
                addLunchBreakRow(grid, row++);
                continue;
            }

            int start = block.startMinutes();
            int end = block.endMinutes();
            grid.add(hourLabel(start, end), 0, row);

            for (int c = 0; c < DAYS.length; c++) {
                String day = DAYS[c];
                int blockIndex = b;
                Label check = new Label("✓");
                check.getStyleClass().add("timetable-cell-check");
                StackPane cell = new StackPane(check);
                cell.getStyleClass().add("timetable-cell");
                cell.setPrefSize(88, 40);
                cell.setCursor(Cursor.HAND);
                boolean isSelected = selected.get(day).contains(blockIndex);
                check.setVisible(isSelected);
                if (isSelected) {
                    cell.getStyleClass().add("timetable-cell-selected");
                }
                if (unavailable.getOrDefault(day, Set.of()).contains(blockIndex)) {
                    cell.getStyleClass().add("timetable-cell-unavailable");
                    Tooltip.install(cell, new Tooltip(I18n.t("schedule.teacher_unavailable", "تسجيل الحضور")));
                } else if (occupied.getOrDefault(day, Set.of()).contains(blockIndex)) {
                    cell.getStyleClass().add("timetable-cell-occupied");
                    Tooltip.install(cell, new Tooltip(I18n.t("schedule.teacher_occupied", "تسجيل الحضور")));
                }
                cell.setOnMouseClicked(ev -> {
                    Set<Integer> daySelection = selected.get(day);
                    if (daySelection.contains(blockIndex)) {
                        daySelection.remove(blockIndex);
                        cell.getStyleClass().remove("timetable-cell-selected");
                        check.setVisible(false);
                        return;
                    }
                    if (unavailable.getOrDefault(day, Set.of()).contains(blockIndex)) {
                        DialogUtil.error(I18n.t("schedule.unavailable_title", "تسجيل الحضور"),
                                I18n.t("schedule.teacher_unavailable", "تسجيل الحضور") + " " + dayLabel(day) + " "
                                        + fmtHour(block.startMinutes()) + " - " + fmtHour(block.endMinutes()) + ".");
                        return;
                    }
                    if (occupied.getOrDefault(day, Set.of()).contains(blockIndex)) {
                        DialogUtil.error(I18n.t("schedule.conflict_title", "تسجيل الحضور"),
                                I18n.t("schedule.teacher_occupied", "تسجيل الحضور") + " " + dayLabel(day) + " "
                                        + fmtHour(block.startMinutes()) + " - " + fmtHour(block.endMinutes()) + ".");
                        return;
                    }
                    daySelection.add(blockIndex);
                    cell.getStyleClass().add("timetable-cell-selected");
                    check.setVisible(true);
                });
                grid.add(cell, c + 1, row);
            }
            row++;
        }

        return grid;
    }

    private static void addLunchBreakRow(GridPane grid, int row) {
        Label label = new Label(I18n.t("schedule.lunch_break", "تسجيل الحضور"));
        label.getStyleClass().add("timetable-break-label");
        StackPane band = new StackPane(label);
        band.getStyleClass().add("timetable-break-row");
        band.setPrefHeight(32);
        band.setMaxWidth(Double.MAX_VALUE);
        grid.add(band, 0, row, DAYS.length + 1, 1);
        RowConstraints rc = new RowConstraints();
        rc.setMinHeight(32);
        rc.setPrefHeight(32);
        ensureRowConstraints(grid, row, rc);
    }

    private static void ensureRowConstraints(GridPane grid, int row, RowConstraints rc) {
        while (grid.getRowConstraints().size() <= row) {
            grid.getRowConstraints().add(new RowConstraints());
        }
        grid.getRowConstraints().set(row, rc);
    }

    private static VBox hourLabel(int startMinutes, int endMinutes) {
        String startText = (startMinutes / 60) + "h" + (startMinutes % 60 == 0 ? "" : String.format("%02d", startMinutes % 60));
        String endText = (endMinutes / 60) + "h" + (endMinutes % 60 == 0 ? "" : String.format("%02d", endMinutes % 60));
        Label start = new Label(startText);
        Label dash = new Label("—");
        Label end = new Label(endText);
        start.getStyleClass().add("timetable-hour-label");
        end.getStyleClass().add("timetable-hour-label");
        dash.getStyleClass().add("timetable-hour-dash");
        VBox box = new VBox(0, start, dash, end);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    /** Merges contiguous ticked hour blocks per day into "HH:mm-HH:mm" ranges and joins them. */
    private static String buildScheduleString(Map<String, Set<Integer>> selected, List<TimeBlock> blocks) {
        List<String> entries = new ArrayList<>();
        for (String day : DAYS) {
            List<Integer> indices = new ArrayList<>(selected.get(day));
            indices.sort(Integer::compareTo);

            int i = 0;
            while (i < indices.size()) {
                int startBlock = indices.get(i);
                int endBlock = startBlock;
                while (i + 1 < indices.size()
                        && indices.get(i + 1) == endBlock + 1
                        && !blocks.get(endBlock + 1).isBreak()
                        && blocks.get(endBlock).endMinutes() == blocks.get(indices.get(i + 1)).startMinutes()) {
                    endBlock = indices.get(++i);
                }
                int startMins = blocks.get(startBlock).startMinutes();
                int endMins = blocks.get(endBlock).endMinutes();
                entries.add(day + " " + fmtMinutes(startMins) + RANGE_SEPARATOR + fmtMinutes(endMins));
                i++;
            }
        }
        return String.join(ENTRY_SEPARATOR, entries);
    }

    private static String fmtHour(int minutes) {
        return (minutes / 60) + "h" + (minutes % 60 == 0 ? "00" : String.format("%02d", minutes % 60));
    }

    private static String fmtMinutes(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }

    /** Schedule values retain their French day names for backward compatibility; only the UI is translated. */
    private static String dayLabel(String day) {
        return I18n.t("schedule.day." + day.toLowerCase(), "تسجيل الحضور");
    }

    /** Parses an existing schedule string into the set of hour-block indices it covers, per day. */
    private static Map<String, Set<Integer>> initialSelection(String schedule, List<TimeBlock> blocks) {
        Map<String, Set<Integer>> map = new LinkedHashMap<>();
        for (String day : DAYS) {
            map.put(day, new LinkedHashSet<>());
        }
        if (schedule == null || schedule.isBlank()) {
            return map;
        }
        for (String rawEntry : schedule.split(";")) {
            String entry = rawEntry.trim();
            if (entry.isEmpty()) continue;

            int firstSpace = entry.indexOf(' ');
            if (firstSpace < 0) continue;

            String day = entry.substring(0, firstSpace).trim();
            String range = entry.substring(firstSpace + 1).trim();
            String[] parts = range.split(RANGE_SEPARATOR);
            if (parts.length != 2) continue;

            int start = toMinutes(parts[0].trim());
            int end = toMinutes(parts[1].trim());
            if (start < 0 || end < 0) continue;

            String canonicalDay = null;
            for (String candidate : DAYS) {
                if (candidate.equalsIgnoreCase(day)) {
                    canonicalDay = candidate;
                    break;
                }
            }
            if (canonicalDay == null) continue;

            for (int b = 0; b < blocks.size(); b++) {
                if (blocks.get(b).isBreak()) continue;
                int blockStart = blocks.get(b).startMinutes();
                int blockEnd = blocks.get(b).endMinutes();
                boolean overlaps = start < blockEnd && blockStart < end;
                if (overlaps) {
                    map.get(canonicalDay).add(b);
                }
            }
        }
        return map;
    }

    private static int toMinutes(String hhmm) {
        if (hhmm == null || hhmm.isBlank()) {
            return -1;
        }
        try {
            String[] parts = hhmm.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return -1;
        }
    }
}
