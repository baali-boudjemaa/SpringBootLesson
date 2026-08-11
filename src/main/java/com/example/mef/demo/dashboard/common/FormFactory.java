package com.example.mef.demo.dashboard.common;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.time.LocalDate;
import java.util.List;

/**
 * Small reusable form-building helpers, extracted verbatim from
 * DashboardController (textField / comboBox / sectionGrid / addRow).
 * Used today by the student enrollment wizard and the classrooms page;
 * will also serve ModuleFormFactory.
 */
public final class FormFactory {

    private FormFactory() {
    }

    public static TextField textField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    public static ComboBox<String> comboBox(List<String> options) {
        ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList(options));
        cb.setMaxWidth(Double.MAX_VALUE);
        return cb;
    }

    public static GridPane sectionGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.getStyleClass().add("form-grid");
        ColumnConstraints label = new ColumnConstraints(120);
        ColumnConstraints input = new ColumnConstraints();
        // CRITICAL: give the field column a real floor width. Without a minWidth,
        // this column has nothing but Hgrow=ALWAYS to go on — inside a FloatingPanel's
        // ScrollPane(fitToWidth=true) sitting in a plain (non-managing) Pane overlay,
        // the very first layout pass can resolve the GridPane's available width to 0
        // before the panel's real prefWidth (380) is propagated down. When that
        // happens this column collapses to 0px and every editor (TextField, ComboBox,
        // DatePicker) in it renders with zero width — i.e. invisible, even though the
        // Label column next to it is fine. A concrete minWidth means the column can
        // never collapse below something usable, regardless of what the parent chain
        // reports on that first pass.
        input.setMinWidth(180);
        input.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(label, input);
        return grid;
    }
    public static void addRow(GridPane grid, int row, String label, Node editor) {
        grid.add(new Label(label), 0, row);
        grid.add(editor, 1, row);
        GridPane.setHgrow(editor, Priority.ALWAYS);
        // Belt-and-suspenders alongside the column's minWidth: make sure the editor
        // itself never reports a computed-size min narrower than the column floor,
        // for editor types (DatePicker in particular) that don't already get
        // setMaxWidth(Double.MAX_VALUE) from their call site.
        if (editor instanceof javafx.scene.control.Control control) {
            control.setMaxWidth(Double.MAX_VALUE);
        }
    }
    public static void addRow(GridPane grid, int row, String label) {
        grid.add(new Label(label), 0, row);
        grid.setMinWidth(200);
    }
    public static void addRow(GridPane grid, int row, Node editor) {
        grid.add(editor, 0, row);
        GridPane.setHgrow(editor, Priority.ALWAYS);
    }
    /**
     * Reads the current value of the given editor node, returning "" for
     * null/empty. Understands TextField and ComboBox<String>, the only two
     * editor types the app currently builds forms out of.
     */
    public static String getEditorValue(Node editor) {
        if (editor instanceof TextField tf) return tf.getText();
        if (editor instanceof ComboBox<?> cb) {
            Object v = cb.getValue();
            return v == null ? "" : v.toString();
        }
        if (editor instanceof DatePicker dp) {
            LocalDate value = dp.getValue();
            return value == null ? "" : value.toString();
        }
        return "";
    }

    /**
     * Sets the current value of the given editor node. Understands
     * TextField and ComboBox<String>.
     */
    @SuppressWarnings("unchecked")
    public static void setEditorValue(Node editor, String value) {
        if (editor instanceof TextField tf) {
            tf.setText(value == null ? "" : value);
        }
        if (editor instanceof ComboBox<?> cb) {
            ComboBox<String> typed = (ComboBox<String>) cb;
            typed.setValue(value == null || value.isBlank() ? null : value);
        }
        if (editor instanceof DatePicker dp) {
            dp.setValue(parseDate(value));
        }
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > 10) {
            normalized = normalized.substring(0, 10);
        }
        return LocalDate.parse(normalized);
    }

    /** Reads a ComboBox<String>'s value, trimmed, never null. */
    public static String value(ComboBox<String> cb) {
        String v = cb.getValue();
        return v == null ? "" : v.trim();
    }
}