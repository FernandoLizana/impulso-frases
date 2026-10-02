package com.example.impulso.ui.reminders;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.impulso.ImpulsoApplication;
import com.example.impulso.R;
import com.example.impulso.data.db.AppDatabase;
import com.example.impulso.data.db.ReminderEntity;
import com.example.impulso.data.prefs.AppPreferences;
import com.example.impulso.databinding.FragmentRemindersBinding;
import com.example.impulso.domain.DeliveryCapability;
import com.example.impulso.domain.MirrorHours;
import com.example.impulso.domain.NextOccurrenceCalculator;
import com.example.impulso.domain.PlannedAlarm;
import com.example.impulso.domain.QuietHours;
import com.example.impulso.domain.Quote;
import com.example.impulso.domain.QuoteSelector;
import com.example.impulso.domain.ReminderSlot;
import com.example.impulso.notify.NotificationHelper;
import com.example.impulso.notify.ReminderScheduler;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class RemindersFragment extends Fragment {
    private FragmentRemindersBinding binding;
    private AppPreferences prefs;
    private boolean updating;
    private boolean chipsReady;
    private final androidx.activity.result.ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> refresh());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRemindersBinding.inflate(inflater, container, false);
        prefs = new AppPreferences(requireContext());
        bindSwitches();
        binding.buttonNotificationSettings.setOnClickListener(v -> openNotificationSettings());
        binding.buttonExactSettings.setOnClickListener(v -> openExactSettings());
        binding.buttonQuietStart.setOnClickListener(v -> pickQuiet(true));
        binding.buttonQuietEnd.setOnClickListener(v -> pickQuiet(false));
        binding.buttonSuggestion.setOnClickListener(v -> applySuggestion());
        binding.buttonAddTime.setOnClickListener(v -> addCustom());
        binding.buttonTest.setOnClickListener(v -> sendTest());
        ensureChips();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void bindSwitches() {
        updating = true;
        binding.switchMaster.setChecked(prefs.remindersEnabled());
        binding.switchExact.setChecked(prefs.preferExact());
        binding.switchQuiet.setChecked(prefs.quietEnabled());
        binding.switchIncludeUser.setChecked(prefs.includeUserQuotes());
        updating = false;
        binding.switchMaster.setOnCheckedChangeListener((b, checked) -> {
            if (updating) return;
            prefs.setRemindersEnabled(checked);
            if (checked && Build.VERSION.SDK_INT >= 33) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
            changed();
        });
        binding.switchExact.setOnCheckedChangeListener((b, checked) -> {
            if (updating) return;
            prefs.setPreferExact(checked);
            changed();
        });
        binding.switchQuiet.setOnCheckedChangeListener((b, checked) -> {
            if (updating) return;
            prefs.setQuietEnabled(checked);
            changed();
        });
        binding.switchIncludeUser.setOnCheckedChangeListener((b, checked) -> {
            if (updating) return;
            prefs.setIncludeUserQuotes(checked);
        });
    }

    private void ensureChips() {
        if (chipsReady) return;
        chipsReady = true;
        DayOfWeek[] days = DayOfWeek.values();
        int[] labels = {R.string.day_mon, R.string.day_tue, R.string.day_wed, R.string.day_thu, R.string.day_fri, R.string.day_sat, R.string.day_sun};
        for (int i = 0; i < days.length; i++) {
            Chip chip = new Chip(requireContext());
            chip.setText(labels[i]);
            chip.setCheckable(true);
            chip.setEnsureMinTouchTargetSize(true);
            chip.setTag(ReminderSlot.bit(days[i]));
            chip.setOnCheckedChangeListener((button, checked) -> {
                if (updating) return;
                saveDays();
            });
            binding.chipsDays.addView(chip);
        }
        for (int hour = 0; hour < 24; hour++) {
            Chip chip = new Chip(requireContext());
            chip.setText(time(hour, hour));
            chip.setCheckable(true);
            chip.setEnsureMinTouchTargetSize(true);
            chip.setTag(hour);
            chip.setOnCheckedChangeListener((button, checked) -> {
                if (updating) return;
                saveMirror((int) button.getTag(), checked);
            });
            binding.chipsMirrors.addView(chip);
        }
    }

    private void refresh() {
        if (binding == null) return;
        AppDatabase.IO.execute(() -> {
            List<ReminderEntity> reminders = ImpulsoApplication.from(requireContext()).userData().remindersSync();
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> applyReminders(reminders));
        });
    }

    private void applyReminders(List<ReminderEntity> reminders) {
        if (binding == null) return;
        updating = true;
        int mask = prefs.daysMask();
        for (int i = 0; i < binding.chipsDays.getChildCount(); i++) {
            Chip chip = (Chip) binding.chipsDays.getChildAt(i);
            int bit = (int) chip.getTag();
            chip.setChecked((mask & bit) != 0);
        }
        for (int i = 0; i < binding.chipsMirrors.getChildCount(); i++) {
            Chip chip = (Chip) binding.chipsMirrors.getChildAt(i);
            int hour = (int) chip.getTag();
            chip.setChecked(hasMirror(reminders, hour));
        }
        updating = false;
        binding.buttonQuietStart.setText(getString(R.string.quiet_start) + " " + time(prefs.quietStartMin() / 60, prefs.quietStartMin() % 60));
        binding.buttonQuietEnd.setText(getString(R.string.quiet_end) + " " + time(prefs.quietEndMin() / 60, prefs.quietEndMin() % 60));
        renderCustom(reminders);
        renderQuietWarning(reminders);
        renderStatusAndNext(reminders);
    }

    private void renderCustom(List<ReminderEntity> reminders) {
        binding.customContainer.removeAllViews();
        for (ReminderEntity reminder : reminders) {
            if (!"custom".equals(reminder.kind)) continue;
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            android.widget.TextView label = new android.widget.TextView(requireContext());
            label.setText(time(reminder.hour, reminder.minute));
            label.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            com.google.android.material.button.MaterialButton remove = new com.google.android.material.button.MaterialButton(requireContext());
            remove.setText(R.string.delete_quote);
            remove.setMinHeight(getResources().getDimensionPixelSize(R.dimen.touch));
            String id = reminder.id;
            remove.setOnClickListener(v -> AppDatabase.IO.execute(() -> {
                ImpulsoApplication.from(requireContext()).userData().deleteReminderSync(id);
                changedSync();
                if (isAdded()) requireActivity().runOnUiThread(this::refresh);
            }));
            row.addView(label);
            row.addView(remove);
            binding.customContainer.addView(row);
        }
    }

    private void renderQuietWarning(List<ReminderEntity> reminders) {
        if (!prefs.quietEnabled()) {
            binding.textQuietWarning.setVisibility(View.GONE);
            return;
        }
        QuietHours quiet = QuietHours.ofMinutes(true, prefs.quietStartMin(), prefs.quietEndMin());
        List<String> inside = new ArrayList<>();
        for (ReminderEntity reminder : reminders) {
            if (!reminder.enabled) continue;
            if (quiet.contains(java.time.LocalTime.of(reminder.hour, reminder.minute))) {
                inside.add(time(reminder.hour, reminder.minute));
            }
        }
        if (inside.isEmpty()) {
            binding.textQuietWarning.setVisibility(View.GONE);
        } else {
            binding.textQuietWarning.setVisibility(View.VISIBLE);
            binding.textQuietWarning.setText(getString(R.string.quiet_warning, String.join(", ", inside)));
        }
    }

    private void renderStatusAndNext(List<ReminderEntity> reminders) {
        boolean canExact = ReminderScheduler.canScheduleExact(requireContext());
        String mode = DeliveryCapability.resolve(
                prefs.remindersEnabled(),
                NotificationHelper.notificationsEnabled(requireContext()),
                NotificationHelper.channelEnabled(requireContext()),
                prefs.preferExact(),
                canExact
        );
        int status = switch (mode) {
            case DeliveryCapability.OFF -> R.string.status_off;
            case DeliveryCapability.BLOCKED -> R.string.status_blocked;
            case DeliveryCapability.EXACT -> R.string.status_exact;
            default -> R.string.status_approximate;
        };
        binding.textStatus.setText(status);
        binding.textExact.setText(!canExact && prefs.preferExact() ? R.string.exact_unavailable : R.string.delayed_notice);
        if (DeliveryCapability.OFF.equals(mode)) {
            binding.textNext.setText(R.string.next_none_off);
            return;
        }
        if (DeliveryCapability.BLOCKED.equals(mode)) {
            binding.textNext.setText(R.string.next_none_blocked);
            return;
        }
        List<ReminderSlot> slots = new ArrayList<>();
        for (ReminderEntity reminder : reminders) {
            if (reminder.enabled) {
                slots.add(new ReminderSlot(reminder.id, reminder.hour, reminder.minute, reminder.daysMask));
            }
        }
        QuietHours quiet = QuietHours.ofMinutes(prefs.quietEnabled(), prefs.quietStartMin(), prefs.quietEndMin());
        List<PlannedAlarm> plans = NextOccurrenceCalculator.plan(slots, quiet, ZonedDateTime.now(ZoneId.systemDefault()));
        StringBuilder builder = new StringBuilder();
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("EEE d MMM", new Locale("es"));
        boolean any = false;
        for (PlannedAlarm plan : plans) {
            if (plan.status == PlannedAlarm.Status.SCHEDULED && plan.when != null) {
                any = true;
                builder.append(getString(
                        R.string.next_item,
                        plan.when.format(dateFormat),
                        time(plan.slot.hour, plan.slot.minute),
                        getString(R.string.status_scheduled)
                )).append('\n');
            } else if (plan.status == PlannedAlarm.Status.INSIDE_QUIET) {
                builder.append(time(plan.slot.hour, plan.slot.minute))
                        .append(" · ")
                        .append(getString(R.string.status_quiet))
                        .append('\n');
            }
        }
        binding.textNext.setText(any || builder.length() > 0 ? builder.toString().trim() : getString(R.string.next_none_quiet));
    }

    private void saveDays() {
        int mask = 0;
        for (int i = 0; i < binding.chipsDays.getChildCount(); i++) {
            Chip chip = (Chip) binding.chipsDays.getChildAt(i);
            if (chip.isChecked()) mask |= (int) chip.getTag();
        }
        int finalMask = mask;
        prefs.setDaysMask(finalMask);
        AppDatabase.IO.execute(() -> {
            ImpulsoApplication.from(requireContext()).userData().updateDaysSync(finalMask);
            changedSync();
            if (isAdded()) requireActivity().runOnUiThread(this::refresh);
        });
    }

    private void saveMirror(int hour, boolean enabled) {
        AppDatabase.IO.execute(() -> {
            ImpulsoApplication app = ImpulsoApplication.from(requireContext());
            String id = mirrorId(hour);
            if (enabled) {
                ReminderEntity entity = new ReminderEntity();
                entity.id = id;
                entity.hour = hour;
                entity.minute = hour;
                entity.daysMask = prefs.daysMask();
                entity.enabled = true;
                entity.kind = "mirror";
                app.userData().saveReminderSync(entity);
            } else {
                app.userData().deleteReminderSync(id);
            }
            changedSync();
            if (isAdded()) requireActivity().runOnUiThread(this::refresh);
        });
    }

    private void applySuggestion() {
        AppDatabase.IO.execute(() -> {
            ImpulsoApplication app = ImpulsoApplication.from(requireContext());
            for (int[] pair : MirrorHours.SUGGESTED) {
                ReminderEntity entity = new ReminderEntity();
                entity.id = mirrorId(pair[0]);
                entity.hour = pair[0];
                entity.minute = pair[1];
                entity.daysMask = prefs.daysMask();
                entity.enabled = true;
                entity.kind = "mirror";
                app.userData().saveReminderSync(entity);
            }
            changedSync();
            if (isAdded()) requireActivity().runOnUiThread(this::refresh);
        });
    }

    private void addCustom() {
        ZonedDateTime now = ZonedDateTime.now();
        new TimePickerDialog(requireContext(), (view, hour, minute) -> {
            if (MirrorHours.isMirror(hour, minute)) {
                Snackbar.make(binding.getRoot(), R.string.custom_is_mirror, Snackbar.LENGTH_SHORT).show();
                saveMirror(hour, true);
                return;
            }
            AppDatabase.IO.execute(() -> {
                ReminderEntity entity = new ReminderEntity();
                entity.id = "custom_" + UUID.randomUUID();
                entity.hour = hour;
                entity.minute = minute;
                entity.daysMask = prefs.daysMask();
                entity.enabled = true;
                entity.kind = "custom";
                ImpulsoApplication.from(requireContext()).userData().saveReminderSync(entity);
                changedSync();
                if (isAdded()) requireActivity().runOnUiThread(this::refresh);
            });
        }, now.getHour(), now.getMinute(), true).show();
    }

    private void pickQuiet(boolean start) {
        int minutes = start ? prefs.quietStartMin() : prefs.quietEndMin();
        new TimePickerDialog(requireContext(), (view, hour, minute) -> {
            if (start) prefs.setQuietStart(hour * 60 + minute);
            else prefs.setQuietEnd(hour * 60 + minute);
            changed();
        }, minutes / 60, minutes % 60, true).show();
    }

    private void sendTest() {
        AppDatabase.IO.execute(() -> {
            if (!NotificationHelper.canPost(requireContext())) {
                if (isAdded()) requireActivity().runOnUiThread(() ->
                        Snackbar.make(binding.getRoot(), R.string.test_blocked, Snackbar.LENGTH_LONG).show());
                return;
            }
            ImpulsoApplication app = ImpulsoApplication.from(requireContext());
            Quote quote = QuoteSelector.select(app.catalog().quotes(), prefs.goal(), prefs.tone(), app.userData().recentIdsSync(), null, System.nanoTime());
            if (quote == null) return;
            NotificationHelper.showQuote(requireContext(), getString(R.string.test_title), quote.text, quote.author, "catalog", quote.id, true);
            if (isAdded()) requireActivity().runOnUiThread(() ->
                    Snackbar.make(binding.getRoot(), R.string.test_sent, Snackbar.LENGTH_SHORT).show());
        });
    }

    private void changed() {
        new ReminderScheduler(requireContext()).reconcileAsync();
        refresh();
    }

    private void changedSync() {
        new ReminderScheduler(requireContext()).reconcile();
    }

    private void openNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
        startActivity(intent);
    }

    private void openExactSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
            startActivity(intent);
        }
    }

    private static boolean hasMirror(List<ReminderEntity> reminders, int hour) {
        String id = mirrorId(hour);
        for (ReminderEntity reminder : reminders) {
            if (id.equals(reminder.id) && reminder.enabled) return true;
        }
        return false;
    }

    private static String mirrorId(int hour) {
        return String.format(Locale.ROOT, "mirror_%02d_%02d", hour, hour);
    }

    private static String time(int hour, int minute) {
        return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
