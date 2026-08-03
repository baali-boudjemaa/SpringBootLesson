package com.example.mef.demo.dashboard.students;

import com.example.mef.demo.Model.AnneeScolaire;
import com.example.mef.demo.Model.Classroom;
import com.example.mef.demo.Model.Guardian;
import com.example.mef.demo.Model.Inscription;
import com.example.mef.demo.Model.Payment;
import com.example.mef.demo.Model.Student;
import com.example.mef.demo.Services.ClassroomService;
import com.example.mef.demo.Services.EnrollmentRecordService;
import com.example.mef.demo.Services.EnrollmentService;
import com.example.mef.demo.Services.EnrollmentSettingsKeys;
import com.example.mef.demo.Services.GuardianService;
import com.example.mef.demo.Services.PaymentService;
import com.example.mef.demo.Services.SettingService;
import com.example.mef.demo.Services.StudentService;
import com.example.mef.demo.dashboard.common.AsyncTasks;
import com.example.mef.demo.dashboard.common.FormFactory;
import com.example.mef.demo.enums.AttendancePlan;
import com.example.mef.demo.enums.BloodType;
import com.example.mef.demo.enums.EnrollmentStatus;
import com.example.mef.demo.enums.PaymentStatus;
import com.example.mef.demo.enums.PaymentType;
import com.example.mef.demo.enums.SessionName;
import com.example.mef.demo.enums.Sexe;
import com.example.mef.demo.util.DialogUtil;
import com.example.mef.demo.util.I18n;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 5-step "New Enrollment" wizard:
 * <ol>
 *   <li>Student — search an existing student, or fill in a new student form.</li>
 *   <li>Guardian — search an existing guardian, or fill in a new guardian form.</li>
 *   <li>Enrollment — Academic Year, Classroom, Session, Registration Fee.</li>
 *   <li>Payment — record an initial payment: Amount, Payment Method, Receipt preview.</li>
 *   <li>Summary — review everything, then Finish.</li>
 * </ol>
 * Backed by {@link Inscription} (the entity that actually powers the
 * "enrollments" module / {@code EnrollmentsView}), plus {@link Guardian}
 * and {@link Payment}, via {@link StudentService}, {@link GuardianService},
 * {@link ClassroomService}, {@link EnrollmentService} and {@link PaymentService}.
 */
@Component
public class EnrollmentWizard {

    private static final NumberFormat FEE_FORMAT = NumberFormat.getNumberInstance(Locale.FRANCE);
    private static final DateTimeFormatter RECEIPT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<String> BLOOD_TYPES = List.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
    private static final List<String> RELATIONS = List.of("Mere", "Pere", "Tuteur", "Autre");
    /** Sun -> Sat, matching the order the custom-days checkboxes are shown in. */
    private static final List<DayOfWeek> WEEK_DAYS = List.of(
            DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);
    /** Full-week plan = every day except Friday & Saturday. */
    private static final String FULL_WEEK_DAYS = "SUNDAY,MONDAY,TUESDAY,WEDNESDAY,THURSDAY";

    private final StudentService studentService;
    private final GuardianService guardianService;
    private final ClassroomService classroomService;
    private final EnrollmentService enrollmentService;
    private final PaymentService paymentService;
    private final SettingService settingService;

    public EnrollmentWizard(StudentService studentService, GuardianService guardianService,
                            ClassroomService classroomService, EnrollmentService enrollmentService,
                            PaymentService paymentService, SettingService settingService) {
        this.studentService = studentService;
        this.guardianService = guardianService;
        this.classroomService = classroomService;
        this.enrollmentService = enrollmentService;
        this.paymentService = paymentService;
        this.settingService = settingService;
    }

    /** Loads pickers in the background, then renders step 1. */
    public void show(BorderPane contentPane, Label pageTitleLabel, Runnable onBackToList) {
        pageTitleLabel.setText(I18n.t("ewizard.title"));
        contentPane.setCenter(new Label(I18n.t("table.loading")));

        AsyncTasks.run(
                () -> {
                    List<Classroom> classrooms = classroomService.findAll();
                    Map<String, Integer> remainingSeats = new LinkedHashMap<>();
                    for (Classroom c : classrooms) {
                        remainingSeats.put(c.getId(), classroomService.remainingSeats(c));
                    }
                    return new WizardData(
                            studentService.findAll(),
                            guardianService.findAll(),
                            classrooms,
                            enrollmentService.findAllSchoolYears().stream().map(AnneeScolaire::getLibelleAnneesc).toList(),
                            remainingSeats,
                            settingService.getInt(EnrollmentSettingsKeys.MIN_AGE, EnrollmentSettingsKeys.MIN_AGE_DEFAULT)
                    );
                },
                data -> buildWizard(contentPane, pageTitleLabel, data, onBackToList),
                err -> contentPane.setCenter(new Label("Erreur : " + err.getMessage()))
        );
    }

