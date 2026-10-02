package com.example.impulso.domain;

import java.util.ArrayList;
import java.util.List;

public final class BackupData {
    public static final String FORMAT = "impulso-backup";
    public static final int VERSION = 1;
    public static final int MAX_BYTES = 2_000_000;

    public String format;
    public int version;
    public String exportedAt;
    public boolean includeHistory;
    public Preferences preferences = new Preferences();
    public List<Favorite> favorites = new ArrayList<>();
    public List<OwnQuote> userQuotes = new ArrayList<>();
    public List<DayAction> actions = new ArrayList<>();
    public List<Reminder> reminders = new ArrayList<>();
    public List<HistoryItem> history = new ArrayList<>();

    public static final class Preferences {
        public String name = "";
        public String goal = "";
        public String tone = "";
        public String theme = "system";
        public boolean skipWelcome;
        public float ttsRate = 1f;
        public String ttsVoice = "";
        public boolean quietEnabled = true;
        public int quietStartMin = 22 * 60;
        public int quietEndMin = 8 * 60;
        public int daysMask = ReminderSlot.ALL_DAYS;
        public boolean includeUserQuotes;
        public boolean preferExact;
    }

    public static final class Favorite {
        public String quoteKey;
        public String quoteId;
        public String source;
        public long createdAt;
    }

    public static final class OwnQuote {
        public String id;
        public String text;
        public String category;
        public long createdAt;
        public long updatedAt;
    }

    public static final class DayAction {
        public String date;
        public String status;
        public long updatedAt;
    }

    public static final class Reminder {
        public String id;
        public int hour;
        public int minute;
        public int daysMask;
        public boolean enabled;
        public String kind;
    }

    public static final class HistoryItem {
        public String quoteKey;
        public String quoteId;
        public String source;
        public long viewedAt;
    }
}
