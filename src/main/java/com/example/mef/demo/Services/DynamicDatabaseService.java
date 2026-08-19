package com.example.mef.demo.Services;

import com.example.mef.demo.Model.Attendance;
import com.example.mef.demo.Model.AnneeScolaire;
import com.example.mef.demo.Model.Classroom;
import com.example.mef.demo.Model.Employee;
import com.example.mef.demo.Model.Guardian;
import com.example.mef.demo.Model.Inscription;
import com.example.mef.demo.Model.Payment;
import com.example.mef.demo.Model.Student;
import com.example.mef.demo.Model.User;
import com.example.mef.demo.enums.AttendanceStatus;
import com.example.mef.demo.enums.EmployeeRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Transactional
public class DynamicDatabaseService {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private static final Map<String, Class<?>> ENTITIES = Map.ofEntries(
            Map.entry("students", Student.class),
            Map.entry("teachers", Employee.class),
            Map.entry("classes", Classroom.class),
            Map.entry("guardians", Guardian.class),
            Map.entry("attendance", Attendance.class),
            Map.entry("enrollments", Inscription.class),
            Map.entry("payments", Payment.class),
            Map.entry("users", User.class)
    );

    private static final Map<String, Map<String, String>> PROPERTY_ALIASES = Map.ofEntries(
            Map.entry("students", Map.ofEntries(
                    Map.entry("student_number", "studentNumber"),
                    Map.entry("first_name", "firstName"),
                    Map.entry("last_name", "lastName"),
                    Map.entry("date_of_birth", "dateOfBirth"),
                    Map.entry("medical_info", "medicalInfo"),
                    Map.entry("enrollment_date", "enrollmentDate")
            )),
            Map.entry("teachers", Map.ofEntries(
                    Map.entry("employee_number", "employeeNumber"),
                    Map.entry("first_name", "firstName"),
                    Map.entry("last_name", "lastName"),
                    Map.entry("phone", "phoneNumber"),
                    Map.entry("phone_number", "phoneNumber"),
                    Map.entry("specialty", "certifications"),
                    Map.entry("status", "role")
            )),
            Map.entry("guardians", Map.ofEntries(
                    Map.entry("first_name", "firstName"),
                    Map.entry("last_name", "lastName"),
                    Map.entry("phone", "phoneNumber"),
                    Map.entry("phone_number", "phoneNumber"),
                    Map.entry("relationship", "relation")
            )),
            Map.entry("classes", Map.ofEntries(
                    Map.entry("age_group", "ageGroup"),
                    Map.entry("grade_level", "ageGroup")
            )),
            Map.entry("attendance", Map.ofEntries(
                    Map.entry("attendance_date", "date"),
                    Map.entry("student_id", "student"),
                    Map.entry("student_name", "student")
            )),
            Map.entry("enrollments", Map.ofEntries(
                    Map.entry("student_id", "student"),
                    Map.entry("class_id", "classroom"),
                    Map.entry("course_name", "classroom"),
                    Map.entry("enrollment_date", "dateInscription")
            )),
            Map.entry("payments", Map.ofEntries(
                    Map.entry("inscription_id", "inscription"),
                    Map.entry("student_name", "inscription"),
                    Map.entry("payment_date", "datePay"),
                    Map.entry("method", "paymentMethod")
            )),
            Map.entry("users", Map.ofEntries(
                    Map.entry("full_name", "name"),
                    Map.entry("username", "email"),
                    Map.entry("password_hash", "password")
            ))
    );

    @PersistenceContext
    private EntityManager entityManager;

    private static String validIdentifier(String identifier) {
        if (identifier == null || !SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Invalid or unsafe identifier: " + identifier);
        }
        return identifier;
    }

    private static Class<?> entityType(String table) {
        validIdentifier(table);
        Class<?> entityType = ENTITIES.get(table);
        if (entityType == null) {
            throw new IllegalArgumentException("No JPA entity is registered for module table: " + table);
        }
        return entityType;
    }

