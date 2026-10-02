package com.example.impulso.data.repo;

import androidx.lifecycle.LiveData;

import com.example.impulso.data.db.AppDatabase;
import com.example.impulso.data.db.DailyStateEntity;
import com.example.impulso.data.db.FavoriteEntity;
import com.example.impulso.data.db.HistoryEntity;
import com.example.impulso.data.db.OccurrenceEntity;
import com.example.impulso.data.db.ReminderEntity;
import com.example.impulso.data.db.UserQuoteEntity;
import com.example.impulso.domain.BackupData;
import com.example.impulso.domain.BackupMerger;
import com.example.impulso.domain.QuoteRefs;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class UserDataRepository {
    private final AppDatabase database;

    public UserDataRepository(AppDatabase database) {
        this.database = database;
    }

    public LiveData<List<FavoriteEntity>> observeFavorites() {
        return database.favoriteDao().observeAll();
    }

    public LiveData<List<UserQuoteEntity>> observeUserQuotes() {
        return database.userQuoteDao().observeAll();
    }

    public LiveData<List<HistoryEntity>> observeHistory() {
        return database.historyDao().observeRecent(BackupMerger.HISTORY_LIMIT);
    }

    public void io(Runnable work) {
        AppDatabase.IO.execute(work);
    }

    public void recordView(String source, String id) {
        if (source == null || id == null || id.isEmpty()) {
            return;
        }
        AppDatabase.IO.execute(() -> {
            String key = QuoteRefs.key(source, id);
            HistoryEntity latest = database.historyDao().latest();
            long now = System.currentTimeMillis();
            if (latest != null && key.equals(latest.quoteKey) && now - latest.viewedAt < 2000) {
                return;
            }
            HistoryEntity row = new HistoryEntity();
            row.quoteKey = key;
            row.quoteId = id;
            row.source = source;
            row.viewedAt = now;
            database.historyDao().insert(row);
            database.historyDao().trim(BackupMerger.HISTORY_LIMIT);
        });
    }

    public void toggleFavorite(String source, String id, Consumer<Boolean> onDone) {
        AppDatabase.IO.execute(() -> {
            String key = QuoteRefs.key(source, id);
            FavoriteEntity existing = database.favoriteDao().find(key);
            boolean nowFavorite;
            if (existing == null) {
                FavoriteEntity row = new FavoriteEntity();
                row.quoteKey = key;
                row.quoteId = id;
                row.source = source;
                row.createdAt = System.currentTimeMillis();
                database.favoriteDao().upsert(row);
                nowFavorite = true;
            } else {
                database.favoriteDao().delete(key);
                nowFavorite = false;
            }
            if (onDone != null) {
                onDone.accept(nowFavorite);
            }
        });
    }

    public boolean isFavoriteSync(String source, String id) {
        return database.favoriteDao().find(QuoteRefs.key(source, id)) != null;
    }

    public UserQuoteEntity findUserSync(String id) {
        return database.userQuoteDao().find(id);
    }

    public List<UserQuoteEntity> userQuotesSync() {
        return database.userQuoteDao().all();
    }

    public List<String> recentIdsSync() {
        return database.historyDao().idsNewestFirst();
    }

    public DailyStateEntity findDailySync(String date) {
        return database.dailyDao().find(date);
    }

    public void saveDailySync(DailyStateEntity entity) {
        database.dailyDao().upsert(entity);
    }

    public List<HistoryEntity> historySync() {
        return database.historyDao().all();
    }

    public List<DailyStateEntity> daysSync(String from, String to) {
        return database.dailyDao().between(from, to);
    }

    public void setAction(String date, String status) {
        AppDatabase.IO.execute(() -> {
            DailyStateEntity row = database.dailyDao().find(date);
            if (row == null) {
                return;
            }
            row.actionStatus = status;
            row.actionUpdatedAt = System.currentTimeMillis();
            database.dailyDao().upsert(row);
        });
    }

    public String saveUserQuoteSync(String id, String text, String category) {
        long now = System.currentTimeMillis();
        UserQuoteEntity row = id == null ? null : database.userQuoteDao().find(id);
        if (row == null) {
            row = new UserQuoteEntity();
            row.id = "user_" + UUID.randomUUID();
            row.createdAt = now;
        }
        row.text = text;
        row.category = category == null ? "" : category;
        row.updatedAt = now;
        database.userQuoteDao().upsert(row);
        return row.id;
    }

    public void deleteUserQuote(String id, Runnable onDone) {
        AppDatabase.IO.execute(() -> {
            database.userQuoteDao().delete(id);
            database.favoriteDao().delete(QuoteRefs.key(QuoteRefs.USER, id));
            if (onDone != null) {
                onDone.run();
            }
        });
    }

    public List<ReminderEntity> remindersSync() {
        return database.reminderDao().all();
    }

    public void saveReminderSync(ReminderEntity entity) {
        database.reminderDao().upsert(entity);
    }

    public void deleteReminderSync(String id) {
        database.reminderDao().delete(id);
    }

    public void updateDaysSync(int mask) {
        database.reminderDao().updateDays(mask);
    }

    public OccurrenceEntity findOccurrenceSync(String key) {
        return database.occurrenceDao().find(key);
    }

    public void saveOccurrenceSync(OccurrenceEntity entity) {
        database.occurrenceDao().upsert(entity);
    }

    public BackupData snapshotSync(boolean includeHistory) {
        BackupData data = new BackupData();
        data.format = BackupData.FORMAT;
        data.version = BackupData.VERSION;
        data.includeHistory = includeHistory;
        for (FavoriteEntity favorite : database.favoriteDao().all()) {
            BackupData.Favorite item = new BackupData.Favorite();
            item.quoteKey = favorite.quoteKey;
            item.quoteId = favorite.quoteId;
            item.source = favorite.source;
            item.createdAt = favorite.createdAt;
            data.favorites.add(item);
        }
        for (UserQuoteEntity quote : database.userQuoteDao().all()) {
            BackupData.OwnQuote item = new BackupData.OwnQuote();
            item.id = quote.id;
            item.text = quote.text;
            item.category = quote.category;
            item.createdAt = quote.createdAt;
            item.updatedAt = quote.updatedAt;
            data.userQuotes.add(item);
        }
        for (DailyStateEntity day : database.dailyDao().all()) {
            BackupData.DayAction item = new BackupData.DayAction();
            item.date = day.date;
            item.status = day.actionStatus == null ? "none" : day.actionStatus;
            item.updatedAt = day.actionUpdatedAt;
            data.actions.add(item);
        }
        for (ReminderEntity reminder : database.reminderDao().all()) {
            BackupData.Reminder item = new BackupData.Reminder();
            item.id = reminder.id;
            item.hour = reminder.hour;
            item.minute = reminder.minute;
            item.daysMask = reminder.daysMask;
            item.enabled = reminder.enabled;
            item.kind = reminder.kind;
            data.reminders.add(item);
        }
        if (includeHistory) {
            for (HistoryEntity history : database.historyDao().all()) {
                BackupData.HistoryItem item = new BackupData.HistoryItem();
                item.quoteKey = history.quoteKey;
                item.quoteId = history.quoteId;
                item.source = history.source;
                item.viewedAt = history.viewedAt;
                data.history.add(item);
            }
        }
        return data;
    }

    public void applyBackupSync(BackupData merged) {
        database.runInTransaction(() -> {
            database.favoriteDao().deleteAll();
            for (BackupData.Favorite favorite : merged.favorites) {
                FavoriteEntity row = new FavoriteEntity();
                row.quoteKey = favorite.quoteKey;
                row.quoteId = favorite.quoteId;
                row.source = favorite.source;
                row.createdAt = favorite.createdAt;
                database.favoriteDao().upsert(row);
            }
            database.userQuoteDao().deleteAll();
            for (BackupData.OwnQuote quote : merged.userQuotes) {
                UserQuoteEntity row = new UserQuoteEntity();
                row.id = quote.id;
                row.text = quote.text;
                row.category = quote.category == null ? "" : quote.category;
                row.createdAt = quote.createdAt;
                row.updatedAt = quote.updatedAt;
                database.userQuoteDao().upsert(row);
            }
            for (BackupData.DayAction action : merged.actions) {
                DailyStateEntity row = database.dailyDao().find(action.date);
                if (row == null) {
                    row = new DailyStateEntity();
                    row.date = action.date;
                    row.quoteId = "";
                    row.source = QuoteRefs.CATALOG;
                }
                row.actionStatus = action.status;
                row.actionUpdatedAt = action.updatedAt;
                database.dailyDao().upsert(row);
            }
            database.reminderDao().deleteAll();
            for (BackupData.Reminder reminder : merged.reminders) {
                ReminderEntity row = new ReminderEntity();
                row.id = reminder.id;
                row.hour = reminder.hour;
                row.minute = reminder.minute;
                row.daysMask = reminder.daysMask;
                row.enabled = reminder.enabled;
                row.kind = reminder.kind;
                database.reminderDao().upsert(row);
            }
            database.historyDao().deleteAll();
            for (BackupData.HistoryItem history : merged.history) {
                HistoryEntity row = new HistoryEntity();
                row.quoteKey = history.quoteKey;
                row.quoteId = history.quoteId;
                row.source = history.source;
                row.viewedAt = history.viewedAt;
                database.historyDao().insert(row);
            }
            database.historyDao().trim(BackupMerger.HISTORY_LIMIT);
        });
    }

    public void clearHistorySync() {
        database.historyDao().deleteAll();
    }

    public void clearAllSync() {
        database.clearAllTables();
    }
}
