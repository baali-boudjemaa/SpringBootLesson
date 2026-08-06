package com.example.mef.demo.dashboard.courses;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
 * Modal "weekly timetable" picker: days across the top, one-hour rows down
 * the side (08h → 18h, with the 12h–14h lunch break shown as a non-clickable
 * band, same shape as the school's printable weekly planner). The user
 * clicks cells to toggle them on/off; contiguous ticked cells on the same
 * day are merged into a single time range on save.
 *
 * Produces (and parses back) the same compact string other pickers used,
 * e.g. "Lundi 08:00-10:00; Mercredi 14:00-16:00", stored verbatim in
 * {@link com.example.mef.demo.Model.Course#getSchedule()}.
 */
public final class SchedulePickerDialog {

    private static final String[] DAYS = {
            "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"
    };

    /** [startHour, endHour] blocks shown as rows, in display order (lunch break omitted here). */
    private static final int[][] HOUR_BLOCKS = {
            {8, 9}, {9, 10}, {10, 11}, {11, 12}, {14, 15}, {15, 16}, {16, 17}, {17, 18}
    };
    private static final int LUNCH_BREAK_ROW_INDEX = 4; // grid row inserted between block index 3 and 4

    private static final String ENTRY_SEPARATOR = "; ";
    private static final String RANGE_SEPARATOR = "-";

    private SchedulePickerDialog() {}

    /** Opens the picker pre-filled from {@code currentSchedule} and blocks until closed. */
    public static Optional<String> show(Window owner, String currentSchedule) {
        Map<String, Set<Integer>> selected = initialSelection(currentSchedule);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle("Horaire du cours");
        dialog.setResizable(false);

        Label subtitle = new Label("Cliquez sur les créneaux pour composer l'horaire du cours.");
        subtitle.getStyleClass().add("timetable-subtitle");
        subtitle.setWrapText(true);

        GridPane grid = buildGrid(selected);
        VBox card = new VBox(grid);
        card.getStyleClass().add("timetable-card");

        Button clearAll = new Button("Tout effacer");
        clearAll.getStyleClass().add("secondary-button");
        clearAll.setOnAction(e -> {
            selected.values().forEach(Set::clear);
            grid.lookupAll(".timetable-cell").forEach(n -> n.getStyleClass().remove("timetable-cell-selected"));
            grid.lookupAll(".timetable-cell-check").forEach(n -> n.setVisible(false));
        });

        Button cancel = new Button("Annuler");
        cancel.getStyleClass().add("secondary-button");

        Button ok = new Button("Valider");
        ok.getStyleClass().add("primary-button");

        final String[] result = new String[1];

        ok.setOnAction(e -> {
            result[0] = buildScheduleString(selected);
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

    private static javafx.scene.layout.Region spacer() {
        javafx.scene.layout.Region r = new javafx.scene.layout.Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    /** Builds the day-header + hour-row grid, wiring click handlers that flip {@code selected}. */
    private static GridPane buildGrid(Map<String, Set<Integer>> selected) {
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
            Label header = new Label(DAYS[c].toUpperCase());
            header.getStyleClass().add("timetable-day-header");
            header.setMaxWidth(Double.MAX_VALUE);
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
                cell.setOnMouseClicked(ev -> {
                    Set<Integer> daySelection = selected.get(day);
                    if (daySelection.contains(blockIndex)) {
                        daySelection.remove(blockIndex);
                        cell.getStyleClass().remove("timetable-cell-selected");
                        check.setVisible(false);
                    } else {
                        daySelection.add(blockIndex);
                        cell.getStyleClass().add("timetable-cell-selected");
                        check.setVisible(true);
                    }
                });
                grid.add(cell, c + 1, row);
            }
            row++;
        }

        return grid;
    }

    private static void addLunchBreakRow(GridPane grid, int row) {
        Label label = new Label("Pause déjeuner");
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

    /** Merges contiguous ticked hour blocks per day into "HH:mm-HH:mm" ranges and joins them. */
    private static String buildScheduleString(Map<String, Set<Integer>> selected) {
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
                        && HOUR_BLOCKS[endBlock][1] == HOUR_BLOCKS[indices.get(i + 1)][0]) {
                    endBlock = indices.get(++i);
                }
                int startHour = HOUR_BLOCKS[startBlock][0];
                int endHour = HOUR_BLOCKS[endBlock][1];
                entries.add(day + " " + fmt(startHour) + RANGE_SEPARATOR + fmt(endHour));
                i++;
            }
        }
        return String.join(ENTRY_SEPARATOR, entries);
    }

    private static String fmt(int hour) {
        return String.format("%02d:00", hour);
    }

    /** Parses an existing schedule string into the set of hour-block indices it covers, per day. */
    private static Map<String, Set<Integer>> initialSelection(String schedule) {
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

            for (int b = 0; b < HOUR_BLOCKS.length; b++) {
                int blockStart = HOUR_BLOCKS[b][0] * 60;
                int blockEnd = HOUR_BLOCKS[b][1] * 60;
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