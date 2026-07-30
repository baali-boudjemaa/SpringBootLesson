package com.example.mef.demo.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DynamicDatabaseService {

    private final JdbcTemplate jdbcTemplate;

    public DynamicDatabaseService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long count(String table) {
        Long result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return result != null ? result : 0;
    }

    public Double sum(String table, String column) {
        Double result = jdbcTemplate.queryForObject("SELECT SUM(" + column + ") FROM " + table, Double.class);
        return result != null ? result : 0.0;
    }

    public Map<String, Integer> attendanceSummary() {
        Map<String, Integer> summary = new HashMap<>();
        summary.put("PRESENT", 0);
        summary.put("ABSENT", 0);
        summary.put("LATE", 0);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT status, COUNT(*) as count FROM attendance GROUP BY status");
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
        String cols = columns.isEmpty() ? "*" : String.join(", ", columns) + ", id"; // always ensure id is fetched if possible, though UI may have defined it.
        // Actually, to perfectly match the generic UI, we should just select * to be safe, or explicitly select the requested columns + id.
        String sql = "SELECT * FROM " + table + (orderBy != null && !orderBy.isBlank() ? " ORDER BY " + orderBy : "");
        
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

        String sql = "INSERT INTO " + table + " (" + String.join(", ", insertCols) + ") VALUES (" + String.join(", ", placeholders) + ")";
        jdbcTemplate.update(sql, insertVals.toArray());
    }

    @Transactional
    public void update(String table, List<String> columns, Map<String, String> values) {
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
        jdbcTemplate.update("DELETE FROM " + table + " WHERE id = ?", id);
    }

    /**
     * Aggregate data for the monthly report screen.
     * @param startDate inclusive start date string (yyyy-MM-dd)
     * @param endDate   inclusive end date string   (yyyy-MM-dd)
     */
    public MonthlyReportData monthlyReport(String startDate, String endDate) {
        // Payment totals
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

        // Attendance
        int present = 0, absent = 0, late = 0;
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT status, COUNT(*) as cnt FROM attendance "
                + "WHERE attendance_date >= ? AND attendance_date <= ? GROUP BY status",
                startDate, endDate);
            for (Map<String, Object> row : rows) {
                String st = (String) row.get("status");
                Number n  = (Number)  row.get("cnt");
                if (st == null || n == null) continue;
                switch (st.toUpperCase()) {
                    case "PRESENT" -> present = n.intValue();
                    case "ABSENT"  -> absent  = n.intValue();
                    case "LATE"    -> late    = n.intValue();
                }
            }
        } catch (Exception ignored) {}

        return new MonthlyReportData(income != null ? income : 0.0, paymentCount, present, absent, late);
    }

    public record MonthlyReportData(double income, int paymentCount, int present, int absent, int late) {}

    @Transactional
    public void createStudentEnrollment(Map<String, String> student, Map<String, String> guardian, String course, Map<String, String> payment) {
        // Insert student
        insert("students", new ArrayList<>(student.keySet()), student);
        
        // Fetch inserted student ID (assuming simple setup, this is a naive fetch for highest ID which is unsafe for concurrent, but mimics basic DAO)
        Integer studentId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM students", Integer.class);
        String studentName = student.get("first_name") + " " + student.get("last_name");

        // Insert guardian
        guardian.put("student_name", studentName);
        insert("guardians", new ArrayList<>(guardian.keySet()), guardian);

        // Insert enrollment
        Map<String, String> enrollment = new LinkedHashMap<>();
        enrollment.put("enrollment_date", java.time.LocalDate.now().toString());
        enrollment.put("student_name", studentName);
        enrollment.put("course_name", course);
        enrollment.put("status", "ACTIVE");
        insert("enrollments", new ArrayList<>(enrollment.keySet()), enrollment);

        // Insert payment if exists
        if (payment != null) {
            payment.put("payment_date", java.time.LocalDate.now().toString());
            payment.put("student_name", studentName);
            insert("payments", new ArrayList<>(payment.keySet()), payment);
        }
    }
}
