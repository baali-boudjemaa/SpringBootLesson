package com.example.mef.demo.dashboard.courses;

import com.example.mef.demo.Model.Classroom;
import com.example.mef.demo.Model.Course;
import com.example.mef.demo.Model.Employee;
import com.example.mef.demo.dashboard.common.TimeSlots;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Enforces the school's timetable rules before a course is saved:
 *
 * <ul>
 *   <li>a teacher cannot be booked twice at the same time (❌ المعلم محجوز في نفس الوقت)</li>
 *   <li>a class/room cannot be booked twice at the same time, which also
 *       covers "conflict with another session of the same class"
 *       (❌ القاعة محجوزة / ❌ تعارض مع حصة أخرى لنفس القسم — in this app a
 *       Classroom entity represents both the section and its room, so
 *       these two rules collapse into one check)</li>
 *   <li>an exact duplicate of another session — same teacher, same class,
 *       same day/time — is reported as a single "duplicate" error rather
 *       than three overlapping ones (❌ تكرار نفس الحصة)</li>
 *   <li>class capacity must not be exceeded (❌ تجاوز السعة)</li>
 *   <li>the teacher's configured working days/hours must be respected
 *       (❌ تجاوز ساعات عمل المعلم)</li>
 *   <li>the session must fall within the class's configured period
 *       (❌ الحصة خارج فترة القسم)</li>
 *   <li>the session's day must be one of the class's attendance days
 *       (❌ الحصة خارج أيام الحضور)</li>
 *   <li>the session's day must not be a configured weekly closure day
 *       (❌ الحصة في يوم عطلة)</li>
 * </ul>
 */
public final class ScheduleValidator {

    public static final String ENTRY_SEPARATOR = "; ";
    public static final String RANGE_SEPARATOR = "-";

    private ScheduleValidator() {
    }

    public record Slot(String day, int startMinutes, int endMinutes) {
        String range() {
            return format(startMinutes) + RANGE_SEPARATOR + format(endMinutes);
        }
    }

