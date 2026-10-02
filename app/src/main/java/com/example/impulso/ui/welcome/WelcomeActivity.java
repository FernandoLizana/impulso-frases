package com.example.impulso.ui.welcome;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.impulso.ImpulsoApplication;
import com.example.impulso.R;
import com.example.impulso.data.db.AppDatabase;
import com.example.impulso.data.prefs.AppPreferences;
import com.example.impulso.databinding.ActivityWelcomeBinding;
import com.example.impulso.domain.Quote;
import com.example.impulso.domain.QuoteSelector;
import com.example.impulso.domain.Tones;
import com.example.impulso.ui.SystemBarPadding;
import com.example.impulso.ui.main.MainActivity;
import com.example.impulso.ui.onboarding.OnboardingActivity;

import java.util.List;

public class WelcomeActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable advance = this::continueNext;
    private boolean proceeded;
    private ActivityWelcomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.example.impulso.ui.Themes.apply(this);
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        AppPreferences prefs = new AppPreferences(this);
        if (prefs.skipWelcome()) {
            openNext(prefs);
            return;
        }
        EdgeToEdge.enable(this);
        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBarPadding.apply(binding.getRoot());
        binding.buttonContinue.setOnClickListener(v -> continueNext());
        if (savedInstanceState != null) {
            binding.textQuote.setText(savedInstanceState.getString("quote"));
            binding.textAuthor.setText(savedInstanceState.getString("author"));
        } else {
            loadQuote();
        }
    }

    private void loadQuote() {
        AppDatabase.IO.execute(() -> {
            List<Quote> quotes = ImpulsoApplication.from(this).catalog().quotes();
            Quote quote = QuoteSelector.select(quotes, "", Tones.CERCANO, List.of(), null, System.nanoTime());
            String text = quote == null ? getString(R.string.welcome_fallback_quote) : quote.text;
            String author = quote == null ? getString(R.string.author_original) : quote.author;
            runOnUiThread(() -> {
                if (binding == null) {
                    return;
                }
                binding.textQuote.setText(text);
                binding.textAuthor.setText(author);
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!manualOnly()) {
            handler.postDelayed(advance, 3000);
        }
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(advance);
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding != null) {
            outState.putString("quote", String.valueOf(binding.textQuote.getText()));
            outState.putString("author", String.valueOf(binding.textAuthor.getText()));
        }
    }

    private boolean manualOnly() {
        AccessibilityManager manager = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        boolean talkBack = manager != null && manager.isEnabled() && manager.isTouchExplorationEnabled();
        float scale = Settings.Global.getFloat(getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        return talkBack || scale == 0f;
    }

    private void continueNext() {
        if (proceeded) {
            return;
        }
        proceeded = true;
        handler.removeCallbacks(advance);
        openNext(new AppPreferences(this));
    }

    private void openNext(AppPreferences prefs) {
        Class<?> next = prefs.isOnboardingComplete() ? MainActivity.class : OnboardingActivity.class;
        Intent intent = new Intent(this, next);
        if (getIntent() != null) {
            intent.setAction(getIntent().getAction());
        }
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        binding = null;
        super.onDestroy();
    }
}
