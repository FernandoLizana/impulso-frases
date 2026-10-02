package com.example.impulso.domain;

import java.time.DayOfWeek;

public final class ReminderSlot {
    public static final int ALL_DAYS = 0b1111111;

    public final String id;
    public final int hour;
    public final int minute;
    public final int daysMask;

    public ReminderSlot(String id, int hour, int minute, int daysMask) {
        this.id = id;
        this.hour = hour;
        this.minute = minute;
        this.daysMask = daysMask;
    }

    public boolean matches(DayOfWeek day) {
        if (day == null || daysMask == 0) {
            return false;
        }
        int bit = 1 << (day.getValue() - 1);
        return (daysMask & bit) != 0;
    }

    public static int bit(DayOfWeek day) {
        return 1 << (day.getValue() - 1);
    }
}
