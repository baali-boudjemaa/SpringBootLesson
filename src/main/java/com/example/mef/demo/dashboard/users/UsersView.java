package com.example.mef.demo.dashboard.users;

import com.example.mef.demo.Model.User;
import com.example.mef.demo.Services.UserServices;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.enums.UserRole;
import com.example.mef.demo.util.DialogUtil;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Typed CRUD screen for the "users" module (User entity — app accounts,
 * not students/staff). Replaces the generic ModuleTableView fallback.
 *
 * Password is a PasswordField and is optional when editing an existing
 * user: leaving it blank keeps the current hash, matching
 * UserServices.updateUser's existing behavior. It is required when
 * creating a new user.
 */
@Component
public class UsersView {

    private final UserServices userServices;

    private final ObservableList<User> rows = FXCollections.observableArrayList();
    private final TableView<User> table = new TableView<>(rows);
    { com.example.mef.demo.dashboard.common.TableStyleKit.applyTheme(table, "users"); }

    private final TextField nameField = FormFactory.textField("Nom complet");
    private final TextField emailField = FormFactory.textField("Email");
    private final PasswordField passwordField = new PasswordField();
    private final ComboBox<UserRole> roleField = new ComboBox<>(FXCollections.observableArrayList(UserRole.values()));

    private User selected;

    public UsersView(UserServices userServices) {
        this.userServices = userServices;
        passwordField.setPromptText("Laisser vide pour conserver le mot de passe actuel");
        passwordField.setMaxWidth(Double.MAX_VALUE);
        roleField.setMaxWidth(Double.MAX_VALUE);
    }

    public void render(BorderPane contentPane, Label pageTitleLabel) {
        pageTitleLabel.setText("Utilisateurs");

        table.getColumns().clear();
        TableColumn<User, String> name = new TableColumn<>("Nom");
        name.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getFullName()));
        name.setPrefWidth(180);
        TableColumn<User, String> email = new TableColumn<>("Email");
        email.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getEmail()));
        email.setPrefWidth(200);
        TableColumn<User, String> role = new TableColumn<>("Rôle");
        role.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getRole() == null ? "" : d.getValue().getRole().name()));
        table.getColumns().addAll(List.of(name, email, role));

        VBox listPane = new VBox(10, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> selectRow(val));

        VBox form = buildForm();
        BorderPane layout = new BorderPane();
        layout.setCenter(listPane);
        layout.setRight(form);
        BorderPane.setMargin(form, new Insets(0, 0, 0, 16));
        form.setPrefWidth(320);

        contentPane.setCenter(layout);
        reload();
    }

    private VBox buildForm() {
        GridPane grid = FormFactory.sectionGrid();
        FormFactory.addRow(grid, 0, "Nom complet", nameField);
        FormFactory.addRow(grid, 1, "Email", emailField);
        FormFactory.addRow(grid, 2, "Mot de passe", passwordField);
        FormFactory.addRow(grid, 3, "Rôle", roleField);

        Button save = new Button("Enregistrer");
        save.getStyleClass().add("primary-button");
        save.setOnAction(e -> save());
        Button clear = new Button("Nouveau");
        clear.getStyleClass().add("secondary-button");
        clear.setOnAction(e -> clearForm());
        Button delete = new Button("Supprimer");
        delete.getStyleClass().add("danger-button");
        delete.setOnAction(e -> delete());

        return new VBox(12, new Label("Détails de l'utilisateur"), grid, new HBox(8, save, clear, delete));
    }

    private void selectRow(User user) {
        selected = user;
        if (user == null) { clearForm(); return; }
        nameField.setText(user.getName());
        emailField.setText(user.getEmail());
        passwordField.clear();
        roleField.setValue(user.getRole());
    }

    private void clearForm() {
        selected = null;
        nameField.clear();
        emailField.clear();
        passwordField.clear();
        roleField.setValue(null);
        table.getSelectionModel().clearSelection();
    }

    private void save() {
        if (nameField.getText().isBlank() || emailField.getText().isBlank()) {
            DialogUtil.error("Champs requis", "Le nom et l'email sont obligatoires.");
            return;
        }
        boolean isInsert = (selected == null);
        if (isInsert && passwordField.getText().isBlank()) {
            DialogUtil.error("Champ requis", "Le mot de passe est obligatoire pour un nouvel utilisateur.");
            return;
        }

        User user = isInsert ? new User() : selected;
        user.setName(nameField.getText().trim());
        user.setEmail(emailField.getText().trim());
        user.setRole(roleField.getValue() == null ? UserRole.USER : roleField.getValue());
        String rawPassword = passwordField.getText();

        AsyncTasks.run(
                () -> {
                    if (isInsert) {
                        userServices.addUser(user, rawPassword);
                    } else {
                        userServices.updateUser(user, user.getId(), rawPassword);
                    }
                },
                () -> { clearForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de l'enregistrement : " + err.getMessage())
        );
    }

    private void delete() {
        if (selected == null) return;
        if (!DialogUtil.confirm("Confirmer", "Supprimer cet utilisateur ?")) return;
        int id = selected.getId();
        AsyncTasks.run(
                () -> userServices.deleteUser(id),
                () -> { clearForm(); reload(); },
                err -> DialogUtil.error("Erreur", "Échec de la suppression : " + err.getMessage())
        );
    }

    private void reload() {
        AsyncTasks.run(
                userServices::getAllUser,
                list -> rows.setAll(list),
                err -> DialogUtil.error("Erreur", "Échec du chargement : " + err.getMessage())
        );
    }
}