    private static Class<?> entityTypeOrNull(String table) {
        validIdentifier(table);
        return ENTITIES.get(table);
    }

    private static String propertyName(String table, String column) {
        validIdentifier(column);
        return PROPERTY_ALIASES.getOrDefault(table, Map.of()).getOrDefault(column, column);
    }

    private static List<String> validIdentifiers(List<String> identifiers) {
        identifiers.forEach(DynamicDatabaseService::validIdentifier);
        return identifiers;
    }

    private static String orderBy(String table, String orderBy) {
        if (orderBy == null || orderBy.isBlank()) {
            return "";
        }

        String[] terms = orderBy.trim().split(",");
        List<String> jpqlTerms = new ArrayList<>();
        for (String term : terms) {
            String trimmed = term.trim();
            if (!Pattern.matches("^[a-zA-Z_][a-zA-Z0-9_]*(\\s+(?i:ASC|DESC))?$", trimmed)) {
                throw new IllegalArgumentException("Invalid ORDER BY clause: " + orderBy);
            }

            String[] parts = trimmed.split("\\s+");
            String property = propertyName(table, parts[0]);
            String direction = parts.length == 2 ? " " + parts[1].toUpperCase(Locale.ROOT) : "";
            jpqlTerms.add("e." + property + direction);
        }
        return " ORDER BY " + String.join(", ", jpqlTerms);
    }

    @Transactional(readOnly = true)
    public long count(String table) {
        Class<?> entityType = entityTypeOrNull(table);
        if (entityType == null) {
            return 0;
        }
        Long result = entityManager.createQuery(
                        "select count(e) from " + entityType.getSimpleName() + " e", Long.class)
                .getSingleResult();
        return result != null ? result : 0;
    }

