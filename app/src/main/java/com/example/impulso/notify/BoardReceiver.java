package com.example.impulso.notify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.impulso.data.db.AppDatabase;

public class BoardReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        PendingResult pending = goAsync();
        String action = intent.getAction();
        AppDatabase.IO.execute(() -> {
            try {
                BoardScheduler.onBoardAlarm(context.getApplicationContext(), action);
            } finally {
                pending.finish();
            }
        });
    }
}
