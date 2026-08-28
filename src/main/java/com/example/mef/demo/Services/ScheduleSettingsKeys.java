package com.example.mef.demo.Services;

/**
 * Setting keys shared between the Settings screen ({@code SettingsView})
 * and course-schedule validation ({@code ScheduleValidator}), kept in one
 * place so the two never drift apart.
 */
public final class ScheduleSettingsKeys {

    private ScheduleSettingsKeys() {
    }

    /**
     * Comma-separated list of day names (e.g. "Vendredi,Dimanche") on which
     * the school is closed every week. Courses cannot be scheduled on these
     * days. Empty/blank = no weekly closure configured.
     *
     * Schedules in this app are recurring weekly slots (day name + time),
     * not calendar dates, so "jours de congé" is modeled here as a
     * school-wide weekly closure rather than a dated holiday calendar.
     */
    public static final String CLOSED_DAYS = "schedule.closed_days";
    public static final String CLOSED_DAYS_DEFAULT = "";

    /** Daily timetable boundaries and the non-teaching rest interval (HH:mm). */
    public static final String DAY_START = "schedule.day_start";
    public static final String DAY_END = "schedule.day_end";
    public static final String BREAK_START = "schedule.break_start";
    public static final String BREAK_END = "schedule.break_end";

    public static final String DAY_START_DEFAULT = "08:00";
    public static final String DAY_END_DEFAULT = "18:00";
    public static final String BREAK_START_DEFAULT = "12:00";
    public static final String BREAK_END_DEFAULT = "14:00";

    // Compatibility names used by course scheduling/validation code.
    public static final String REST_START = BREAK_START;
    public static final String REST_END = BREAK_END;
    public static final String REST_START_DEFAULT = BREAK_START_DEFAULT;
    public static final String REST_END_DEFAULT = BREAK_END_DEFAULT;
}
