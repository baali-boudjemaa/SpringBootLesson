package com.example.mef.demo.dashboard.guardians;

import com.example.mef.demo.Model.Guardian;
import com.example.mef.demo.Model.Student;
import com.example.mef.demo.Services.GuardianService;
import com.example.mef.demo.Services.StudentService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.dashboard.common.TableStyleKit;
import com.example.mef.demo.util.DialogUtil;
import com.example.mef.demo.util.I18n;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/** Typed CRUD screen for the "guardians" module, styled to match StudentsView. */
@Component
public class GuardiansView {

    private final GuardianService guardianService;
    private final StudentService studentService;

    private final ObservableList<Guardian> rows = FXCollections.observableArrayList();
    private final TableView<Guardian> table = new TableView<>(rows);
    {
        TableStyleKit.applyTheme(table, "guardians", TableStyleKit.AVATAR_ROW_HEIGHT);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private final TextField searchField = FormFactory.textField(I18n.t("guardian.search"));
    private final Label countLabel = new Label();

    private final TextField firstNameField = FormFactory.textField(I18n.t("field.first_name"));
    private final TextField lastNameField = FormFactory.textField(I18n.t("field.last_name"));
    private final TextField phoneField = FormFactory.textField(I18n.t("field.phone"));
    private final TextField emailField = FormFactory.textField(I18n.t("field.email"));
    private final TextField relationField = FormFactory.textField(I18n.t("guardian.relation_hint"));
    private final TextField addressField = FormFactory.textField(I18n.t("field.address"));
    private final ComboBox<Student> studentField = new ComboBox<>();

    private BorderPane layout;
    private VBox form;
    private Guardian selected;

    public GuardiansView(GuardianService guardianService, StudentService studentService) {
        this.guardianService = guardianService;
        this.studentService = studentService;
        studentField.setMaxWidth(Double.MAX_VALUE);
        studentField.setCellFactory(cb -> studentCell());
        studentField.setButtonCell(studentCell());
        // Explicit converter: without this, the button cell can fall back to
        // Student#toString() (e.g. "com.example...@1a2b3c") instead of the
        // name, particularly when the selected value isn't reference-equal
        // to an item already loaded into the combo's items list.
        studentField.setConverter(new javafx.util.StringConverter<Student>() {
            @Override
            public String toString(Student s) {
                return s == null ? "" : s.getFirstName() + " " + s.getLastName();
            }

            @Override
            public Student fromString(String s) {
                return studentField.getValue();
            }
        });
    }

    private ListCell<Student> studentCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Student item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getFirstName() + " " + item.getLastName());
            }
        };
    }

    public void render(BorderPane contentPane, Label pageTitleLabel) {
        pageTitleLabel.setText(I18n.t("guardian.title"));
        table.setColumnResizePolicy(
                TableView.UNCONSTRAINED_RESIZE_POLICY
        );
        buildColumns();

        Label title = new Label(I18n.t("guardian.title"));
        title.getStyleClass().add("page-title");
        countLabel.getStyleClass().add("stat-caption");

        Button add = new Button("+  " + I18n.t("guardian.add"));
        add.getStyleClass().add("primary-button");
        add.setOnAction(e -> startCreate());

        HBox headerRow = new HBox(12, title);
        HBox.setHgrow(title, Priority.ALWAYS);
        headerRow.getChildren().add(add);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox headerBlock = new VBox(4, headerRow, countLabel);

        searchField.getStyleClass().add("filter-field");
        searchField.textProperty().addListener((obs, old, val) -> reload());

        VBox listPane = new VBox(14, headerBlock, searchField, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        listPane.setPadding(new Insets(24));
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> selectRow(val));

        form = buildForm();

        layout = new BorderPane();
        layout.setCenter(listPane);

        contentPane.setCenter(layout);
        contentPane.setPadding(new Insets(20));
        loadStudents();
        reload();
    }

    private void buildColumns() {
        table.getColumns().clear();

        TableColumn<Guardian, Guardian> guardian = new TableColumn<>(I18n.t("guardian.title").toUpperCase());
        guardian.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        guardian.setCellFactory(col -> guardianCell());
        guardian.setPrefWidth(240);

        TableColumn<Guardian, String> relation = new TableColumn<>(I18n.t("field.relationship").toUpperCase());
        relation.setCellValueFactory(d -> new ReadOnlyStringWrapper(translateRelation(d.getValue().getRelation())));
        relation.setCellFactory(col -> pillCell("#EEF2FF", "#4338CA"));
        relation.setPrefWidth(110);

        TableColumn<Guardian, String> student = new TableColumn<>(I18n.t("field.student").toUpperCase());
        student.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getStudent() == null ? "—" :
                        d.getValue().getStudent().getFirstName() + " " + d.getValue().getStudent().getLastName()));
        student.setCellFactory(col -> dashIfBlankCell());
        student.setPrefWidth(160);

        TableColumn<Guardian, String> phone = new TableColumn<>(I18n.t("field.phone").toUpperCase());
        phone.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getPhoneNumber()));
        phone.setCellFactory(col -> dashIfBlankCell());
        phone.setPrefWidth(130);

        TableColumn<Guardian, String> email = new TableColumn<>(I18n.t("field.email").toUpperCase());
        email.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getEmail()));
        email.setCellFactory(col -> dashIfBlankCell());
        email.setPrefWidth(160);

        table.getColumns().addAll(List.of(guardian, relation, student, phone, email));
    }

    private TableCell<Guardian, Guardian> guardianCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Guardian g, boolean empty) {
                super.updateItem(g, empty);
                if (empty || g == null) {
                    setGraphic(null);
                    return;
                }
                String initials = TableStyleKit.initialsOf(g.getFirstName(), g.getLastName());
                String color = TableStyleKit.colorFor(g.getRelation() == null ? "" : g.getRelation());
                String fullName = (g.getFirstName() == null ? "" : g.getFirstName()) + " " +
                        (g.getLastName() == null ? "" : g.getLastName());
                String subtitle = g.getAddress() == null || g.getAddress().isBlank() ? "—" : g.getAddress();
                setGraphic(TableStyleKit.avatarNameCell(initials, color, fullName.trim(), subtitle));
            }
        };
    }

    private TableCell<Guardian, String> pillCell(String bg, String fg) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setGraphic(null);
                } else {
                    setGraphic(TableStyleKit.pill(item, bg, fg));
                }
            }
        };
    }

    private TableCell<Guardian, String> dashIfBlankCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null || item.isBlank() ? "—" : item);
            }
        };
    }

    private String translateRelation(String relation) {
        if (relation == null || relation.isBlank()) {
            return "—";
        }
        return switch (relation.trim().toLowerCase(Locale.ROOT)) {
            case "mère", "mere", "mother", "الأم" -> I18n.t("guardian.relation.mother");
            case "père", "pere", "father", "الأب" -> I18n.t("guardian.relation.father");
            case "tuteur", "tutrice", "guardian", "ولي الأمر" -> I18n.t("guardian.relation.guardian");
            default -> relation;
        };
    }

    private VBox buildForm() {
        GridPane grid = FormFactory.sectionGrid();
        FormFactory.addRow(grid, 0, I18n.t("field.first_name"), firstNameField);
        FormFactory.addRow(grid, 1, I18n.t("field.last_name"), lastNameField);
        FormFactory.addRow(grid, 2, I18n.t("field.relationship"), relationField);
        FormFactory.addRow(grid, 3, I18n.t("field.phone"), phoneField);
        FormFactory.addRow(grid, 4, I18n.t("field.email"), emailField);
        FormFactory.addRow(grid, 5, I18n.t("field.address"), addressField);
        FormFactory.addRow(grid, 6, I18n.t("field.student"), studentField);

        Button save = new Button(I18n.t("action.save"));
        save.getStyleClass().add("primary-button");
        save.setOnAction(e -> save());

        Button cancel = new Button(I18n.t("wizard.cancel"));
        cancel.getStyleClass().add("secondary-button");
        cancel.setOnAction(e -> closeForm());

        Button delete = new Button(I18n.t("action.delete"));
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(e -> delete());

        // Shows every child linked to this same tuteur (there's one Guardian row per
        // child, so a tuteur with several kids appears as several rows sharing the
        // same identity — this groups them back together).
        Button viewChildren = new Button("👨‍👩‍👧  " + I18n.t("guardian.view_children"));
        viewChildren.getStyleClass().add("link-button");
        viewChildren.setOnAction(e -> showChildrenDialog());

        HBox actions = new HBox(8, save, cancel, delete);
        VBox panel = new VBox(12, new Label(I18n.t("guardian.details")), grid, viewChildren, actions);
        panel.getStyleClass().add("side-panel");
        panel.setPrefWidth(320);
        return panel;
    }

    private void startCreate() {
        clearForm();
        showFormPanel();
    }

    private void showFormPanel() {
        layout.setRight(form);
        BorderPane.setMargin(form, new Insets(0, 0, 0, 16));
    }

    private void closeForm() {
        layout.setRight(null);
        clearForm();
    }

    private void loadStudents() {
        AsyncTasks.run(
                studentService::findAll,
                list -> studentField.setItems(FXCollections.observableArrayList(list)),
                err -> DialogUtil.error(I18n.t("dialog.error"), I18n.t("guardian.load_students_failed") + err.getMessage())
        );
    }

    private void selectRow(Guardian guardian) {
        selected = guardian;
        if (guardian == null) {
            return;
        }
        firstNameField.setText(guardian.getFirstName());
        lastNameField.setText(guardian.getLastName());
        relationField.setText(guardian.getRelation());
        phoneField.setText(guardian.getPhoneNumber());
        emailField.setText(guardian.getEmail());
        addressField.setText(guardian.getAddress());
        studentField.setValue(guardian.getStudent());
        showFormPanel();
    }

    private void clearForm() {
        selected = null;
        firstNameField.clear();
        lastNameField.clear();
        relationField.clear();
        phoneField.clear();
        emailField.clear();
        addressField.clear();
        studentField.setValue(null);
        table.getSelectionModel().clearSelection();
    }

    private void save() {
        if (firstNameField.getText().isBlank() || lastNameField.getText().isBlank() || relationField.getText().isBlank()) {
            DialogUtil.error(I18n.t("guardian.required_title"), I18n.t("guardian.required_message"));
            return;
        }
        Guardian guardian = selected != null ? selected : new Guardian();
        guardian.setFirstName(firstNameField.getText().trim());
        guardian.setLastName(lastNameField.getText().trim());
        guardian.setRelation(relationField.getText().trim());
        guardian.setPhoneNumber(phoneField.getText().trim());
        guardian.setEmail(emailField.getText().trim());
        guardian.setAddress(addressField.getText());
        String studentId = studentField.getValue() == null ? null : studentField.getValue().getId();

        AsyncTasks.run(
                () -> guardianService.save(guardian, studentId),
                saved -> { closeForm(); reload(); },
                err -> DialogUtil.error(I18n.t("dialog.error"), I18n.t("guardian.save_failed") + err.getMessage())
        );
    }

    private void delete() {
        if (selected == null) return;
        if (!DialogUtil.confirm(I18n.t("dialog.confirm"), I18n.t("guardian.delete_confirm"))) return;
        String id = selected.getId();
        AsyncTasks.run(
                () -> guardianService.delete(id),
                () -> { closeForm(); reload(); },
                err -> DialogUtil.error(I18n.t("dialog.error"), I18n.t("guardian.delete_failed") + err.getMessage())
        );
    }

    /**
     * Loads every guardian, groups the ones matching the currently selected tuteur's
     * identity (name + phone/email — since a tuteur with several children is stored as
     * several {@link Guardian} rows, one per child), and shows their linked students.
     */
    private void showChildrenDialog() {
        if (selected == null) {
            DialogUtil.info(I18n.t("guardian.children_dialog_title"), I18n.t("guardian.select_first"));
            return;
        }
        String tutorFirstName = normalize(selected.getFirstName());
        String tutorLastName = normalize(selected.getLastName());
        String tutorPhone = normalize(selected.getPhoneNumber());
        String tutorEmail = normalize(selected.getEmail());

        AsyncTasks.run(
                () -> guardianService.search(null),
                all -> {
                    List<Guardian> sameTutor = all.stream()
                            .filter(g -> normalize(g.getFirstName()).equals(tutorFirstName)
                                    && normalize(g.getLastName()).equals(tutorLastName)
                                    && (samePhoneOrEmail(tutorPhone, normalize(g.getPhoneNumber()))
                                    || samePhoneOrEmail(tutorEmail, normalize(g.getEmail()))
                                    // Fall back to name-only match when neither phone nor email is filled in.
                                    || (tutorPhone.isBlank() && tutorEmail.isBlank())))
                            .toList();
                    openChildrenDialog(selected, sameTutor);
                },
                err -> DialogUtil.error(I18n.t("dialog.error"), I18n.t("guardian.load_failed") + err.getMessage())
        );
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    /** Two values match if both are non-blank and equal; blank values never count as a match. */
    private static boolean samePhoneOrEmail(String a, String b) {
        return !a.isBlank() && a.equals(b);
    }

    private void openChildrenDialog(Guardian tutor, List<Guardian> sameTutorRows) {
        String tutorName = ((tutor.getFirstName() == null ? "" : tutor.getFirstName()) + " "
                + (tutor.getLastName() == null ? "" : tutor.getLastName())).trim();

        Label title = new Label(I18n.t("guardian.children_of").replace("{0}", tutorName));
        title.getStyleClass().add("workflow-title");

        Label count = new Label(sameTutorRows.size() + " " + I18n.t(
                sameTutorRows.size() == 1 ? "guardian.child_singular" : "guardian.child_plural"));
        count.getStyleClass().add("stat-caption");

        VBox listBox = new VBox(8);
        List<Student> children = sameTutorRows.stream()
                .map(Guardian::getStudent)
                .filter(s -> s != null)
                .toList();

        if (children.isEmpty()) {
            Label none = new Label(I18n.t("guardian.no_children"));
            none.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px;");
            listBox.getChildren().add(none);
        } else {
            for (Student child : children) {
                listBox.getChildren().add(childRow(child));
            }
        }

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(320);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        Button close = new Button(I18n.t("action.close"));
        close.getStyleClass().add("secondary-button");

        VBox root = new VBox(14, title, count, scroll, close);
        root.getStyleClass().add("workflow-card");
        root.setPadding(new Insets(20));
        root.setPrefWidth(420);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        Window owner = table.getScene() == null ? null : table.getScene().getWindow();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle(I18n.t("guardian.children_dialog_title"));
        dialog.setScene(new Scene(root));
        close.setOnAction(e -> dialog.close());
        dialog.showAndWait();
    }

    /** One row: avatar-style initials + the child's full name, styled with explicit colors
     * so it reads correctly regardless of the dialog's stylesheet context. */
    private HBox childRow(Student s) {
        String initials = TableStyleKit.initialsOf(s.getFirstName(), s.getLastName());
        String color = TableStyleKit.colorFor(s.getGender() == null ? "" : s.getGender().name());
        String fullName = ((s.getFirstName() == null ? "" : s.getFirstName()) + " "
                + (s.getLastName() == null ? "" : s.getLastName())).trim();

        Label avatar = new Label(initials);
        avatar.setMinSize(36, 36);
        avatar.setMaxSize(36, 36);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 18; "
                + "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

        Label nameLbl = new Label(fullName);
        nameLbl.setStyle("-fx-text-fill: #0F172A; -fx-font-weight: bold; -fx-font-size: 13px;");
        String numberText = s.getStudentNumber() == null ? "—" : s.getStudentNumber();
        Label numberLbl = new Label(numberText);
        numberLbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
        VBox nameBox = new VBox(2, nameLbl, numberLbl);

        HBox row = new HBox(12, avatar, nameBox, new Region());
        HBox.setHgrow(row.getChildren().get(2), Priority.ALWAYS);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 8;");
        row.setPadding(new Insets(8, 12, 8, 12));
        return row;
    }

    private void reload() {
        String needle = searchField.getText();
        AsyncTasks.run(
                () -> guardianService.search(needle),
                list -> {
                    rows.setAll(list);
                    countLabel.setText(list.size() + " " + I18n.t(
                            list.size() == 1 ? "guardian.count_singular" : "guardian.count_plural"));
                },
                err -> DialogUtil.error(I18n.t("dialog.error"), I18n.t("guardian.load_failed") + err.getMessage())
        );
    }
}