    /**
     * Validates {@code candidate} (with its teacher/classroom/schedule already set)
     * against every other course, plus the rules configured on its teacher and
     * classroom. Returns a list of human-readable "❌ ..." violation messages;
     * empty means the schedule is valid.
     *
     * @param enrolledInClassroom number of students currently enrolled in the
     *                            candidate's classroom (0 if no classroom set)
     */
    public static List<String> validate(Course candidate, List<Course> allCourses,
                                        Set<String> closedDays, int enrolledInClassroom) {
        List<String> errors = new ArrayList<>();
        List<Slot> slots = parse(candidate.getSchedule());
        if (slots.isEmpty()) {
            return errors;
        }

        Employee teacher = candidate.getTeacher();
        Classroom classroom = candidate.getClassroom();

        if (classroom != null && classroom.getCapacity() != null && enrolledInClassroom > classroom.getCapacity()) {
            errors.add("❌ Capacité dépassée : " + enrolledInClassroom + "/" + classroom.getCapacity()
                    + " élèves inscrits dans « " + classroom.getName() + " ».");
        }

        Set<String> teacherDays = daysOf(teacher == null ? null : teacher.getWorkingDays());
        Set<String> classDays = daysOf(classroom == null ? null : classroom.getAttendanceDays());

        for (Slot slot : slots) {
            if (closedDays.contains(slot.day())) {
                errors.add("❌ " + slot.day() + " " + slot.range() + " : ce jour est un jour de fermeture (voir Paramètres).");
            }

            if (teacher != null) {
                if (!teacherDays.isEmpty() && !teacherDays.contains(slot.day())) {
                    errors.add("❌ " + slot.day() + " : en dehors des jours de travail de l'enseignant.");
                }
                int workStart = TimeSlots.toMinutes(teacher.getWorkStartTime());
                int workEnd = TimeSlots.toMinutes(teacher.getWorkEndTime());
                if (workStart >= 0 && slot.startMinutes() < workStart) {
                    errors.add("❌ " + slot.day() + " " + slot.range()
                            + " : dépasse les heures de travail de l'enseignant (début à " + teacher.getWorkStartTime() + ").");
                }
                if (workEnd >= 0 && slot.endMinutes() > workEnd) {
                    errors.add("❌ " + slot.day() + " " + slot.range()
                            + " : dépasse les heures de travail de l'enseignant (fin à " + teacher.getWorkEndTime() + ").");
                }
            }

            if (classroom != null) {
                int periodStart = TimeSlots.toMinutes(classroom.getPeriodStartTime());
                int periodEnd = TimeSlots.toMinutes(classroom.getPeriodEndTime());
                if (periodStart >= 0 && slot.startMinutes() < periodStart) {
                    errors.add("❌ " + slot.day() + " " + slot.range()
                            + " : la séance est en dehors de la période de la classe (début à " + classroom.getPeriodStartTime() + ").");
                }
                if (periodEnd >= 0 && slot.endMinutes() > periodEnd) {
                    errors.add("❌ " + slot.day() + " " + slot.range()
                            + " : la séance est en dehors de la période de la classe (fin à " + classroom.getPeriodEndTime() + ").");
                }
                if (!classDays.isEmpty() && !classDays.contains(slot.day())) {
                    errors.add("❌ " + slot.day() + " : en dehors des jours de présence de la classe.");
                }
            }

            for (Course other : allCourses) {
                if (other == candidate) continue;
                if (candidate.getId() != null && candidate.getId().equals(other.getId())) continue;

                for (Slot os : parse(other.getSchedule())) {
                    if (!os.day().equals(slot.day())) continue;
                    boolean overlap = slot.startMinutes() < os.endMinutes() && os.startMinutes() < slot.endMinutes();
                    if (!overlap) continue;

                    boolean sameTeacher = teacher != null && other.getTeacher() != null
                            && teacher.getId().equals(other.getTeacher().getId());
                    boolean sameClassroom = classroom != null && other.getClassroom() != null
                            && classroom.getId().equals(other.getClassroom().getId());
                    boolean exactSame = slot.startMinutes() == os.startMinutes() && slot.endMinutes() == os.endMinutes();

                    if (sameTeacher && sameClassroom && exactSame) {
                        errors.add("❌ Séance en double : identique à « " + other.getName()
                                + " » (" + slot.day() + " " + slot.range() + ", même enseignant, même classe).");
                        continue;
                    }
                    if (sameTeacher) {
                        errors.add("❌ L'enseignant est déjà réservé : " + slot.day() + " " + slot.range()
                                + " (cours « " + other.getName() + " »).");
                    }
                    if (sameClassroom) {
                        errors.add("❌ La classe/salle est déjà réservée : " + slot.day() + " " + slot.range()
                                + " (cours « " + other.getName() + " »).");
                    }
                }
            }
        }

        return errors.stream().distinct().toList();
    }

    /** Parses a Setting value like "Vendredi,Dimanche" into a day-name set. */
    public static Set<String> daysOf(String commaSeparated) {
        if (commaSeparated == null || commaSeparated.isBlank()) {
            return Set.of();
        }
        Set<String> set = new LinkedHashSet<>();
        for (String day : commaSeparated.split(",")) {
            String trimmed = day.trim();
            if (!trimmed.isEmpty()) {
                set.add(trimmed);
            }
        }
        return set;
    }

    /** Parses "Lundi 08:00-10:00; Mercredi 14:00-16:00" into slots. Unknown/malformed entries are skipped. */
    public static List<Slot> parse(String schedule) {
        List<Slot> slots = new ArrayList<>();
        if (schedule == null || schedule.isBlank()) {
            return slots;
        }
        for (String rawEntry : schedule.split(";")) {
            String entry = rawEntry.trim();
            if (entry.isEmpty()) continue;

            int firstSpace = entry.indexOf(' ');
            if (firstSpace < 0) continue;

            String day = entry.substring(0, firstSpace).trim();
            String range = entry.substring(firstSpace + 1).trim();
            String[] parts = range.split(RANGE_SEPARATOR);
            if (parts.length != 2) continue;

            int start = TimeSlots.toMinutes(parts[0]);
            int end = TimeSlots.toMinutes(parts[1]);
            if (start < 0 || end < 0) continue;

            slots.add(new Slot(day, start, end));
        }
        return slots;
    }

    private static String format(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }
}