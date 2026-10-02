package com.example.impulso.ui.workshop;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.impulso.ImpulsoApplication;
import com.example.impulso.R;
import com.example.impulso.data.db.AppDatabase;
import com.example.impulso.domain.MirrorHours;
import com.example.impulso.domain.Quote;
import com.example.impulso.ui.Themes;

import java.time.LocalDateTime;
import java.util.List;

/** Una carta a pantalla completa. Cambia cuando llega una hora espejo. */
public class WorkshopActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int shownHour = -1;
    private final Runnable tick = this::show;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Themes.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        findViewById(R.id.reading_close).setOnClickListener(v -> finish());
        show();
    }

    private void show() {
        LocalDateTime now = LocalDateTime.now();
        int hour = MirrorHours.isMirror(now.getHour(), now.getMinute()) ? now.getHour() : shownHour;
        if (hour != shownHour || shownHour < 0) {
            shownHour = now.getHour();
            AppDatabase.IO.execute(() -> {
                List<Quote> quotes = ImpulsoApplication.from(this).catalog().quotes();
                if (quotes.isEmpty()) {
                    return;
                }
                Quote quote = quotes.get(Math.floorMod(now.getDayOfYear() + shownHour, quotes.size()));
                runOnUiThread(() -> {
                    TextView body = findViewById(R.id.reading_quote);
                    TextView author = findViewById(R.id.reading_author);
                    if (body != null) {
                        body.setText(quote.text);
                        author.setText(quote.author);
                    }
                });
            });
        }
        handler.postDelayed(tick, 30_000L);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(tick);
        super.onDestroy();
    }
}
