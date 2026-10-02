package com.example.impulso.notify;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.example.impulso.ImpulsoApplication;
import com.example.impulso.data.db.AppDatabase;
import com.example.impulso.data.db.ReminderEntity;
import com.example.impulso.data.prefs.AppPreferences;
import com.example.impulso.domain.DeliveryCapability;
import com.example.impulso.domain.NextOccurrenceCalculator;
import com.example.impulso.domain.PlannedAlarm;
import com.example.impulso.domain.QuietHours;
import com.example.impulso.domain.ReminderSlot;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Programa solo la próxima ocurrencia. No suma 24 horas en milisegundos.
 * Si los recordatorios están apagados, cancela la alarma y no la reactiva.
 */
public final class ReminderScheduler {
    private static final long WINDOW_MS = 10L * 60L * 1000L;
    private final Context app;

    public ReminderScheduler(Context context) {
        this.app = context.getApplicationContext();
    }

    public void reconcileAsync() {
        AppDatabase.IO.execute(this::reconcile);
    }

    public void reconcile() {
        AppPreferences prefs = new AppPreferences(app);
        NotificationHelper.ensureChannel(app);
        boolean canExact = canScheduleExact(app);
        String mode = DeliveryCapability.resolve(
                prefs.remindersEnabled(),
                NotificationHelper.notificationsEnabled(app),
                NotificationHelper.channelEnabled(app),
                prefs.preferExact(),
                canExact
        );
        if (DeliveryCapability.OFF.equals(mode) || DeliveryCapability.BLOCKED.equals(mode)) {
            cancelAlarm();
            if (DeliveryCapability.OFF.equals(mode)) {
                NotificationHelper.cancelReminders(app);
            }
            prefs.clearSchedule();
            return;
        }
        List<ReminderSlot> slots = new ArrayList<>();
        for (ReminderEntity entity : ImpulsoApplication.from(app).userData().remindersSync()) {
            if (!entity.enabled) {
                continue;
            }
            slots.add(new ReminderSlot(entity.id, entity.hour, entity.minute, entity.daysMask));
        }
        QuietHours quiet = QuietHours.ofMinutes(prefs.quietEnabled(), prefs.quietStartMin(), prefs.quietEndMin());
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        PlannedAlarm next = NextOccurrenceCalculator.soonest(
                NextOccurrenceCalculator.uniqueScheduled(NextOccurrenceCalculator.plan(slots, quiet, now))
        );
        if (next == null || next.when == null) {
            cancelAlarm();
            prefs.clearSchedule();
            return;
        }
        long epoch = next.when.toInstant().toEpochMilli();
        boolean exact = DeliveryCapability.EXACT.equals(mode);
        String storedMode = exact ? DeliveryCapability.EXACT : DeliveryCapability.APPROXIMATE;
        if (next.key.equals(prefs.scheduledKey()) && epoch == prefs.scheduledAt() && storedMode.equals(prefs.scheduledMode())) {
            return;
        }
        schedule(epoch, next.key, exact && canExact);
        prefs.setSchedule(next.key, epoch, exact && canExact ? DeliveryCapability.EXACT : DeliveryCapability.APPROXIMATE);
    }

    public void cancelAlarm() {
        AlarmManager manager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) {
            return;
        }
        PendingIntent pending = alarmIntent(0L, "");
        manager.cancel(pending);
        pending.cancel();
    }

    public static boolean canScheduleExact(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        return manager.canScheduleExactAlarms();
    }

    private void schedule(long epoch, String key, boolean exact) {
        AlarmManager manager = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) {
            return;
        }
        PendingIntent pending = alarmIntent(epoch, key);
        if (exact) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epoch, pending);
        } else {
            manager.setWindow(AlarmManager.RTC_WAKEUP, epoch, WINDOW_MS, pending);
        }
    }

    private PendingIntent alarmIntent(long epoch, String key) {
        Intent intent = new Intent(app, AlarmReceiver.class);
        intent.setAction(AlarmContract.ACTION_ALARM);
        intent.putExtra(AlarmContract.EXTRA_KEY, key);
        intent.putExtra(AlarmContract.EXTRA_AT, epoch);
        return PendingIntent.getBroadcast(
                app,
                AlarmContract.REQUEST_ALARM,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
