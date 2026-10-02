package com.example.impulso.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

public final class Daos {
    private Daos() {
    }

    @Dao
    public interface FavoriteDao {
        @Query("SELECT * FROM favorites ORDER BY createdAt DESC")
        LiveData<List<FavoriteEntity>> observeAll();

        @Query("SELECT * FROM favorites")
        List<FavoriteEntity> all();

        @Query("SELECT * FROM favorites WHERE quoteKey = :key LIMIT 1")
        FavoriteEntity find(String key);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void upsert(FavoriteEntity entity);

        @Query("DELETE FROM favorites WHERE quoteKey = :key")
        void delete(String key);

        @Query("DELETE FROM favorites")
        void deleteAll();
    }

    @Dao
    public interface UserQuoteDao {
        @Query("SELECT * FROM user_quotes ORDER BY updatedAt DESC")
        LiveData<List<UserQuoteEntity>> observeAll();

        @Query("SELECT * FROM user_quotes")
        List<UserQuoteEntity> all();

        @Query("SELECT * FROM user_quotes WHERE id = :id LIMIT 1")
        UserQuoteEntity find(String id);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void upsert(UserQuoteEntity entity);

        @Query("DELETE FROM user_quotes WHERE id = :id")
        void delete(String id);

        @Query("DELETE FROM user_quotes")
        void deleteAll();
    }

    @Dao
    public interface HistoryDao {
        @Query("SELECT * FROM history ORDER BY viewedAt DESC LIMIT :limit")
        LiveData<List<HistoryEntity>> observeRecent(int limit);

        @Query("SELECT * FROM history ORDER BY viewedAt DESC")
        List<HistoryEntity> all();

        @Query("SELECT quoteId FROM history ORDER BY viewedAt DESC")
        List<String> idsNewestFirst();

        @Query("SELECT * FROM history ORDER BY viewedAt DESC LIMIT 1")
        HistoryEntity latest();

        @Insert
        void insert(HistoryEntity entity);

        @Query("DELETE FROM history")
        void deleteAll();

        @Query("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY viewedAt DESC LIMIT :limit)")
        void trim(int limit);
    }

    @Dao
    public interface DailyDao {
        @Query("SELECT * FROM daily_state WHERE date = :date LIMIT 1")
        DailyStateEntity find(String date);

        @Query("SELECT * FROM daily_state WHERE date >= :fromDate AND date <= :toDate")
        List<DailyStateEntity> between(String fromDate, String toDate);

        @Query("SELECT * FROM daily_state")
        List<DailyStateEntity> all();

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void upsert(DailyStateEntity entity);

        @Query("DELETE FROM daily_state")
        void deleteAll();
    }

    @Dao
    public interface ReminderDao {
        @Query("SELECT * FROM reminders ORDER BY hour, minute, id")
        List<ReminderEntity> all();

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void upsert(ReminderEntity entity);

        @Query("DELETE FROM reminders WHERE id = :id")
        void delete(String id);

        @Query("UPDATE reminders SET daysMask = :mask")
        void updateDays(int mask);

        @Query("DELETE FROM reminders")
        void deleteAll();
    }

    @Dao
    public interface OccurrenceDao {
        @Query("SELECT * FROM occurrences WHERE occurrenceKey = :key LIMIT 1")
        OccurrenceEntity find(String key);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void upsert(OccurrenceEntity entity);

        @Query("DELETE FROM occurrences")
        void deleteAll();
    }
}
