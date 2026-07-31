package com.example.mef.demo.Services;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class DynamicDatabaseService {

    /** Table/column names: letters, digits, underscore only. Blocks quotes, spaces, SQL punctuation. */
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private final JdbcTemplate jdbcTemplate;

    public DynamicDatabaseService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static String validIdentifier(String identifier) {
        if (identifier == null || !SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Invalid or unsafe identifier: " + identifier);
        }
        return identifier;
    }

    private static List<String> validIdentifiers(List<String> identifiers) {
        identifiers.forEach(DynamicDatabaseService::validIdentifier);
        return identifiers;
    }

    /** ORDER BY needs a slightly looser pattern: "col" or "col ASC"/"col DESC". */
    private static String validOrderBy(String orderBy) {
        if (orderBy == null || orderBy.isBlank()) return null;
        if (!Pattern.matches("^[a-zA-Z_][a-zA-Z0-9_]*(\\s+(ASC|DESC))?$", orderBy.trim())) {
            throw new IllegalArgumentException("Invalid ORDER BY clause: " + orderBy);
        }
        return orderBy.trim();
    }

    public long count(String table) {
        validIdentifier(table);
        Long result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return result != null ? result : 0;
    }

    public Double sum(String table, String column) {
        validIdentifier(table);
        validIdentifier(column);
        Double result = jdbcTemplate.queryForObject("SELECT SUM(" + column + ") FROM " + table, Double.class);
        return result != null ? result : 0.0;
    }

    public Map<String, Integer> attendanceSummary() {
        Map<String, Integer> summary = new HashMap<>();
        summary.put("PRESENT", 0);
        summary.put("ABSENT", 0);
        summary.put("LATE", 0);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT status, COUNT(*) as count FROM attendance GROUP BY status");
        for (Map<String, Object> row : rows) {
            String status = (String) row.get("status");
            Number count = (Number) row.get("count");
            if (status != null && count != null) {
                summary.put(status.toUpperCase(), count.intValue());
            }
        }
        return summary;
    }

    public List<Map<String, String>> findAll(String table, List<String> columns, String orderBy) {
        validIdentifier(table);
        validIdentifiers(columns);
        String safeOrderBy = validOrderBy(orderBy);

        // Always SELECT * so every column (including id) is available to the UI,
        // regardless of which subset the Module declared.
        String sql = "SELECT * FROM " + table + (safeOrderBy != null ? " ORDER BY " + safeOrderBy : "");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        List<Map<String, String>> result = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            Map<String, String> stringRow = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                stringRow.put(entry.getKey(), entry.getValue() == null ? "" : entry.getValue().toString());
            }
            result.add(stringRow);
        }
        return result;
    }

    @Transactional
    public void insert(String table, List<String> columns, Map<String, String> values) {
        validIdentifier(table);
        validIdentifiers(columns);

        List<String> insertCols = new ArrayList<>();
        List<Object> insertVals = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();

        for (String col : columns) {
            if (values.containsKey(col)) {
                insertCols.add(col);
                insertVals.add(values.get(col));
                placeholders.add("?");
            }
        }

        if (insertCols.isEmpty()) return;

        String sql = "INSERT INTO " + table + " (" + String.join(", ", insertCols)
                + ") VALUES (" + String.join(", ", placeholders) + ")";
        jdbcTemplate.update(sql, insertVals.toArray());
    }

    @Transactional
    public void update(String table, List<String> columns, Map<String, String> values) {
        validIdentifier(table);
        validIdentifiers(columns);

        String idStr = values.get("id");
        if (idStr == null || idStr.isBlank()) {
            throw new IllegalArgumentException("ID is required for update");
        }
        int id = Integer.parseInt(idStr);

        List<String> setClauses = new ArrayList<>();
        List<Object> updateVals = new ArrayList<>();

        for (String col : columns) {
            if (!"id".equals(col) && values.containsKey(col)) {
                setClauses.add(col + " = ?");
                updateVals.add(values.get(col));
            }
        }

        if (setClauses.isEmpty()) return;

        updateVals.add(id);
        String sql = "UPDATE " + table + " SET " + String.join(", ", setClauses) + " WHERE id = ?";
        jdbcTemplate.update(sql, updateVals.toArray());
    }

    @Transactional
    public void delete(String table, int id) {
        validIdentifier(table);
        jdbcTemplate.update("DELETE FROM " + table + " WHERE id = ?", id);
    }

    /**
     * Aggregate data for the monthly report screen.
     * @param startDate inclusive start date string (yyyy-MM-dd)
     * @param endDate   inclusive end date string   (yyyy-MM-dd)
     */
    public MonthlyReportData monthlyReport(String startDate, String endDate) {
        Double income = null;
        int paymentCount = 0;
        try {
            income = jdbcTemplate.queryForObject(
                    "SELECT SUM(amount) FROM payments WHERE payment_date >= ? AND payment_date <= ?",
                    Double.class, startDate, endDate);
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM payments WHERE payment_date >= ? AND payment_date <= ?",
                    Integer.class, startDate, endDate);
            paymentCount = cnt != null ? cnt : 0;
        } catch (Exception ignored) {}

        int present = 0, absent = 0, late = 0;
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT status, COUNT(*) as cnt FROM attendance "
                            + "WHERE attendance_date >= ? AND attendance_date <= ? GROUP BY status",
                    startDate, endDate);
            for (Map<String, Object> row : rows) {
                String st = (String) row.get("status");
                Number n = (Number) row.get("cnt");
                if (st == null || n == null) continue;
                switch (st.toUpperCase()) {
                    case "PRESENT" -> present = n.intValue();
                    case "ABSENT" -> absent = n.intValue();
                    case "LATE" -> late = n.intValue();
                }
            }
        } catch (Exception ignored) {}

        return new MonthlyReportData(income != null ? income : 0.0, paymentCount, present, absent, late);
    }

    public record MonthlyReportData(double income, int paymentCount, int present, int absent, int late) {}

    @Transactional
    public void createStudentEnrollment(Map<String, String> student, Map<String, String> guardian,
                                        String course, Map<String, String> payment) {
        insert("students", new ArrayList<>(student.keySet()), student);

        Integer studentId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM students", Integer.class);
        String studentName = student.get("first_name") + " " + student.get("last_name");

        guardian.put("student_name", studentName);
        insert("guardians", new ArrayList<>(guardian.keySet()), guardian);

        Map<String, String> enrollment = new LinkedHashMap<>();
        enrollment.put("enrollment_date", java.time.LocalDate.now().toString());
        enrollment.put("student_name", studentName);
        enrollment.put("course_name", course);
        enrollment.put("status", "ACTIVE");
        insert("enrollments", new ArrayList<>(enrollment.keySet()), enrollment);

        if (payment != null) {
            payment.put("payment_date", java.time.LocalDate.now().toString());
            payment.put("student_name", studentName);
            insert("payments", new ArrayList<>(payment.keySet()), payment);
        }
    }
}
