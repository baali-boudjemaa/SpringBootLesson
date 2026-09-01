package com.example.mef.demo.dashboard.courses;

import com.example.mef.demo.Model.Classroom;
import com.example.mef.demo.Model.Course;
import com.example.mef.demo.Model.Employee;
import com.example.mef.demo.dashboard.common.TimeSlots;
import com.example.mef.demo.util.I18n;

import java.util.*;

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
 *   <li>the session must not overlap the school's configured daily rest
 *       period, if one is set (❌ الحصة تتعارض مع وقت الراحة)</li>
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
     * @param restStartMinutes start of the school's daily rest/break period,
     *                         in minutes since midnight, or -1 if not configured
     * @param restEndMinutes   end of the daily rest/break period, in minutes
     *                         since midnight, or -1 if not configured
     */
    public static List<String> validate(Course candidate, List<Course> allCourses,
                                        Set<String> closedDays, int enrolledInClassroom,
                                        int restStartMinutes, int restEndMinutes) {
        List<String> errors = new ArrayList<>();
        List<Slot> slots = slotsOf(candidate);
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
        List<Slot> teacherAvailability = teacher == null ? List.of() : availabilityOf(teacher);
        Set<String> classDays = daysOf(classroom == null ? null : classroom.getAttendanceDays());

        for (Slot slot : slots) {
            if (closedDays.contains(slot.day())) {
                errors.add("❌ " + localizedDay(slot.day()) + " " + slot.range() + " : "
                        + I18n.t("schedule.validation.closed_day", "تسجيل الحضور"));
            }

            if (restStartMinutes >= 0 && restEndMinutes >= 0
                    && slot.startMinutes() < restEndMinutes && slot.endMinutes() > restStartMinutes) {
                errors.add("❌ " + slot.day() + " " + slot.range() + " : chevauche le temps de repos ("
                        + format(restStartMinutes) + RANGE_SEPARATOR + format(restEndMinutes) + ") (voir Paramètres).");
            }

            if (teacher != null) {
                if (!teacherAvailability.isEmpty()) {
                    if (!isCoveredByAvailability(slot, teacherAvailability)) {

                        String availableText =
                                teacherAvailability.stream()
                                        .filter(a ->
                                                a.day().equals(slot.day())
                                        )
                                        .sorted(
                                                Comparator.comparingInt(
                                                        Slot::startMinutes
                                                )
                                        )
                                        .map(Slot::range)
                                        .distinct()
                                        .collect(
                                                java.util.stream.Collectors.joining(
                                                        ", "
                                                )
                                        );

                        if (availableText.isBlank()) {

                            errors.add(
                                    "❌ "
                                            + slot.day()
                                            + " "
                                            + slot.range()
                                            + " : l'enseignant n'est pas disponible ce jour."
                            );

                        } else {

                            errors.add(
                                    "❌ "
                                            + slot.day()
                                            + " "
                                            + slot.range()
                                            + " : créneau hors disponibilité de l'enseignant. "
                                            + "Disponibilités ce jour : "
                                            + availableText
                                            + "."
                            );
                        }
                    }
                } else {
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
                    errors.add("❌ " + localizedDay(slot.day()) + " : "
                            + I18n.t("schedule.validation.outside_class_days", "تسجيل الحضور"));
                }
            }

            for (Course other : allCourses) {
                if (other == candidate) continue;
                if (candidate.getId() != null && candidate.getId().equals(other.getId())) continue;

                for (Slot os : slotsOf(other)) {
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

                        String teacherName =
                                teacher.getFirstName()
                                        + " "
                                        + teacher.getLastName();

                        errors.add(
                                "❌ Conflit enseignant : "
                                        + teacherName
                                        + " est déjà affecté au cours « "
                                        + other.getName()
                                        + " » le "
                                        + slot.day()
                                        + " de "
                                        + slot.range()
                                        + "."
                        );
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

    /**
     * True only when the teacher has some declared availability AND this slot isn't
     * covered by it. Teachers with no availability configured are never flagged here
     * (the caller may still want to flag booking conflicts separately).
     */
    public static boolean isOutsideAvailability(Employee teacher, Slot slot) {
        if (teacher == null) {
            return false;
        }
        List<Slot> availability = availabilityOf(teacher);
        if (availability.isEmpty()) {
            return false;
        }
        return !isCoveredByAvailability(slot, availability);
    }

    /** Returns true only when the complete course slot is covered by the teacher's selected cells. */
    private static boolean isCoveredByAvailability(Slot slot, List<Slot> availability) {
        int coveredUntil = slot.startMinutes();
        for (Slot available : availability.stream()
                .filter(a -> a.day().equals(slot.day()))
                .sorted(java.util.Comparator.comparingInt(Slot::startMinutes))
                .toList()) {
            if (available.startMinutes() > coveredUntil) {
                break;
            }
            if (available.endMinutes() > coveredUntil) {
                coveredUntil = available.endMinutes();
            }
            if (coveredUntil >= slot.endMinutes()) {
                return true;
            }
        }
        return false;
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

    /** Reads normalized course slots when present, with text schedules retained for existing records. */
    public static List<Slot> slotsOf(Course course) {
        if (course.getScheduleSlots() != null && !course.getScheduleSlots().isEmpty()) {
            return course.getScheduleSlots().stream()
                    .map(slot -> new Slot(slot.getDayOfWeek(), TimeSlots.toMinutes(slot.getStartTime()),
                            TimeSlots.toMinutes(slot.getEndTime())))
                    .filter(slot -> slot.startMinutes() >= 0 && slot.endMinutes() > slot.startMinutes())
                    .toList();
        }
        return parse(course.getSchedule());
    }

    /** Reads normalized teacher availability when present, with the former text field as a fallback. */
    private static List<Slot> availabilityOf(Employee teacher) {
        if (teacher.getAvailabilitySlots() != null && !teacher.getAvailabilitySlots().isEmpty()) {
            return teacher.getAvailabilitySlots().stream()
                    .map(slot -> new Slot(slot.getDayOfWeek(), TimeSlots.toMinutes(slot.getStartTime()),
                            TimeSlots.toMinutes(slot.getEndTime())))
                    .filter(slot -> slot.startMinutes() >= 0 && slot.endMinutes() > slot.startMinutes())
                    .toList();
        }
        return parse(teacher.getAvailabilitySchedule());
    }

    private static String format(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }

    private static String localizedDay(String raw) {
        return switch (raw == null ? "" : raw.toUpperCase(Locale.ROOT)) {
            case "MONDAY", "LUNDI" -> I18n.t("day.mon", "تسجيل الحضور");
            case "TUESDAY", "MARDI" -> I18n.t("day.tue", "تسجيل الحضور");
            case "WEDNESDAY", "MERCREDI" -> I18n.t("day.wed", "تسجيل الحضور");
            case "THURSDAY", "JEUDI" -> I18n.t("day.thu", "تسجيل الحضور");
            case "FRIDAY", "VENDREDI" -> I18n.t("day.fri", "تسجيل الحضور");
            case "SATURDAY", "SAMEDI" -> I18n.t("day.sat", "تسجيل الحضور");
            case "SUNDAY", "DIMANCHE" -> I18n.t("day.sun", "تسجيل الحضور");
            default -> raw == null ? "" : raw;
        };
    }
}
