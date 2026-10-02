package com.example.impulso.domain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class BackupCodec {
    public static final String INVALID_JSON = "invalid_json";
    public static final String TOO_LARGE = "too_large";
    public static final String WRONG_FORMAT = "wrong_format";
    public static final String WRONG_VERSION = "wrong_version";
    public static final String INVALID_CONTENT = "invalid_content";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private BackupCodec() {
    }

    public static String toJson(BackupData data) {
        return GSON.toJson(data);
    }

    public static Parse parse(String json) {
        if (json == null) {
            return Parse.error(INVALID_JSON);
        }
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > BackupData.MAX_BYTES) {
            return Parse.error(TOO_LARGE);
        }
        BackupData data;
        try {
            data = GSON.fromJson(json, BackupData.class);
        } catch (JsonSyntaxException exception) {
            return Parse.error(INVALID_JSON);
        }
        if (data == null) {
            return Parse.error(INVALID_JSON);
        }
        String problem = validate(data);
        if (problem != null) {
            return Parse.error(problem);
        }
        if (data.favorites == null) data.favorites = List.of();
        if (data.userQuotes == null) data.userQuotes = List.of();
        if (data.actions == null) data.actions = List.of();
        if (data.reminders == null) data.reminders = List.of();
        if (data.history == null) data.history = List.of();
        return Parse.ok(data);
    }

    private static String validate(BackupData data) {
        if (!BackupData.FORMAT.equals(data.format)) {
            return WRONG_FORMAT;
        }
        if (data.version != BackupData.VERSION) {
            return WRONG_VERSION;
        }
        if (data.preferences == null) {
            return INVALID_CONTENT;
        }
        BackupData.Preferences prefs = data.preferences;
        if (!UserQuoteRules.isValidName(UserQuoteRules.normalize(prefs.name == null ? "" : prefs.name))) {
            return INVALID_CONTENT;
        }
        if (prefs.goal != null && !prefs.goal.isEmpty() && !Goals.isValid(prefs.goal)) {
            return INVALID_CONTENT;
        }
        if (prefs.tone != null && !prefs.tone.isEmpty() && !Tones.isValid(prefs.tone)) {
            return INVALID_CONTENT;
        }
        if (!isTheme(prefs.theme) || prefs.ttsRate < 0.5f || prefs.ttsRate > 1.5f) {
            return INVALID_CONTENT;
        }
        if (prefs.quietStartMin < 0 || prefs.quietStartMin > 1439 || prefs.quietEndMin < 0 || prefs.quietEndMin > 1439) {
            return INVALID_CONTENT;
        }
        if (prefs.daysMask < 0 || prefs.daysMask > ReminderSlot.ALL_DAYS) {
            return INVALID_CONTENT;
        }
        if (!validFavorites(data.favorites) || !validQuotes(data.userQuotes) || !validActions(data.actions)
                || !validReminders(data.reminders) || !validHistory(data.history)) {
            return INVALID_CONTENT;
        }
        return null;
    }

    private static boolean isTheme(String theme) {
        return "system".equals(theme) || "light".equals(theme) || "dark".equals(theme);
    }

    private static boolean validFavorites(List<BackupData.Favorite> items) {
        if (items == null) return true;
        Set<String> keys = new HashSet<>();
        for (BackupData.Favorite item : items) {
            if (item == null || blank(item.quoteKey) || blank(item.quoteId) || !isSource(item.source)) {
                return false;
            }
            if (!keys.add(item.quoteKey)) return false;
        }
        return true;
    }

    private static boolean validQuotes(List<BackupData.OwnQuote> items) {
        if (items == null) return true;
        Set<String> ids = new HashSet<>();
        for (BackupData.OwnQuote item : items) {
            if (item == null || blank(item.id) || !UserQuoteRules.isValid(UserQuoteRules.normalize(item.text))) {
                return false;
            }
            if (item.category != null && !item.category.isEmpty() && !Categories.isValid(item.category)) {
                return false;
            }
            if (!ids.add(item.id)) return false;
        }
        return true;
    }

    private static boolean validActions(List<BackupData.DayAction> items) {
        if (items == null) return true;
        Set<String> dates = new HashSet<>();
        for (BackupData.DayAction item : items) {
            if (item == null || item.date == null || !item.date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return false;
            }
            if (!"done".equals(item.status) && !"skipped".equals(item.status) && !"none".equals(item.status)) {
                return false;
            }
            if (!dates.add(item.date)) return false;
        }
        return true;
    }

    private static boolean validReminders(List<BackupData.Reminder> items) {
        if (items == null) return true;
        Set<String> ids = new HashSet<>();
        for (BackupData.Reminder item : items) {
            if (item == null || blank(item.id) || item.hour < 0 || item.hour > 23 || item.minute < 0 || item.minute > 59) {
                return false;
            }
            if (item.daysMask < 0 || item.daysMask > ReminderSlot.ALL_DAYS) return false;
            if (!"mirror".equals(item.kind) && !"custom".equals(item.kind)) return false;
            if ("mirror".equals(item.kind) && !MirrorHours.isMirror(item.hour, item.minute)) return false;
            if (!ids.add(item.id)) return false;
        }
        return true;
    }

    private static boolean validHistory(List<BackupData.HistoryItem> items) {
        if (items == null) return true;
        for (BackupData.HistoryItem item : items) {
            if (item == null || blank(item.quoteKey) || blank(item.quoteId) || !isSource(item.source)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSource(String source) {
        return "catalog".equals(source) || "user".equals(source);
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static final class Parse {
        public final String error;
        public final BackupData data;

        private Parse(String error, BackupData data) {
            this.error = error;
            this.data = data;
        }

        static Parse error(String error) {
            return new Parse(error, null);
        }

        static Parse ok(BackupData data) {
            return new Parse(null, data);
        }

        public boolean isOk() {
            return error == null && data != null;
        }
    }
}
