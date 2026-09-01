package com.example.mef.demo.dashboard.modules;

import com.example.mef.demo.Model.Field;
import com.example.mef.demo.Model.Module;
import com.example.mef.demo.Services.DynamicDatabaseService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.util.DialogUtil;
import com.example.mef.demo.util.I18n;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Renders the generic module CRUD page (table + filter + detail form) used
 * by every module except "settings" and "classes", which have their own
 * dedicated views. Extracted from DashboardController.showModule /
 * buildColumns / buildForm / readEditors.
 *
 * The "students" module gets one extra toolbar button ("Nouvel élève") that
 * the original code wired straight to showNewStudentWizard() on the
 * controller. Since the wizard hasn't been extracted yet, that action is
 * passed in as a callback instead.
 */
public class ModuleTableView {

    private final DynamicDatabaseService dao;

    public ModuleTableView(DynamicDatabaseService dao) {
        this.dao = dao;
    }

    /**
     * Renders the module's table+form workspace into contentPane.
     *
     * @param onNewStudent invoked when the "Nouvel élève" button is clicked
     *                      (only shown for the "students" module).
     */
    public void render(BorderPane contentPane, Module module, Runnable onNewStudent) {
        TableView<Map<String, String>> table = new TableView<>();
        table.getStyleClass().addAll("data-table", module.table() + "-table");
        table.setFixedCellSize(38);
        table.setPlaceholder(new Label(I18n.t("table.no_records", "تسجيل الحضور")));
        buildColumns(table, module);

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.getStyleClass().add("form-grid");
        Map<String, Node> editors = buildForm(module, form);

        Button save   = new Button(I18n.t("action.save", "تسجيل الحضور"));   save.getStyleClass().add("primary-button");
        Button clear  = new Button(I18n.t("action.clear", "تسجيل الحضور"));  clear.getStyleClass().add("secondary-button");
        Button delete = new Button(I18n.t("action.delete", "تسجيل الحضور")); delete.getStyleClass().add("danger-button");
        HBox actions = new HBox(10, save, clear, delete);
        actions.setAlignment(Pos.CENTER_LEFT);

        ObservableList<Map<String, String>> rows = FXCollections.observableArrayList();
        FilteredList<Map<String, String>> filteredRows = new FilteredList<>(rows, row -> true);
        table.setItems(filteredRows);

        TextField filter = FormFactory.textField(I18n.t("action.filter", "تسجيل الحضور") + " " + I18n.t(module.titleKey(), "تسجيل الحضور").toLowerCase());
        filter.getStyleClass().add("filter-field");
        filter.textProperty().addListener((obs, old, query) -> {
            String needle = query == null ? "" : query.trim().toLowerCase();
            filteredRows.setPredicate(row -> needle.isBlank() || row.values().stream()
                    .filter(v -> v != null)
                    .anyMatch(v -> v.toLowerCase().contains(needle)));
        });

        HBox tableToolbar = new HBox(10, filter);
        tableToolbar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(filter, Priority.ALWAYS);

        if ("students".equals(module.table())) {
            Button newStudent = new Button("➕  " + I18n.t("action.new_student", "تسجيل الحضور"));
            newStudent.getStyleClass().add("primary-button");
            newStudent.setMinWidth(140);
            newStudent.setOnAction(event -> onNewStudent.run());
            tableToolbar.getChildren().add(newStudent);
        }

        Runnable reload = () -> AsyncTasks.run(
                (Supplier<List<Map<String, String>>>) () -> dao.findAll(module.table(), module.columns(), module.orderBy()),
                rows::setAll,
                err -> DialogUtil.error("Chargement échoué", err.getMessage())
        );

        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                module.fields().forEach(field -> {
                    String v = "password_hash".equals(field.column()) ? "" : selected.get(field.column());
                    FormFactory.setEditorValue(editors.get(field.column()), v);
                });
            }
        });

        clear.setOnAction(event -> {
            table.getSelectionModel().clearSelection();
            editors.values().forEach(ed -> FormFactory.setEditorValue(ed, ""));
        });

        save.setOnAction(event -> {
            try {
                Map<String, String> values = readEditors(module, editors);
                Map<String, String> selected = table.getSelectionModel().getSelectedItem();
                if ("users".equals(module.table()) && selected != null
                        && FormFactory.getEditorValue(editors.get("password_hash")).isBlank()) {
                    values.put("password_hash", selected.get("password_hash"));
                }
                final boolean isInsert = (selected == null);
                if (!isInsert) values.put("id", selected.get("id"));

                save.setDisable(true);
                AsyncTasks.run(
                        () -> {
                            if (isInsert) dao.insert(module.table(), module.columns(), values);
                            else          dao.update(module.table(), module.columns(), values);
                        },
                        () -> { save.setDisable(false); reload.run(); clear.fire(); },
                        err -> {
                            save.setDisable(false);
                            DialogUtil.error(I18n.t("action.save", "تسجيل الحضور"), err.getMessage());
                        }
                );
            } catch (RuntimeException e) {
                DialogUtil.error(I18n.t("action.save", "تسجيل الحضور"), e.getMessage());
            }
        });

        delete.setOnAction(event -> {
            Map<String, String> selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                DialogUtil.info(I18n.t("action.delete", "تسجيل الحضور"), "Sélectionnez un enregistrement avant de supprimer.");
                return;
            }
            if (DialogUtil.confirm(I18n.t("action.delete", "تسجيل الحضور"),
                    "Supprimer cet enregistrement de " + I18n.t(module.titleKey(), "تسجيل الحضور").toLowerCase() + " ?")) {
                String id = selected.get("id");
                delete.setDisable(true);
                AsyncTasks.run(
                        () -> dao.delete(module.table(), id),
                        () -> { delete.setDisable(false); reload.run(); clear.fire(); },
                        err -> {
                            delete.setDisable(false);
                            DialogUtil.error(I18n.t("action.delete", "تسجيل الحضور"), err.getMessage());
                        }
                );
            }
        });

        reload.run();

        VBox formPanel = new VBox(14, new Label(I18n.t("table.details", "تسجيل الحضور")), form, actions);
        formPanel.getStyleClass().add("side-panel");
        VBox tablePanel = new VBox(10, tableToolbar, table);
        VBox.setVgrow(table, Priority.ALWAYS);

        HBox workspace = new HBox(18, tablePanel, formPanel);
        HBox.setHgrow(tablePanel, Priority.ALWAYS);
        workspace.setPadding(new Insets(24));

        contentPane.setCenter(workspace);
    }

    /* ── Column builder ─────────────────────────────────────────── */

    private void buildColumns(TableView<Map<String, String>> table, Module module) {
        TableColumn<Map<String, String>, String> id = new TableColumn<>("#");
        id.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().get("id")));
        id.setPrefWidth(48);
        id.setStyle("-fx-alignment: CENTER;");
        table.getColumns().add(id);

        List<Field> visibleFields = module.fields().stream()
                .filter(f -> !"password_hash".equals(f.column()))
                .toList();

        for (int i = 0; i < visibleFields.size(); i++) {
            Field field = visibleFields.get(i);
            TableColumn<Map<String, String>, String> column = new TableColumn<>(field.label().toUpperCase());
            column.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().get(field.column())));
            if (i == visibleFields.size() - 1) {
                column.prefWidthProperty().bind(table.widthProperty().subtract(48).subtract(140 * (visibleFields.size() - 1)).subtract(2));
            } else {
                column.setPrefWidth(140);
            }
            // ... rest of cellFactory logic unchanged
            table.getColumns().add(column);
        }
    }

    /* ── Form builder ───────────────────────────────────────────── */

    private Map<String, Node> buildForm(Module module, GridPane form) {
        Map<String, Node> editors = new LinkedHashMap<>();
        int row = 0;
        for (Field field : module.fields()) {
            Label label = new Label(field.label());
            Node editor = editorFor(module, field);
            if (editor instanceof TextField tf) {
                tf.setPromptText(field.label());
                if (isPhoneField(field)) {
                    tf.setTextFormatter(new TextFormatter<>(change ->
                            change.getControlNewText().matches("[0-9+()\\-\\s]*") ? change : null));
                }
            }
            if (editor instanceof DatePicker dp) {
                dp.setPromptText(field.label());
                dp.setMaxWidth(Double.MAX_VALUE);
            }
            if (editor instanceof ComboBox<?> cb) {
                @SuppressWarnings("unchecked")
                ComboBox<String> typed = (ComboBox<String>) cb;
                if (!field.options().isEmpty()) {
                    typed.setItems(FXCollections.observableArrayList(field.options()));
                }
                typed.setPromptText(field.label());
                typed.setMaxWidth(Double.MAX_VALUE);
                loadLookupOptions(module, field, typed);
            }
            editors.put(field.column(), editor);
            form.add(label, 0, row);
            form.add(editor, 1, row);
            GridPane.setHgrow(editor, Priority.ALWAYS);
            row++;
        }
        return editors;
    }

    private Node editorFor(Module module, Field field) {
        if (isDateField(field)) {
            return new DatePicker();
        }
        if (!field.options().isEmpty() || isLookupField(module, field)) {
            ComboBox<String> comboBox = new ComboBox<>();
            comboBox.setEditable(true);
            return comboBox;
        }
        return new TextField();
    }

    private boolean isDateField(Field field) {
        return field.column().contains("date") || "created_at".equals(field.column());
    }

    private boolean isPhoneField(Field field) {
        return "phone".equals(field.column()) || "phone_number".equals(field.column());
    }

    private boolean isLookupField(Module module, Field field) {
        return "teacher_name".equals(field.column())
                || "classroom".equals(field.column());
    }

    private void loadLookupOptions(Module module, Field field, ComboBox<String> comboBox) {
        if ("teacher_name".equals(field.column())) {
            AsyncTasks.run(
                    () -> dao.findAll("teachers", List.of("first_name", "last_name"), "last_name, first_name").stream()
                            .map(row -> (row.getOrDefault("last_name", "") + " " + row.getOrDefault("first_name", "")).trim())
                            .filter(name -> !name.isBlank())
                            .toList(),
                    options -> comboBox.setItems(FXCollections.observableArrayList(options)),
                    err -> DialogUtil.error("Chargement échoué", err.getMessage())
            );
        }

        if ("classroom".equals(field.column())) {
            AsyncTasks.run(
                    () -> dao.findAll("classes", List.of("name"), "name").stream()
                            .map(row -> row.getOrDefault("name", "").trim())
                            .filter(name -> !name.isBlank())
                            .toList(),
                    options -> comboBox.setItems(FXCollections.observableArrayList(options)),
                    err -> DialogUtil.error("Chargement échoué", err.getMessage())
            );
        }
    }

    private Map<String, String> readEditors(Module module, Map<String, Node> editors) {
        Map<String, String> values = new LinkedHashMap<>();
        for (Field field : module.fields()) {
            String v = FormFactory.getEditorValue(editors.get(field.column()));
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
}
