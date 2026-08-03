package com.example.mef.demo.dashboard.students;

import com.example.mef.demo.Model.Classroom;
import com.example.mef.demo.Model.Enrollment;
import com.example.mef.demo.Model.Student;
import com.example.mef.demo.Services.ClassroomService;
import com.example.mef.demo.Services.EnrollmentRecordService;
import com.example.mef.demo.Services.StudentService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.util.DialogUtil;
import com.example.mef.demo.util.I18n;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 3-step "New Enrollment" wizard:
 * <ol>
 *   <li>Student — pick an existing student, or enter a new one's name.</li>
 *   <li>Enrollment — Academic Year, Class Section, Registration Fee.</li>
 *   <li>Summary — review and save.</li>
 * </ol>
 * Backed by the {@link Enrollment} entity (classSection / academicYear /
 * registrationFee), via {@link EnrollmentRecordService}.
 */
@Component
public class EnrollmentWizard {

    private static final NumberFormat FEE_FORMAT = NumberFormat.getNumberInstance(Locale.FRANCE);

    private final StudentService studentService;
    private final ClassroomService classroomService;
    private final EnrollmentRecordService enrollmentRecordService;

    public EnrollmentWizard(StudentService studentService, ClassroomService classroomService,
                            EnrollmentRecordService enrollmentRecordService) {
        this.studentService = studentService;
        this.classroomService = classroomService;
        this.enrollmentRecordService = enrollmentRecordService;
    }

    /** Loads pickers in the background, then renders step 1. */
    public void show(BorderPane contentPane, Label pageTitleLabel, Runnable onBackToList) {
        pageTitleLabel.setText(I18n.t("ewizard.title"));
        contentPane.setCenter(new Label(I18n.t("table.loading")));

        AsyncTasks.run(
                () -> new WizardData(
                        studentService.findAll(),
                        classroomService.findAll().stream().map(Classroom::getName).toList(),
                        enrollmentRecordService.academicYearOptions()
                ),
                data -> buildWizard(contentPane, pageTitleLabel, data, onBackToList),
                err -> contentPane.setCenter(new Label("Erreur : " + err.getMessage()))
        );
    }

    private record WizardData(List<Student> students, List<String> classSections, List<String> academicYears) {}

