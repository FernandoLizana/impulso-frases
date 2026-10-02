package com.example.impulso.domain;

import java.time.ZonedDateTime;

public final class PlannedAlarm {
    public enum Status {
        SCHEDULED,
        INSIDE_QUIET,
        NO_MATCHING_DAY
    }

    public final ReminderSlot slot;
    public final Status status;
    public final ZonedDateTime when;
    public final String key;

    public PlannedAlarm(ReminderSlot slot, Status status, ZonedDateTime when, String key) {
        this.slot = slot;
        this.status = status;
        this.when = when;
        this.key = key;
    }
}
