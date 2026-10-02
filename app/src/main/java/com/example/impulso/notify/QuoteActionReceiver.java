package com.example.impulso.notify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.impulso.ImpulsoApplication;
import com.example.impulso.data.db.AppDatabase;
import com.example.impulso.data.db.DailyStateEntity;

import java.time.LocalDate;
import java.time.ZoneId;

public class QuoteActionReceiver extends BroadcastReceiver {
    public static final String ACTION_DONE = "com.example.impulso.action.DAILY_DONE";
    public static final String ACTION_SKIP = "com.example.impulso.action.DAILY_SKIP";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        String status = ACTION_SKIP.equals(intent.getAction()) ? "skipped" : "done";
        PendingResult pending = goAsync();
        AppDatabase.IO.execute(() -> {
            try {
                String date = LocalDate.now(ZoneId.systemDefault()).toString();
                DailyStateEntity row = ImpulsoApplication.from(context).userData().findDailySync(date);
                if (row != null) {
                    row.actionStatus = status;
                    row.actionUpdatedAt = System.currentTimeMillis();
                    ImpulsoApplication.from(context).userData().saveDailySync(row);
                }
                BoardScheduler.refresh(context.getApplicationContext());
            } finally {
                pending.finish();
            }
        });
    }
}