    private record WizardData(List<Student> students, List<Guardian> guardians,
                              List<Classroom> classrooms, List<String> academicYears,
                              Map<String, Integer> remainingSeats, int minAge) {}

    private record EnrollmentResult(String studentName, String guardianName, Inscription inscription,
                                    Payment registrationPayment, Payment tuitionPayment) {}

    private void buildWizard(BorderPane contentPane, Label pageTitleLabel, WizardData data, Runnable onBackToList) {

        /* ── Step 1 — Student ─────────────────────────────────────── */
        CheckBox existingStudentCheck = new CheckBox(I18n.t("ewizard.existing_student"));

        TextField firstName = FormFactory.textField(I18n.t("field.first_name"));
        TextField lastName  = FormFactory.textField(I18n.t("field.last_name"));
        ComboBox<Sexe> gender = new ComboBox<>(FXCollections.observableArrayList(Sexe.values()));
        gender.setMaxWidth(Double.MAX_VALUE);
        DatePicker dateOfBirth = new DatePicker();
        dateOfBirth.setPromptText(I18n.t("field.date_of_birth"));
        dateOfBirth.setMaxWidth(Double.MAX_VALUE);
        ComboBox<String> bloodType = FormFactory.comboBox(BLOOD_TYPES);
        TextField phone = FormFactory.textField(I18n.t("field.phone"));
        TextField medicalInfo = FormFactory.textField(I18n.t("field.medical_info"));

        GridPane newStudentForm = FormFactory.sectionGrid();
        FormFactory.addRow(newStudentForm, 0, I18n.t("field.last_name"), lastName);
        FormFactory.addRow(newStudentForm, 1, I18n.t("field.first_name"), firstName);
        FormFactory.addRow(newStudentForm, 2, I18n.t("field.gender"), gender);
        FormFactory.addRow(newStudentForm, 3, I18n.t("field.date_of_birth"), dateOfBirth);
        FormFactory.addRow(newStudentForm, 4, I18n.t("field.blood_group"), bloodType);
        FormFactory.addRow(newStudentForm, 5, I18n.t("field.phone"), phone);
        FormFactory.addRow(newStudentForm, 6, I18n.t("field.medical_info"), medicalInfo);

        TextField studentSearchField = FormFactory.textField(I18n.t("ewizard.search_student"));
        ComboBox<Student> existingStudentCombo = new ComboBox<>(FXCollections.observableArrayList(data.students()));
        existingStudentCombo.setMaxWidth(Double.MAX_VALUE);
        existingStudentCombo.setCellFactory(cb -> studentCell());
        existingStudentCombo.setButtonCell(studentCell());
        HBox studentSearchRow = new HBox(8, studentSearchField, existingStudentCombo);
        HBox.setHgrow(studentSearchField, Priority.ALWAYS);
        HBox.setHgrow(existingStudentCombo, Priority.ALWAYS);
        VBox existingStudentBox = new VBox(10, studentSearchRow);

        studentSearchField.textProperty().addListener((obs, old, val) -> AsyncTasks.run(
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

        /* ── Step 2 — Guardian ────────────────────────────────────── */
        CheckBox existingGuardianCheck = new CheckBox(I18n.t("ewizard.existing_guardian"));

        TextField guardianFirstName = FormFactory.textField(I18n.t("field.first_name"));
        TextField guardianLastName  = FormFactory.textField(I18n.t("field.last_name"));
        ComboBox<String> relation = FormFactory.comboBox(RELATIONS);
        TextField guardianPhone = FormFactory.textField(I18n.t("field.phone"));
        TextField guardianEmail = FormFactory.textField(I18n.t("field.email"));
        TextField guardianAddress = FormFactory.textField(I18n.t("field.address"));

        GridPane newGuardianForm = FormFactory.sectionGrid();
        FormFactory.addRow(newGuardianForm, 0, I18n.t("field.last_name"), guardianLastName);
        FormFactory.addRow(newGuardianForm, 1, I18n.t("field.first_name"), guardianFirstName);
        FormFactory.addRow(newGuardianForm, 2, I18n.t("field.relationship"), relation);
        FormFactory.addRow(newGuardianForm, 3, I18n.t("field.phone"), guardianPhone);
        FormFactory.addRow(newGuardianForm, 4, I18n.t("field.email"), guardianEmail);
        FormFactory.addRow(newGuardianForm, 5, I18n.t("field.address"), guardianAddress);

        TextField guardianSearchField = FormFactory.textField(I18n.t("ewizard.search_guardian"));
        ComboBox<Guardian> existingGuardianCombo = new ComboBox<>(FXCollections.observableArrayList(data.guardians()));
        existingGuardianCombo.setMaxWidth(Double.MAX_VALUE);
        existingGuardianCombo.setCellFactory(cb -> guardianCell());
        existingGuardianCombo.setButtonCell(guardianCell());
        HBox guardianSearchRow = new HBox(8, guardianSearchField, existingGuardianCombo);
        HBox.setHgrow(guardianSearchField, Priority.ALWAYS);
        HBox.setHgrow(existingGuardianCombo, Priority.ALWAYS);
        VBox existingGuardianBox = new VBox(10, guardianSearchRow);

        guardianSearchField.textProperty().addListener((obs, old, val) -> AsyncTasks.run(
                () -> guardianService.search(val),
                list -> existingGuardianCombo.setItems(FXCollections.observableArrayList(list)),
                err -> {}
        ));

        newGuardianForm.setVisible(true);
        newGuardianForm.setManaged(true);
        existingGuardianBox.setVisible(false);
        existingGuardianBox.setManaged(false);
        existingGuardianCheck.selectedProperty().addListener((obs, old, selected) -> {
            newGuardianForm.setVisible(!selected);
            newGuardianForm.setManaged(!selected);
            existingGuardianBox.setVisible(selected);
            existingGuardianBox.setManaged(selected);
        });

        VBox guardianStep = new VBox(14, existingGuardianCheck, newGuardianForm, existingGuardianBox);

        /* ── Step 3 — Enrollment (year / classroom / session / fee) ─ */
        ComboBox<String> academicYear = FormFactory.comboBox(data.academicYears());
        academicYear.setEditable(true);
        academicYear.setValue(EnrollmentRecordService.currentSchoolYearLabel());
        ComboBox<Classroom> classroom = new ComboBox<>(FXCollections.observableArrayList(data.classrooms()));
        classroom.setMaxWidth(Double.MAX_VALUE);
        classroom.setCellFactory(cb -> classroomCell(data.remainingSeats()));
        classroom.setButtonCell(classroomCell(data.remainingSeats()));
        ComboBox<SessionName> session = new ComboBox<>(FXCollections.observableArrayList(SessionName.values()));
        session.setMaxWidth(Double.MAX_VALUE);
        session.setCellFactory(cb -> sessionCell());
        session.setButtonCell(sessionCell());
        TextField registrationFee = FormFactory.textField(I18n.t("ewizard.registration_fee"));

        /* ── Attendance: start date + attendance plan (+ custom days) ── */
        DatePicker startDate = new DatePicker(LocalDate.now());
        startDate.setMaxWidth(Double.MAX_VALUE);
        ComboBox<AttendancePlan> attendancePlan = new ComboBox<>(FXCollections.observableArrayList(AttendancePlan.values()));
        attendancePlan.setMaxWidth(Double.MAX_VALUE);
        attendancePlan.setCellFactory(cb -> attendancePlanCell());
        attendancePlan.setButtonCell(attendancePlanCell());
        attendancePlan.setValue(AttendancePlan.FULL_WEEK);

        List<CheckBox> dayChecks = List.of(
                new CheckBox(I18n.t("day.sun")), new CheckBox(I18n.t("day.mon")), new CheckBox(I18n.t("day.tue")),
                new CheckBox(I18n.t("day.wed")), new CheckBox(I18n.t("day.thu")), new CheckBox(I18n.t("day.fri")),
                new CheckBox(I18n.t("day.sat")));
        FlowPane customDaysBox = new FlowPane(12, 8);
        customDaysBox.getChildren().addAll(dayChecks);
        customDaysBox.setVisible(false);
        customDaysBox.setManaged(false);
        Label customDaysLabel = new Label(I18n.t("ewizard.custom_days"));
        VBox customDaysWrap = new VBox(6, customDaysLabel, customDaysBox);
        customDaysLabel.setVisible(false);
        customDaysLabel.setManaged(false);
        attendancePlan.valueProperty().addListener((obs, old, plan) -> {
            boolean custom = plan == AttendancePlan.CUSTOM_DAYS;
            customDaysBox.setVisible(custom);
            customDaysBox.setManaged(custom);
            customDaysLabel.setVisible(custom);
            customDaysLabel.setManaged(custom);
        });

        GridPane enrollmentForm = FormFactory.sectionGrid();
        FormFactory.addRow(enrollmentForm, 0, I18n.t("ewizard.academic_year"), academicYear);
        FormFactory.addRow(enrollmentForm, 1, I18n.t("field.classroom"), classroom);
        FormFactory.addRow(enrollmentForm, 2, I18n.t("ewizard.session"), session);
        FormFactory.addRow(enrollmentForm, 3, I18n.t("ewizard.registration_fee"), registrationFee);
        FormFactory.addRow(enrollmentForm, 4, I18n.t("ewizard.start_date"), startDate);
        FormFactory.addRow(enrollmentForm, 5, I18n.t("ewizard.attendance_plan"), attendancePlan);
        VBox enrollmentStep = new VBox(16, enrollmentForm, customDaysWrap);

        /* ── Step 4 — Payment (amount / method / receipt) ──────────── */
        CheckBox recordPayment = new CheckBox(I18n.t("ewizard.record_payment"));
        TextField paymentAmount = FormFactory.textField(I18n.t("field.amount"));
        ComboBox<PaymentType> paymentMethod = new ComboBox<>(FXCollections.observableArrayList(PaymentType.values()));
        paymentMethod.setMaxWidth(Double.MAX_VALUE);
        paymentMethod.setCellFactory(cb -> paymentMethodCell());
        paymentMethod.setButtonCell(paymentMethodCell());
        paymentAmount.setDisable(true);
        paymentMethod.setDisable(true);
        recordPayment.selectedProperty().addListener((obs, old, selected) -> {
            paymentAmount.setDisable(!selected);
            paymentMethod.setDisable(!selected);
        });

        GridPane paymentForm = FormFactory.sectionGrid();
        FormFactory.addRow(paymentForm, 0, I18n.t("field.amount"), paymentAmount);
        FormFactory.addRow(paymentForm, 1, I18n.t("field.method"), paymentMethod);

        Label receiptTitle = new Label("\uD83E\uDDFE  " + I18n.t("ewizard.receipt"));
        receiptTitle.getStyleClass().add("receipt-title");
        Label receiptStudent = new Label();
        Label receiptAmount = new Label();
        Label receiptMethod = new Label();
        Label receiptDate = new Label(LocalDate.now().format(RECEIPT_DATE_FORMAT));
        GridPane receiptGrid = FormFactory.sectionGrid();
        FormFactory.addRow(receiptGrid, 0, I18n.t("ewizard.summary.student"), receiptStudent);
        FormFactory.addRow(receiptGrid, 1, I18n.t("field.amount"), receiptAmount);
        FormFactory.addRow(receiptGrid, 2, I18n.t("field.method"), receiptMethod);
        FormFactory.addRow(receiptGrid, 3, I18n.t("field.date"), receiptDate);
        VBox receiptBox = new VBox(10, receiptTitle, receiptGrid);
        receiptBox.getStyleClass().add("receipt-box");

        VBox paymentStep = new VBox(16, recordPayment, paymentForm, receiptBox);

        /* ── Step 5 — Summary ─────────────────────────────────────── */
        Label summaryStudent  = new Label();
        Label summaryGuardian = new Label();
        Label summaryClass    = new Label();
        Label summaryYear     = new Label();
        Label summarySession  = new Label();
        Label summaryFee      = new Label();
        Label summaryPayment  = new Label();
        Label summaryStartDate = new Label();
        Label summaryAttendance = new Label();
        GridPane summaryGrid = FormFactory.sectionGrid();
        FormFactory.addRow(summaryGrid, 0, I18n.t("ewizard.summary.student"),  summaryStudent);
        FormFactory.addRow(summaryGrid, 1, I18n.t("ewizard.summary.guardian"), summaryGuardian);
        FormFactory.addRow(summaryGrid, 2, I18n.t("ewizard.summary.class"),    summaryClass);
        FormFactory.addRow(summaryGrid, 3, I18n.t("ewizard.summary.year"),     summaryYear);
        FormFactory.addRow(summaryGrid, 4, I18n.t("ewizard.summary.session"),  summarySession);
        FormFactory.addRow(summaryGrid, 5, I18n.t("ewizard.start_date"),       summaryStartDate);
        FormFactory.addRow(summaryGrid, 6, I18n.t("ewizard.attendance_plan"),  summaryAttendance);
        FormFactory.addRow(summaryGrid, 7, I18n.t("ewizard.summary.fee"),      summaryFee);
        FormFactory.addRow(summaryGrid, 8, I18n.t("ewizard.summary.payment"),  summaryPayment);
        VBox summaryStep = new VBox(12, summaryGrid);

        /* ── Nav buttons ───────────────────────────────────────────── */
        Button finish = new Button(I18n.t("ewizard.finish"));
        finish.getStyleClass().add("success-button");
        finish.setVisible(false);
        finish.setManaged(false);
        Button clear = new Button(I18n.t("action.clear"));
        clear.getStyleClass().add("secondary-button");
        Button previous = new Button(I18n.t("wizard.previous"));
        previous.getStyleClass().add("secondary-button");
        Button next = new Button(I18n.t("wizard.next"));
        next.getStyleClass().add("primary-button");
        HBox actions = new HBox(10, previous, next, finish, clear);
        actions.setAlignment(Pos.CENTER_LEFT);

        clear.setOnAction(event -> {
            existingStudentCheck.setSelected(false);
            firstName.clear();
            lastName.clear();
            gender.setValue(null);
            dateOfBirth.setValue(null);
            bloodType.setValue(null);
            phone.clear();
            medicalInfo.clear();
            studentSearchField.clear();
            existingStudentCombo.setValue(null);
            existingStudentCombo.setItems(FXCollections.observableArrayList(data.students()));

            existingGuardianCheck.setSelected(false);
            guardianFirstName.clear();
            guardianLastName.clear();
            relation.setValue(null);
            guardianPhone.clear();
            guardianEmail.clear();
            guardianAddress.clear();
            guardianSearchField.clear();
            existingGuardianCombo.setValue(null);
            existingGuardianCombo.setItems(FXCollections.observableArrayList(data.guardians()));

            academicYear.setValue(EnrollmentRecordService.currentSchoolYearLabel());
            classroom.setValue(null);
            session.setValue(null);
            registrationFee.clear();
            startDate.setValue(LocalDate.now());
            attendancePlan.setValue(AttendancePlan.FULL_WEEK);
            dayChecks.forEach(cb -> cb.setSelected(false));

            recordPayment.setSelected(false);
            paymentAmount.clear();
            paymentMethod.setValue(null);
        });

        /* ── Step rendering scaffold ──────────────────────────────── */
        Label detailTitle = new Label();
        detailTitle.getStyleClass().add("workflow-title");
        VBox detailBody = new VBox(18);
        VBox detailCard = new VBox(18, detailTitle, detailBody, actions);
        detailCard.getStyleClass().add("workflow-card");
        HBox.setHgrow(detailCard, Priority.ALWAYS);

        List<String> stepTitles = List.of(
                I18n.t("ewizard.step.student"),
                I18n.t("ewizard.step.guardian"),
                I18n.t("ewizard.step.enrollment"),
                I18n.t("ewizard.step.payment"),
                I18n.t("ewizard.step.summary")
        );
        List<Node> stepContent = List.of(studentStep, guardianStep, enrollmentStep, paymentStep, summaryStep);
        List<Button> stepButtons = new ArrayList<>();
        VBox stepList = new VBox(8);
        stepList.getStyleClass().add("workflow-list");
        Label stepListTitle = new Label(I18n.t("ewizard.title"));
        stepListTitle.getStyleClass().add("workflow-list-title");
        stepList.getChildren().add(stepListTitle);

        int[] activeStep = {0};
        Runnable[] renderStep = new Runnable[1];
        renderStep[0] = () -> {
            if (activeStep[0] == 3) {
                receiptStudent.setText(studentDisplayName(existingStudentCheck, existingStudentCombo, firstName, lastName));
                receiptAmount.setText(recordPayment.isSelected() ? formatFee(parseAmountSafely(paymentAmount.getText())) : "—");
                receiptMethod.setText(paymentMethod.getValue() == null ? "—" : paymentMethodLabel(paymentMethod.getValue()));
            }
            if (activeStep[0] == 4) {
                summaryStudent.setText(studentDisplayName(existingStudentCheck, existingStudentCombo, firstName, lastName));
                summaryGuardian.setText(guardianDisplayName(existingGuardianCheck, existingGuardianCombo, guardianFirstName, guardianLastName));
                summaryClass.setText(classroom.getValue() == null ? "—" : classroom.getValue().getName());
                summaryYear.setText(FormFactory.value(academicYear));
                summarySession.setText(session.getValue() == null ? "—" : sessionLabel(session.getValue()));
                summaryStartDate.setText(startDate.getValue() == null ? "—" : startDate.getValue().format(RECEIPT_DATE_FORMAT));
                summaryAttendance.setText(attendancePlan.getValue() == null ? "—" : attendancePlanLabel(attendancePlan.getValue())
                        + (attendancePlan.getValue() == AttendancePlan.CUSTOM_DAYS
                        ? " (" + customDaysSummary(dayChecks) + ")" : ""));
                summaryFee.setText(formatFee(parseAmountSafely(registrationFee.getText())));
                summaryPayment.setText(recordPayment.isSelected()
                        ? formatFee(parseAmountSafely(paymentAmount.getText())) + " (" + (paymentMethod.getValue() == null ? "—" : paymentMethodLabel(paymentMethod.getValue())) + ")"
                        : I18n.t("ewizard.summary.no_payment"));
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
            finish.setVisible(activeStep[0] == stepTitles.size() - 1);
            finish.setManaged(activeStep[0] == stepTitles.size() - 1);
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
                validateStep(activeStep[0], existingStudentCheck, existingStudentCombo, firstName, lastName, dateOfBirth,
                        existingGuardianCheck, existingGuardianCombo, guardianFirstName, guardianLastName, relation, guardianPhone, guardianEmail,
                        academicYear, classroom, session, registrationFee, attendancePlan, dayChecks,
                        recordPayment, paymentAmount, paymentMethod, data.remainingSeats(), data.minAge());
                activeStep[0]++;
                renderStep[0].run();
            } catch (RuntimeException e) {
                DialogUtil.error(I18n.t("wizard.next"), e.getMessage());
            }
        });

        /* ── Final save ────────────────────────────────────────────── */
        finish.setOnAction(event -> {
            try {
                for (int s = 0; s < stepTitles.size() - 1; s++) {
                    validateStep(s, existingStudentCheck, existingStudentCombo, firstName, lastName, dateOfBirth,
                            existingGuardianCheck, existingGuardianCombo, guardianFirstName, guardianLastName, relation, guardianPhone, guardianEmail,
                            academicYear, classroom, session, registrationFee, attendancePlan, dayChecks,
                            recordPayment, paymentAmount, paymentMethod, data.remainingSeats(), data.minAge());
                }

                boolean isExistingStudent = existingStudentCheck.isSelected();
                Student selectedStudent = existingStudentCombo.getValue();
                String newFirstName = valueOf(firstName);
                String newLastName  = valueOf(lastName);
                Sexe genderValue = gender.getValue();
                LocalDate dobValue = dateOfBirth.getValue();
                String bloodTypeValue = bloodType.getValue();
                String phoneValue = valueOf(phone);
                String medicalInfoValue = valueOf(medicalInfo);

                boolean isExistingGuardian = existingGuardianCheck.isSelected();
                Guardian selectedGuardian = existingGuardianCombo.getValue();
                String newGuardianFirstName = valueOf(guardianFirstName);
                String newGuardianLastName  = valueOf(guardianLastName);
                String relationValue = relation.getValue();
                String guardianPhoneValue = valueOf(guardianPhone);
                String guardianEmailValue = valueOf(guardianEmail);
                String guardianAddressValue = valueOf(guardianAddress);

                String yearValue = FormFactory.value(academicYear);
                Classroom classroomValue = classroom.getValue();
                SessionName sessionValue = session.getValue();
                double feeValue = parseAmount(registrationFee.getText());

                boolean paymentChecked = recordPayment.isSelected();
                double paymentAmountValue = paymentChecked ? parseAmount(paymentAmount.getText()) : 0.0;
                PaymentType paymentMethodValue = paymentMethod.getValue();

                String studentDisplayName = studentDisplayName(existingStudentCheck, existingStudentCombo, firstName, lastName);
                String guardianDisplayName = guardianDisplayName(existingGuardianCheck, existingGuardianCombo, guardianFirstName, guardianLastName);

                finish.setDisable(true);
                AsyncTasks.run(
                        () -> {
                            String studentId;
                            if (isExistingStudent) {
                                studentId = selectedStudent.getId();
                            } else {
                                Student created = new Student();
                                created.setFirstName(newFirstName);
                                created.setLastName(newLastName);
                                created.setGender(genderValue);
                                created.setDateOfBirth(dobValue == null ? null : dobValue.atStartOfDay());
                                created.setBloodType(bloodTypeValue == null ? null : BloodType.fromLabel(bloodTypeValue));
                                created.setPhone(phoneValue);
                                created.setMedicalInfo(medicalInfoValue);
                                created = studentService.save(created);
                                studentId = created.getId();
                            }

                            if (isExistingGuardian) {
                                guardianService.save(selectedGuardian, studentId);
                            } else {
                                Guardian created = new Guardian();
                                created.setFirstName(newGuardianFirstName);
                                created.setLastName(newGuardianLastName);
                                created.setRelation(relationValue);
                                created.setPhoneNumber(guardianPhoneValue);
                                created.setEmail(guardianEmailValue);
                                created.setAddress(guardianAddressValue);
                                guardianService.save(created, studentId);
                            }

                            AnneeScolaire schoolYear = enrollmentService.createSchoolYear(yearValue);
                            Inscription inscription = new Inscription();
                            inscription.setSession(sessionValue == null ? SessionName.JOURNEE_COMPLETE : sessionValue);
                            inscription.setStatus(EnrollmentStatus.ACTIVE);
                            Inscription savedInscription = enrollmentService.save(
                                    inscription, studentId, classroomValue.getId(), schoolYear.getId());

                            Payment registrationPayment = null;
                            if (feeValue > 0) {
                                Payment p = new Payment();
                                p.setAmount(feeValue);
                                p.setPaymentMethod(PaymentType.CASH);
                                p.setLabel(I18n.t("ewizard.registration_fee"));
                                p.setStatus(PaymentStatus.PAID);
                                registrationPayment = paymentService.save(p, savedInscription.getId());
                            }

                            Payment tuitionPayment = null;
                            if (paymentChecked && paymentAmountValue > 0) {
                                Payment p = new Payment();
                                p.setAmount(paymentAmountValue);
                                p.setPaymentMethod(paymentMethodValue == null ? PaymentType.CASH : paymentMethodValue);
                                p.setLabel(I18n.t("ewizard.step.payment"));
                                p.setStatus(PaymentStatus.PAID);
                                tuitionPayment = paymentService.save(p, savedInscription.getId());
                            }

                            return new EnrollmentResult(studentDisplayName, guardianDisplayName, savedInscription,
                                    registrationPayment, tuitionPayment);
                        },
                        result -> {
                            finish.setDisable(false);
                            showEnrollSuccessCard(contentPane, pageTitleLabel, result, onBackToList);
                        },
                        err -> {
                            finish.setDisable(false);
                            DialogUtil.error(I18n.t("ewizard.finish"), err.getMessage());
                        }
                );
            } catch (RuntimeException e) {
                DialogUtil.error(I18n.t("ewizard.finish"), e.getMessage());
            }
        });

        renderStep[0].run();

        HBox workflow = new HBox(22, stepList, detailCard);
        workflow.setAlignment(Pos.TOP_LEFT);

        VBox root = new VBox(18, workflow);
        root.setPadding(new Insets(24));
        contentPane.setCenter(root);
    }

    private void showEnrollSuccessCard(BorderPane contentPane, Label pageTitleLabel, EnrollmentResult result, Runnable onBackToList) {
        Label icon = new Label("✅");
        icon.setStyle("-fx-font-size: 40px;");
        Label title = new Label(I18n.t("ewizard.success"));
        title.getStyleClass().add("success-card-title");
        Label body = new Label(result.studentName() + "  ·  " + result.guardianName());
        body.getStyleClass().add("success-card-body");
        body.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #065F46;");

        double totalPaid = (result.registrationPayment() == null ? 0.0 : result.registrationPayment().getAmount())
                + (result.tuitionPayment() == null ? 0.0 : result.tuitionPayment().getAmount());
        Label receipt = new Label(totalPaid > 0
                ? I18n.t("ewizard.receipt") + " : " + formatFee(totalPaid)
                : I18n.t("ewizard.summary.no_payment"));
        receipt.getStyleClass().add("success-card-body");

        Button newOne = new Button("➕  " + I18n.t("ewizard.new"));
        newOne.getStyleClass().add("primary-button");
        newOne.setOnAction(e -> show(contentPane, pageTitleLabel, onBackToList));

        Button backToList = new Button("📋  " + I18n.t("ewizard.back_to_list"));
        backToList.getStyleClass().add("secondary-button");
        backToList.setOnAction(e -> onBackToList.run());

        HBox btns = new HBox(12, newOne, backToList);
        btns.setAlignment(Pos.CENTER);

        VBox card = new VBox(16, icon, title, body, receipt, btns);
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

    private ListCell<Guardian> guardianCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Guardian item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String rel = item.getRelation() == null ? "" : " (" + item.getRelation() + ")";
                    setText(item.getFirstName() + " " + item.getLastName() + rel);
                }
            }
        };
    }

    private ListCell<Classroom> classroomCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Classroom item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        };
    }

    private ListCell<SessionName> sessionCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(SessionName item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : sessionLabel(item));
            }
        };
    }

    private ListCell<PaymentType> paymentMethodCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(PaymentType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : paymentMethodLabel(item));
            }
        };
    }

    private String sessionLabel(SessionName session) {
        return switch (session) {
            case MATINEE -> I18n.t("session.matinee");
            case JOURNEE_COMPLETE -> I18n.t("session.journee_complete");
            case PERISCOLAIRE -> I18n.t("session.periscolaire");
        };
    }

    private String paymentMethodLabel(PaymentType type) {
        return switch (type) {
            case CASH -> I18n.t("payment_method.cash");
            case CARD -> I18n.t("payment_method.card");
            case TRANSFER -> I18n.t("payment_method.transfer");
        };
    }

    private String valueOf(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private String studentDisplayName(CheckBox existingStudentCheck, ComboBox<Student> existingStudentCombo,
                                      TextField firstName, TextField lastName) {
        if (existingStudentCheck.isSelected() && existingStudentCombo.getValue() != null) {
            Student s = existingStudentCombo.getValue();
            return (s.getFirstName() == null ? "" : s.getFirstName()) + " " + (s.getLastName() == null ? "" : s.getLastName());
        }
        return (valueOf(firstName) + " " + valueOf(lastName)).trim();
    }

    private String guardianDisplayName(CheckBox existingGuardianCheck, ComboBox<Guardian> existingGuardianCombo,
                                       TextField guardianFirstName, TextField guardianLastName) {
        if (existingGuardianCheck.isSelected() && existingGuardianCombo.getValue() != null) {
            Guardian g = existingGuardianCombo.getValue();
            return (g.getFirstName() == null ? "" : g.getFirstName()) + " " + (g.getLastName() == null ? "" : g.getLastName());
        }
        return (valueOf(guardianFirstName) + " " + valueOf(guardianLastName)).trim();
    }

    private double parseAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(raw.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(I18n.t("field.amount") + " " + I18n.t("ewizard.must_be_numeric"));
        }
    }

    /** Same as {@link #parseAmount}, but never throws — used for read-only previews. */
    private double parseAmountSafely(String raw) {
        try {
            return parseAmount(raw);
        } catch (RuntimeException e) {
            return 0.0;
        }
    }

    private String formatFee(double fee) {
        return FEE_FORMAT.format(fee);
    }

    private void validateStep(int step,
                              CheckBox existingStudentCheck, ComboBox<Student> existingStudentCombo, TextField firstName, TextField lastName,
                              CheckBox existingGuardianCheck, ComboBox<Guardian> existingGuardianCombo, TextField guardianFirstName, TextField guardianLastName,
                              ComboBox<String> relation, TextField guardianPhone, TextField guardianEmail,
                              ComboBox<String> academicYear, ComboBox<Classroom> classroom, ComboBox<SessionName> session, TextField registrationFee,
                              CheckBox recordPayment, TextField paymentAmount, ComboBox<PaymentType> paymentMethod) {
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
            if (existingGuardianCheck.isSelected()) {
                if (existingGuardianCombo.getValue() == null) {
                    throw new IllegalArgumentException(I18n.t("ewizard.select_guardian"));
                }
            } else {
                if (guardianFirstName.getText() == null || guardianFirstName.getText().isBlank()) {
                    guardianFirstName.getStyleClass().add("field-error");
                    throw new IllegalArgumentException(I18n.t("field.first_name") + " est requis.");
                }
                guardianFirstName.getStyleClass().remove("field-error");
                if (guardianLastName.getText() == null || guardianLastName.getText().isBlank()) {
                    guardianLastName.getStyleClass().add("field-error");
                    throw new IllegalArgumentException(I18n.t("field.last_name") + " est requis.");
                }
                guardianLastName.getStyleClass().remove("field-error");
                if (guardianPhone.getText() == null || guardianPhone.getText().isBlank()) {
                    guardianPhone.getStyleClass().add("field-error");
                    throw new IllegalArgumentException(I18n.t("field.phone") + " est requis.");
                }
                guardianPhone.getStyleClass().remove("field-error");
                if (guardianEmail.getText() == null || guardianEmail.getText().isBlank()) {
                    guardianEmail.getStyleClass().add("field-error");
                    throw new IllegalArgumentException(I18n.t("field.email") + " est requis.");
                }
                guardianEmail.getStyleClass().remove("field-error");
            }
        }
        if (step == 2) {
            if (FormFactory.value(academicYear).isBlank()) {
                throw new IllegalArgumentException(I18n.t("ewizard.academic_year") + " est requis.");
            }
            if (classroom.getValue() == null) {
                throw new IllegalArgumentException(I18n.t("ewizard.select_classroom"));
            }
            if (session.getValue() == null) {
                throw new IllegalArgumentException(I18n.t("ewizard.select_session"));
            }
            parseAmount(registrationFee.getText());
        }
        if (step == 3) {
            if (recordPayment.isSelected()) {
                double amount = parseAmount(paymentAmount.getText());
                if (amount <= 0) {
                    throw new IllegalArgumentException(I18n.t("field.amount") + " " + I18n.t("ewizard.must_be_numeric"));
                }
                if (paymentMethod.getValue() == null) {
                    throw new IllegalArgumentException(I18n.t("ewizard.select_payment_method"));
                }
            }
        }
    }
}