package com.example.impulso.notify;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.impulso.ImpulsoApplication;
import com.example.impulso.R;
import com.example.impulso.data.db.DailyStateEntity;
import com.example.impulso.data.prefs.AppPreferences;
import com.example.impulso.domain.MirrorHours;
import com.example.impulso.domain.QuietHours;
import com.example.impulso.domain.Quote;
import com.example.impulso.domain.QuoteRefs;
import com.example.impulso.domain.QuoteSelector;
import com.example.impulso.ui.widget.TodayWidget;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class BoardScheduler {
    public static final String ACTION_MIDNIGHT = "com.example.impulso.action.MIDNIGHT";
    public static final String ACTION_CLOSE = "com.example.impulso.action.DAY_CLOSE";
    public static final String ACTION_TIMER = "com.example.impulso.action.SILENCE_TIMER";
    public static final int REQUEST_MIDNIGHT = 4108;
    public static final int REQUEST_CLOSE = 4109;
    public static final int REQUEST_TIMER = 4110;

    private BoardScheduler() {
    }

    public static void refreshAsync(Context context) {
        Context app = context.getApplicationContext();
        com.example.impulso.data.db.AppDatabase.IO.execute(() -> refresh(app));
    }

    public static void refresh(Context context) {
        Quote quote = ensureDaily(context);
        TodayWidget.updateAll(context, quote, nextMirrorLabel(context));
        if (new AppPreferences(context).lockscreenQuote() && quote != null) {
            NotificationHelper.showLockscreen(context, quote.text, quote.author, quote.id);
        } else {
            NotificationHelper.cancelLockscreen(context);
        }
        scheduleMidnight(context);
        scheduleClose(context);
        PhraseShortcuts.publish(context);
    }

    public static void startSilence(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) {
            return;
        }
        long when = System.currentTimeMillis() + 5L * 60L * 1000L;
        PendingIntent pending = pending(context, ACTION_TIMER, REQUEST_TIMER);
        manager.set(AlarmManager.RTC_WAKEUP, when, pending);
    }

    public static Quote ensureDaily(Context context) {
        ImpulsoApplication app = ImpulsoApplication.from(context);
        AppPreferences prefs = new AppPreferences(context);
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        String date = today.toString();
        DailyStateEntity row = app.userData().findDailySync(date);
        boolean missing = row != null && row.quoteId != null && !row.quoteId.isEmpty()
                && app.catalog().find(row.quoteId) == null;
        if (row == null || row.quoteId == null || row.quoteId.isEmpty() || missing) {
            if (missing) {
                row.actionStatus = "none";
            }
            Quote selected = QuoteSelector.select(
                    app.catalog().quotes(),
                    prefs.goal(),
                    prefs.tone(),
                    app.userData().recentIdsSync(),
                    null,
                    today.toEpochDay(),
                    List.of(),
                    prefs.deck()
            );
            if (selected == null) {
                return null;
            }
            if (row == null) {
                row = new DailyStateEntity();
                row.date = date;
                row.actionStatus = "none";
            }
            row.quoteId = selected.id;
            row.source = QuoteRefs.CATALOG;
            app.userData().saveDailySync(row);
            return selected;
        }
        return app.catalog().find(row.quoteId);
    }

    public static String nextMirrorLabel(Context context) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime best = null;
        for (LocalTime time : MirrorHours.all()) {
            ZonedDateTime candidate = now.toLocalDate().atTime(time).atZone(now.getZone());
            if (!candidate.isAfter(now)) {
                candidate = candidate.plusDays(1);
            }
            if (best == null || candidate.isBefore(best)) {
                best = candidate;
            }
        }
        if (best == null) {
            return "";
        }
        String clock = best.format(DateTimeFormatter.ofPattern("HH:mm"));
        return context.getString(R.string.next_mirror, clock);
    }

    private static void scheduleMidnight(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) {
            return;
        }
        ZonedDateTime when = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault());
        manager.set(AlarmManager.RTC, when.toInstant().toEpochMilli(), pending(context, ACTION_MIDNIGHT, REQUEST_MIDNIGHT));
    }

    private static void scheduleClose(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) {
            return;
        }
        PendingIntent pending = pending(context, ACTION_CLOSE, REQUEST_CLOSE);
        AppPreferences prefs = new AppPreferences(context);
        if (!prefs.dayCloseEnabled()) {
            manager.cancel(pending);
            return;
        }
        LocalTime time = QuietHours.minutesToTime(prefs.dayCloseMin());
        ZonedDateTime when = LocalDate.now().atTime(time).atZone(ZoneId.systemDefault());
        if (!when.isAfter(ZonedDateTime.now())) {
            when = when.plusDays(1);
        }
        manager.set(AlarmManager.RTC_WAKEUP, when.toInstant().toEpochMilli(), pending);
    }

    static void onBoardAlarm(Context context, String action) {
        if (ACTION_TIMER.equals(action)) {
            NotificationHelper.showPlain(context, AlarmContract.NOTIF_TIMER,
                    context.getString(R.string.silence_done_title),
                    context.getString(R.string.silence_done_body));
            return;
        }
        if (ACTION_CLOSE.equals(action)) {
            AppPreferences prefs = new AppPreferences(context);
            QuietHours quiet = QuietHours.ofMinutes(prefs.quietEnabled(), prefs.quietStartMin(), prefs.quietEndMin());
            if (!quiet.contains(LocalTime.now())) {
                Quote quote = ensureDaily(context);
                String question = quote == null || quote.reflection == null ? "" : quote.reflection;
                NotificationHelper.showPlain(context, AlarmContract.NOTIF_CLOSE,
                        context.getString(R.string.day_close_title), question);
            }
        }
        refresh(context);
    }

    private static PendingIntent pending(Context context, String action, int request) {
        Intent intent = new Intent(context, BoardReceiver.class);
        intent.setAction(action);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, request, intent, flags);
    }

    public static void pokeWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        if (manager == null) {
            return;
        }
        ComponentName name = new ComponentName(context, TodayWidget.class);
        int[] ids = manager.getAppWidgetIds(name);
        if (ids != null && ids.length > 0) {
            TodayWidget.updateAll(context, null, nextMirrorLabel(context));
        }
    }
}
