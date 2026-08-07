package com.example.mef.demo.dashboard.common;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Compact clickable timetable used to define when a class is occupied. */
public final class WeeklyOccupancyGrid {

    private static final String[] DAYS = {"Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"};
    private static final int[][] BLOCKS = {{7, 9}, {9, 11}, {11, 13}, {13, 15}, {15, 17}, {17, 19}};

    private final Map<String, Set<Integer>> selected = new LinkedHashMap<>();
    private final GridPane grid = new GridPane();
    private final Map<String, StackPane> cells = new LinkedHashMap<>();

    public WeeklyOccupancyGrid() {
        for (String day : DAYS) selected.put(day, new LinkedHashSet<>());
        buildGrid();
        refresh(); // ensure the grid reflects the (empty) initial state before it's ever shown
    }

    public GridPane getNode() {
        return grid;
    }

    public void clear() {
        selected.values().forEach(Set::clear);
        refresh();
    }

    /** Reads the new schedule string, or converts legacy days + shared daily period when needed. */
    public void setValue(String schedule, String legacyDays, String legacyStart, String legacyEnd) {
        clear();
        if (schedule != null && !schedule.isBlank()) {
            parseSchedule(schedule);
        } else {
            applyLegacySchedule(legacyDays, legacyStart, legacyEnd);
        }
        refresh();
    }

    /** e.g. "Lundi 07:00-09:00; Mardi 09:00-11:00". */
    public String getValue() {
        List<String> entries = new ArrayList<>();
        for (String day : DAYS) {
            for (Integer block : selected.get(day)) {
                entries.add(day + " " + format(BLOCKS[block][0]) + "-" + format(BLOCKS[block][1]));
            }
        }
        return String.join("; ", entries);
    }

    /** Legacy compatibility: all days that have at least one selected block. */
    public String getDays() {
        List<String> days = new ArrayList<>();
        for (String day : DAYS) if (!selected.get(day).isEmpty()) days.add(day);
        return String.join(",", days);
    }

    /** Legacy compatibility: earliest occupied time across the week. */
    public String getEarliestStart() {
        return selected.values().stream().flatMap(Set::stream).mapToInt(index -> BLOCKS[index][0]).min()
                .isPresent() ? format(selected.values().stream().flatMap(Set::stream).mapToInt(index -> BLOCKS[index][0]).min().orElseThrow()) : null;
    }

    /** Legacy compatibility: latest occupied time across the week. */
    public String getLatestEnd() {
        return selected.values().stream().flatMap(Set::stream).mapToInt(index -> BLOCKS[index][1]).max()
                .isPresent() ? format(selected.values().stream().flatMap(Set::stream).mapToInt(index -> BLOCKS[index][1]).max().orElseThrow()) : null;
    }

    private void buildGrid() {
        grid.getStyleClass().add("class-occupancy-grid");
        ColumnConstraints timeColumn = new ColumnConstraints(44);
        grid.getColumnConstraints().add(timeColumn);
        for (int i = 0; i < DAYS.length; i++) {
            ColumnConstraints dayColumn = new ColumnConstraints(35);
            dayColumn.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(dayColumn);
        }

        for (int column = 0; column < DAYS.length; column++) {
            Label header = new Label(DAYS[column].substring(0, 3));
            header.getStyleClass().add("class-occupancy-day");
            header.setMaxWidth(Double.MAX_VALUE);
            header.setAlignment(Pos.CENTER);
            grid.add(header, column + 1, 0);
        }

        for (int row = 0; row < BLOCKS.length; row++) {
            Label time = new Label(BLOCKS[row][0] + "–" + BLOCKS[row][1] + "h");
            time.getStyleClass().add("class-occupancy-time");
            grid.add(time, 0, row + 1);
            for (int column = 0; column < DAYS.length; column++) {
                String day = DAYS[column];
                int block = row;
                Label mark = new Label("✓");
                mark.getStyleClass().add("class-occupancy-mark");
                mark.setVisible(false); // hidden until refresh() marks it as actually selected
                StackPane cell = new StackPane(mark);
                cell.getStyleClass().add("class-occupancy-cell");
                cell.setPrefHeight(29);
                cell.setCursor(Cursor.HAND);
                cell.setOnMouseClicked(event -> {
                    Set<Integer> daySelection = selected.get(day);
                    if (daySelection.contains(block)) daySelection.remove(block); else daySelection.add(block);
                    refresh();
                });
                cells.put(day + ":" + block, cell);
                grid.add(cell, column + 1, row + 1);
            }
        }
    }

    private void refresh() {
        cells.forEach((key, cell) -> {
            String[] parts = key.split(":");
            boolean occupied = selected.get(parts[0]).contains(Integer.parseInt(parts[1]));
            if (occupied && !cell.getStyleClass().contains("class-occupancy-cell-selected")) {
                cell.getStyleClass().add("class-occupancy-cell-selected");
            }
            if (!occupied) cell.getStyleClass().remove("class-occupancy-cell-selected");
            ((Label) cell.getChildren().getFirst()).setVisible(occupied);
        });
    }

    private void parseSchedule(String schedule) {
        for (String raw : schedule.split(";")) {
            String entry = raw.trim();
            int split = entry.indexOf(' ');
            if (split < 0) continue;
            String day = entry.substring(0, split);
            String[] range = entry.substring(split + 1).split("-");
            if (!selected.containsKey(day) || range.length != 2) continue;
            int start = TimeSlots.toMinutes(range[0]);
            int end = TimeSlots.toMinutes(range[1]);
            for (int index = 0; index < BLOCKS.length; index++) {
                if (start < BLOCKS[index][1] * 60 && BLOCKS[index][0] * 60 < end) selected.get(day).add(index);
            }
        }
    }

    private void applyLegacySchedule(String days, String startTime, String endTime) {
        if (days == null || days.isBlank()) return;
        int start = TimeSlots.toMinutes(startTime);
        int end = TimeSlots.toMinutes(endTime);
        if (start < 0 || end <= start) return;
        for (String day : days.split(",")) {
            Set<Integer> daySelection = selected.get(day.trim());
            if (daySelection == null) continue;
            for (int index = 0; index < BLOCKS.length; index++) {
                if (start < BLOCKS[index][1] * 60 && BLOCKS[index][0] * 60 < end) daySelection.add(index);
            }
        }
    }

    private String format(int hour) {
        return String.format("%02d:00", hour);
    }
}