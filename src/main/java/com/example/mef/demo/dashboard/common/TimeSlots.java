package com.example.mef.demo.dashboard.common;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Shared half-hour time-of-day slots (07:00 → 20:00) used by every schedule-related picker. */
public final class TimeSlots {

    private TimeSlots() {
    }

    /** Half-hour slots from start to end, formatted "HH:mm". */
    public static List<String> slots(LocalTime start, LocalTime end) {
        List<String> slots = new ArrayList<>();
        int startMinutes = start.getHour() * 60 + start.getMinute();
        int endMinutes = end.getHour() * 60 + end.getMinute();
        for (int minutes = startMinutes; minutes <= endMinutes; minutes += 30) {
            slots.add(String.format("%02d:%02d", minutes / 60, minutes % 60));
        }
        return slots;
    }

    /** Half-hour slots from 07:00 to 20:00, formatted "HH:mm" (Legacy). */
    public static List<String> slots() {
        return slots(LocalTime.of(7, 0), LocalTime.of(20, 0));
    }

    public record TimeBlock(int startMinutes, int endMinutes, boolean isBreak) {}

    public static List<TimeBlock> generateBlocks(String dayStart, String breakStart, String breakEnd, String dayEnd) {
        int startMin = toMinutes(dayStart != null ? dayStart : "08:00");
        int bStartMin = toMinutes(breakStart != null ? breakStart : "12:00");
        int bEndMin = toMinutes(breakEnd != null ? breakEnd : "14:00");
        int endMin = toMinutes(dayEnd != null ? dayEnd : "18:00");

        List<TimeBlock> blocks = new ArrayList<>();

        // Morning blocks (1 hour each)
        int current = startMin;
        while (current < bStartMin) {
            int next = Math.min(current + 60, bStartMin);
            blocks.add(new TimeBlock(current, next, false));
            current = next;
        }

        // Lunch break
        if (bStartMin < bEndMin) {
            blocks.add(new TimeBlock(bStartMin, bEndMin, true));
        }

        // Afternoon blocks (1 hour each)
        current = bEndMin;
        while (current < endMin) {
            int next = Math.min(current + 60, endMin);
            blocks.add(new TimeBlock(current, next, false));
            current = next;
        }

        return blocks;
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