package com.example.impulso;

import android.app.Application;

import com.example.impulso.data.catalog.CatalogRepository;
import com.example.impulso.data.db.AppDatabase;
import com.example.impulso.data.prefs.AppPreferences;
import com.example.impulso.data.repo.UserDataRepository;
import com.example.impulso.notify.NotificationHelper;
import com.example.impulso.notify.ReminderScheduler;

public class ImpulsoApplication extends Application {
    private AppDatabase database;
    private CatalogRepository catalog;
    private UserDataRepository userData;

    @Override
    public void onCreate() {
        super.onCreate();
        new AppPreferences(this).applyTheme();
        database = AppDatabase.get(this);
        catalog = new CatalogRepository(this);
        userData = new UserDataRepository(database);
        NotificationHelper.ensureChannel(this);
        new ReminderScheduler(this).reconcileAsync();
        com.example.impulso.notify.BoardScheduler.refreshAsync(this);
    }

    public static ImpulsoApplication from(android.content.Context context) {
        return (ImpulsoApplication) context.getApplicationContext();
    }

    public CatalogRepository catalog() {
        return catalog;
    }

    public UserDataRepository userData() {
        return userData;
    }
}
