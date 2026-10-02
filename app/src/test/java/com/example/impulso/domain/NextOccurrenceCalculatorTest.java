package com.example.impulso.domain;

import org.junit.Test;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NextOccurrenceCalculatorTest {
    @Test
    public void mirrorSetRunsFromMidnightToTwentyThreeAndSkipsTwelveTwentyOne() {
        List<LocalTime> mirrors = MirrorHours.all();
        assertEquals(24, mirrors.size());
        assertTrue(mirrors.contains(LocalTime.MIDNIGHT));
        assertTrue(mirrors.contains(LocalTime.of(23, 23)));
        assertTrue(mirrors.contains(LocalTime.of(9, 9)));
        assertFalse(MirrorHours.isMirror(12, 21));
    }

    @Test
    public void pastTimeMovesToTheNextMatchingDay() {
        ZonedDateTime now = ZonedDateTime.of(2026, 9, 29, 15, 0, 0, 0, ZoneId.of("America/Santiago"));
        PlannedAlarm plan = NextOccurrenceCalculator.next(daily(9, 9), QuietHours.disabled(), now);
        assertEquals(PlannedAlarm.Status.SCHEDULED, plan.status);
        assertEquals(LocalDate.of(2026, 9, 30), plan.when.toLocalDate());
        assertEquals(LocalTime.of(9, 9), plan.when.toLocalTime());
    }

    @Test
    public void midnightAfterItPassedIsTomorrow() {
        ZonedDateTime now = ZonedDateTime.of(2026, 9, 29, 0, 30, 0, 0, ZoneId.of("America/Santiago"));
        PlannedAlarm plan = NextOccurrenceCalculator.next(daily(0, 0), QuietHours.disabled(), now);
        assertEquals(LocalDateTime.of(2026, 9, 30, 0, 0), plan.when.toLocalDateTime());
    }

    @Test
    public void quietHoursThatCrossMidnightBlockTheSlot() {
        QuietHours quiet = new QuietHours(true, LocalTime.of(22, 0), LocalTime.of(8, 0));
        ZonedDateTime now = ZonedDateTime.of(2026, 9, 29, 12, 0, 0, 0, ZoneId.of("America/Santiago"));
        PlannedAlarm blocked = NextOccurrenceCalculator.next(daily(23, 23), quiet, now);
        assertEquals(PlannedAlarm.Status.INSIDE_QUIET, blocked.status);
        assertNull(blocked.when);
        PlannedAlarm allowed = NextOccurrenceCalculator.next(daily(9, 9), quiet, now);
        assertEquals(PlannedAlarm.Status.SCHEDULED, allowed.status);
        assertEquals(LocalTime.of(8, 0), QuietHours.minutesToTime(8 * 60));
        assertFalse(quiet.contains(LocalTime.of(8, 0)));
        assertTrue(quiet.contains(LocalTime.of(22, 0)));
        assertTrue(quiet.contains(LocalTime.of(7, 7)));
    }

    @Test
    public void weekendMaskSkipsWeekdays() {
        LocalDate wednesday = LocalDate.of(2026, 9, 30);
        while (wednesday.getDayOfWeek() != DayOfWeek.WEDNESDAY) {
            wednesday = wednesday.plusDays(1);
        }
        int weekend = ReminderSlot.bit(DayOfWeek.SATURDAY) | ReminderSlot.bit(DayOfWeek.SUNDAY);
        ReminderSlot slot = new ReminderSlot("weekend", 9, 9, weekend);
        ZonedDateTime now = wednesday.atTime(8, 0).atZone(ZoneId.of("America/Santiago"));
        PlannedAlarm plan = NextOccurrenceCalculator.next(slot, QuietHours.disabled(), now);
        assertEquals(DayOfWeek.SATURDAY, plan.when.getDayOfWeek());
    }

    @Test
    public void noSelectedDayProducesNoAlarm() {
        PlannedAlarm plan = NextOccurrenceCalculator.next(
                new ReminderSlot("none", 9, 9, 0),
                QuietHours.disabled(),
                ZonedDateTime.of(2026, 9, 29, 8, 0, 0, 0, ZoneId.of("America/Santiago")));
        assertEquals(PlannedAlarm.Status.NO_MATCHING_DAY, plan.status);
    }

    @Test
    public void santiagoKeepsLocalTimeInWinterAndSummer() {
        assertLocalNine(ZonedDateTime.of(2026, 7, 15, 8, 0, 0, 0, ZoneId.of("America/Santiago")));
        assertLocalNine(ZonedDateTime.of(2026, 1, 15, 8, 0, 0, 0, ZoneId.of("America/Santiago")));
    }

    @Test
    public void coincidentSlotsCollapseToOneDelivery() {
        ZonedDateTime now = ZonedDateTime.of(2026, 5, 2, 8, 0, 0, 0, ZoneId.of("Europe/Madrid"));
        List<PlannedAlarm> plans = NextOccurrenceCalculator.plan(List.of(
                new ReminderSlot("b", 11, 11, ReminderSlot.ALL_DAYS),
                new ReminderSlot("a", 11, 11, ReminderSlot.ALL_DAYS)), QuietHours.disabled(), now);
        List<PlannedAlarm> unique = NextOccurrenceCalculator.uniqueScheduled(plans);
        assertEquals(1, unique.size());
        assertEquals("a", unique.get(0).slot.id);
    }

    @Test
    public void gapIsSkippedAndOverlapIsDeliveredOnce() {
        ZoneId zone = ZoneId.of("America/New_York");
        ZoneOffsetTransition gap = findTransition(zone, true);
        ZoneOffsetTransition overlap = findTransition(zone, false);
        assertNotNull(gap);
        assertNotNull(overlap);

        LocalDateTime missing = gap.getDateTimeBefore().plusMinutes(30);
        assertTrue(zone.getRules().getValidOffsets(missing).isEmpty());
        ZonedDateTime beforeGap = gap.getDateTimeBefore().minusHours(1).atZone(zone);
        PlannedAlarm afterGap = NextOccurrenceCalculator.next(
                new ReminderSlot("gap", missing.getHour(), missing.getMinute(), ReminderSlot.ALL_DAYS),
                QuietHours.disabled(),
                beforeGap);
        assertEquals(PlannedAlarm.Status.SCHEDULED, afterGap.status);
        assertNotEquals(missing.toLocalDate(), afterGap.when.toLocalDate());
        assertEquals(missing.toLocalTime(), afterGap.when.toLocalTime());

        LocalDateTime repeated = overlap.getDateTimeAfter().plusMinutes(30);
        List<ZoneOffset> offsets = zone.getRules().getValidOffsets(repeated);
        assertEquals(2, offsets.size());
        ZonedDateTime earlier = ZonedDateTime.ofStrict(repeated, offsets.get(0), zone);
        ZonedDateTime later = ZonedDateTime.ofStrict(repeated, offsets.get(1), zone);
        if (later.toInstant().isBefore(earlier.toInstant())) {
            ZonedDateTime swap = earlier;
            earlier = later;
            later = swap;
        }
        ReminderSlot slot = new ReminderSlot("overlap", repeated.getHour(), repeated.getMinute(), ReminderSlot.ALL_DAYS);
        PlannedAlarm first = NextOccurrenceCalculator.next(slot, QuietHours.disabled(), earlier.minusMinutes(10));
        assertEquals(repeated.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")), first.key);
        assertEquals(earlier.toInstant(), first.when.toInstant());
        PlannedAlarm second = NextOccurrenceCalculator.next(slot, QuietHours.disabled(), earlier.plusMinutes(20));
        assertNotEquals(first.key, second.key);
        assertTrue(second.when.toInstant().isAfter(later.toInstant()));
    }

    @Test
    public void santiagoSeasonalTransitionDoesNotDuplicateLocalTime() {
        ZoneId zone = ZoneId.of("America/Santiago");
        ZoneOffsetTransition transition = findAnyTransition(zone);
        assertNotNull(transition);
        if (transition.isGap()) {
            LocalDateTime missing = transition.getDateTimeBefore().plusMinutes(20);
            if (!zone.getRules().getValidOffsets(missing).isEmpty()) {
                missing = transition.getDateTimeBefore();
            }
            PlannedAlarm plan = NextOccurrenceCalculator.next(
                    new ReminderSlot("scl", missing.getHour(), missing.getMinute(), ReminderSlot.ALL_DAYS),
                    QuietHours.disabled(),
                    transition.getDateTimeBefore().minusHours(2).atZone(zone));
            assertNotEquals(missing.toLocalDate(), plan.when.toLocalDate());
        } else {
            LocalDateTime repeated = transition.getDateTimeAfter().plusMinutes(20);
            if (zone.getRules().getValidOffsets(repeated).size() < 2) {
                repeated = transition.getDateTimeAfter();
            }
            PlannedAlarm first = NextOccurrenceCalculator.next(
                    new ReminderSlot("scl", repeated.getHour(), repeated.getMinute(), ReminderSlot.ALL_DAYS),
                    QuietHours.disabled(),
                    repeated.atZone(zone).withEarlierOffsetAtOverlap().minusMinutes(5));
            PlannedAlarm second = NextOccurrenceCalculator.next(
                    new ReminderSlot("scl", repeated.getHour(), repeated.getMinute(), ReminderSlot.ALL_DAYS),
                    QuietHours.disabled(),
                    first.when.plusMinutes(30));
            assertNotEquals(first.key, second.key);
        }
    }

    @Test
    public void lateBoundaryIsStrictlyOverSixtyMinutes() {
        long start = 1_000_000L;
        assertFalse(NextOccurrenceCalculator.isTooLate(start, start + NextOccurrenceCalculator.LATE_AFTER_MS));
        assertTrue(NextOccurrenceCalculator.isTooLate(start, start + NextOccurrenceCalculator.LATE_AFTER_MS + 1));
    }

    @Test
    public void exactModeRequiresBothDesireAndCapability() {
        assertEquals(DeliveryCapability.OFF, DeliveryCapability.resolve(false, true, true, true, true));
        assertEquals(DeliveryCapability.BLOCKED, DeliveryCapability.resolve(true, false, true, true, true));
        assertEquals(DeliveryCapability.BLOCKED, DeliveryCapability.resolve(true, true, false, true, true));
        assertEquals(DeliveryCapability.APPROXIMATE, DeliveryCapability.resolve(true, true, true, true, false));
        assertEquals(DeliveryCapability.APPROXIMATE, DeliveryCapability.resolve(true, true, true, false, true));
        assertEquals(DeliveryCapability.EXACT, DeliveryCapability.resolve(true, true, true, true, true));
    }

    private static void assertLocalNine(ZonedDateTime now) {
        PlannedAlarm plan = NextOccurrenceCalculator.next(daily(9, 9), QuietHours.disabled(), now);
        assertEquals(ZoneId.of("America/Santiago"), plan.when.getZone());
        assertEquals(LocalTime.of(9, 9), plan.when.toLocalTime());
        assertEquals(now.toLocalDate(), plan.when.toLocalDate());
    }

    private static ReminderSlot daily(int hour, int minute) {
        return new ReminderSlot("slot-" + hour + "-" + minute, hour, minute, ReminderSlot.ALL_DAYS);
    }

    private static ZoneOffsetTransition findTransition(ZoneId zone, boolean gap) {
        return findTransitionSince(zone, gap, Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static ZoneOffsetTransition findAnyTransition(ZoneId zone) {
        ZoneOffsetTransition transition = findTransitionSince(zone, true, Instant.parse("2020-01-01T00:00:00Z"));
        if (transition != null) {
            return transition;
        }
        return findTransitionSince(zone, false, Instant.parse("2020-01-01T00:00:00Z"));
    }

    private static ZoneOffsetTransition findTransitionSince(ZoneId zone, boolean gap, Instant start) {
        ZoneRules rules = zone.getRules();
        Instant cursor = start;
        for (int i = 0; i < 8; i++) {
            ZoneOffsetTransition transition = rules.nextTransition(cursor);
            if (transition == null) {
                return null;
            }
            if (transition.isGap() == gap) {
                return transition;
            }
            cursor = transition.getInstant().plusSeconds(1);
        }
        return null;
    }
}
