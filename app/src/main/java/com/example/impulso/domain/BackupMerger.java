package com.example.impulso.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Combina por identificador estable. Si hay conflicto, se queda la versión local.
 * Nunca enciende recordatorios: ese interruptor no viaja como permiso.
 */
public final class BackupMerger {
    public static final int HISTORY_LIMIT = 200;

    public static final class Result {
        public BackupData data;
        public boolean preferencesKeptLocal;
        public int favoritesAdded;
        public int quotesAdded;
        public int quotesKeptLocal;
        public int actionsAdded;
        public int actionsKeptLocal;
        public int remindersAdded;
        public int remindersKeptLocal;
        public int historyAdded;
    }

    private BackupMerger() {
    }

    public static Result merge(BackupData local, boolean localProfileEstablished, BackupData incoming) {
        Result result = new Result();
        BackupData merged = new BackupData();
        merged.format = BackupData.FORMAT;
        merged.version = BackupData.VERSION;
        if (localProfileEstablished) {
            merged.preferences = copyPrefs(local.preferences);
            result.preferencesKeptLocal = true;
        } else {
            merged.preferences = copyPrefs(incoming.preferences);
            result.preferencesKeptLocal = false;
        }

        Map<String, BackupData.Favorite> favorites = mapFavorites(local.favorites);
        int favBefore = favorites.size();
        for (BackupData.Favorite favorite : safeFavorites(incoming)) {
            favorites.putIfAbsent(favorite.quoteKey, favorite);
        }
        merged.favorites = new ArrayList<>(favorites.values());
        result.favoritesAdded = favorites.size() - favBefore;

        Map<String, BackupData.OwnQuote> quotes = mapQuotes(local.userQuotes);
        for (BackupData.OwnQuote quote : safeQuotes(incoming)) {
            if (quotes.containsKey(quote.id)) {
                result.quotesKeptLocal++;
            } else {
                quotes.put(quote.id, quote);
                result.quotesAdded++;
            }
        }
        merged.userQuotes = new ArrayList<>(quotes.values());

        Map<String, BackupData.DayAction> actions = mapActions(local.actions);
        for (BackupData.DayAction action : safeActions(incoming)) {
            if (actions.containsKey(action.date)) {
                result.actionsKeptLocal++;
            } else {
                actions.put(action.date, action);
                result.actionsAdded++;
            }
        }
        merged.actions = new ArrayList<>(actions.values());

        Map<String, BackupData.Reminder> reminders = mapReminders(local.reminders);
        for (BackupData.Reminder reminder : safeReminders(incoming)) {
            if (reminders.containsKey(reminder.id)) {
                result.remindersKeptLocal++;
            } else {
                reminders.put(reminder.id, reminder);
                result.remindersAdded++;
            }
        }
        merged.reminders = new ArrayList<>(reminders.values());

        Map<String, BackupData.HistoryItem> history = new LinkedHashMap<>();
        for (BackupData.HistoryItem item : safeHistory(local)) {
            history.put(historyKey(item), item);
        }
        if (incoming.includeHistory) {
            for (BackupData.HistoryItem item : safeHistory(incoming)) {
                if (history.putIfAbsent(historyKey(item), item) == null) {
                    result.historyAdded++;
                }
            }
        }
        List<BackupData.HistoryItem> historyList = new ArrayList<>(history.values());
        historyList.sort((a, b) -> Long.compare(b.viewedAt, a.viewedAt));
        if (historyList.size() > HISTORY_LIMIT) {
            historyList = new ArrayList<>(historyList.subList(0, HISTORY_LIMIT));
        }
        merged.history = historyList;
        merged.includeHistory = incoming.includeHistory;
        result.data = merged;
        return result;
    }

    private static BackupData.Preferences copyPrefs(BackupData.Preferences source) {
        BackupData.Preferences copy = new BackupData.Preferences();
        if (source == null) {
            return copy;
        }
        copy.name = source.name == null ? "" : UserQuoteRules.normalize(source.name);
        copy.goal = source.goal == null ? "" : source.goal;
        copy.tone = source.tone == null ? "" : source.tone;
        copy.theme = source.theme == null ? "system" : source.theme;
        copy.skipWelcome = source.skipWelcome;
        copy.ttsRate = source.ttsRate;
        copy.ttsVoice = source.ttsVoice == null ? "" : source.ttsVoice;
        copy.quietEnabled = source.quietEnabled;
        copy.quietStartMin = source.quietStartMin;
        copy.quietEndMin = source.quietEndMin;
        copy.daysMask = source.daysMask;
        copy.includeUserQuotes = source.includeUserQuotes;
        copy.preferExact = source.preferExact;
        return copy;
    }

    private static Map<String, BackupData.Favorite> mapFavorites(List<BackupData.Favorite> items) {
        Map<String, BackupData.Favorite> map = new LinkedHashMap<>();
        for (BackupData.Favorite item : safeFavoritesOf(items)) map.put(item.quoteKey, item);
        return map;
    }

    private static Map<String, BackupData.OwnQuote> mapQuotes(List<BackupData.OwnQuote> items) {
        Map<String, BackupData.OwnQuote> map = new LinkedHashMap<>();
        for (BackupData.OwnQuote item : safeQuotesOf(items)) map.put(item.id, item);
        return map;
    }

    private static Map<String, BackupData.DayAction> mapActions(List<BackupData.DayAction> items) {
        Map<String, BackupData.DayAction> map = new LinkedHashMap<>();
        for (BackupData.DayAction item : safeActionsOf(items)) map.put(item.date, item);
        return map;
    }

    private static Map<String, BackupData.Reminder> mapReminders(List<BackupData.Reminder> items) {
        Map<String, BackupData.Reminder> map = new LinkedHashMap<>();
        for (BackupData.Reminder item : safeRemindersOf(items)) map.put(item.id, item);
        return map;
    }

    private static String historyKey(BackupData.HistoryItem item) {
        return item.quoteKey + "@" + item.viewedAt;
    }

    private static List<BackupData.Favorite> safeFavorites(BackupData data) {
        return safeFavoritesOf(data.favorites);
    }

    private static List<BackupData.OwnQuote> safeQuotes(BackupData data) {
        return safeQuotesOf(data.userQuotes);
    }

    private static List<BackupData.DayAction> safeActions(BackupData data) {
        return safeActionsOf(data.actions);
    }

    private static List<BackupData.Reminder> safeReminders(BackupData data) {
        return safeRemindersOf(data.reminders);
    }

    private static List<BackupData.HistoryItem> safeHistory(BackupData data) {
        return data.history == null ? List.of() : data.history;
    }

    private static List<BackupData.Favorite> safeFavoritesOf(List<BackupData.Favorite> items) {
        return items == null ? List.of() : items;
    }

    private static List<BackupData.OwnQuote> safeQuotesOf(List<BackupData.OwnQuote> items) {
        return items == null ? List.of() : items;
    }

    private static List<BackupData.DayAction> safeActionsOf(List<BackupData.DayAction> items) {
        return items == null ? List.of() : items;
    }

    private static List<BackupData.Reminder> safeRemindersOf(List<BackupData.Reminder> items) {
        return items == null ? List.of() : items;
    }
}
