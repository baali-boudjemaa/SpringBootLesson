package com.example.mef.demo.Model;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Central definition of every CRUD module available in the app
 * (Students, Teachers, Classes, ...). Extracted from
 * DashboardController.registerModules() so that NavigationBuilder,
 * GlobalSearch, and ModuleView can all share the same list instead
 * of each depending on the controller directly.
 */
@Component
public final class ModuleRegistry {

    private final List<Module> modules = new ArrayList<>();

    public ModuleRegistry() {
        register();
    }

    /** Immutable snapshot of all registered modules, in nav display order. */
    public List<Module> all() {
        return List.copyOf(modules);
    }

    public Module byTable(String table) {
        return modules.stream()
                .filter(m -> m.table().equals(table))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown module table: " + table));
    }

    public Module byTitleKey(String titleKey) {
        return modules.stream()
                .filter(m -> m.titleKey().equals(titleKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown module titleKey: " + titleKey));
    }

    private void register() {
        modules.add(new Module("nav.students", "students", "last_name, first_name",
                List.of(
                        new Field("first_name",    "field.first_name"),
                        new Field("last_name",     "field.last_name"),
                        new Field("gender",        "field.gender",    List.of("Fille", "Garçon", "Autre")),
                        new Field("date_of_birth", "field.date_of_birth"),
                        new Field("classroom",     "field.classroom"),
                        new Field("phone",         "field.phone"),
                        new Field("status",        "field.status",    List.of("ACTIVE", "INACTIVE"))
                )));
        modules.add(new Module("nav.teachers", "teachers", "last_name, first_name",
                List.of(
                        new Field("first_name", "field.first_name"),
                        new Field("last_name",  "field.last_name"),
                        new Field("email",      "field.email"),
                        new Field("phone",      "field.phone"),
                        new Field("specialty",  "field.specialty"),
                        new Field("status",     "field.status",   List.of("TEACHER", "ASSISTANT", "KITCHEN", "CLEANER", "ADMIN"))
                )));
        modules.add(new Module("nav.classes", "classes", "name",
                List.of(
                        new Field("name",         "field.name"),
                        new Field("grade_level",  "field.grade_level"),
                        new Field("room",         "field.room"),
                        new Field("teacher_name", "field.teacher"),
                        new Field("capacity",     "field.capacity"),
                        new Field("status",       "field.status", List.of("ACTIVE", "INACTIVE"))
                )));
        modules.add(new Module("nav.rooms", "rooms", "name",
                List.of(
                        new Field("name",     "field.name"),
                        new Field("location", "field.location"),
                        new Field("capacity", "field.capacity"),
                        new Field("active",   "field.status", List.of("ACTIVE", "INACTIVE"))
                )));
        modules.add(new Module("nav.guardians", "guardians", "last_name, first_name",
                List.of(
                        new Field("first_name",   "field.first_name"),
                        new Field("last_name",    "field.last_name"),
                        new Field("relationship", "field.relationship"),
                        new Field("phone",        "field.phone"),
                        new Field("email",        "field.email"),
                        new Field("student_name", "field.student")
                )));
        modules.add(new Module("nav.courses", "courses", "name",
                List.of(
                        new Field("name",         "field.name"),
                        new Field("teacher_name", "field.teacher"),
                        new Field("classroom",    "field.classroom"),
                        new Field("schedule",     "field.schedule"),
                        new Field("monthly_fee",  "field.monthly_fee"),
                        new Field("status",       "field.status", List.of("ACTIVE", "INACTIVE"))
                )));
        modules.add(new Module("nav.attendance", "attendance", "attendance_date DESC",
                List.of(
                        new Field("attendance_date", "field.date"),
                        new Field("student_name",    "field.student"),
                        new Field("course_name",     "field.course"),
                        new Field("status",          "field.status", List.of("PRESENT", "ABSENT", "LATE")),
                        new Field("notes",           "field.notes")
                )));
        modules.add(new Module("nav.enrollments", "enrollments", "enrollment_date DESC",
                List.of(
                        new Field("enrollment_date", "field.date"),
                        new Field("student_name",    "field.student"),
                        new Field("course_name",     "field.course"),
                        new Field("status",          "field.status", List.of("ACTIVE", "COMPLETED", "DROPPED"))
                )));
        modules.add(new Module("nav.payments", "payments", "payment_date DESC",
                List.of(
                        new Field("payment_date",  "field.date"),
                        new Field("student_name",  "field.student"),
                        new Field("amount",        "field.amount"),
                        new Field("method",        "field.method",   List.of("Cash", "Virement", "Carte", "Chèque")),
                        new Field("category",      "field.category", List.of("Scolarité", "Cours", "Transport", "Autre")),
                        new Field("status",        "field.status",   List.of("PAID", "PENDING", "OVERDUE"))
                )));
        modules.add(new Module("nav.reports", "reports", "created_at DESC",
                List.of(
                        new Field("title",       "field.title"),
                        new Field("report_type", "field.type", List.of("Academic", "Financial", "Attendance", "General")),
                        new Field("created_at",  "field.date"),
                        new Field("summary",     "field.summary")
                )));
        modules.add(new Module("nav.users", "users", "full_name",
                List.of(
                        new Field("username",      "field.username"),
                        new Field("password_hash", "field.password"),
                        new Field("full_name",     "field.full_name"),
                        new Field("role",          "field.role", List.of("ADMIN", "TEACHER", "STAFF"))
                )));
        modules.add(new Module("nav.outcoming", "outcoming", "date_outcome DESC",
                List.of(
                        new Field("date_outcome",  "field.date"),
                        new Field("label",         "field.label"),
                        new Field("beneficiary",   "field.beneficiary"),
                        new Field("amount",        "field.amount"),
                        new Field("method",        "field.method",   List.of("Cash", "Virement", "Carte", "Chèque")),
                        new Field("category",      "field.category", List.of("Salaires", "Loyer", "Fournitures", "Électricité", "Eau", "Maintenance", "Nourriture", "Transport", "Autre")),
                        new Field("status",        "field.status",   List.of("PAID", "PENDING", "OVERDUE"))
                )));
        modules.add(new Module("nav.settings", "settings", "setting_key",
                List.of(
                        new Field("setting_key",   "field.setting"),
                        new Field("setting_value", "field.value"),
                        new Field("description",   "field.description")
                )));

    }
}