    @Transactional(readOnly = true)
    public Double sum(String table, String column) {
        Class<?> entityType = entityTypeOrNull(table);
        if (entityType == null) {
            return 0.0;
        }
        String property = propertyName(table, column);
        Number result = (Number) entityManager.createQuery(
                        "select coalesce(sum(e." + property + "), 0) from " + entityType.getSimpleName() + " e")
                .getSingleResult();
        return result != null ? result.doubleValue() : 0.0;
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> attendanceSummary() {
        Map<String, Integer> summary = new HashMap<>();
        summary.put("PRESENT", 0);
        summary.put("ABSENT", 0);
        summary.put("LATE", 0);
        summary.put("EXCUSED", 0);

        List<Object[]> rows = entityManager.createQuery(
                        "select a.status, count(a) from Attendance a group by a.status", Object[].class)
                .getResultList();
        for (Object[] row : rows) {
            AttendanceStatus status = (AttendanceStatus) row[0];
            Number count = (Number) row[1];
            if (status != null && count != null) {
                summary.put(status.name(), count.intValue());
            }
        }
        return summary;
    }

    @Transactional(readOnly = true)
    public List<Map<String, String>> findAll(String table, List<String> columns, String orderBy) {
        Class<?> entityType = entityTypeOrNull(table);
        if (entityType == null) {
            return List.of();
        }
        validIdentifiers(columns);

        List<?> rows = entityManager.createQuery(
                        "select e from " + entityType.getSimpleName() + " e" + orderBy(table, orderBy),
                        entityType)
                .getResultList();

        List<Map<String, String>> result = new ArrayList<>();
        for (Object entity : rows) {
            Map<String, String> row = toRow(table, entity);
            addLogicalColumnAliases(table, row);
            result.add(row);
        }
        return result;
    }

    @Transactional
    public void insert(String table, List<String> columns, Map<String, String> values) {
        Class<?> entityType = entityType(table);
        validIdentifiers(columns);
        try {
            Object entity = entityType.getDeclaredConstructor().newInstance();
            applyValues(table, entity, columns, values);
            prepareForInsert(table, entity);
            entityManager.persist(entity);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not create " + entityType.getSimpleName(), e);
        }
    }

    @Transactional
    public void update(String table, List<String> columns, Map<String, String> values) {
        Class<?> entityType = entityType(table);
        validIdentifiers(columns);

        String id = values.get("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("ID is required for update");
        }

        Object entity = entityManager.find(entityType, parseId(entityType, id));
        if (entity == null) {
            throw new IllegalArgumentException("No " + entityType.getSimpleName() + " found with id " + id);
        }
        applyValues(table, entity, columns, values);
    }

    @Transactional
    public void delete(String table, String id) {
        Class<?> entityType = entityType(table);
        Object entity = entityManager.find(entityType, parseId(entityType, id));
        if (entity != null) {
            entityManager.remove(entity);
        }
    }

    @Transactional(readOnly = true)
    public MonthlyReportData monthlyReport(String startDate, String endDate) {
        LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime end = LocalDate.parse(endDate).atTime(LocalTime.MAX);

        Number income = (Number) entityManager.createQuery(
                        "select coalesce(sum(p.amount), 0) from Payment p where p.datePay between :start and :end")
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult();
        Long paymentCount = entityManager.createQuery(
                        "select count(p) from Payment p where p.datePay between :start and :end", Long.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .getSingleResult();

        int present = 0;
        int absent = 0;
        int late = 0;
        List<Object[]> rows = entityManager.createQuery(
                        "select a.status, count(a) from Attendance a where a.date between :start and :end group by a.status",
                        Object[].class)
                .setParameter("start", start)
                .setParameter("end", end)
                .getResultList();
        for (Object[] row : rows) {
            AttendanceStatus status = (AttendanceStatus) row[0];
            Number count = (Number) row[1];
            if (status == AttendanceStatus.PRESENT) {
                present = count.intValue();
            } else if (status == AttendanceStatus.ABSENT) {
                absent = count.intValue();
            }
        }

        return new MonthlyReportData(income.doubleValue(), paymentCount.intValue(), present, absent, late);
    }

    public record MonthlyReportData(double income, int paymentCount, int present, int absent, int late) {}

    /**
     * Creates a handful of sample Payment and Attendance rows for the given
     * month, reusing existing students/inscriptions already in the database
     * (never invents students or inscriptions). Intended for demo/testing
     * so the monthly report has something to show. Returns how many rows
     * were created, or 0 if there are no students/inscriptions to attach to.
     */
    @Transactional
    public int seedSampleDataForMonth(LocalDate month) {
        List<Student> students = entityManager.createQuery("select s from Student s", Student.class)
                .setMaxResults(15)
                .getResultList();
        List<Inscription> inscriptions = entityManager.createQuery("select i from Inscription i", Inscription.class)
                .setMaxResults(15)
                .getResultList();

        if (students.isEmpty() && inscriptions.isEmpty()) {
            return 0;
        }

        int daysInMonth = month.lengthOfMonth();
        java.util.Random random = new java.util.Random();
        int created = 0;

        for (Inscription inscription : inscriptions) {
            LocalDateTime payDate = month.withDayOfMonth(1 + random.nextInt(daysInMonth)).atTime(10, 0);
            com.example.mef.demo.Model.Payment payment = com.example.mef.demo.Model.Payment.builder()
                    .inscription(inscription)
                    .amount(3000.0 + random.nextInt(6) * 500.0)
                    .paymentMethod(com.example.mef.demo.enums.PaymentType.CASH)
                    .datePay(payDate)
                    .label("Scolarite")
                    .status(com.example.mef.demo.enums.PaymentStatus.PAID)
                    .build();
            entityManager.persist(payment);
            created++;
        }

        for (Student student : students) {
            for (int day = 1; day <= daysInMonth; day += 2) {
                AttendanceStatus status = random.nextInt(10) < 8
                        ? AttendanceStatus.PRESENT
                        : AttendanceStatus.ABSENT;
                Attendance attendance = Attendance.builder()
                        .student(student)
                        .date(month.withDayOfMonth(day).atTime(8, 30))
                        .status(status)
                        .build();
                entityManager.persist(attendance);
                created++;
            }
        }

        return created;
    }

    @Transactional
    public void createStudentEnrollment(Map<String, String> student, Map<String, String> guardian,
                                        String course, Map<String, String> payment) {
        Classroom classroom = findClassroom(student.get("classroom"));
        AnneeScolaire schoolYear = findOrCreateCurrentSchoolYear();

        Student savedStudent = new Student();
        applyValues("students", savedStudent, new ArrayList<>(student.keySet()), student);
        if (savedStudent.getStudentNumber() == null || savedStudent.getStudentNumber().isBlank()) {
            savedStudent.setStudentNumber("STU-" + System.currentTimeMillis());
        }
        if (savedStudent.getEnrollmentDate() == null) {
            savedStudent.setEnrollmentDate(LocalDateTime.now());
        }
        entityManager.persist(savedStudent);

        Guardian savedGuardian = new Guardian();
        applyValues("guardians", savedGuardian, new ArrayList<>(guardian.keySet()), guardian);
        entityManager.persist(savedGuardian);

        Inscription inscription = new Inscription();
        inscription.setStudent(savedStudent);
        inscription.setClassroom(classroom);
        inscription.setAnneeScolaire(schoolYear);
        inscription.setDateInscription(LocalDateTime.now());
        entityManager.persist(inscription);

        if (payment != null && payment.containsKey("amount")) {
            Payment savedPayment = new Payment();
            savedPayment.setInscription(inscription);
            setFieldValue(savedPayment, "amount", payment.get("amount"));
            setFieldValue(savedPayment, "label", payment.getOrDefault("category", course == null ? "Enrollment" : course));
            setFieldValue(savedPayment, "paymentMethod", payment.getOrDefault("method", "CASH"));
            setFieldValue(savedPayment, "datePay", payment.getOrDefault("payment_date", LocalDateTime.now().toString()));
            entityManager.persist(savedPayment);
        }
    }

    private Classroom findClassroom(String classroomName) {
        if (classroomName == null || classroomName.isBlank()) {
            throw new IllegalArgumentException("Classroom is required for enrollment.");
        }
        return entityManager.createQuery(
                        "select c from Classroom c where lower(c.name) = lower(:name)", Classroom.class)
                .setParameter("name", classroomName.trim())
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No classroom found named " + classroomName));
    }

    private AnneeScolaire findOrCreateCurrentSchoolYear() {
        String label = currentSchoolYearLabel();
        return entityManager.createQuery(
                        "select a from AnneeScolaire a where a.libelleAnneesc = :label", AnneeScolaire.class)
                .setParameter("label", label)
                .setMaxResults(1)
                .getResultStream()
                .findFirst()
                .orElseGet(() -> {
                    AnneeScolaire schoolYear = new AnneeScolaire();
                    schoolYear.setLibelleAnneesc(label);
                    entityManager.persist(schoolYear);
                    return schoolYear;
                });
    }

    private static String currentSchoolYearLabel() {
        LocalDate today = LocalDate.now();
        int startYear = today.getMonthValue() >= 9 ? today.getYear() : today.getYear() - 1;
        return startYear + "-" + (startYear + 1);
    }

    private static Map<String, String> toRow(String table, Object entity) {
        Map<String, String> row = new LinkedHashMap<>();
        for (Field field : entity.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(entity);
                row.put(field.getName(), formatValue(value));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Could not read " + field.getName(), e);
            }
        }
        addDerivedColumns(table, entity, row);
        return row;
    }

    private static void addLogicalColumnAliases(String table, Map<String, String> row) {
        PROPERTY_ALIASES.getOrDefault(table, Map.of()).forEach((logical, property) -> {
            if (row.containsKey(property)) {
                row.putIfAbsent(logical, row.get(property));
            }
        });
    }

    private static void addDerivedColumns(String table, Object entity, Map<String, String> row) {
        if (entity instanceof Attendance attendance && attendance.getStudent() != null) {
            row.put("student_name", fullName(attendance.getStudent()));
            row.put("student_id", attendance.getStudent().getId());
        } else if (entity instanceof Inscription inscription) {
            if (inscription.getStudent() != null) {
                row.put("student_name", fullName(inscription.getStudent()));
                row.put("student_id", inscription.getStudent().getId());
            }
            if (inscription.getClassroom() != null) {
                row.put("course_name", inscription.getClassroom().getName());
                row.put("class_id", inscription.getClassroom().getId());
            }
        } else if (entity instanceof Payment payment && payment.getInscription() != null) {
            row.put("inscription_id", payment.getInscription().getId());
            if (payment.getInscription().getStudent() != null) {
                row.put("student_name", fullName(payment.getInscription().getStudent()));
            }
        } else if ("classes".equals(table)) {
            row.putIfAbsent("status", "ACTIVE");
        }
    }

    private static String fullName(Student student) {
        return ((student.getFirstName() == null ? "" : student.getFirstName()) + " "
                + (student.getLastName() == null ? "" : student.getLastName())).trim();
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof Student student) {
            return fullName(student);
        }
        if (value instanceof Classroom classroom) {
            return classroom.getName();
        }
        if (value instanceof Inscription inscription) {
            return inscription.getId();
        }
        return value.toString();
    }

