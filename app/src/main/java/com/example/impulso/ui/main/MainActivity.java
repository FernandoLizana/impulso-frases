package com.example.impulso.ui.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.impulso.R;
import com.example.impulso.data.prefs.AppPreferences;
import com.example.impulso.databinding.ActivityMainBinding;
import com.example.impulso.notify.ReminderScheduler;
import com.example.impulso.ui.SystemBarPadding;
import com.example.impulso.ui.detail.QuoteDetailActivity;
import com.example.impulso.ui.onboarding.OnboardingActivity;

public class MainActivity extends AppCompatActivity {
    private boolean speakNow;

    public boolean consumeSpeak() {
        boolean value = speakNow;
        speakNow = false;
        return value;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.example.impulso.ui.Themes.apply(this);
        super.onCreate(savedInstanceState);
        speakNow = getIntent() != null && "com.example.impulso.LISTEN".equals(getIntent().getAction());
        AppPreferences prefs = new AppPreferences(this);
        if (!prefs.isOnboardingComplete()) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }
        EdgeToEdge.enable(this);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBarPadding.apply(binding.getRoot());
        NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host);
        if (host != null) {
            NavigationUI.setupWithNavController(binding.bottomNav, host.getNavController());
        }
        String pendingId = prefs.pendingQuoteId();
        if (pendingId != null && !pendingId.isEmpty() && savedInstanceState == null) {
            String source = prefs.pendingQuoteSource();
            prefs.clearPendingQuote();
            Intent detail = new Intent(this, QuoteDetailActivity.class);
            detail.putExtra(QuoteDetailActivity.EXTRA_ID, pendingId);
            detail.putExtra(QuoteDetailActivity.EXTRA_SOURCE, source);
            startActivity(detail);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        com.example.impulso.notify.BoardScheduler.refreshAsync(this);
        new ReminderScheduler(this).reconcileAsync();
    }
}