    private void buildWizard(BorderPane contentPane, Label pageTitleLabel, WizardData data, Runnable onBackToList) {

        /* ── Step 1 — Student ─────────────────────────────────────── */
        CheckBox existingStudentCheck = new CheckBox(I18n.t("ewizard.existing_student"));

        TextField firstName = FormFactory.textField(I18n.t("field.first_name"));
        TextField lastName  = FormFactory.textField(I18n.t("field.last_name"));
        GridPane newStudentForm = FormFactory.sectionGrid();
        FormFactory.addRow(newStudentForm, 0, I18n.t("field.last_name"), lastName);
        FormFactory.addRow(newStudentForm, 1, I18n.t("field.first_name"), firstName);

        TextField searchField = FormFactory.textField(I18n.t("ewizard.search_student"));
        ComboBox<Student> existingStudentCombo = new ComboBox<>(FXCollections.observableArrayList(data.students()));
        existingStudentCombo.setMaxWidth(Double.MAX_VALUE);
        existingStudentCombo.setCellFactory(cb -> studentCell());
        existingStudentCombo.setButtonCell(studentCell());
        HBox searchRow = new HBox(8, searchField, existingStudentCombo);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        HBox.setHgrow(existingStudentCombo, Priority.ALWAYS);
        VBox existingStudentBox = new VBox(10, searchRow);

        searchField.textProperty().addListener((obs, old, val) -> AsyncTasks.run(
                () -> studentService.search(val),
                list -> existingStudentCombo.setItems(FXCollections.observableArrayList(list)),
                err -> {}
        ));

        newStudentForm.setVisible(true);
        newStudentForm.setManaged(true);
        existingStudentBox.setVisible(false);
        existingStudentBox.setManaged(false);
        existingStudentCheck.selectedProperty().addListener((obs, old, selected) -> {
            newStudentForm.setVisible(!selected);
            newStudentForm.setManaged(!selected);
            existingStudentBox.setVisible(selected);
            existingStudentBox.setManaged(selected);
        });

        VBox studentStep = new VBox(14, existingStudentCheck, newStudentForm, existingStudentBox);

        /* ── Step 2 — Enrollment details ──────────────────────────── */
        ComboBox<String> academicYear = FormFactory.comboBox(data.academicYears());
        academicYear.setEditable(true);
        academicYear.setValue(EnrollmentRecordService.currentSchoolYearLabel());
        ComboBox<String> classSection = FormFactory.comboBox(data.classSections());
        classSection.setEditable(true);
        TextField registrationFee = FormFactory.textField(I18n.t("ewizard.registration_fee"));

        GridPane enrollmentForm = FormFactory.sectionGrid();
        FormFactory.addRow(enrollmentForm, 0, I18n.t("ewizard.academic_year"), academicYear);
        FormFactory.addRow(enrollmentForm, 1, I18n.t("ewizard.class_section"), classSection);
        FormFactory.addRow(enrollmentForm, 2, I18n.t("ewizard.registration_fee"), registrationFee);

        /* ── Step 3 — Summary ─────────────────────────────────────── */
        Label summaryStudent = new Label();
        Label summaryClass   = new Label();
        Label summaryYear    = new Label();
        Label summaryFee     = new Label();
        GridPane summaryGrid = FormFactory.sectionGrid();
        FormFactory.addRow(summaryGrid, 0, I18n.t("ewizard.summary.student"), summaryStudent);
        FormFactory.addRow(summaryGrid, 1, I18n.t("ewizard.summary.class"),   summaryClass);
        FormFactory.addRow(summaryGrid, 2, I18n.t("ewizard.summary.year"),    summaryYear);
        FormFactory.addRow(summaryGrid, 3, I18n.t("ewizard.summary.fee"),     summaryFee);
        VBox summaryStep = new VBox(12, summaryGrid);

        /* ── Nav buttons ───────────────────────────────────────────── */
        Button enroll = new Button(I18n.t("ewizard.save"));
        enroll.getStyleClass().add("success-button");
        enroll.setVisible(false);
        enroll.setManaged(false);
        Button clear = new Button(I18n.t("action.clear"));
        clear.getStyleClass().add("secondary-button");
        Button previous = new Button(I18n.t("wizard.previous"));
        previous.getStyleClass().add("secondary-button");
        Button next = new Button(I18n.t("wizard.next"));
        next.getStyleClass().add("primary-button");
        HBox actions = new HBox(10, previous, next, enroll, clear);
        actions.setAlignment(Pos.CENTER_LEFT);

        clear.setOnAction(event -> {
            existingStudentCheck.setSelected(false);
            firstName.clear();
            lastName.clear();
            searchField.clear();
            existingStudentCombo.setValue(null);
            existingStudentCombo.setItems(FXCollections.observableArrayList(data.students()));
            academicYear.setValue(EnrollmentRecordService.currentSchoolYearLabel());
            classSection.setValue(null);
            registrationFee.clear();
        });

        /* ── Step rendering scaffold (same pattern as StudentEnrollmentWizard) ── */
        Label detailTitle = new Label();
        detailTitle.getStyleClass().add("workflow-title");
        VBox detailBody = new VBox(18);
        VBox detailCard = new VBox(18, detailTitle, detailBody, actions);
        detailCard.getStyleClass().add("workflow-card");
        HBox.setHgrow(detailCard, Priority.ALWAYS);

        List<String> stepTitles = List.of(
                I18n.t("ewizard.step.student"),
                I18n.t("ewizard.step.enrollment"),
                I18n.t("ewizard.step.summary")
        );
        List<Node> stepContent = List.of(studentStep, enrollmentForm, summaryStep);
        List<Button> stepButtons = new ArrayList<>();
        VBox stepList = new VBox(8);
        stepList.getStyleClass().add("workflow-list");
        Label stepListTitle = new Label(I18n.t("ewizard.title"));
        stepListTitle.getStyleClass().add("workflow-list-title");
        stepList.getChildren().add(stepListTitle);

        int[] activeStep = {0};
        Runnable[] renderStep = new Runnable[1];
        renderStep[0] = () -> {
            if (activeStep[0] == 2) {
                summaryStudent.setText(studentDisplayName(existingStudentCheck, existingStudentCombo, firstName, lastName));
                summaryClass.setText(FormFactory.value(classSection));
                summaryYear.setText(FormFactory.value(academicYear));
                summaryFee.setText(formatFee(parseFee(registrationFee.getText())));
            }
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
                validateStep(activeStep[0], existingStudentCheck, existingStudentCombo, firstName, lastName,
                        academicYear, classSection, registrationFee);
                activeStep[0]++;
                renderStep[0].run();
            } catch (RuntimeException e) {
                DialogUtil.error(I18n.t("wizard.next"), e.getMessage());
            }
        });

        /* ── Final save ────────────────────────────────────────────── */
        enroll.setOnAction(event -> {
            try {
                validateStep(0, existingStudentCheck, existingStudentCombo, firstName, lastName,
                        academicYear, classSection, registrationFee);
                validateStep(1, existingStudentCheck, existingStudentCombo, firstName, lastName,
                        academicYear, classSection, registrationFee);

                boolean isExisting = existingStudentCheck.isSelected();
                Student selectedStudent = existingStudentCombo.getValue();
                String newFirstName = firstName.getText() == null ? "" : firstName.getText().trim();
                String newLastName  = lastName.getText() == null ? "" : lastName.getText().trim();
                String yearValue    = FormFactory.value(academicYear);
                String sectionValue = FormFactory.value(classSection);
                double feeValue     = parseFee(registrationFee.getText());
                String studentDisplayName = studentDisplayName(existingStudentCheck, existingStudentCombo, firstName, lastName);

                enroll.setDisable(true);
                AsyncTasks.run(
                        () -> {
                            String studentId;
                            if (isExisting) {
                                studentId = selectedStudent.getId();
                            } else {
                                Student created = new Student();
                                created.setFirstName(newFirstName);
                                created.setLastName(newLastName);
                                created = studentService.save(created);
                                studentId = created.getId();
                            }
                            Enrollment enrollment = new Enrollment();
                            enrollment.setClassSection(sectionValue);
                            enrollment.setAcademicYear(yearValue);
                            enrollment.setRegistrationFee(feeValue);
                            return enrollmentRecordService.save(enrollment, studentId);
                        },
                        saved -> {
                            enroll.setDisable(false);
                            showEnrollSuccessCard(contentPane, pageTitleLabel, studentDisplayName, onBackToList);
                        },
                        err -> {
                            enroll.setDisable(false);
                            DialogUtil.error(I18n.t("ewizard.save"), err.getMessage());
                        }
                );
            } catch (RuntimeException e) {
                DialogUtil.error(I18n.t("ewizard.save"), e.getMessage());
            }
        });

        renderStep[0].run();

        HBox workflow = new HBox(22, stepList, detailCard);
        workflow.setAlignment(Pos.TOP_LEFT);

        VBox root = new VBox(18, workflow);
        root.setPadding(new Insets(24));
        contentPane.setCenter(root);
    }

    private void showEnrollSuccessCard(BorderPane contentPane, Label pageTitleLabel, String studentName, Runnable onBackToList) {
        Label icon = new Label("✅");
        icon.setStyle("-fx-font-size: 40px;");
        Label title = new Label(I18n.t("ewizard.success"));
        title.getStyleClass().add("success-card-title");
        Label body = new Label(studentName);
        body.getStyleClass().add("success-card-body");
        body.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #065F46;");

        Button newOne = new Button("➕  " + I18n.t("ewizard.new"));
        newOne.getStyleClass().add("primary-button");
        newOne.setOnAction(e -> show(contentPane, pageTitleLabel, onBackToList));

        Button backToList = new Button("📋  " + I18n.t("ewizard.back_to_list"));
        backToList.getStyleClass().add("secondary-button");
        backToList.setOnAction(e -> onBackToList.run());

        HBox btns = new HBox(12, newOne, backToList);
        btns.setAlignment(Pos.CENTER);

        VBox card = new VBox(16, icon, title, body, btns);
        card.getStyleClass().add("success-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(480);

        StackPane center = new StackPane(card);
        center.setPadding(new Insets(60));
        contentPane.setCenter(center);
    }

    /* ── Helpers ──────────────────────────────────────────────────── */

    private ListCell<Student> studentCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Student item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String number = item.getStudentNumber() == null ? "" : " (" + item.getStudentNumber() + ")";
                    setText(item.getFirstName() + " " + item.getLastName() + number);
                }
            }
        };
    }

    private String studentDisplayName(CheckBox existingStudentCheck, ComboBox<Student> existingStudentCombo,
                                      TextField firstName, TextField lastName) {
        if (existingStudentCheck.isSelected() && existingStudentCombo.getValue() != null) {
            Student s = existingStudentCombo.getValue();
            return (s.getFirstName() == null ? "" : s.getFirstName()) + " " + (s.getLastName() == null ? "" : s.getLastName());
        }
        String fn = firstName.getText() == null ? "" : firstName.getText().trim();
        String ln = lastName.getText() == null ? "" : lastName.getText().trim();
        return (fn + " " + ln).trim();
    }

    private double parseFee(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(raw.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(I18n.t("ewizard.registration_fee") + " " + I18n.t("ewizard.must_be_numeric"));
        }
    }

    private String formatFee(double fee) {
        return FEE_FORMAT.format(fee);
    }

    private void validateStep(int step, CheckBox existingStudentCheck, ComboBox<Student> existingStudentCombo,
                              TextField firstName, TextField lastName,
                              ComboBox<String> academicYear, ComboBox<String> classSection, TextField registrationFee) {
        if (step == 0) {
            if (existingStudentCheck.isSelected()) {
                if (existingStudentCombo.getValue() == null) {
                    throw new IllegalArgumentException(I18n.t("ewizard.select_student"));
                }
            } else {
                if (firstName.getText() == null || firstName.getText().isBlank()) {
                    firstName.getStyleClass().add("field-error");
                    throw new IllegalArgumentException(I18n.t("field.first_name") + " est requis.");
                }
                firstName.getStyleClass().remove("field-error");
                if (lastName.getText() == null || lastName.getText().isBlank()) {
                    lastName.getStyleClass().add("field-error");
                    throw new IllegalArgumentException(I18n.t("field.last_name") + " est requis.");
                }
                lastName.getStyleClass().remove("field-error");
            }
        }
        if (step == 1) {
            if (FormFactory.value(academicYear).isBlank()) {
                throw new IllegalArgumentException(I18n.t("ewizard.academic_year") + " est requis.");
            }
            if (FormFactory.value(classSection).isBlank()) {
                throw new IllegalArgumentException(I18n.t("ewizard.class_section") + " est requis.");
            }
            parseFee(registrationFee.getText());
        }
    }
}