package com.example.impulso.notify;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.impulso.R;
import com.example.impulso.ui.detail.QuoteDetailActivity;

public final class NotificationHelper {
    private NotificationHelper() {
    }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                AlarmContract.CHANNEL_ID,
                context.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription(context.getString(R.string.channel_description));
        manager.createNotificationChannel(channel);
    }

    public static boolean notificationsEnabled(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || !manager.areNotificationsEnabled()) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    public static boolean channelEnabled(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return true;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return false;
        }
        NotificationChannel channel = manager.getNotificationChannel(AlarmContract.CHANNEL_ID);
        return channel == null || channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
    }

    public static boolean canPost(Context context) {
        ensureChannel(context);
        return notificationsEnabled(context) && channelEnabled(context);
    }

    public static void showQuote(Context context, String title, String text, String author,
                                 String source, String id, boolean test) {
        if (!canPost(context)) {
            return;
        }
        Intent open = new Intent(context, QuoteDetailActivity.class);
        open.putExtra(QuoteDetailActivity.EXTRA_ID, id);
        open.putExtra(QuoteDetailActivity.EXTRA_SOURCE, source);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int request = test ? AlarmContract.REQUEST_OPEN_TEST : AlarmContract.REQUEST_OPEN;
        PendingIntent content = PendingIntent.getActivity(
                context,
                request,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        String body = text + "\n" + author;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, AlarmContract.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(content)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVibrate(new long[]{0, 40, 80, 40});
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        int notificationId = test ? AlarmContract.NOTIF_TEST : AlarmContract.NOTIF_REMINDER;
        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        } catch (SecurityException ignored) {
            // El permiso pudo revocarse entre la comprobación y el aviso.
        }
    }

    public static void showLockscreen(Context context, String text, String author, String id) {
        if (!canPost(context)) {
            return;
        }
        Intent open = new Intent(context, QuoteDetailActivity.class);
        open.putExtra(QuoteDetailActivity.EXTRA_ID, id);
        open.putExtra(QuoteDetailActivity.EXTRA_SOURCE, com.example.impulso.domain.QuoteRefs.CATALOG);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(
                context, 4111, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, AlarmContract.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.lockscreen_title))
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text + "\n" + author))
                .setContentIntent(content)
                .setAutoCancel(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(0, context.getString(R.string.action_done), action(context, QuoteActionReceiver.ACTION_DONE, 4112))
                .addAction(0, context.getString(R.string.action_skip), action(context, QuoteActionReceiver.ACTION_SKIP, 4113));
        post(context, AlarmContract.NOTIF_LOCK, builder);
    }

    public static void showPlain(Context context, int id, String title, String body) {
        if (!canPost(context)) {
            return;
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, AlarmContract.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true);
        post(context, id, builder);
    }

    public static void cancelLockscreen(Context context) {
        NotificationManagerCompat.from(context).cancel(AlarmContract.NOTIF_LOCK);
    }

    private static PendingIntent action(Context context, String action, int request) {
        Intent intent = new Intent(context, QuoteActionReceiver.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, request, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void post(Context context, int id, NotificationCompat.Builder builder) {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build());
        } catch (SecurityException ignored) {
            // El permiso pudo revocarse entre la comprobación y el aviso.
        }
    }

    public static void cancelReminders(Context context) {
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        manager.cancel(AlarmContract.NOTIF_REMINDER);
        manager.cancel(AlarmContract.NOTIF_TEST);
    }
}