    private static void applyValues(String table, Object entity, List<String> columns, Map<String, String> values) {
        for (String column : columns) {
            if ("id".equals(column) || !values.containsKey(column)) {
                continue;
            }
            String property = propertyName(table, column);
            if (findField(entity.getClass(), property) != null) {
                setFieldValue(entity, property, values.get(column));
            }
        }
    }

    private static void prepareForInsert(String table, Object entity) {
        if (entity instanceof Student student) {
            if (student.getStudentNumber() == null || student.getStudentNumber().isBlank()) {
                student.setStudentNumber("STU-" + System.currentTimeMillis());
            }
            if (student.getEnrollmentDate() == null) {
                student.setEnrollmentDate(LocalDateTime.now());
            }
        }
        if (entity instanceof Employee employee) {
            if (employee.getEmployeeNumber() == null || employee.getEmployeeNumber().isBlank()) {
                employee.setEmployeeNumber("EMP-" + System.currentTimeMillis());
            }
            if (employee.getRole() == null) {
                employee.setRole(EmployeeRole.TEACHER);
            }
        }
    }

    private static void setFieldValue(Object entity, String property, String rawValue) {
        Field field = findField(entity.getClass(), property);
        if (field == null || rawValue == null || rawValue.isBlank()) {
            return;
        }
        field.setAccessible(true);
        try {
            field.set(entity, convertValue(field.getType(), rawValue));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not set " + property, e);
        }
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convertValue(Class<?> targetType, String rawValue) {
        if (String.class.equals(targetType)) {
            return rawValue;
        }
        if (Integer.class.equals(targetType) || int.class.equals(targetType)) {
            return Integer.parseInt(rawValue);
        }
        if (Double.class.equals(targetType) || double.class.equals(targetType)) {
            return Double.parseDouble(rawValue);
        }
        if (LocalDateTime.class.equals(targetType)) {
            return rawValue.length() == 10
                    ? LocalDate.parse(rawValue).atStartOfDay()
                    : LocalDateTime.parse(rawValue);
        }
        if (targetType.isEnum()) {
            return Enum.valueOf((Class<? extends Enum>) targetType, normalizeEnumValue(rawValue));
        }
        return rawValue;
    }

    private static String normalizeEnumValue(String rawValue) {
        return rawValue.trim()
                .replace("Garçon", "MALE")
                .replace("Fille", "FEMALE")
                .replace("Carte", "CARD")
                .replace("Virement", "TRANSFER")
                .replace("Cash", "CASH")
                .replace("Chèque", "CASH")
                .toUpperCase(Locale.ROOT);
    }

    private static Object parseId(Class<?> entityType, String rawId) {
        Field idField = findField(entityType, "id");
        if (idField != null && (Integer.class.equals(idField.getType()) || int.class.equals(idField.getType()))) {
            return Integer.parseInt(rawId);
        }
        return rawId;
    }
}