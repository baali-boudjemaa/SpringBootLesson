package com.example.mef.demo.dashboard.common;

import java.util.ArrayList;
import java.util.List;

/** Shared half-hour time-of-day slots (07:00 → 20:00) used by every schedule-related picker. */
public final class TimeSlots {

    private TimeSlots() {
    }

    /** Half-hour slots from 07:00 to 20:00, formatted "HH:mm". */
    public static List<String> slots() {
        List<String> slots = new ArrayList<>();
        for (int minutes = 7 * 60; minutes <= 20 * 60; minutes += 30) {
            slots.add(String.format("%02d:%02d", minutes / 60, minutes % 60));
        }
        return slots;
    }

    /** Parses "HH:mm" into minutes since midnight. Returns -1 if blank/invalid. */
    public static int toMinutes(String hhmm) {
        if (hhmm == null || hhmm.isBlank()) {
            return -1;
        }
        try {
            String[] parts = hhmm.trim().split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return -1;
        }
    }
}