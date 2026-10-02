package com.example.impulso.notify;

import static org.junit.Assert.assertTrue;

import android.app.NotificationManager;
import android.content.Context;
import android.service.notification.StatusBarNotification;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.impulso.ImpulsoApplication;
import com.example.impulso.data.db.AppDatabase;
import com.example.impulso.data.db.ReminderEntity;
import com.example.impulso.data.prefs.AppPreferences;
import com.example.impulso.domain.ReminderSlot;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.ZonedDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class ReminderDeliveryTest {
    @Test
    public void scheduledAlarmShowsTheNotification() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue("Hace falta el permiso de notificaciones para esta prueba.", NotificationHelper.canPost(context));
        AppPreferences prefs = new AppPreferences(context);
        boolean previous = prefs.remindersEnabled();
        boolean previousQuiet = prefs.quietEnabled();
        boolean previousExact = prefs.preferExact();
        prefs.setQuietEnabled(false);
        prefs.setPreferExact(true);
        prefs.setRemindersEnabled(true);
        ZonedDateTime when = ZonedDateTime.now().plusSeconds(75);
        CountDownLatch scheduled = new CountDownLatch(1);
        AppDatabase.IO.execute(() -> {
            try {
                ReminderEntity entity = new ReminderEntity();
                entity.id = "custom_instrumented_delivery";
                entity.hour = when.getHour();
                entity.minute = when.getMinute();
                entity.daysMask = ReminderSlot.ALL_DAYS;
                entity.enabled = true;
                entity.kind = "custom";
                ImpulsoApplication.from(context).userData().saveReminderSync(entity);
                prefs.clearSchedule();
                new ReminderScheduler(context).reconcile();
            } finally {
                scheduled.countDown();
            }
        });
        assertTrue(scheduled.await(15, TimeUnit.SECONDS));
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        long deadline = System.currentTimeMillis() + 150_000L;
        boolean found = false;
        while (System.currentTimeMillis() < deadline) {
            for (StatusBarNotification notification : manager.getActiveNotifications()) {
                if (notification.getId() == AlarmContract.NOTIF_REMINDER) {
                    found = true;
                    break;
                }
            }
            if (found) {
                break;
            }
            Thread.sleep(2000);
        }
        AppDatabase.IO.execute(() -> {
            ImpulsoApplication.from(context).userData().deleteReminderSync("custom_instrumented_delivery");
            prefs.setRemindersEnabled(previous);
            prefs.setQuietEnabled(previousQuiet);
            prefs.setPreferExact(previousExact);
            prefs.clearSchedule();
            new ReminderScheduler(context).reconcile();
            if (!previous) {
                NotificationHelper.cancelReminders(context);
            }
        });
        assertTrue("La alarma programada no mostró la notificación.", found);
    }
}
