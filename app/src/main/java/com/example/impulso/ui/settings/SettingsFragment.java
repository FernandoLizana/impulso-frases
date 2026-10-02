package com.example.impulso.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.impulso.BuildConfig;
import com.example.impulso.ImpulsoApplication;
import com.example.impulso.R;
import com.example.impulso.data.db.AppDatabase;
import com.example.impulso.data.prefs.AppPreferences;
import com.example.impulso.databinding.FragmentSettingsBinding;
import com.example.impulso.domain.BackupCodec;
import com.example.impulso.domain.BackupData;
import com.example.impulso.domain.BackupMerger;
import com.example.impulso.domain.CatalogLoader;
import com.example.impulso.domain.Decks;
import com.example.impulso.domain.Goals;
import com.example.impulso.domain.Tones;
import com.example.impulso.domain.UserQuoteRules;
import com.example.impulso.notify.BoardScheduler;
import com.example.impulso.notify.NotificationHelper;
import com.example.impulso.notify.ReminderScheduler;
import com.example.impulso.share.MonthPrinter;
import com.example.impulso.share.QrCodes;
import com.example.impulso.ui.workshop.WorkshopActivity;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.example.impulso.tts.SpeechReader;
import com.example.impulso.ui.Labels;
import com.example.impulso.ui.onboarding.OnboardingActivity;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private AppPreferences prefs;
    private final SpeechReader speech = new SpeechReader();
    private boolean updating;
    private final ActivityResultLauncher<ScanOptions> scanQr = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() == null || !result.getContents().startsWith(QrCodes.PREFIX) || !isAdded()) {
            return;
        }
        String text = result.getContents().substring(QrCodes.PREFIX.length()).trim();
        if (text.length() < 1 || text.length() > 500) {
            return;
        }
        ImpulsoApplication.from(requireContext()).userData().io(() -> {
            ImpulsoApplication.from(requireContext()).userData().saveUserQuoteSync(null, text, "");
            BoardScheduler.refresh(requireContext().getApplicationContext());
            if (isAdded()) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), R.string.imported_quote, Toast.LENGTH_SHORT).show());
            }
        });
    });
    private final ActivityResultLauncher<String> createDocument =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), this::exportTo);
    private final ActivityResultLauncher<String[]> openDocument =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::importFrom);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        prefs = new AppPreferences(requireContext());
        fillRadios(binding.groupGoal, Goals.IDS, true);
        fillRadios(binding.groupTone, Tones.IDS, false);
        binding.inputName.setText(prefs.name());
        check(binding.groupGoal, prefs.goal());
        check(binding.groupTone, prefs.tone());
        updating = true;
        if ("light".equals(prefs.theme())) binding.themeLight.setChecked(true);
        else if ("dark".equals(prefs.theme())) binding.themeDark.setChecked(true);
        else if ("illustration".equals(prefs.theme())) binding.themeIllustration.setChecked(true);
        else binding.themeSystem.setChecked(true);
        binding.switchSkipWelcome.setChecked(prefs.skipWelcome());
        binding.seekSpeed.setProgress(Math.round((prefs.ttsRate() - 0.5f) * 100f));
        updating = false;
        binding.buttonSaveProfile.setOnClickListener(v -> saveProfile());
        binding.groupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            if (updating) return;
            String theme = "system";
            if (checkedId == R.id.theme_light) theme = "light";
            else if (checkedId == R.id.theme_dark) theme = "dark";
            else if (checkedId == R.id.theme_illustration) theme = "illustration";
            prefs.setTheme(theme);
            requireActivity().recreate();
        });
        binding.switchSkipWelcome.setOnCheckedChangeListener((b, checked) -> {
            if (!updating) prefs.setSkipWelcome(checked);
        });
        binding.seekSpeed.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                prefs.setTtsRate(0.5f + seekBar.getProgress() / 100f);
            }
        });
        binding.buttonExport.setOnClickListener(v -> createDocument.launch("impulso-respaldo.json"));
        binding.buttonImport.setOnClickListener(v -> openDocument.launch(new String[]{"application/json", "text/plain", "*/*"}));
        binding.buttonClearHistory.setOnClickListener(v -> confirmHistory());
        binding.buttonDeleteAll.setOnClickListener(v -> confirmAll());
        binding.textAbout.setText(getString(R.string.about_body, CatalogLoader.EXPECTED_VERSION));
        binding.textVersion.setText(getString(R.string.version_line, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        addExtras();
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        speech.init(requireContext(), ok -> {
            if (binding == null) return;
            List<String> labels = new ArrayList<>();
            List<String> names = new ArrayList<>();
            labels.add(getString(R.string.voice_default));
            names.add("");
            for (SpeechReader.VoiceOption voice : speech.voices()) {
                names.add(voice.name);
                labels.add(voice.network ? getString(R.string.voice_network, voice.label) : voice.label);
            }
            binding.spinnerVoice.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, labels));
            String current = prefs.ttsVoice();
            for (int i = 0; i < names.size(); i++) {
                if (names.get(i).equals(current)) binding.spinnerVoice.setSelection(i);
            }
            binding.spinnerVoice.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    prefs.setTtsVoice(names.get(position));
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {
                }
            });
            if (!ok && speech.voices().isEmpty()) {
                Snackbar.make(binding.getRoot(), R.string.tts_missing, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onStop() {
        speech.shutdown();
        super.onStop();
    }

    private void fillRadios(android.widget.RadioGroup group, String[] ids, boolean goals) {
        for (String id : ids) {
            RadioButton button = new RadioButton(requireContext());
            button.setText(goals ? Labels.goal(requireContext(), id) : Labels.tone(requireContext(), id));
            button.setTag(id);
            button.setMinHeight(getResources().getDimensionPixelSize(R.dimen.touch));
            group.addView(button);
        }
    }

    private void check(android.widget.RadioGroup group, String id) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (id != null && id.equals(child.getTag())) group.check(child.getId());
        }
    }

    private String selected(android.widget.RadioGroup group) {
        int id = group.getCheckedRadioButtonId();
        if (id == -1) return "";
        View view = group.findViewById(id);
        return view == null || view.getTag() == null ? "" : String.valueOf(view.getTag());
    }

    private void addExtras() {
        android.widget.LinearLayout column = (android.widget.LinearLayout) ((android.view.ViewGroup) binding.getRoot()).getChildAt(0);
        TextTitle(column, R.string.deck_title);
        deckButton(column, R.string.deck_all, Decks.ALL);
        deckButton(column, R.string.deck_rest, Decks.REST);
        deckButton(column, R.string.deck_craft, Decks.CRAFT);
        deckButton(column, R.string.deck_inner, Decks.INNER);
        deckButton(column, R.string.deck_own, Decks.OWN);
        switchRow(column, R.string.lockscreen_switch, prefs.lockscreenQuote(), checked -> {
            prefs.setLockscreenQuote(checked);
            BoardScheduler.refreshAsync(requireContext());
        });
        switchRow(column, R.string.chime_switch, prefs.chimeEnabled(), prefs::setChimeEnabled);
        switchRow(column, R.string.day_close_switch, prefs.dayCloseEnabled(), checked -> {
            prefs.setDayCloseEnabled(checked);
            BoardScheduler.refreshAsync(requireContext());
        });
        com.google.android.material.button.MaterialButton time = new com.google.android.material.button.MaterialButton(requireContext());
        time.setText(formatMin(prefs.dayCloseMin()));
        time.setMinHeight(getResources().getDimensionPixelSize(R.dimen.touch));
        time.setOnClickListener(v -> {
            int minutes = prefs.dayCloseMin();
            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                    .setHour(minutes / 60)
                    .setMinute(minutes % 60)
                    .build();
            picker.addOnPositiveButtonClickListener(ignored -> {
                prefs.setDayCloseMin(picker.getHour() * 60 + picker.getMinute());
                time.setText(formatMin(prefs.dayCloseMin()));
                BoardScheduler.refreshAsync(requireContext());
            });
            picker.show(getParentFragmentManager(), "close");
        });
        column.addView(time);
        textButton(column, R.string.workshop, () -> startActivity(new Intent(requireContext(), WorkshopActivity.class)));
        textButton(column, R.string.print_month, () -> MonthPrinter.print(requireActivity()));
        textButton(column, R.string.scan_qr, () -> {
            ScanOptions options = new ScanOptions();
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
            options.setPrompt(getString(R.string.scan_qr));
            scanQr.launch(options);
        });
    }

    private void TextTitle(android.widget.LinearLayout column, int text) {
        android.widget.TextView title = new android.widget.TextView(requireContext());
        title.setText(text);
        title.setPadding(0, 32, 0, 8);
        column.addView(title);
    }

    private void deckButton(android.widget.LinearLayout column, int label, String deck) {
        textButton(column, label, () -> {
            prefs.setDeck(deck);
            Toast.makeText(requireContext(), R.string.profile_saved, Toast.LENGTH_LONG).show();
        });
    }

    private void textButton(android.widget.LinearLayout column, int label, Runnable action) {
        com.google.android.material.button.MaterialButton button = new com.google.android.material.button.MaterialButton(requireContext());
        button.setText(label);
        button.setMinHeight(getResources().getDimensionPixelSize(R.dimen.touch));
        button.setOnClickListener(v -> action.run());
        column.addView(button);
    }

    private void switchRow(android.widget.LinearLayout column, int label, boolean checked, java.util.function.Consumer<Boolean> onChange) {
        MaterialSwitch row = new MaterialSwitch(requireContext());
        row.setText(label);
        row.setChecked(checked);
        row.setMinHeight(getResources().getDimensionPixelSize(R.dimen.touch));
        row.setOnCheckedChangeListener((b, value) -> onChange.accept(value));
        column.addView(row);
    }

    private static String formatMin(int minutes) {
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes / 60, minutes % 60);
    }

    private void saveProfile() {
        String name = UserQuoteRules.normalize(String.valueOf(binding.inputName.getText()));
        if (!UserQuoteRules.isValidName(name)) {
            Toast.makeText(requireContext(), R.string.quote_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        prefs.saveProfile(name, selected(binding.groupGoal), selected(binding.groupTone));
        Snackbar.make(binding.getRoot(), R.string.profile_saved, Snackbar.LENGTH_LONG).show();
    }

    private void exportTo(Uri uri) {
        if (uri == null || binding == null) return;
        boolean history = binding.switchHistory.isChecked();
        AppDatabase.IO.execute(() -> {
            try {
                BackupData data = ImpulsoApplication.from(requireContext()).userData().snapshotSync(history);
                data.preferences = prefs.exportPreferences();
                data.exportedAt = OffsetDateTime.now().toString();
                data.format = BackupData.FORMAT;
                data.version = BackupData.VERSION;
                byte[] bytes = BackupCodec.toJson(data).getBytes(StandardCharsets.UTF_8);
                try (OutputStream stream = requireContext().getContentResolver().openOutputStream(uri)) {
                    if (stream != null) stream.write(bytes);
                }
            } catch (Exception exception) {
                if (isAdded()) requireActivity().runOnUiThread(() ->
                        Snackbar.make(binding.getRoot(), R.string.import_error_invalid, Snackbar.LENGTH_LONG).show());
            }
        });
    }

    private void importFrom(Uri uri) {
        if (uri == null) return;
        AppDatabase.IO.execute(() -> {
            try {
                String json = readLimited(uri);
                if (!isAdded()) return;
                if (json == null) {
                    requireActivity().runOnUiThread(() -> showError(R.string.import_error_large));
                    return;
                }
                BackupCodec.Parse parse = BackupCodec.parse(json);
                if (!parse.isOk()) {
                    int message = switch (parse.error) {
                        case BackupCodec.TOO_LARGE -> R.string.import_error_large;
                        case BackupCodec.WRONG_FORMAT -> R.string.import_error_format;
                        case BackupCodec.WRONG_VERSION -> R.string.import_error_version;
                        case BackupCodec.INVALID_CONTENT -> R.string.import_error_content;
                        default -> R.string.import_error_invalid;
                    };
                    requireActivity().runOnUiThread(() -> showError(message));
                    return;
                }
                BackupData local = ImpulsoApplication.from(requireContext()).userData().snapshotSync(true);
                local.preferences = prefs.exportPreferences();
                BackupMerger.Result preview = BackupMerger.merge(local, prefs.isOnboardingComplete(), parse.data);
                requireActivity().runOnUiThread(() -> confirmImport(preview));
            } catch (Exception exception) {
                if (isAdded()) requireActivity().runOnUiThread(() -> showError(R.string.import_error_invalid));
            }
        });
    }

    private void confirmImport(BackupMerger.Result preview) {
        if (binding == null) return;
        int conflicts = preview.quotesKeptLocal + preview.actionsKeptLocal + preview.remindersKeptLocal + (preview.preferencesKeptLocal ? 1 : 0);
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.import_title)
                .setMessage(getString(R.string.import_counts, preview.favoritesAdded, preview.quotesAdded, conflicts, preview.remindersAdded))
                .setPositiveButton(R.string.import_apply, (d, w) -> AppDatabase.IO.execute(() -> {
                    if (!preview.preferencesKeptLocal) prefs.applyImportedPreferences(preview.data.preferences);
                    ImpulsoApplication.from(requireContext()).userData().applyBackupSync(preview.data);
                    new ReminderScheduler(requireContext()).reconcile();
                    if (isAdded()) requireActivity().runOnUiThread(() ->
                            Snackbar.make(binding.getRoot(), R.string.import_done, Snackbar.LENGTH_LONG).show());
                }))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showError(int message) {
        if (binding != null) Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    private String readLimited(Uri uri) throws Exception {
        try (InputStream input = requireContext().getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IllegalStateException("empty");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) >= 0) {
                total += read;
                if (total > BackupData.MAX_BYTES) return null;
                output.write(buffer, 0, read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private void confirmHistory() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.clear_history_title)
                .setMessage(R.string.clear_history_message)
                .setPositiveButton(R.string.delete_all_confirm, (d, w) -> AppDatabase.IO.execute(() -> {
                    ImpulsoApplication.from(requireContext()).userData().clearHistorySync();
                    if (isAdded()) requireActivity().runOnUiThread(() ->
                            Snackbar.make(binding.getRoot(), R.string.delete_history_done, Snackbar.LENGTH_SHORT).show());
                }))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmAll() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_all_title)
                .setMessage(R.string.delete_all_message)
                .setPositiveButton(R.string.delete_all_confirm, (d, w) -> AppDatabase.IO.execute(() -> {
                    ImpulsoApplication.from(requireContext()).userData().clearAllSync();
                    prefs.clear();
                    new ReminderScheduler(requireContext()).cancelAlarm();
                    NotificationHelper.cancelReminders(requireContext());
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        startActivity(new Intent(requireContext(), OnboardingActivity.class));
                        requireActivity().finish();
                    });
                }))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
