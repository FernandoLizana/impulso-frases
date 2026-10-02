package com.example.impulso.notify;

public final class AlarmContract {
    public static final String ACTION_ALARM = "com.example.impulso.action.REMINDER_ALARM";
    public static final int REQUEST_ALARM = 4101;
    public static final int REQUEST_OPEN = 4102;
    public static final int REQUEST_OPEN_TEST = 4103;
    public static final int NOTIF_REMINDER = 5101;
    public static final int NOTIF_TEST = 5102;
    public static final int NOTIF_LOCK = 5103;
    public static final int NOTIF_CLOSE = 5104;
    public static final int NOTIF_TIMER = 5105;
    public static final String EXTRA_KEY = "occurrence_key";
    public static final String EXTRA_AT = "scheduled_at";
    public static final String CHANNEL_ID = "impulso_reminders";

    private AlarmContract() {
    }
}
