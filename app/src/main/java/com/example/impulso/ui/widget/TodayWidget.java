package com.example.impulso.ui.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

import com.example.impulso.R;
import com.example.impulso.domain.Quote;
import com.example.impulso.notify.BoardScheduler;

public class TodayWidget extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        BoardScheduler.refreshAsync(context);
    }

    public static void updateAll(Context context, Quote quote, String mirror) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        if (manager == null) {
            return;
        }
        int[] ids = manager.getAppWidgetIds(new android.content.ComponentName(context, TodayWidget.class));
        if (ids == null) {
            return;
        }
        String text = quote == null ? "" : quote.text;
        String author = quote == null ? "" : quote.author;
        for (int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_today);
            views.setTextViewText(R.id.widget_quote, text);
            views.setTextViewText(R.id.widget_author, author);
            views.setTextViewText(R.id.widget_mirror, mirror == null ? "" : mirror);
            manager.updateAppWidget(id, views);
        }
    }
}
