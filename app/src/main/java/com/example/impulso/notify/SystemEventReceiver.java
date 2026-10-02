package com.example.impulso.notify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.impulso.data.db.AppDatabase;

/** Reinicio, cambio de hora o actualización: reconcilia sin encender lo que estaba apagado. */
public class SystemEventReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        PendingResult pending = goAsync();
        Context app = context.getApplicationContext();
        AppDatabase.IO.execute(() -> {
            try {
                new ReminderScheduler(app).reconcile();
                BoardScheduler.refresh(app);
            } finally {
                pending.finish();
            }
        });
    }
}
