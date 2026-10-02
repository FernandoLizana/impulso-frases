package com.example.impulso.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(
        entities = {
                FavoriteEntity.class,
                UserQuoteEntity.class,
                HistoryEntity.class,
                DailyStateEntity.class,
                ReminderEntity.class,
                OccurrenceEntity.class
        },
        version = 2,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE daily_state ADD COLUMN placeNote TEXT NOT NULL DEFAULT ''");
        }
    };

    public static final ExecutorService IO = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "impulso-io");
        thread.setDaemon(true);
        return thread;
    });

    private static volatile AppDatabase instance;

    public abstract Daos.FavoriteDao favoriteDao();

    public abstract Daos.UserQuoteDao userQuoteDao();

    public abstract Daos.HistoryDao historyDao();

    public abstract Daos.DailyDao dailyDao();

    public abstract Daos.ReminderDao reminderDao();

    public abstract Daos.OccurrenceDao occurrenceDao();

    public static AppDatabase get(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "impulso.db"
                    ).addMigrations(MIGRATION_1_2).build();
                }
            }
        }
        return instance;
    }
}
