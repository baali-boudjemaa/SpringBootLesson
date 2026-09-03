package com.example.mef.demo.dashboard.courses;

import com.example.mef.demo.Model.Course;
import com.example.mef.demo.Model.Employee;
import com.example.mef.demo.dashboard.common.TimeSlots.TimeBlock;
import com.example.mef.demo.util.DialogUtil;
import com.example.mef.demo.util.I18n;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
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
 * Modal weekly timetable picker. Press a cell then drag to paint a contiguous
 * range (or click a day header to fill that column). Contiguous selected cells
 * on the same day are merged into one time range on save.
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
        return show(owner, currentSchedule, title, subtitleText, null, List.of(), blocks, Set.of());
    }

    /** Same as {@link #show(Window, String, String, String, List)} with weekly closure days locked. */
    public static Optional<String> show(Window owner, String currentSchedule, String title, String subtitleText,
                                        List<TimeBlock> blocks, Set<String> closedDays) {
        return show(owner, currentSchedule, title, subtitleText, null, List.of(), blocks, closedDays);
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
        return show(owner, currentSchedule, title, subtitleText, teacher, otherCourses, blocks, Set.of());
    }

    public static Optional<String> show(Window owner, String currentSchedule, String title, String subtitleText,
                                        Employee teacher, List<Course> otherCourses, List<TimeBlock> blocks,
                                        Set<String> closedDays) {
        Set<String> closed = canonicalClosedDays(closedDays);
        Map<String, Set<Integer>> selected = initialSelection(currentSchedule, blocks);
        for (String day : DAYS) {
            if (closed.contains(day)) {
                selected.get(day).clear();
            }
        }
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

        GridPane grid = buildGrid(selected, unavailable, occupied, blocks, closed);
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
        installPaintHandlers(scene, selected, unavailable, occupied);
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

    /** Click/drag handle stored on each selectable cell. */
    private record CellHandle(String day, int blockIndex, int startMinutes, int endMinutes,
                              StackPane cell, Label check, boolean closed) {}

    /** Builds the day-header + hour-row grid. Cells are painted via {@link #installPaintHandlers}. */
    private static GridPane buildGrid(Map<String, Set<Integer>> selected,
                                      Map<String, Set<Integer>> unavailable,
                                      Map<String, Set<Integer>> occupied,
                                      List<TimeBlock> blocks,
                                      Set<String> closedDays) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("timetable-grid");

        javafx.scene.layout.ColumnConstraints hourCol = new javafx.scene.layout.ColumnConstraints(78);
        grid.getColumnConstraints().add(hourCol);
        for (int i = 0; i < DAYS.length; i++) {
            javafx.scene.layout.ColumnConstraints dayCol = new javafx.scene.layout.ColumnConstraints();
            dayCol.setHgrow(Priority.ALWAYS);
            dayCol.setMinWidth(88);
            grid.getColumnConstraints().add(dayCol);
        }

        Map<String, List<CellHandle>> cellsByDay = new LinkedHashMap<>();
        for (String day : DAYS) {
            cellsByDay.put(day, new ArrayList<>());
        }

        // Row 0: day headers (click to fill / clear the whole day).
        grid.add(new StackPane(), 0, 0);
        for (int c = 0; c < DAYS.length; c++) {
            String day = DAYS[c];
            Label header = new Label(dayLabel(day).toUpperCase());
            header.getStyleClass().add("timetable-day-header");
            header.setMaxWidth(Double.MAX_VALUE);
            StackPane wrap = new StackPane(header);
            wrap.setPadding(new Insets(0, 4, 8, 4));
            GridPane.setHgrow(wrap, Priority.ALWAYS);
            if (closedDays.contains(day)) {
                header.getStyleClass().add("timetable-day-header-closed");
                Tooltip.install(header, new Tooltip(I18n.t("schedule.closed_day_tooltip", "تسجيل الحضور")));
            } else {
                header.setCursor(Cursor.HAND);
                Tooltip.install(header, new Tooltip(I18n.t("schedule.day_header_hint", "تسجيل الحضور")));
                header.setOnMouseClicked(ev -> toggleDayColumn(
                        day, cellsByDay.get(day), selected, unavailable, occupied));
            }
            grid.add(wrap, c + 1, 0);
        }

        int row = 1;
        for (int b = 0; b < blocks.size(); b++) {
            TimeBlock block = blocks.get(b);
            if (block.isBreak()) {
                addLunchBreakRow(grid, row++);
                continue;
            }

            grid.add(hourLabel(block.startMinutes(), block.endMinutes()), 0, row);
            RowConstraints hourRc = new RowConstraints(40);
            hourRc.setMinHeight(40);
            hourRc.setPrefHeight(40);
            hourRc.setMaxHeight(40);
            ensureRowConstraints(grid, row, hourRc);

            for (int c = 0; c < DAYS.length; c++) {
                String day = DAYS[c];
                int blockIndex = b;
                Label check = new Label("✓");
                check.getStyleClass().add("timetable-cell-check");
                check.setMouseTransparent(true);
                StackPane cell = new StackPane(check);
                cell.getStyleClass().add("timetable-cell");
                cell.setPrefSize(88, 40);
                cell.setMinHeight(40);
                cell.setMaxHeight(40);
                CellHandle handle = new CellHandle(day, blockIndex, block.startMinutes(), block.endMinutes(),
                        cell, check, closedDays.contains(day));
                cell.setUserData(handle);
                cellsByDay.get(day).add(handle);

                if (handle.closed()) {
                    cell.getStyleClass().add("timetable-cell-closed");
                    cell.setCursor(Cursor.DEFAULT);
                    check.setVisible(false);
                    Tooltip.install(cell, new Tooltip(I18n.t("schedule.closed_day_tooltip", "تسجيل الحضور")));
                    grid.add(cell, c + 1, row);
                    continue;
                }
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
                grid.add(cell, c + 1, row);
            }
            row++;
        }

        return grid;
    }

    /**
     * Paint-select: press a cell to turn it on or off, then drag to apply the same
     * action to every cell the pointer crosses. Avoids the old click-toggle gaps.
     */
    private static void installPaintHandlers(Scene scene, Map<String, Set<Integer>> selected,
                                             Map<String, Set<Integer>> unavailable,
                                             Map<String, Set<Integer>> occupied) {
        final boolean[] painting = {false};
        final boolean[] paintOn = {false};
        final boolean[] showedBlocker = {false};

        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, ev -> {
            if (!ev.isPrimaryButtonDown()) {
                return;
            }
            CellHandle handle = cellHandleFrom(ev);
            if (handle == null || handle.closed()) {
                return;
            }
            painting[0] = true;
            showedBlocker[0] = false;
            boolean currentlyOn = selected.get(handle.day()).contains(handle.blockIndex());
            paintOn[0] = !currentlyOn;
            applyPaint(handle, paintOn[0], selected, unavailable, occupied, true, showedBlocker);
        });
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, ev -> {
            if (!painting[0] || !ev.isPrimaryButtonDown()) {
                return;
            }
            CellHandle handle = cellHandleFrom(ev);
            if (handle == null || handle.closed()) {
                return;
            }
            applyPaint(handle, paintOn[0], selected, unavailable, occupied, false, showedBlocker);
        });
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, ev -> painting[0] = false);
    }

    private static CellHandle cellHandleFrom(MouseEvent ev) {
        Node n = ev.getPickResult().getIntersectedNode();
        while (n != null) {
            if (n.getUserData() instanceof CellHandle handle) {
                return handle;
            }
            n = n.getParent();
        }
        return null;
    }

    private static void applyPaint(CellHandle handle, boolean turnOn, Map<String, Set<Integer>> selected,
                                   Map<String, Set<Integer>> unavailable, Map<String, Set<Integer>> occupied,
                                   boolean showDialog, boolean[] showedBlocker) {
        Set<Integer> daySelection = selected.get(handle.day());
        if (!turnOn) {
            daySelection.remove(handle.blockIndex());
            handle.cell().getStyleClass().remove("timetable-cell-selected");
            handle.check().setVisible(false);
            return;
        }
        if (unavailable.getOrDefault(handle.day(), Set.of()).contains(handle.blockIndex())) {
            if (showDialog && !showedBlocker[0]) {
                showedBlocker[0] = true;
                DialogUtil.error(I18n.t("schedule.unavailable_title", "تسجيل الحضور"),
                        I18n.t("schedule.teacher_unavailable", "تسجيل الحضور") + " " + dayLabel(handle.day()) + " "
                                + fmtHour(handle.startMinutes()) + " - " + fmtHour(handle.endMinutes()) + ".");
            }
            return;
        }
        if (occupied.getOrDefault(handle.day(), Set.of()).contains(handle.blockIndex())) {
            if (showDialog && !showedBlocker[0]) {
                showedBlocker[0] = true;
                DialogUtil.error(I18n.t("schedule.conflict_title", "تسجيل الحضور"),
                        I18n.t("schedule.teacher_occupied", "تسجيل الحضور") + " " + dayLabel(handle.day()) + " "
                                + fmtHour(handle.startMinutes()) + " - " + fmtHour(handle.endMinutes()) + ".");
            }
            return;
        }
        daySelection.add(handle.blockIndex());
        if (!handle.cell().getStyleClass().contains("timetable-cell-selected")) {
            handle.cell().getStyleClass().add("timetable-cell-selected");
        }
        handle.check().setVisible(true);
    }

    private static void toggleDayColumn(String day, List<CellHandle> cells, Map<String, Set<Integer>> selected,
                                        Map<String, Set<Integer>> unavailable, Map<String, Set<Integer>> occupied) {
        if (cells == null || cells.isEmpty()) {
            return;
        }
        boolean turnOn = cells.stream().anyMatch(h -> !h.closed()
                && !selected.get(day).contains(h.blockIndex())
                && !unavailable.getOrDefault(day, Set.of()).contains(h.blockIndex())
                && !occupied.getOrDefault(day, Set.of()).contains(h.blockIndex()));
        boolean[] ignore = {true};
        for (CellHandle handle : cells) {
            if (handle.closed()) {
                continue;
            }
            applyPaint(handle, turnOn, selected, unavailable, occupied, false, ignore);
        }
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

    /** One compact range per row (8h–9h) so 9h is not shown as both the end of 8–9 and the start of 9–10. */
    private static Label hourLabel(int startMinutes, int endMinutes) {
        Label start = new Label(fmtHourLabel(startMinutes) + " – " + fmtHourLabel(endMinutes));
        start.getStyleClass().add("timetable-hour-label");
        start.setWrapText(false);
        start.setMinHeight(40);
        start.setPrefHeight(40);
        start.setMaxHeight(40);
        start.setMaxWidth(78);
        start.setAlignment(Pos.CENTER);
        return start;
    }

    private static String fmtHourLabel(int minutes) {
        return (minutes / 60) + "h" + (minutes % 60 == 0 ? "" : String.format("%02d", minutes % 60));
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

    private static Set<String> canonicalClosedDays(Set<String> raw) {
        Set<String> closed = new LinkedHashSet<>();
        if (raw == null || raw.isEmpty()) {
            return closed;
        }
        for (String day : DAYS) {
            for (String candidate : raw) {
                if (candidate != null && candidate.trim().equalsIgnoreCase(day)) {
                    closed.add(day);
                    break;
                }
            }
        }
        return closed;
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
