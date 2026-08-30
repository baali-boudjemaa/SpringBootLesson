package com.example.mef.demo.dashboard.common;

import com.example.mef.demo.util.I18n;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Weekly occupancy editor/timetable.
 *
 * Features:
 * - Clickable days
 * - Individual start/end time for every day
 * - Visual weekly timetable
 * - Multiple time slots per day
 * - Add/remove slots
 *
 * Existing ClassroomsView compatibility methods:
 *
 * getNode()
 * clear()
 * setValue(...)
 * getValue()
 * getDays()
 * getEarliestStart()
 * getLatestEnd()
 *
 * New:
 *
 * getDailySchedules()
 */
public class WeeklyOccupancyGrid {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    private static final LocalTime DEFAULT_START =
            LocalTime.of(8, 0);

    private static final LocalTime DEFAULT_END =
            LocalTime.of(12, 0);

    private static final LocalTime GRID_START =
            LocalTime.of(7, 0);

    private static final LocalTime GRID_END =
            LocalTime.of(19, 0);

    private static final int SLOT_MINUTES = 30;

    private static final double DAY_WIDTH = 110;

    private final VBox root = new VBox(10);

    private final Map<DayOfWeek, List<TimeSlot>> schedules =
            new EnumMap<>(DayOfWeek.class);

    private final Map<DayOfWeek, ToggleButton> dayButtons =
            new EnumMap<>(DayOfWeek.class);

    private final Map<DayOfWeek, VBox> daySlotContainers =
            new EnumMap<>(DayOfWeek.class);

    private final GridPane timetableGrid = new GridPane();

    private final ScrollPane timetableScroll =
            new ScrollPane();

    private final Label selectedDayLabel =
            new Label();

    private DayOfWeek selectedDay =
            DayOfWeek.MONDAY;

    private final ComboBox<String> startTimeBox =
            new ComboBox<>();

    private final ComboBox<String> endTimeBox =
            new ComboBox<>();

    private final Button addSlotButton =
            new Button("＋ " + I18n.t("classroom.occupancy.add"));

    private final Button clearDayButton =
            new Button(I18n.t("classroom.occupancy.clear_day"));

    private final VBox editor =
            new VBox(8);

    private final List<String> timeValues =
            new ArrayList<>();

    /**
     * Creates the weekly occupancy editor.
     */
    public WeeklyOccupancyGrid() {

        for (DayOfWeek day : DayOfWeek.values()) {
            schedules.put(day, new ArrayList<>());
        }

        createTimeValues();
        buildUI();

        selectDay(DayOfWeek.MONDAY);
    }

    /**
     * Creates all available time values.
     */
    private void createTimeValues() {

        LocalTime time = LocalTime.of(6, 0);

        while (!time.isAfter(LocalTime.of(22, 0))) {

            timeValues.add(
                    time.format(TIME_FORMAT)
            );

            time = time.plusMinutes(30);
        }
    }

    /**
     * Builds the complete UI.
     */
    private void buildUI() {

        root.setPadding(new Insets(8));
        root.setSpacing(10);

        root.getStyleClass().add("weekly-occupancy");

        Label title =
                new Label(I18n.t("classroom.occupancy.title"));

        title.setStyle(
                "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #0F172A;"
        );

        /*
         * =========================================================
         * DAY SELECTOR
         * =========================================================
         */

        HBox daysBox =
                new HBox(6);

        daysBox.setAlignment(Pos.CENTER_LEFT);

        for (DayOfWeek day : DayOfWeek.values()) {

            ToggleButton button =
                    new ToggleButton(dayName(day));

            button.setMinWidth(DAY_WIDTH);

            button.setToggleGroup(null);

            button.setOnAction(event ->
                    selectDay(day)
            );

            button.getStyleClass()
                    .add("occupancy-day-button");

            dayButtons.put(day, button);

            daysBox.getChildren().add(button);
        }

        ScrollPane daysScroll =
                new ScrollPane(daysBox);

        daysScroll.setFitToHeight(true);
        daysScroll.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.AS_NEEDED
        );
        daysScroll.setVbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        daysScroll.setPrefHeight(48);
        daysScroll.getStyleClass().add("Occupation-hebdomadaire");
        daysScroll.getStyleClass().add("weekly-scroll");
        /*
         * =========================================================
         * DAY EDITOR
         * =========================================================
         */

