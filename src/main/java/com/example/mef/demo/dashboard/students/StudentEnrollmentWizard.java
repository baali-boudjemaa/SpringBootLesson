package com.example.mef.demo.dashboard.students;

import com.example.mef.demo.Model.Module;
import com.example.mef.demo.Model.ModuleRegistry;
import com.example.mef.demo.Services.DynamicDatabaseService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.util.DialogUtil;
import com.example.mef.demo.util.I18n;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The multi-step "new student" enrollment wizard (student info → guardian
 * info → payment), extracted from DashboardController.showNewStudentWizard /
 * buildStudentWizard / showEnrollSuccessCard.
 *
 * onShowModule is invoked with the "students" module when the success card's
 * "go to list" button is clicked — the original code called
 * showModule(registry.byTable("students")) directly on the controller.
 */
public class StudentEnrollmentWizard {

    private final DynamicDatabaseService dao;
    private final ModuleRegistry registry;
    private final Consumer<Module> onShowModule;

    public StudentEnrollmentWizard(DynamicDatabaseService dao, ModuleRegistry registry, Consumer<Module> onShowModule) {
        this.dao = dao;
        this.registry = registry;
        this.onShowModule = onShowModule;
    }

    /** Loads classrooms/courses in the background, then renders step 1. */
    public void show(BorderPane contentPane, Label pageTitleLabel) {
        pageTitleLabel.setText(I18n.t("wizard.title"));
        contentPane.setCenter(new Label(I18n.t("table.loading")));

        AsyncTasks.run(
                () -> {
                    List<String> classrooms = dao.findAll("classes", List.of("name"), "name").stream()
                            .map(row -> row.get("name")).toList();
                    List<String> courses = dao.findAll("courses", List.of("name"), "name").stream()
                            .map(row -> row.get("name")).toList();
                    return new WizardData(classrooms, courses);
                },
                data -> buildWizard(contentPane, pageTitleLabel, data),
                err -> contentPane.setCenter(new Label("Erreur : " + err.getMessage()))
        );
    }

    private record WizardData(List<String> classrooms, List<String> courses) {}

