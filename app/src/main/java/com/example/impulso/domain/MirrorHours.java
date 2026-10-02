package com.example.impulso.domain;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Horas espejo: hora y minuto iguales, de 00:00 a 23:23. 12:21 no pertenece a este conjunto. */
public final class MirrorHours {
    public static final int[][] SUGGESTED = {{9, 9}, {11, 11}, {20, 20}};

    private MirrorHours() {
    }

    public static boolean isMirror(int hour, int minute) {
        return hour >= 0 && hour <= 23 && hour == minute;
    }

    public static List<LocalTime> all() {
        List<LocalTime> times = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            times.add(LocalTime.of(hour, hour));
        }
        return times;
    }
}
