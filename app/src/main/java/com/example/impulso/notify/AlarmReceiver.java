package com.example.impulso.notify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.impulso.ImpulsoApplication;
import com.example.impulso.R;
import com.example.impulso.data.db.AppDatabase;
import com.example.impulso.data.db.OccurrenceEntity;
import com.example.impulso.data.db.UserQuoteEntity;
import com.example.impulso.data.prefs.AppPreferences;
import com.example.impulso.domain.NextOccurrenceCalculator;
import com.example.impulso.domain.QuietHours;
import com.example.impulso.domain.Quote;
import com.example.impulso.domain.QuoteRefs;
import com.example.impulso.domain.QuoteSelector;
import com.example.impulso.domain.Tones;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !AlarmContract.ACTION_ALARM.equals(intent.getAction())) {
            return;
        }
        PendingResult pending = goAsync();
        String key = intent.getStringExtra(AlarmContract.EXTRA_KEY);
        long scheduledAt = intent.getLongExtra(AlarmContract.EXTRA_AT, 0L);
        AppDatabase.IO.execute(() -> {
            try {
                deliver(context.getApplicationContext(), key, scheduledAt);
            } finally {
                pending.finish();
            }
        });
    }

    private void deliver(Context context, String key, long scheduledAt) {
        AppPreferences prefs = new AppPreferences(context);
        ReminderScheduler scheduler = new ReminderScheduler(context);
        if (!prefs.remindersEnabled()) {
            scheduler.cancelAlarm();
            NotificationHelper.cancelReminders(context);
            prefs.clearSchedule();
            return;
        }
        long now = System.currentTimeMillis();
        if (key != null && NextOccurrenceCalculator.isTooLate(scheduledAt, now)) {
            saveStatus(context, key, scheduledAt, "", "", "skipped_late");
            prefs.clearSchedule();
            scheduler.reconcile();
            return;
        }
        if (!NotificationHelper.canPost(context)) {
            saveStatus(context, key, scheduledAt, "", "", "skipped_permission");
            prefs.clearSchedule();
            scheduler.reconcile();
            return;
        }
        ZonedDateTime zonedNow = ZonedDateTime.now(ZoneId.systemDefault());
        QuietHours quiet = QuietHours.ofMinutes(prefs.quietEnabled(), prefs.quietStartMin(), prefs.quietEndMin());
        if (quiet.contains(zonedNow.toLocalTime())) {
            saveStatus(context, key, scheduledAt, "", "", "skipped_quiet");
            prefs.clearSchedule();
            scheduler.reconcile();
            return;
        }
        ImpulsoApplication application = ImpulsoApplication.from(context);
        OccurrenceEntity existing = key == null ? null : application.userData().findOccurrenceSync(key);
        if (existing != null && "delivered".equals(existing.status)) {
            prefs.clearSchedule();
            scheduler.reconcile();
            return;
        }
        Quote quote = pick(application, prefs, key);
        if (quote == null || key == null) {
            prefs.clearSchedule();
            scheduler.reconcile();
            return;
        }
        String source = "user".equals(quote.authorship) ? QuoteRefs.USER : QuoteRefs.CATALOG;
        saveStatus(context, key, scheduledAt, quote.id, source, "delivered");
        NotificationHelper.showQuote(
                context,
                context.getString(R.string.app_name),
                quote.text,
                quote.author,
                source,
                quote.id,
                false
        );
        prefs.clearSchedule();
        scheduler.reconcile();
    }

    private static Quote pick(ImpulsoApplication application, AppPreferences prefs, String key) {
        List<Quote> extras = new ArrayList<>();
        if (prefs.includeUserQuotes()) {
            for (UserQuoteEntity own : application.userData().userQuotesSync()) {
                extras.add(new Quote(
                        own.id,
                        own.text,
                        "Tu frase",
                        "user",
                        null,
                        own.category == null || own.category.isEmpty() ? null : own.category,
                        Tones.CERCANO,
                        List.of(),
                        "",
                        "",
                        "es",
                        1,
                        null
                ));
            }
        }
        long seed = key == null ? 0L : key.hashCode();
        return QuoteSelector.select(
                application.catalog().quotes(),
                prefs.goal(),
                prefs.tone(),
                application.userData().recentIdsSync(),
                null,
                seed,
                extras
        );
    }

    private static void saveStatus(Context context, String key, long scheduledAt, String quoteId, String source, String status) {
        if (key == null || key.isEmpty()) {
            return;
        }
        OccurrenceEntity row = new OccurrenceEntity();
        row.occurrenceKey = key;
        row.quoteId = quoteId;
        row.source = source;
        row.scheduledAtEpoch = scheduledAt;
        row.status = status;
        ImpulsoApplication.from(context).userData().saveOccurrenceSync(row);
    }
}
