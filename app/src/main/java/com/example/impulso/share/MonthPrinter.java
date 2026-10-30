package com.example.impulso.share;

import android.app.Activity;
import android.print.PrintManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.impulso.ImpulsoApplication;
import com.example.impulso.data.db.AppDatabase;
import com.example.impulso.data.db.DailyStateEntity;
import com.example.impulso.domain.Quote;

import java.time.LocalDate;
import java.time.ZoneId;

public final class MonthPrinter {
    private MonthPrinter() {
    }

    public static void print(Activity activity) {
        AppDatabase.IO.execute(() -> {
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            StringBuilder html = new StringBuilder();
            html.append("<html><body style='font-family:serif;padding:24px'>");
            html.append("<h1>Impulso</h1>");
            for (int i = 0; i < 31; i++) {
                LocalDate date = today.minusDays(i);
                DailyStateEntity row = ImpulsoApplication.from(activity).userData().findDailySync(date.toString());
                if (row == null || row.quoteId == null) {
                    continue;
                }
                Quote quote = ImpulsoApplication.from(activity).catalog().find(row.quoteId);
                String text = quote == null ? "" : quote.text;
                String author = quote == null ? "" : quote.author;
                String status = row.actionStatus == null ? "none" : row.actionStatus;
                String place = row.placeNote == null ? "" : row.placeNote;
                html.append("<p><strong>").append(date).append("</strong><br>")
                        .append(escape(text)).append("<br><em>").append(escape(author)).append("</em><br>")
                        .append(escape(status));
                if (!place.isEmpty()) {
                    html.append("<br>").append(escape(place));
                }
                html.append("</p>");
            }
            html.append("<p>Una frase no promete resultados.</p></body></html>");
            String page = html.toString();
            activity.runOnUiThread(() -> {
                WebView web = new WebView(activity);
                web.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        PrintManager manager = (PrintManager) activity.getSystemService(Activity.PRINT_SERVICE);
                        if (manager != null) {
                            manager.print("impulso-mes", view.createPrintDocumentAdapter("impulso-mes"), null);
                        }
                    }
                });
                web.loadDataWithBaseURL(null, page, "text/html", "UTF-8", null);
            });
        });
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
