package com.example.mef.demo.dashboard.form;


import com.example.mef.demo.Model.Field;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.springframework.security.crypto.bcrypt.BCrypt;
import com.example.mef.demo.Model.Module;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the generic details/edit form for any CRUD module (a TextField
 * or ComboBox per Field), and reads/writes editor values back into a
 * plain Map&lt;String, String&gt; row.
 *
 * Extracted from DashboardController.buildForm(...) / readEditors(...) /
 * getEditorValue(...) / setEditorValue(...).
 */
public final class FormBuilder {

    private FormBuilder() {
    }

    /** Adds one label+editor row per field into {@code form} and returns the editors keyed by column. */
    public static Map<String, Node> buildForm(Module module, GridPane form) {
        Map<String, Node> editors = new LinkedHashMap<>();
        int row = 0;
        for (Field field : module.fields()) {
            Label label = new Label(field.label());
            Node editor = field.options().isEmpty() ? new TextField() : new ComboBox<String>();
            if (editor instanceof TextField tf) tf.setPromptText(field.label());
            if (editor instanceof ComboBox<?> cb) {
                @SuppressWarnings("unchecked")
                ComboBox<String> typed = (ComboBox<String>) cb;
                typed.setItems(FXCollections.observableArrayList(field.options()));
                typed.setMaxWidth(Double.MAX_VALUE);
            }
            editors.put(field.column(), editor);
            form.add(label, 0, row);
            form.add(editor, 1, row);
            GridPane.setHgrow(editor, Priority.ALWAYS);
            row++;
        }
        return editors;
    }

    /**
     * Reads current editor values into a row map, applying the same
     * side effects as the original code: password_hash gets hashed
     * (or defaulted to "changeme" if left blank), and created_at
     * defaults to today if left blank.
     */
    public static Map<String, String> readEditors(Module module, Map<String, Node> editors) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Field field : module.fields()) {
            String v = getEditorValue(editors.get(field.column()));
            if ("password_hash".equals(field.column())) {
                v = v.isBlank() ? BCrypt.hashpw("changeme", BCrypt.gensalt()) : BCrypt.hashpw(v, BCrypt.gensalt());
            }
            if ("created_at".equals(field.column()) && v.isBlank()) {
                v = LocalDate.now().toString();
            }
            values.put(field.column(), v);
        }
        return values;
    }

    public static String getEditorValue(Node editor) {
        if (editor instanceof TextField tf) return tf.getText();
        if (editor instanceof ComboBox<?> cb) {
            Object v = cb.getValue();
            return v == null ? "" : v.toString();
        }
        return "";
    }

    public static void setEditorValue(Node editor, String value) {
        if (editor instanceof TextField tf) tf.setText(value == null ? "" : value);
        if (editor instanceof ComboBox<?> cb) {
            @SuppressWarnings("unchecked")
            ComboBox<String> typed = (ComboBox<String>) cb;
            typed.setValue(value == null || value.isBlank() ? null : value);
        }
    }

    /** Same GridPane styling used for the student-wizard sub-forms (kept here to avoid duplication). */
    public static GridPane sectionGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.getStyleClass().add("form-grid");
        var label = new javafx.scene.layout.ColumnConstraints(120);
        var input = new javafx.scene.layout.ColumnConstraints();
        input.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(label, input);
        return grid;
    }

    public static void addRow(GridPane grid, int row, String label, Node editor) {
        grid.add(new Label(label), 0, row);
        grid.add(editor, 1, row);
        GridPane.setHgrow(editor, Priority.ALWAYS);
    }

    public static TextField textField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    public static ComboBox<String> comboBox(java.util.List<String> options) {
        ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList(options));
        cb.setMaxWidth(Double.MAX_VALUE);
        return cb;
    }
}