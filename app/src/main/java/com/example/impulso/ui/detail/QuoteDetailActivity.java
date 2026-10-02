package com.example.impulso.ui.detail;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.impulso.ImpulsoApplication;
import com.example.impulso.R;
import com.example.impulso.data.db.AppDatabase;
import com.example.impulso.data.db.UserQuoteEntity;
import com.example.impulso.data.prefs.AppPreferences;
import com.example.impulso.databinding.ActivityQuoteDetailBinding;
import com.example.impulso.domain.Quote;
import com.example.impulso.domain.QuoteRefs;
import com.example.impulso.share.ShareDialogFragment;
import com.example.impulso.tts.SpeechReader;
import com.example.impulso.ui.Labels;
import com.example.impulso.ui.SystemBarPadding;
import com.example.impulso.ui.collection.QuoteEditor;
import com.example.impulso.ui.main.MainActivity;
import com.example.impulso.ui.onboarding.OnboardingActivity;
import com.google.android.material.snackbar.Snackbar;

public class QuoteDetailActivity extends AppCompatActivity {
    public static final String EXTRA_ID = "quote_id";
    public static final String EXTRA_SOURCE = "quote_source";

    private ActivityQuoteDetailBinding binding;
    private final SpeechReader speech = new SpeechReader();
    private boolean speaking;
    private String quoteId;
    private String source;
    private String text = "";
    private String author = "";
    private boolean favorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.example.impulso.ui.Themes.apply(this);
        super.onCreate(savedInstanceState);
        AppPreferences prefs = new AppPreferences(this);
        quoteId = getIntent().getStringExtra(EXTRA_ID);
        source = getIntent().getStringExtra(EXTRA_SOURCE);
        if (!prefs.isOnboardingComplete()) {
            if (quoteId != null) {
                prefs.setPendingQuote(source, quoteId);
            }
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }
        EdgeToEdge.enable(this);
        binding = ActivityQuoteDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBarPadding.apply(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> goBack());
        binding.buttonFavorite.setOnClickListener(v -> toggleFavorite());
        binding.buttonListen.setOnClickListener(v -> toggleSpeech());
        binding.buttonShare.setOnClickListener(v -> ShareDialogFragment.show(getSupportFragmentManager(), text, author));
        binding.buttonEdit.setOnClickListener(v -> edit());
        if (savedInstanceState == null && quoteId != null) {
            ImpulsoApplication.from(this).userData().recordView(source, quoteId);
        }
        load();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        quoteId = intent.getStringExtra(EXTRA_ID);
        source = intent.getStringExtra(EXTRA_SOURCE);
        load();
    }

    @Override
    protected void onStart() {
        super.onStart();
        speech.init(this, ignored -> {
        });
    }

    @Override
    protected void onStop() {
        speech.stop();
        speaking = false;
        speech.shutdown();
        super.onStop();
    }

    private void load() {
        AppDatabase.IO.execute(() -> {
            ImpulsoApplication app = ImpulsoApplication.from(this);
            String shownText;
            String shownAuthor;
            String category = "";
            String reflection = "";
            String micro = "";
            boolean missing = false;
            boolean user = QuoteRefs.USER.equals(source);
            if (user) {
                UserQuoteEntity own = app.userData().findUserSync(quoteId);
                if (own == null) {
                    missing = true;
                    shownText = "";
                    shownAuthor = "";
                } else {
                    shownText = own.text;
                    shownAuthor = getString(R.string.your_quote);
                    category = own.category;
                }
            } else {
                Quote quote = app.catalog().find(quoteId);
                if (quote == null) {
                    missing = true;
                    shownText = "";
                    shownAuthor = "";
                } else {
                    shownText = quote.text;
                    shownAuthor = quote.author;
                    category = quote.category;
                    reflection = quote.reflection;
                    micro = quote.microAction;
                }
            }
            boolean fav = !missing && app.userData().isFavoriteSync(source, quoteId);
            boolean isMissing = missing;
            String cat = category;
            String ref = reflection;
            String act = micro;
            String body = shownText;
            String who = shownAuthor;
            StringBuilder others = new StringBuilder();
            if (!user && !missing && who != null && !who.isEmpty()) {
                others.append(getString(R.string.same_author));
                int added = 0;
                for (com.example.impulso.domain.Quote quote : app.catalog().quotes()) {
                    if (quote.id.equals(quoteId) || !who.equals(quote.author)) {
                        continue;
                    }
                    others.append("\n").append(quote.text);
                    added++;
                    if (added == 3) {
                        break;
                    }
                }
                if (added == 0) {
                    others.setLength(0);
                }
            }
            String same = others.toString();
            runOnUiThread(() -> bind(isMissing, user, fav, body, who, cat, ref, act, same));
        });
    }

    private void bind(boolean missing, boolean user, boolean fav, String body, String who, String category, String reflection, String micro, String sameAuthor) {
        if (binding == null) {
            return;
        }
        text = body;
        author = who;
        favorite = fav;
        binding.textMissing.setVisibility(missing ? View.VISIBLE : View.GONE);
        binding.content.setVisibility(missing ? View.GONE : View.VISIBLE);
        if (missing) {
            return;
        }
        binding.textQuote.setText(body);
        binding.textAuthor.setText(who);
        binding.textCategory.setText(Labels.category(this, category));
        boolean catalog = !user;
        int extra = catalog ? View.VISIBLE : View.GONE;
        binding.labelReflection.setVisibility(extra);
        binding.textReflection.setVisibility(extra);
        binding.labelMicro.setVisibility(extra);
        binding.textMicro.setVisibility(extra);
        binding.textReflection.setText(reflection);
        binding.textMicro.setText(micro);
        binding.textSameAuthor.setText(sameAuthor);
        binding.textSameAuthor.setVisibility(sameAuthor == null || sameAuthor.isEmpty() ? View.GONE : View.VISIBLE);
        binding.buttonEdit.setVisibility(user ? View.VISIBLE : View.GONE);
        paintFavorite();
    }

    private void paintFavorite() {
        int tint = ContextCompat.getColor(this, favorite ? R.color.primary : R.color.text_secondary);
        binding.buttonFavorite.setColorFilter(tint, PorterDuff.Mode.SRC_IN);
        binding.buttonFavorite.setContentDescription(getString(favorite ? R.string.cd_unfavorite : R.string.cd_favorite));
    }

    private void toggleFavorite() {
        ImpulsoApplication.from(this).userData().toggleFavorite(source, quoteId, now -> runOnUiThread(() -> {
            favorite = now;
            if (binding != null) {
                paintFavorite();
            }
        }));
    }

    private void toggleSpeech() {
        if (speaking) {
            speech.stop();
            speaking = false;
            return;
        }
        if (!speech.spanishAvailable() && speech.voices().isEmpty()) {
            Snackbar.make(binding.getRoot(), R.string.tts_missing, Snackbar.LENGTH_LONG).show();
            return;
        }
        AppPreferences prefs = new AppPreferences(this);
        if (speech.voiceNeedsNetwork(prefs.ttsVoice())) {
            Snackbar.make(binding.getRoot(), R.string.tts_network, Snackbar.LENGTH_LONG).show();
        }
        speaking = true;
        speech.speak(text + ". " + author, prefs.ttsRate(), prefs.ttsVoice(), () -> {
            speaking = false;
            if (binding != null) {
                Snackbar.make(binding.getRoot(), R.string.tts_failed, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void edit() {
        AppDatabase.IO.execute(() -> {
            UserQuoteEntity own = ImpulsoApplication.from(this).userData().findUserSync(quoteId);
            runOnUiThread(() -> {
                if (own == null || binding == null) {
                    return;
                }
                QuoteEditor.show(this, own, this::load);
            });
        });
    }

    private void goBack() {
        if (isTaskRoot()) {
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        binding = null;
        super.onDestroy();
    }
}
