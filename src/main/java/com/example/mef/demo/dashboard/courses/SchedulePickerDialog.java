package com.example.mef.demo.dashboard.courses;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Modal dialog letting the user tick the days of the week a course runs on
 * and pick a start/end time for each ticked day. Produces (and parses back)
 * a compact string such as "Lundi 08:00-10:00; Mercredi 14:00-16:00" that is
 * stored verbatim in {@link com.example.mef.demo.Model.Course#getSchedule()}.
 */
public final class SchedulePickerDialog {

    private static final String[] DAYS = {
            "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"
    };
    private static final String ENTRY_SEPARATOR = "; ";
    private static final String RANGE_SEPARATOR = "-";

    private SchedulePickerDialog() {}

    /** Opens the picker pre-filled from {@code currentSchedule} and blocks until closed. */
    public static Optional<String> show(Window owner, String currentSchedule) {
        Map<String, String[]> initial = parse(currentSchedule);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle("Horaire du cours");
        dialog.setMinWidth(480);
        dialog.setResizable(false);

        List<String> slots = timeSlots();

        Map<String, CheckBox> dayChecks = new LinkedHashMap<>();
        Map<String, ComboBox<String>> startBoxes = new LinkedHashMap<>();
        Map<String, ComboBox<String>> endBoxes = new LinkedHashMap<>();

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.getStyleClass().add("form-grid");

        Label colStart = new Label("De");
        Label colEnd = new Label("À");
        colStart.setStyle("-fx-text-fill: -muted; -fx-font-size: 11px;");
        colEnd.setStyle("-fx-text-fill: -muted; -fx-font-size: 11px;");
        grid.add(colStart, 1, 0);
        grid.add(colEnd, 2, 0);

        for (int i = 0; i < DAYS.length; i++) {
            String day = DAYS[i];
            int row = i + 1;

            CheckBox check = new CheckBox(day);
            check.setPrefWidth(100);

            ComboBox<String> start = new ComboBox<>(FXCollections.observableArrayList(slots));
            ComboBox<String> end = new ComboBox<>(FXCollections.observableArrayList(slots));
            start.setMaxWidth(Double.MAX_VALUE);
            end.setMaxWidth(Double.MAX_VALUE);
            start.setDisable(true);
            end.setDisable(true);

            String[] existing = initial.get(day);
            if (existing != null) {
                check.setSelected(true);
                start.setValue(existing[0]);
                end.setValue(existing[1]);
            } else {
                start.setValue("08:00");
                end.setValue("09:00");
            }
            start.setDisable(!check.isSelected());
            end.setDisable(!check.isSelected());

            check.selectedProperty().addListener((obs, was, isNow) -> {
                start.setDisable(!isNow);
                end.setDisable(!isNow);
            });

            dayChecks.put(day, check);
            startBoxes.put(day, start);
            endBoxes.put(day, end);

            grid.add(check, 0, row);
            grid.add(start, 1, row);
            grid.add(end, 2, row);

            GridPane.setHgrow(start, Priority.ALWAYS);
            GridPane.setHgrow(end, Priority.ALWAYS);
        }

        Label error = new Label();
        error.setStyle("-fx-text-fill: -danger; -fx-font-size: 12px;");
        error.setWrapText(true);
        error.setManaged(false);
        error.setVisible(false);

        Button clearAll = new Button("Tout effacer");
        clearAll.getStyleClass().add("secondary-button");
        clearAll.setOnAction(e -> dayChecks.values().forEach(c -> c.setSelected(false)));

        Button cancel = new Button("Annuler");
        cancel.getStyleClass().add("secondary-button");

        Button ok = new Button("Valider");
        ok.getStyleClass().add("primary-button");

        final String[] result = new String[1];

        ok.setOnAction(e -> {
            List<String> entries = new ArrayList<>();
            for (String day : DAYS) {
                if (!dayChecks.get(day).isSelected()) {
                    continue;
                }
                String s = startBoxes.get(day).getValue();
                String en = endBoxes.get(day).getValue();
                if (s == null || en == null) {
                    error.setText("Choisissez une heure de début et de fin pour " + day + ".");
                    error.setManaged(true);
                    error.setVisible(true);
                    return;
                }
                if (slots.indexOf(en) <= slots.indexOf(s)) {
                    error.setText("Pour " + day + ", l'heure de fin doit être après l'heure de début.");
                    error.setManaged(true);
                    error.setVisible(true);
                    return;
                }
                entries.add(day + " " + s + RANGE_SEPARATOR + en);
            }
            result[0] = String.join(ENTRY_SEPARATOR, entries);
            dialog.close();
        });

        cancel.setOnAction(e -> dialog.close());

        HBox buttons = new HBox(8, clearAll, new Label(), cancel, ok);
        HBox.setHgrow(buttons.getChildren().get(1), Priority.ALWAYS);
        buttons.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Cochez les jours du cours et réglez l'horaire de chacun.");
        title.setWrapText(true);
        title.setStyle("-fx-text-fill: -muted; -fx-font-size: 12px;");

        VBox root = new VBox(14, title, grid, error, buttons);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                Objects.requireNonNull(SchedulePickerDialog.class.getResource("/css/style.css")).toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();

        return Optional.ofNullable(result[0]);
    }

    /** Half-hour slots from 07:00 to 20:00. */
    private static List<String> timeSlots() {
        List<String> slots = new ArrayList<>();
        for (int minutes = 7 * 60; minutes <= 20 * 60; minutes += 30) {
            slots.add(String.format("%02d:%02d", minutes / 60, minutes % 60));
        }
        return slots;
    }

    /** Parses "Lundi 08:00-10:00; Mercredi 14:00-16:00" into day -> [start, end]. */
    private static Map<String, String[]> parse(String schedule) {
        Map<String, String[]> map = new LinkedHashMap<>();
        if (schedule == null || schedule.isBlank()) {
            return map;
        }
        for (String rawEntry : schedule.split(";")) {
            String entry = rawEntry.trim();
            if (entry.isEmpty()) {
                continue;
            }
            int firstSpace = entry.indexOf(' ');
            if (firstSpace < 0) {
                continue;
            }
            String day = entry.substring(0, firstSpace).trim();
            String range = entry.substring(firstSpace + 1).trim();
            String[] parts = range.split(RANGE_SEPARATOR);
            if (parts.length != 2) {
                continue;
            }
            for (String candidate : DAYS) {
                if (candidate.equalsIgnoreCase(day)) {
                    map.put(candidate, new String[]{parts[0].trim(), parts[1].trim()});
                    break;
                }
            }
        }
        return map;
    }
}
