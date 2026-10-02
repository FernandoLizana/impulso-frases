package com.example.impulso.ui.today;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.impulso.R;
import com.example.impulso.data.prefs.AppPreferences;
import com.example.impulso.databinding.FragmentTodayBinding;
import com.example.impulso.notify.BoardScheduler;
import com.example.impulso.share.ShareDialogFragment;
import com.example.impulso.tts.SpeechReader;
import com.example.impulso.ui.Labels;
import com.example.impulso.ui.cards.CardsActivity;
import com.example.impulso.ui.map.ConstellationActivity;
import com.example.impulso.ui.read.ReadingActivity;
import com.google.android.material.snackbar.Snackbar;

public class TodayFragment extends Fragment {
    private FragmentTodayBinding binding;
    private TodayViewModel viewModel;
    private final SpeechReader speech = new SpeechReader();
    private boolean speaking;
    private boolean showingEnglish;
    private String shownId = "";
    private boolean chimed;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTodayBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(TodayViewModel.class);
        viewModel.ui().observe(getViewLifecycleOwner(), this::render);
        binding.buttonAnother.setOnClickListener(v -> viewModel.showAnother());
        binding.buttonBackDaily.setOnClickListener(v -> viewModel.backToDaily());
        binding.buttonSkip.setOnClickListener(v -> viewModel.setAction("skipped"));
        binding.buttonUndo.setOnClickListener(v -> viewModel.setAction("none"));
        binding.buttonFavorite.setOnClickListener(v -> viewModel.toggleFavorite());
        binding.buttonShare.setOnClickListener(v -> share());
        binding.buttonListen.setOnClickListener(v -> toggleSpeech(false));
        binding.buttonListenAction.setOnClickListener(v -> toggleSpeech(true));
        binding.buttonFlip.setOnClickListener(v -> {
            showingEnglish = !showingEnglish;
            TodayViewModel.Ui ui = viewModel.ui().getValue();
            if (ui != null) {
                render(ui);
            }
        });
        binding.buttonAnchor.setOnClickListener(v -> viewModel.setAnchorFromDaily());
        binding.buttonRead.setOnClickListener(v -> openReading());
        binding.buttonCards.setOnClickListener(v -> startActivity(new Intent(requireContext(), CardsActivity.class)));
        binding.buttonMap.setOnClickListener(v -> startActivity(new Intent(requireContext(), ConstellationActivity.class)));
        binding.buttonDone.setOnClickListener(v -> {
            viewModel.setAction("done");
            offerSilence();
        });
        binding.inputPlace.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && binding != null) {
                viewModel.setPlace(String.valueOf(binding.inputPlace.getText()));
            }
        });
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        speech.init(requireContext(), ignored -> {
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refresh();
        maybeChime();
        if (getActivity() instanceof com.example.impulso.ui.main.MainActivity main && main.consumeSpeak()) {
            binding.getRoot().post(() -> toggleSpeech(false));
        }
    }

    @Override
    public void onStop() {
        speech.stop();
        speaking = false;
        speech.shutdown();
        super.onStop();
    }

    private void render(TodayViewModel.Ui ui) {
        if (binding == null || !isAdded()) {
            return;
        }
        AppPreferences prefs = new AppPreferences(requireContext());
        binding.textGreeting.setText(Labels.greeting(requireContext(), prefs.name()));
        binding.textContext.setText(ui.browsing ? R.string.label_browsing : R.string.label_daily);
        binding.buttonBackDaily.setVisibility(ui.browsing ? View.VISIBLE : View.GONE);
        binding.textEmpty.setVisibility(ui.hasCard ? View.GONE : View.VISIBLE);
        if (!ui.cardId.equals(shownId)) {
            showingEnglish = false;
            shownId = ui.cardId;
        }
        boolean english = showingEnglish && ui.cardEnglish != null && !ui.cardEnglish.isEmpty();
        binding.textQuote.setText(english ? ui.cardEnglish : ui.cardText);
        binding.buttonFlip.setVisibility(ui.cardEnglish == null || ui.cardEnglish.isEmpty() ? View.GONE : View.VISIBLE);
        binding.buttonFlip.setText(english ? R.string.flip_spanish : R.string.flip_english);
        binding.textAuthor.setText(ui.cardAuthor);
        binding.textCategory.setText(ui.hasCard ? Labels.category(requireContext(), ui.cardCategory) : "");
        int tint = ContextCompat.getColor(requireContext(), ui.cardFavorite ? R.color.primary : R.color.text_secondary);
        binding.buttonFavorite.setColorFilter(tint, PorterDuff.Mode.SRC_IN);
        binding.buttonFavorite.setContentDescription(getString(ui.cardFavorite ? R.string.cd_unfavorite : R.string.cd_favorite));
        binding.textReflection.setText(ui.dailyReflection);
        binding.textMicro.setText(ui.dailyMicro);
        int practice = ui.quietNow ? View.GONE : View.VISIBLE;
        binding.labelReflection.setVisibility(practice);
        binding.textReflection.setVisibility(practice);
        binding.labelAction.setVisibility(practice);
        binding.textMicro.setVisibility(practice);
        binding.buttonListenAction.setVisibility(practice);
        binding.textQuiet.setVisibility(ui.quietNow ? View.VISIBLE : View.GONE);
        binding.textEcho.setVisibility(ui.showEcho ? View.VISIBLE : View.GONE);
        binding.textEcho.setText(getString(R.string.echo_label) + "\n" + ui.echoText + "\n" + ui.echoAuthor);
        binding.textAnchor.setVisibility(ui.showAnchor ? View.VISIBLE : View.GONE);
        binding.textAnchor.setText(getString(R.string.anchor_label) + "\n" + ui.anchorText + "\n" + ui.anchorAuthor);
        binding.textTension.setVisibility(ui.tensionText == null || ui.tensionText.isEmpty() ? View.GONE : View.VISIBLE);
        binding.textTension.setText(getString(R.string.tension_label) + "\n" + ui.tensionText + "\n" + ui.tensionAuthor);
        if (!binding.inputPlace.hasFocus() && !String.valueOf(binding.inputPlace.getText()).equals(ui.placeNote)) {
            binding.inputPlace.setText(ui.placeNote);
        }
        int actionVisibility = ui.hasDaily && !ui.quietNow ? View.VISIBLE : View.GONE;
        binding.buttonDone.setVisibility(actionVisibility);
        binding.buttonSkip.setVisibility(actionVisibility);
        binding.textActionState.setText(statusLabel(ui.actionStatus));
        binding.buttonUndo.setVisibility(!"none".equals(ui.actionStatus) && ui.hasDaily ? View.VISIBLE : View.GONE);
        binding.historyContainer.removeAllViews();
        for (TodayViewModel.DayRow day : ui.days) {
            TextView line = new TextView(requireContext());
            line.setText(day.label + " · " + statusLabel(day.status));
            line.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            line.setPadding(0, 8, 0, 8);
            binding.historyContainer.addView(line);
        }
    }

    private String statusLabel(String status) {
        if ("done".equals(status)) {
            return getString(R.string.history_done);
        }
        if ("skipped".equals(status)) {
            return getString(R.string.history_skipped);
        }
        return getString(R.string.history_none);
    }

    private void share() {
        TodayViewModel.Ui ui = viewModel.ui().getValue();
        if (ui == null || !ui.hasCard) {
            return;
        }
        ShareDialogFragment.show(getParentFragmentManager(), ui.cardText, ui.cardAuthor);
    }

    private void openReading() {
        TodayViewModel.Ui ui = viewModel.ui().getValue();
        if (ui == null || !ui.hasCard) {
            return;
        }
        Intent intent = new Intent(requireContext(), ReadingActivity.class);
        intent.putExtra(ReadingActivity.EXTRA_TEXT, showingEnglish && !ui.cardEnglish.isEmpty() ? ui.cardEnglish : ui.cardText);
        intent.putExtra(ReadingActivity.EXTRA_AUTHOR, ui.cardAuthor);
        startActivity(intent);
    }

    private void offerSilence() {
        if (!isAdded()) {
            return;
        }
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.silence_offer)
                .setPositiveButton(R.string.silence_start, (d, w) -> BoardScheduler.startSilence(requireContext()))
                .setNeutralButton(R.string.silence_dnd, (d, w) -> {
                    Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.silence_later, null)
                .show();
    }

    private void maybeChime() {
        if (chimed || !isAdded()) {
            return;
        }
        AppPreferences prefs = new AppPreferences(requireContext());
        if (!prefs.chimeEnabled()) {
            return;
        }
        String today = java.time.LocalDate.now().toString();
        if (today.equals(prefs.chimeDate())) {
            chimed = true;
            return;
        }
        float scale = Settings.Global.getFloat(requireContext().getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        AudioManager audio = (AudioManager) requireContext().getSystemService(android.content.Context.AUDIO_SERVICE);
        if (scale == 0f || audio == null || audio.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            return;
        }
        chimed = true;
        prefs.setChimeDate(today);
        ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 30);
        tone.startTone(ToneGenerator.TONE_PROP_ACK, 90);
        new Handler(Looper.getMainLooper()).postDelayed(tone::release, 400);
    }

    private void toggleSpeech(boolean microOnly) {
        if (speaking) {
            speech.stop();
            speaking = false;
            binding.buttonListen.setContentDescription(getString(R.string.cd_listen));
            return;
        }
        TodayViewModel.Ui ui = viewModel.ui().getValue();
        if (ui == null || !ui.hasCard) {
            return;
        }
        if (!speech.spanishAvailable() && speech.voices().isEmpty()) {
            Snackbar.make(binding.getRoot(), R.string.tts_missing, Snackbar.LENGTH_LONG).show();
            return;
        }
        AppPreferences prefs = new AppPreferences(requireContext());
        if (speech.voiceNeedsNetwork(prefs.ttsVoice())) {
            Snackbar.make(binding.getRoot(), R.string.tts_network, Snackbar.LENGTH_LONG).show();
        }
        speaking = true;
        binding.buttonListen.setContentDescription(getString(R.string.cd_stop));
        String phrase = (showingEnglish && !ui.cardEnglish.isEmpty() ? ui.cardEnglish : ui.cardText) + ". " + ui.cardAuthor;
        if (microOnly) {
            speech.speak(ui.dailyMicro, prefs.ttsRate(), prefs.ttsVoice(), () -> {
                speaking = false;
                if (binding != null) {
                    binding.buttonListen.setContentDescription(getString(R.string.cd_listen));
                }
            });
            return;
        }
        speech.speakThen(phrase, ui.dailyReflection, 700, prefs.ttsRate(), prefs.ttsVoice(), () -> {
            speaking = false;
            if (binding != null) {
                Snackbar.make(binding.getRoot(), R.string.tts_failed, Snackbar.LENGTH_LONG).show();
                binding.buttonListen.setContentDescription(getString(R.string.cd_listen));
            }
        });
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
