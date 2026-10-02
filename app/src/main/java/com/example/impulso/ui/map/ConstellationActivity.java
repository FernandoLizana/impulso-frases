package com.example.impulso.ui.map;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.impulso.ImpulsoApplication;
import com.example.impulso.R;
import com.example.impulso.data.db.AppDatabase;
import com.example.impulso.data.db.DailyStateEntity;
import com.example.impulso.data.db.HistoryEntity;
import com.example.impulso.domain.Categories;
import com.example.impulso.domain.Quote;
import com.example.impulso.domain.QuoteRefs;
import com.example.impulso.ui.Labels;
import com.example.impulso.ui.Themes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConstellationActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Themes.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_constellation);
        LinearLayout column = findViewById(R.id.map_column);
        findViewById(R.id.map_close).setOnClickListener(v -> finish());
        AppDatabase.IO.execute(() -> {
            Map<String, Integer> counts = new LinkedHashMap<>();
            for (String category : Categories.IDS) {
                counts.put(category, 0);
            }
            ImpulsoApplication app = ImpulsoApplication.from(this);
            for (HistoryEntity item : app.userData().historySync()) {
                add(counts, categoryOf(app, item.quoteId, item.source));
            }
            for (DailyStateEntity day : app.userData().daysSync("2000-01-01", "2999-01-01")) {
                if ("done".equals(day.actionStatus)) {
                    add(counts, categoryOf(app, day.quoteId, day.source));
                }
            }
            runOnUiThread(() -> {
                for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                    TextView line = new TextView(this);
                    line.setText(Labels.category(this, entry.getKey()) + "  " + dots(entry.getValue()));
                    line.setPadding(0, 16, 0, 16);
                    column.addView(line);
                }
            });
        });
    }

    private static void add(Map<String, Integer> counts, String category) {
        if (category != null && counts.containsKey(category)) {
            counts.put(category, counts.get(category) + 1);
        }
    }

    private static String categoryOf(ImpulsoApplication app, String id, String source) {
        if (QuoteRefs.USER.equals(source)) {
            var own = app.userData().findUserSync(id);
            return own == null ? null : own.category;
        }
        Quote quote = app.catalog().find(id);
        return quote == null ? null : quote.category;
    }

    private static String dots(int count) {
        int shown = Math.min(count, 12);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < shown; i++) {
            builder.append("●");
        }
        if (count > shown) {
            builder.append(" +").append(count - shown);
        }
        return builder.toString();
    }
}
