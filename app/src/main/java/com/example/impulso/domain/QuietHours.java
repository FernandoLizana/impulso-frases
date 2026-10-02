package com.example.impulso.domain;

import java.time.LocalTime;

/**
 * La hora de inicio entra en el silencio. La hora final no: 22:00–08:00 cubre
 * desde las 22:00 inclusive hasta las 08:00 exclusive. Si inicio y fin coinciden,
 * la franja no silencia nada.
 */
public final class QuietHours {
    public final boolean enabled;
    public final LocalTime start;
    public final LocalTime end;

    public QuietHours(boolean enabled, LocalTime start, LocalTime end) {
        this.enabled = enabled;
        this.start = start;
        this.end = end;
    }

    public static QuietHours disabled() {
        return new QuietHours(false, LocalTime.of(22, 0), LocalTime.of(8, 0));
    }

    public static QuietHours ofMinutes(boolean enabled, int startMin, int endMin) {
        return new QuietHours(enabled, minutesToTime(startMin), minutesToTime(endMin));
    }

    public boolean contains(LocalTime time) {
        if (!enabled || start.equals(end) || time == null) {
            return false;
        }
        if (start.isBefore(end)) {
            return !time.isBefore(start) && time.isBefore(end);
        }
        return !time.isBefore(start) || time.isBefore(end);
    }

    public static LocalTime minutesToTime(int minutes) {
        int safe = Math.floorMod(minutes, 24 * 60);
        return LocalTime.of(safe / 60, safe % 60);
    }
}
