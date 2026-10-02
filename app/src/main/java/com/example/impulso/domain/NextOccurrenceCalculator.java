package com.example.impulso.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula la próxima ocurrencia con calendario local, sin sumar 24 horas en milisegundos.
 * Hueco de horario de verano: esa hora local no existe y se omite ese día.
 * Solapamiento: una sola ocurrencia local. Si el desfase más temprano ya pasó, no se entrega el tardío.
 * Un aviso con más de 60 minutos de retraso se considera tarde.
 */
public final class NextOccurrenceCalculator {
    public static final long LATE_AFTER_MS = 60L * 60L * 1000L;
    private static final DateTimeFormatter KEY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
    private static final int SEARCH_DAYS = 21;

    private NextOccurrenceCalculator() {
    }

    public static boolean isTooLate(long scheduledAtEpochMs, long nowEpochMs) {
        return nowEpochMs - scheduledAtEpochMs > LATE_AFTER_MS;
    }

    public static PlannedAlarm next(ReminderSlot slot, QuietHours quiet, ZonedDateTime now) {
        if (slot.daysMask == 0) {
            return new PlannedAlarm(slot, PlannedAlarm.Status.NO_MATCHING_DAY, null, null);
        }
        LocalTime time = LocalTime.of(slot.hour, slot.minute);
        if (quiet != null && quiet.contains(time)) {
            return new PlannedAlarm(slot, PlannedAlarm.Status.INSIDE_QUIET, null, null);
        }
        ZoneId zone = now.getZone();
        LocalDate start = now.toLocalDate();
        for (int offset = 0; offset < SEARCH_DAYS; offset++) {
            LocalDate date = start.plusDays(offset);
            if (!slot.matches(date.getDayOfWeek())) {
                continue;
            }
            LocalDateTime local = LocalDateTime.of(date, time);
            List<ZoneOffset> offsets = zone.getRules().getValidOffsets(local);
            if (offsets.isEmpty()) {
                continue;
            }
            ZonedDateTime earliest = null;
            for (ZoneOffset zoneOffset : offsets) {
                ZonedDateTime zoned = ZonedDateTime.ofStrict(local, zoneOffset, zone);
                if (earliest == null || zoned.toInstant().isBefore(earliest.toInstant())) {
                    earliest = zoned;
                }
            }
            if (earliest != null && earliest.isAfter(now)) {
                return new PlannedAlarm(slot, PlannedAlarm.Status.SCHEDULED, earliest, local.format(KEY_FORMAT));
            }
        }
        return new PlannedAlarm(slot, PlannedAlarm.Status.NO_MATCHING_DAY, null, null);
    }

    public static List<PlannedAlarm> plan(List<ReminderSlot> slots, QuietHours quiet, ZonedDateTime now) {
        List<PlannedAlarm> plans = new ArrayList<>();
        if (slots == null) {
            return plans;
        }
        for (ReminderSlot slot : slots) {
            plans.add(next(slot, quiet, now));
        }
        return plans;
    }

    public static PlannedAlarm soonest(List<PlannedAlarm> plans) {
        PlannedAlarm best = null;
        if (plans == null) {
            return null;
        }
        for (PlannedAlarm plan : plans) {
            if (plan.status != PlannedAlarm.Status.SCHEDULED || plan.when == null) {
                continue;
            }
            if (best == null) {
                best = plan;
                continue;
            }
            int compare = plan.when.toInstant().compareTo(best.when.toInstant());
            if (compare < 0 || (compare == 0 && plan.slot.id.compareTo(best.slot.id) < 0)) {
                best = plan;
            }
        }
        return best;
    }

    /** Una sola entrega por clave local, aunque varios horarios coincidan. */
    public static List<PlannedAlarm> uniqueScheduled(List<PlannedAlarm> plans) {
        Map<String, PlannedAlarm> byKey = new LinkedHashMap<>();
        List<PlannedAlarm> scheduled = new ArrayList<>();
        if (plans != null) {
            for (PlannedAlarm plan : plans) {
                if (plan.status == PlannedAlarm.Status.SCHEDULED) {
                    scheduled.add(plan);
                }
            }
        }
        scheduled.sort(Comparator
                .comparing((PlannedAlarm plan) -> plan.when.toInstant())
                .thenComparing(plan -> plan.slot.id));
        for (PlannedAlarm plan : scheduled) {
            byKey.putIfAbsent(plan.key, plan);
        }
        return new ArrayList<>(byKey.values());
    }
}