        selectedDayLabel.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #334155;"
        );

        startTimeBox.setItems(
                FXCollections.observableArrayList(timeValues)
        );

        endTimeBox.setItems(
                FXCollections.observableArrayList(timeValues)
        );

        startTimeBox.setValue(
                DEFAULT_START.format(TIME_FORMAT)
        );

        endTimeBox.setValue(
                DEFAULT_END.format(TIME_FORMAT)
        );

        startTimeBox.setPrefWidth(100);
        endTimeBox.setPrefWidth(100);

        Label fromLabel =
                new Label(I18n.t("classroom.occupancy.from"));

        Label toLabel =
                new Label(I18n.t("classroom.occupancy.to"));

        addSlotButton.getStyleClass()
                .add("primary-button");

        clearDayButton.getStyleClass()
                .add("secondary-button");

        addSlotButton.setOnAction(
                event -> addCurrentSlot()
        );

        clearDayButton.setOnAction(
                event -> {

                    schedules.get(selectedDay)
                            .clear();

                    refreshDaySlots();
                    refreshTimetable();
                }
        );

        HBox timeEditor =
                new HBox(
                        8,
                        fromLabel,
                        startTimeBox,
                        toLabel,
                        endTimeBox,
                        addSlotButton,
                        clearDayButton
                );

        timeEditor.setAlignment(Pos.CENTER_LEFT);

        editor.getChildren().addAll(
                selectedDayLabel,
                timeEditor
        );
        ScrollPane editorScroll =
                new ScrollPane(editor);

        editorScroll.setFitToHeight(true);
        editorScroll.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.AS_NEEDED
        );
        editorScroll.setVbarPolicy(
                ScrollPane.ScrollBarPolicy.NEVER
        );

        editorScroll.setPrefHeight(60);
        editorScroll.getStyleClass().add("days-occupancy");
        editorScroll.getStyleClass().add("weekly-scroll");
        VBox timedaysEditor=new VBox(2,selectedDayLabel,editorScroll);

        /*
         * =========================================================
         * CURRENT DAY SLOTS
         * =========================================================
         */

        VBox slotsPanel =
                new VBox(5);

        Label slotsTitle =
                new Label(I18n.t("classroom.occupancy.slots"));

        slotsTitle.setStyle(
                "-fx-font-weight: bold;" +
                        "-fx-text-fill: #475569;"
        );

        slotsPanel.getChildren().add(slotsTitle);

        for (DayOfWeek day : DayOfWeek.values()) {

            VBox container =
                    new VBox(4);

            daySlotContainers.put(
                    day,
                    container
            );

            if (day != selectedDay) {
                container.setManaged(false);
                container.setVisible(false);
            }
            container.getStyleClass().add("slotsPanel");
            slotsPanel.getChildren().add(container);
            slotsPanel.getStyleClass().add("slotsPanel");
        }

        /*
         * =========================================================
         * WEEKLY TIMETABLE
         * =========================================================
         */

        buildTimetable();

        timetableScroll.setContent(
                timetableGrid
        );

        timetableScroll.setFitToHeight(true);

        timetableScroll.setHbarPolicy(
                ScrollPane.ScrollBarPolicy.AS_NEEDED
        );

        timetableScroll.setVbarPolicy(
                ScrollPane.ScrollBarPolicy.AS_NEEDED
        );

        timetableScroll.setPrefHeight(340);
        timetableScroll.getStyleClass().add("slotsPanel");
        timetableScroll.getStyleClass().add("weekly-scroll");
        /*
         * =========================================================
         * ROOT
         * =========================================================
         */

        root.getChildren().addAll(
                title,
                daysScroll,
                timedaysEditor,
                slotsPanel,
                timetableScroll
        );

        VBox.setVgrow(
                timetableScroll,
                Priority.ALWAYS
        );
    }

    /**
     * Selects a day.
     */
    private void selectDay(DayOfWeek day) {

        selectedDay = day;

        for (Map.Entry<DayOfWeek, ToggleButton> entry :
                dayButtons.entrySet()) {

            ToggleButton button =
                    entry.getValue();

            boolean selected =
                    entry.getKey() == day;

            button.setSelected(selected);

            if (selected) {
                button.setStyle(
                        "-fx-background-color: #2563EB;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-weight: bold;"
                );
            } else {
                button.setStyle(
                        "-fx-background-color: #E2E8F0;" +
                                "-fx-text-fill: #334155;"
                );
            }
        }

        selectedDayLabel.setText(
                I18n.t("classroom.occupancy.for_day").replace("{0}", dayName(day))
        );

        TimeSlot first =
                schedules.get(day).isEmpty()
                        ? null
                        : schedules.get(day).get(0);

        if (first != null) {

            startTimeBox.setValue(
                    first.start.format(TIME_FORMAT)
            );

            endTimeBox.setValue(
                    first.end.format(TIME_FORMAT)
            );

        } else {

            startTimeBox.setValue(
                    DEFAULT_START.format(TIME_FORMAT)
            );

            endTimeBox.setValue(
                    DEFAULT_END.format(TIME_FORMAT)
            );
        }

        refreshDaySlots();

        refreshTimetable();
    }

    /**
     * Adds a new time slot to the selected day.
     */
    private void addCurrentSlot() {

        LocalTime start =
                parseTime(startTimeBox.getValue());

        LocalTime end =
                parseTime(endTimeBox.getValue());

        if (start == null || end == null) {
            return;
        }

        if (!end.isAfter(start)) {
            showValidationMessage(
                    "L'heure de fin doit être après l'heure de début."
            );
            return;
        }

        TimeSlot newSlot =
                new TimeSlot(start, end);

        /*
         * Prevent overlapping slots.
         */
        for (TimeSlot existing :
                schedules.get(selectedDay)) {

            if (newSlot.overlaps(existing)) {

                showValidationMessage(
                        "Ce créneau chevauche un autre créneau."
                );

                return;
            }
        }

        schedules.get(selectedDay)
                .add(newSlot);

        schedules.get(selectedDay)
                .sort((a, b) ->
                        a.start.compareTo(b.start)
                );

        refreshDaySlots();
        refreshTimetable();
    }

    /**
     * Refreshes the list of slots for the currently selected day.
     */
    private void refreshDaySlots() {

        for (Map.Entry<DayOfWeek, VBox> entry :
                daySlotContainers.entrySet()) {

            DayOfWeek day =
                    entry.getKey();

            VBox container =
                    entry.getValue();

            container.getChildren().clear();

            if (day != selectedDay) {

                container.setVisible(false);
                container.setManaged(false);

                continue;
            }

            container.setVisible(true);
            container.setManaged(true);

            List<TimeSlot> slots =
                    schedules.get(day);

            if (slots.isEmpty()) {

                Label empty =
                        new Label(
                                I18n.t("classroom.occupancy.none")
                        );

                empty.setStyle(
                        "-fx-text-fill: #94A3B8;"
                );

                container.getChildren()
                        .add(empty);

                continue;
            }

            for (TimeSlot slot : slots) {

                Label time =
                        new Label(
                                slot.start.format(TIME_FORMAT)
                                        + " → "
                                        + slot.end.format(TIME_FORMAT)
                        );

                time.setStyle(
                        "-fx-font-weight: bold;" +
                                "-fx-text-fill: #1E40AF;"
                );

                Button remove =
                        new Button("×");

                remove.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-text-fill: #DC2626;" +
                                "-fx-font-weight: bold;"
                );

                remove.setOnAction(event -> {

                    schedules.get(day)
                            .remove(slot);

                    refreshDaySlots();
                    refreshTimetable();
                });

                HBox row =
                        new HBox(
                                10,
                                time,
                                remove
                        );

                row.setAlignment(
                        Pos.CENTER_LEFT
                );

                row.setPadding(
                        new Insets(5, 10, 5, 10)
                );

                row.setStyle(
                        "-fx-background-color: #EFF6FF;" +
                                "-fx-background-radius: 6;"
                );

                container.getChildren()
                        .add(row);
            }
        }
    }

    /**
     * Builds the weekly visual timetable.
     */
    private void buildTimetable() {

        timetableGrid.getChildren().clear();

        timetableGrid.setHgap(1);
        timetableGrid.setVgap(1);

        /*
         * Header.
         */

        StackPane emptyHeader =
                timetableCell("", 70, 35);

        timetableGrid.add(
                emptyHeader,
                0,
                0
        );

        int dayColumn = 1;

        for (DayOfWeek day :
                DayOfWeek.values()) {

            StackPane header =
                    timetableCell(
                            dayName(day),
                            DAY_WIDTH,
                            35
                    );

            header.setStyle(
                    "-fx-background-color: #1E293B;" +
                            "-fx-background-radius: 4;"
            );

            timetableGrid.add(
                    header,
                    dayColumn++,
                    0
            );
        }

        /*
         * Time rows.
         */

        int row = 1;

        LocalTime time =
                GRID_START;

        while (!time.isAfter(GRID_END)) {

            Label timeLabel =
                    new Label(
                            time.format(TIME_FORMAT)
                    );

            timeLabel.setPrefWidth(70);
            timeLabel.setAlignment(
                    Pos.TOP_RIGHT
            );

            timeLabel.setPadding(
                    new Insets(2, 6, 0, 0)
            );

            timeLabel.setStyle(
                    "-fx-text-fill: #64748B;" +
                            "-fx-font-size: 11px;"
            );

            timetableGrid.add(
                    timeLabel,
                    0,
                    row
            );

            int column = 1;

            for (DayOfWeek day :
                    DayOfWeek.values()) {

                StackPane cell =
                        timetableCell(
                                "",
                                DAY_WIDTH,
                                26
                        );

                cell.setStyle(
                        "-fx-background-color: #F8FAFC;" +
                                "-fx-border-color: #E2E8F0;" +
                                "-fx-border-width: 0 0 1 1;"
                );

                if (isOccupied(day, time)) {

                    cell.setStyle(
                            "-fx-background-color: #3B82F6;" +
                                    "-fx-background-radius: 2;"
                    );

                    Label occupied =
                            new Label("●");

                    occupied.setStyle(
                            "-fx-text-fill: white;" +
                                    "-fx-font-size: 9px;"
                    );

                    cell.getChildren()
                            .add(occupied);
                }

                timetableGrid.add(
                        cell,
                        column++,
                        row
                );
            }

            time =
                    time.plusMinutes(SLOT_MINUTES);

            row++;
        }
    }

    /**
     * Refreshes the timetable.
     */
    private void refreshTimetable() {

        buildTimetable();
    }

    /**
     * Checks whether a day is occupied at a given time.
     */
    private boolean isOccupied(
            DayOfWeek day,
            LocalTime time) {

        LocalTime next =
                time.plusMinutes(SLOT_MINUTES);

        for (TimeSlot slot :
                schedules.get(day)) {

            /*
             * The 30-minute visual cell is occupied
             * if it intersects the actual slot.
             */
            if (time.isBefore(slot.end)
                    && next.isAfter(slot.start)) {

                return true;
            }
        }

        return false;
    }

    /**
     * Creates a timetable cell.
     */
    private StackPane timetableCell(
            String text,
            double width,
            double height) {

        Label label =
                new Label(text);

        StackPane pane =
                new StackPane(label);

        pane.setPrefWidth(width);
        pane.setMinWidth(width);
        pane.setPrefHeight(height);

        pane.setAlignment(
                Pos.CENTER
        );

        return pane;
    }

    /**
     * Returns the JavaFX node.
     */
    public Node getNode() {
        return root;
    }

    /**
     * Clears all schedules.
     */
    public void clear() {

        for (List<TimeSlot> slots :
                schedules.values()) {

            slots.clear();
        }

        selectDay(DayOfWeek.MONDAY);
    }

    /**
     * Existing compatibility method.
     *
     * Loads the old classroom representation:
     *
     * attendanceDays
     * periodStartTime
     * periodEndTime
     *
     * All selected days receive the same time range.
     *
     * This keeps compatibility with your current Classroom model.
     */
    public void setValue(
            Object occupancySchedule,
            String attendanceDays,
            String periodStartTime,
            String periodEndTime) {

        clear();

        // New format: one or more concrete weekly slots, for example
        // "MONDAY 08:00-10:00; WEDNESDAY 14:00-16:00".  This is the
        // format generated from course schedules and preserves each period.
        if (loadStoredSlots(occupancySchedule)) {
            refreshDaySlots();
            refreshTimetable();
            return;
        }

        if (attendanceDays == null
                || attendanceDays.isEmpty()) {

            return;
        }

        LocalTime start =
                periodStartTime == null || periodStartTime.isBlank()
                        ? DEFAULT_START
                        : parseTime(periodStartTime);

        LocalTime end =
                periodEndTime == null || periodEndTime.isBlank()
                        ? DEFAULT_END
                        : parseTime(periodEndTime);

        for (String value : attendanceDays.split(",")) {

            DayOfWeek day =
                    convertDay(value.trim());

            if (day != null) {

                schedules.get(day)
                        .add(
                                new TimeSlot(
                                        start,
                                        end
                                )
                        );
            }
        }

        refreshDaySlots();
        refreshTimetable();
    }
    /**
     * Returns a simple value representing the complete schedule.
     *
     * This is retained for compatibility.
     */
    public Map<DayOfWeek, List<TimeSlot>>
    getDailySchedules() {

        Map<DayOfWeek, List<TimeSlot>> copy =
                new EnumMap<>(DayOfWeek.class);

        for (DayOfWeek day :
                DayOfWeek.values()) {

            copy.put(
                    day,
                    new ArrayList<>(
                            schedules.get(day)
                    )
            );
        }

        return copy;
    }

    /**
     * Compatibility method.
     *
     * Returns true when at least one day has an occupancy slot.
     */
    public Map<DayOfWeek, List<TimeSlot>> getValue() {

        return getDailySchedules();
    }

    /** Stable database representation of all individual slots. */
    public String getStorageSchedule() {
        return schedules.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream().map(slot -> entry.getKey().name() + " "
                        + slot.start.format(TIME_FORMAT) + "-" + slot.end.format(TIME_FORMAT)))
                .collect(Collectors.joining("; "));
    }

    /** Stable database representation of selected day names. */
    public String getStorageDays() {
        return getDays().stream().map(DayOfWeek::name).collect(Collectors.joining(","));
    }

    private boolean loadStoredSlots(Object occupancySchedule) {
        if (occupancySchedule == null) return false;
        String stored = occupancySchedule.toString();
        if (stored.isBlank() || stored.startsWith("{")) return false; // Legacy Map#toString format.

        boolean loaded = false;
        for (String raw : stored.split(";")) {
            String entry = raw.trim();
            int space = entry.indexOf(' ');
            if (space < 0) continue;
            DayOfWeek day = convertDay(entry.substring(0, space));
            String[] range = entry.substring(space + 1).trim().split("-");
            if (day == null || range.length != 2) continue;
            LocalTime start = parseTime(range[0].trim());
            LocalTime end = parseTime(range[1].trim());
            if (start == null || end == null || !end.isAfter(start)) continue;
            schedules.get(day).add(new TimeSlot(start, end));
            loaded = true;
        }
        return loaded;
    }

    /**
     * Returns all occupied days.
     */
    public List<DayOfWeek> getDays() {

        return schedules.entrySet()
                .stream()
                .filter(entry ->
                        !entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Returns the earliest start time among all days.
     */
    public LocalTime getEarliestStart() {

        return schedules.values()
                .stream()
                .flatMap(List::stream)
                .map(slot -> slot.start)
                .min(LocalTime::compareTo)
                .orElse(null);
    }

    /**
     * Returns the latest end time among all days.
     */
    public LocalTime getLatestEnd() {

        return schedules.values()
                .stream()
                .flatMap(List::stream)
                .map(slot -> slot.end)
                .max(LocalTime::compareTo)
                .orElse(null);
    }

    /**
     * Converts different day representations to DayOfWeek.
     */
    private DayOfWeek convertDay(Object value) {

        if (value == null) {
            return null;
        }

        if (value instanceof DayOfWeek) {
            return (DayOfWeek) value;
        }

        String text =
                value.toString()
                        .trim()
                        .toUpperCase();

        return switch (text) {

            case "MONDAY", "LUNDI", "1" ->
                    DayOfWeek.MONDAY;

            case "TUESDAY", "MARDI", "2" ->
                    DayOfWeek.TUESDAY;

            case "WEDNESDAY", "MERCREDI", "3" ->
                    DayOfWeek.WEDNESDAY;

            case "THURSDAY", "JEUDI", "4" ->
                    DayOfWeek.THURSDAY;

            case "FRIDAY", "VENDREDI", "5" ->
                    DayOfWeek.FRIDAY;

            case "SATURDAY", "SAMEDI", "6" ->
                    DayOfWeek.SATURDAY;

            case "SUNDAY", "DIMANCHE", "7" ->
                    DayOfWeek.SUNDAY;

            default ->
                    null;
        };
    }

    private String dayName(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> I18n.t("day.mon");
            case TUESDAY -> I18n.t("day.tue");
            case WEDNESDAY -> I18n.t("day.wed");
            case THURSDAY -> I18n.t("day.thu");
            case FRIDAY -> I18n.t("day.fri");
            case SATURDAY -> I18n.t("day.sat");
            case SUNDAY -> I18n.t("day.sun");
        };
    }

    private LocalTime parseTime(
            String value) {

        if (value == null
                || value.isBlank()) {

            return null;
        }

        return LocalTime.parse(
                value,
                TIME_FORMAT
        );
    }

    /**
     * Small validation dialog.
     *
     * Replace this with DialogUtil if you prefer.
     */
    private void showValidationMessage(
            String message) {

        javafx.scene.control.Alert alert =
                new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.WARNING
                );

        alert.setTitle(
                I18n.t("classroom.occupancy.title")
        );

        alert.setHeaderText(null);

        alert.setContentText(message);

        alert.showAndWait();
    }

    /**
     * Represents one occupied time interval.
     */
    public static final class TimeSlot {

        private final LocalTime start;
        private final LocalTime end;

        public TimeSlot(
                LocalTime start,
                LocalTime end) {

            if (start == null
                    || end == null) {

                throw new IllegalArgumentException(
                        "Les heures sont obligatoires."
                );
            }

            if (!end.isAfter(start)) {

                throw new IllegalArgumentException(
                        "L'heure de fin doit être après l'heure de début."
                );
            }

            this.start = start;
            this.end = end;
        }

        public LocalTime getStart() {
            return start;
        }

        public LocalTime getEnd() {
            return end;
        }

        public boolean overlaps(
                TimeSlot other) {

            Objects.requireNonNull(other);

            return start.isBefore(other.end)
                    && end.isAfter(other.start);
        }

        @Override
        public String toString() {

            return start.format(TIME_FORMAT)
                    + " → "
                    + end.format(TIME_FORMAT);
        }
    }
}
