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
}