package com.example.mef.demo.dashboard.common;

import javafx.scene.control.CheckBox;
import javafx.scene.layout.FlowPane;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A row of day-of-week checkboxes (Lundi → Dimanche) used wherever a
 * "which days" preference needs to be captured: teacher working days,
 * class attendance days, weekly school-closure days, etc.
 *
 * Value is stored/read as a comma-separated string (e.g. "Lundi,Mercredi").
 * An empty value means "no restriction" / "not configured" — every
 * checkbox unchecked.
 */
public final class DaysPicker {

    public static final List<String> DAYS = List.of(
            "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"
    );

    private final Map<String, CheckBox> checks = new LinkedHashMap<>();
    private final FlowPane pane = new FlowPane(10, 4);

    public DaysPicker() {
        for (String day : DAYS) {
            CheckBox box = new CheckBox(day);
            checks.put(day, box);
            pane.getChildren().add(box);
        }
    }

    public FlowPane getNode() {
        return pane;
    }

    /** Comma-separated selected days, e.g. "Lundi,Mercredi", or "" if none selected. */
    public String getValue() {
        return checks.entrySet().stream()
                .filter(e -> e.getValue().isSelected())
                .map(Map.Entry::getKey)
                .collect(Collectors.joining(","));
    }

    public void setValue(String commaSeparatedDays) {
        checks.values().forEach(cb -> cb.setSelected(false));
        if (commaSeparatedDays == null || commaSeparatedDays.isBlank()) {
            return;
        }
        for (String day : commaSeparatedDays.split(",")) {
            CheckBox box = checks.get(day.trim());
            if (box != null) {
                box.setSelected(true);
            }
        }
    }

    public void clear() {
        checks.values().forEach(cb -> cb.setSelected(false));
    }
}