    private void buildWizard(BorderPane contentPane, Label pageTitleLabel, WizardData data) {
        // Step 1 — Student info
        TextField firstName   = FormFactory.textField(I18n.t("field.first_name"));
        TextField lastName    = FormFactory.textField(I18n.t("field.last_name"));
        ComboBox<String> gender = FormFactory.comboBox(List.of("Fille", "Garçon", "Autre"));
        DatePicker birthDate  = new DatePicker();
        birthDate.setPromptText(I18n.t("field.date_of_birth"));
        ComboBox<String> classroom = FormFactory.comboBox(data.classrooms());
        classroom.setEditable(true);
        ComboBox<String> bloodGroup = FormFactory.comboBox(List.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"));

        GridPane studentForm = FormFactory.sectionGrid();
        FormFactory.addRow(studentForm, 0, I18n.t("field.last_name"),     lastName);
        FormFactory.addRow(studentForm, 1, I18n.t("field.first_name"),    firstName);
        FormFactory.addRow(studentForm, 2, I18n.t("field.gender"),        gender);
        FormFactory.addRow(studentForm, 3, I18n.t("field.date_of_birth"), birthDate);
        FormFactory.addRow(studentForm, 4, I18n.t("field.classroom"),     classroom);
        FormFactory.addRow(studentForm, 5, I18n.t("field.blood_group"),   bloodGroup);

        // Step 2 — Guardian info
        TextField guardianFirstName = FormFactory.textField(I18n.t("field.first_name"));
        TextField guardianLastName  = FormFactory.textField(I18n.t("field.last_name"));
        ComboBox<String> relationship = FormFactory.comboBox(List.of("Mère", "Père", "Tuteur", "Autre"));
        TextField phone = FormFactory.textField(I18n.t("field.phone"));
        TextField email = FormFactory.textField(I18n.t("field.email"));

        GridPane guardianForm = FormFactory.sectionGrid();
        FormFactory.addRow(guardianForm, 0, I18n.t("field.last_name"),    guardianLastName);
        FormFactory.addRow(guardianForm, 1, I18n.t("field.first_name"),   guardianFirstName);
        FormFactory.addRow(guardianForm, 2, I18n.t("field.relationship"), relationship);
        FormFactory.addRow(guardianForm, 3, I18n.t("field.phone"),        phone);
        FormFactory.addRow(guardianForm, 4, I18n.t("field.email"),        email);

        // Step 3 — Payment
        ComboBox<String> course = FormFactory.comboBox(data.courses());
        course.setEditable(true);
        CheckBox firstPayment = new CheckBox(I18n.t("wizard.payment"));
        TextField amount  = FormFactory.textField(I18n.t("field.amount"));
        ComboBox<String> method   = FormFactory.comboBox(List.of("Cash", "Virement", "Carte", "Chèque"));
        ComboBox<String> category = FormFactory.comboBox(List.of("Scolarité", "Cours", "Transport", "Autre"));
        amount.setDisable(true);
        method.setDisable(true);
        category.setDisable(true);
        firstPayment.selectedProperty().addListener((obs, old, sel) -> {
            amount.setDisable(!sel);
            method.setDisable(!sel);
            category.setDisable(!sel);
        });
        GridPane courseForm = FormFactory.sectionGrid();
        FormFactory.addRow(courseForm, 0, I18n.t("field.course"), course);
        GridPane paymentForm = FormFactory.sectionGrid();
        FormFactory.addRow(paymentForm, 0, I18n.t("field.amount"),   amount);
        FormFactory.addRow(paymentForm, 1, I18n.t("field.method"),   method);
        FormFactory.addRow(paymentForm, 2, I18n.t("field.category"), category);

        // Nav buttons
        Button enroll   = new Button(I18n.t("wizard.enroll"));
        enroll.getStyleClass().add("success-button");
        enroll.setVisible(false);
        enroll.setManaged(false);
        Button clear    = new Button(I18n.t("action.clear"));
        clear.getStyleClass().add("secondary-button");
        Button previous = new Button(I18n.t("wizard.previous"));
        previous.getStyleClass().add("secondary-button");
        Button next     = new Button(I18n.t("wizard.next"));
        next.getStyleClass().add("primary-button");
        HBox actions = new HBox(10, previous, next, enroll, clear);
        actions.setAlignment(Pos.CENTER_LEFT);

        // Clear action
        clear.setOnAction(event -> {
            List.of(firstName, lastName, guardianFirstName, guardianLastName, phone, email, amount)
                    .forEach(f -> { f.setText(""); f.getStyleClass().remove("field-error"); });
            List.of(gender, classroom, relationship, course, method, category, bloodGroup)
                    .forEach(c -> c.setValue(null));
            birthDate.setValue(null);
            firstPayment.setSelected(false);
        });

        // Enroll action
        enroll.setOnAction(event -> {
            try {
                validateEnrollmentStep(0, firstName, lastName, gender, birthDate, classroom, bloodGroup,
                        guardianFirstName, guardianLastName, relationship, phone, email,
                        course, firstPayment, amount, method, category);
                validateEnrollmentStep(1, firstName, lastName, gender, birthDate, classroom, bloodGroup,
                        guardianFirstName, guardianLastName, relationship, phone, email,
                        course, firstPayment, amount, method, category);
                validateEnrollmentStep(2, firstName, lastName, gender, birthDate, classroom, bloodGroup,
                        guardianFirstName, guardianLastName, relationship, phone, email,
                        course, firstPayment, amount, method, category);

                Map<String, String> student = new LinkedHashMap<>();
                student.put("first_name",    firstName.getText());
                student.put("last_name",     lastName.getText());
                student.put("gender",        FormFactory.value(gender));
                student.put("date_of_birth", birthDate.getValue() == null ? "" : birthDate.getValue().toString());
                student.put("classroom",     FormFactory.value(classroom));
                student.put("status",        "ACTIVE");
                student.put("phone",         phone.getText());

                Map<String, String> guardian = new LinkedHashMap<>();
                guardian.put("first_name",   guardianFirstName.getText());
                guardian.put("last_name",    guardianLastName.getText());
                guardian.put("relationship", FormFactory.value(relationship));
                guardian.put("phone",        phone.getText());
                guardian.put("email",        email.getText());

                Map<String, String> payment = null;
                if (firstPayment.isSelected()) {
                    payment = new LinkedHashMap<>();
                    payment.put("amount",   amount.getText());
                    payment.put("method",   FormFactory.value(method));
                    payment.put("category", FormFactory.value(category));
                    payment.put("status",   "PAID");
                }

                final Map<String, String> paymentFinal = payment;
                final String studentName = lastName.getText().trim() + " " + firstName.getText().trim();

                enroll.setDisable(true);
                AsyncTasks.run(
                        () -> dao.createStudentEnrollment(student, guardian, FormFactory.value(course), paymentFinal),
                        () -> {
                            enroll.setDisable(false);
                            showEnrollSuccessCard(contentPane, pageTitleLabel, studentName);
                        },
                        err -> {
                            enroll.setDisable(false);
                            DialogUtil.error(I18n.t("wizard.enroll"), err.getMessage());
                        }
                );
            } catch (RuntimeException e) {
                DialogUtil.error(I18n.t("wizard.enroll"), e.getMessage());
            }
        });

        // Wizard step rendering
        Label detailTitle = new Label();
        detailTitle.getStyleClass().add("workflow-title");
        VBox detailBody = new VBox(18);
        VBox detailCard = new VBox(18, detailTitle, detailBody, actions);
        detailCard.getStyleClass().add("workflow-card");
        HBox.setHgrow(detailCard, Priority.ALWAYS);

        List<String> stepTitles = List.of(
                I18n.t("wizard.student"),
                I18n.t("wizard.guardian"),
                I18n.t("wizard.payment")
        );
        List<Node> stepContent = List.of(
                studentForm,
                guardianForm,
                new VBox(12, courseForm, firstPayment, paymentForm)
        );
        List<Button> stepButtons = new ArrayList<>();
        VBox stepList = new VBox(8);
        stepList.getStyleClass().add("workflow-list");
        Label stepListTitle = new Label(I18n.t("wizard.enrollment"));
        stepListTitle.getStyleClass().add("workflow-list-title");
        stepList.getChildren().add(stepListTitle);

        int[] activeStep = {0};
        Runnable[] renderStep = new Runnable[1];
        renderStep[0] = () -> {
            detailTitle.setText(stepTitles.get(activeStep[0]));
            detailBody.getChildren().setAll(stepContent.get(activeStep[0]));
            for (int i = 0; i < stepButtons.size(); i++) {
                stepButtons.get(i).getStyleClass().remove("workflow-step-active");
                if (i == activeStep[0]) stepButtons.get(i).getStyleClass().add("workflow-step-active");
            }
            previous.setDisable(activeStep[0] == 0);
            next.setVisible(activeStep[0] < stepTitles.size() - 1);
            next.setManaged(activeStep[0] < stepTitles.size() - 1);
            enroll.setVisible(activeStep[0] == stepTitles.size() - 1);
            enroll.setManaged(activeStep[0] == stepTitles.size() - 1);
        };

        for (int i = 0; i < stepTitles.size(); i++) {
            int stepIndex = i;
            Button step = new Button((i + 1) + ". " + stepTitles.get(i));
            step.getStyleClass().add("workflow-step");
            step.setMaxWidth(Double.MAX_VALUE);
            step.setOnAction(event -> { activeStep[0] = stepIndex; renderStep[0].run(); });
            stepButtons.add(step);
            stepList.getChildren().add(step);
        }

        previous.setOnAction(event -> { if (activeStep[0] > 0) { activeStep[0]--; renderStep[0].run(); } });
        next.setOnAction(event -> {
            try {
                validateEnrollmentStep(activeStep[0], firstName, lastName, gender, birthDate, classroom, bloodGroup,
                        guardianFirstName, guardianLastName, relationship, phone, email,
                        course, firstPayment, amount, method, category);
                activeStep[0]++;
                renderStep[0].run();
            } catch (RuntimeException e) {
                DialogUtil.error(I18n.t("wizard.next"), e.getMessage());
            }
        });

        renderStep[0].run();

        HBox workflow = new HBox(22, stepList, detailCard);
        workflow.setAlignment(Pos.TOP_LEFT);

        VBox root = new VBox(18, workflow);
        root.setPadding(new Insets(24));
        contentPane.setCenter(root);
    }

    private void showEnrollSuccessCard(BorderPane contentPane, Label pageTitleLabel, String studentName) {
        Label icon = new Label("✅");
        icon.setStyle("-fx-font-size: 40px;");
        Label title = new Label(I18n.t("wizard.success"));
        title.getStyleClass().add("success-card-title");
        Label body = new Label(studentName);
        body.getStyleClass().add("success-card-body");
        body.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #065F46;");

        Button newOne = new Button("➕  " + I18n.t("action.new_student"));
        newOne.getStyleClass().add("primary-button");
        newOne.setOnAction(e -> show(contentPane, pageTitleLabel));

        Button goToList = new Button("📋  " + I18n.t("nav.students"));
        goToList.getStyleClass().add("secondary-button");
        goToList.setOnAction(e -> onShowModule.accept(registry.byTable("students")));

        HBox btns = new HBox(12, newOne, goToList);
        btns.setAlignment(Pos.CENTER);

        VBox card = new VBox(16, icon, title, body, btns);
        card.getStyleClass().add("success-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(480);

        StackPane center = new StackPane(card);
        center.setPadding(new Insets(60));
        contentPane.setCenter(center);
    }

    /* ── Validation helpers ───────────────────────────────────────── */

    private void requireField(TextField field, String labelKey) {
        if (field.getText() == null || field.getText().isBlank()) {
            field.getStyleClass().add("field-error");
            throw new IllegalArgumentException(I18n.t(labelKey) + " est requis.");
        }
        field.getStyleClass().remove("field-error");
    }

    private void requireDate(DatePicker picker, String labelKey) {
        if (picker.getValue() == null) {
            picker.getStyleClass().add("field-error");
            throw new IllegalArgumentException(I18n.t(labelKey) + " est requis.");
        }
        picker.getStyleClass().remove("field-error");
    }

    private void requireCombo(ComboBox<String> cb, String labelKey) {
        if (FormFactory.value(cb).isBlank()) {
            throw new IllegalArgumentException(I18n.t(labelKey) + " est requis.");
        }
    }

    private void validateEnrollmentStep(int step, TextField fn, TextField ln,
                                        ComboBox<String> gender, DatePicker birthDate, ComboBox<String> cls, ComboBox<String> bloodGroup,
                                        TextField gFn, TextField gLn, ComboBox<String> relationship, TextField phone, TextField email,
                                        ComboBox<String> course, CheckBox firstPayment, TextField amount, ComboBox<String> method, ComboBox<String> category) {
        if (step == 0) {
            requireField(ln, "field.last_name");
            requireField(fn, "field.first_name");
            requireCombo(gender, "field.gender");
            requireDate(birthDate, "field.date_of_birth");
            requireCombo(cls, "field.classroom");
            requireCombo(bloodGroup, "field.blood_group");
        }
        if (step == 1) {
            requireField(gLn, "field.last_name");
            requireField(gFn, "field.first_name");
            requireCombo(relationship, "field.relationship");
            requireField(phone, "field.phone");
            requireField(email, "field.email");
        }
        if (step == 2) {
            requireCombo(course, "field.course");
            if (firstPayment.isSelected()) {
                requireField(amount, "field.amount");
                requireCombo(method, "field.method");
                requireCombo(category, "field.category");
            }
        }
    }
}