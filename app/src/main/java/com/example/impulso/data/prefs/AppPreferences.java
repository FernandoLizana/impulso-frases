package com.example.impulso.data.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.example.impulso.domain.BackupData;
import com.example.impulso.domain.Decks;
import com.example.impulso.domain.PreferenceMigration;
import com.example.impulso.domain.ReminderSlot;
import com.example.impulso.domain.UserQuoteRules;

public final class AppPreferences {
    public static final String FILE = "impulso_prefs";
    private static final String ONBOARDING = "onboarding_complete";
    private static final String STEP = "onboarding_step";
    private static final String DRAFT_NAME = "draft_name";
    private static final String DRAFT_GOAL = "draft_goal";
    private static final String DRAFT_TONE = "draft_tone";
    private static final String NAME = "profile_name";
    private static final String GOAL = "profile_goal";
    private static final String TONE = "profile_tone";
    private static final String THEME = "theme";
    private static final String SKIP_WELCOME = "skip_welcome";
    private static final String TTS_RATE = "tts_rate";
    private static final String TTS_VOICE = "tts_voice";
    private static final String REMINDERS = "reminders_enabled";
    private static final String QUIET_ENABLED = "quiet_enabled";
    private static final String QUIET_START = "quiet_start_min";
    private static final String QUIET_END = "quiet_end_min";
    private static final String PREFER_EXACT = "prefer_exact";
    private static final String INCLUDE_USER = "include_user_quotes";
    private static final String DAYS = "days_mask";
    private static final String PENDING_ID = "pending_quote_id";
    private static final String PENDING_SOURCE = "pending_quote_source";
    private static final String SCHEDULED_KEY = "scheduled_key";
    private static final String SCHEDULED_AT = "scheduled_at";
    private static final String SCHEDULED_MODE = "scheduled_mode";
    private static final String DECK = "deck";
    private static final String ANCHOR_ID = "anchor_id";
    private static final String ANCHOR_DAY = "anchor_day";
    private static final String ECHO_DATE = "echo_date";
    private static final String LOCKSCREEN = "lockscreen_quote";
    private static final String CHIME = "chime_enabled";
    private static final String CHIME_DATE = "chime_date";
    private static final String DAY_CLOSE = "day_close_enabled";
    private static final String DAY_CLOSE_MIN = "day_close_min";

    private final Context context;
    private final SharedPreferences prefs;

    public AppPreferences(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        migrateLegacy();
    }

    public boolean isOnboardingComplete() {
        return prefs.getBoolean(ONBOARDING, false);
    }

    public int onboardingStep() {
        return prefs.getInt(STEP, 0);
    }

    public void setOnboardingStep(int step) {
        prefs.edit().putInt(STEP, step).apply();
    }

    public String draftName() {
        return prefs.getString(DRAFT_NAME, "");
    }

    public String draftGoal() {
        return prefs.getString(DRAFT_GOAL, "");
    }

    public String draftTone() {
        return prefs.getString(DRAFT_TONE, "");
    }

    public void saveDraft(String name, String goal, String tone, int step) {
        prefs.edit()
                .putString(DRAFT_NAME, name == null ? "" : name)
                .putString(DRAFT_GOAL, goal == null ? "" : goal)
                .putString(DRAFT_TONE, tone == null ? "" : tone)
                .putInt(STEP, step)
                .apply();
    }

    public void completeOnboarding(String name, String goal, String tone) {
        prefs.edit()
                .putString(NAME, UserQuoteRules.normalize(name))
                .putString(GOAL, goal == null ? "" : goal)
                .putString(TONE, tone == null ? "" : tone)
                .putBoolean(ONBOARDING, true)
                .remove(DRAFT_NAME)
                .remove(DRAFT_GOAL)
                .remove(DRAFT_TONE)
                .remove(STEP)
                .apply();
    }

    public String name() {
        return prefs.getString(NAME, "");
    }

    public String goal() {
        return prefs.getString(GOAL, "");
    }

    public String tone() {
        return prefs.getString(TONE, "");
    }

    public void saveProfile(String name, String goal, String tone) {
        prefs.edit()
                .putString(NAME, UserQuoteRules.normalize(name))
                .putString(GOAL, goal == null ? "" : goal)
                .putString(TONE, tone == null ? "" : tone)
                .putBoolean(ONBOARDING, true)
                .apply();
    }

    public String theme() {
        return prefs.getString(THEME, "system");
    }

    public void setTheme(String theme) {
        prefs.edit().putString(THEME, theme).apply();
        applyTheme();
    }

    public void applyTheme() {
        int mode = switch (theme()) {
            case "light", "illustration" -> AppCompatDelegate.MODE_NIGHT_NO;
            case "dark" -> AppCompatDelegate.MODE_NIGHT_YES;
            default -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        };
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public boolean skipWelcome() {
        return prefs.getBoolean(SKIP_WELCOME, false);
    }

    public void setSkipWelcome(boolean skip) {
        prefs.edit().putBoolean(SKIP_WELCOME, skip).apply();
    }

    public float ttsRate() {
        return prefs.getFloat(TTS_RATE, 1f);
    }

    public void setTtsRate(float rate) {
        prefs.edit().putFloat(TTS_RATE, rate).apply();
    }

    public String ttsVoice() {
        return prefs.getString(TTS_VOICE, "");
    }

    public void setTtsVoice(String voice) {
        prefs.edit().putString(TTS_VOICE, voice == null ? "" : voice).apply();
    }

    public boolean remindersEnabled() {
        return prefs.getBoolean(REMINDERS, false);
    }

    public void setRemindersEnabled(boolean enabled) {
        prefs.edit().putBoolean(REMINDERS, enabled).apply();
    }

    public boolean quietEnabled() {
        return prefs.getBoolean(QUIET_ENABLED, true);
    }

    public void setQuietEnabled(boolean enabled) {
        prefs.edit().putBoolean(QUIET_ENABLED, enabled).apply();
    }

    public int quietStartMin() {
        return prefs.getInt(QUIET_START, 22 * 60);
    }

    public int quietEndMin() {
        return prefs.getInt(QUIET_END, 8 * 60);
    }

    public void setQuietStart(int minutes) {
        prefs.edit().putInt(QUIET_START, minutes).apply();
    }

    public void setQuietEnd(int minutes) {
        prefs.edit().putInt(QUIET_END, minutes).apply();
    }

    public boolean preferExact() {
        return prefs.getBoolean(PREFER_EXACT, false);
    }

    public void setPreferExact(boolean prefer) {
        prefs.edit().putBoolean(PREFER_EXACT, prefer).apply();
    }

    public boolean includeUserQuotes() {
        return prefs.getBoolean(INCLUDE_USER, false);
    }

    public void setIncludeUserQuotes(boolean include) {
        prefs.edit().putBoolean(INCLUDE_USER, include).apply();
    }

    public int daysMask() {
        return prefs.getInt(DAYS, ReminderSlot.ALL_DAYS);
    }

    public void setDaysMask(int mask) {
        prefs.edit().putInt(DAYS, mask).apply();
    }

    public void setPendingQuote(String source, String id) {
        prefs.edit().putString(PENDING_SOURCE, source).putString(PENDING_ID, id).apply();
    }

    public String pendingQuoteId() {
        return prefs.getString(PENDING_ID, "");
    }

    public String pendingQuoteSource() {
        return prefs.getString(PENDING_SOURCE, "");
    }

    public void clearPendingQuote() {
        prefs.edit().remove(PENDING_ID).remove(PENDING_SOURCE).apply();
    }

    public String scheduledKey() {
        return prefs.getString(SCHEDULED_KEY, "");
    }

    public long scheduledAt() {
        return prefs.getLong(SCHEDULED_AT, 0L);
    }

    public String scheduledMode() {
        return prefs.getString(SCHEDULED_MODE, "");
    }

    public void setSchedule(String key, long at, String mode) {
        prefs.edit()
                .putString(SCHEDULED_KEY, key)
                .putLong(SCHEDULED_AT, at)
                .putString(SCHEDULED_MODE, mode)
                .apply();
    }

    public void clearSchedule() {
        prefs.edit().remove(SCHEDULED_KEY).remove(SCHEDULED_AT).remove(SCHEDULED_MODE).apply();
    }

    public String deck() {
        return prefs.getString(DECK, Decks.ALL);
    }

    public void setDeck(String deck) {
        prefs.edit().putString(DECK, deck == null || deck.isEmpty() ? Decks.ALL : deck).apply();
    }

    public String anchorId() {
        return prefs.getString(ANCHOR_ID, "");
    }

    public long anchorDay() {
        return prefs.getLong(ANCHOR_DAY, -1L);
    }

    public void setAnchor(String quoteId, long epochDay) {
        prefs.edit().putString(ANCHOR_ID, quoteId == null ? "" : quoteId).putLong(ANCHOR_DAY, epochDay).apply();
    }

    public void clearAnchor() {
        prefs.edit().remove(ANCHOR_ID).remove(ANCHOR_DAY).apply();
    }

    public String echoDate() {
        return prefs.getString(ECHO_DATE, "");
    }

    public void setEchoDate(String date) {
        prefs.edit().putString(ECHO_DATE, date == null ? "" : date).apply();
    }

    public boolean lockscreenQuote() {
        return prefs.getBoolean(LOCKSCREEN, false);
    }

    public void setLockscreenQuote(boolean enabled) {
        prefs.edit().putBoolean(LOCKSCREEN, enabled).apply();
    }

    public boolean chimeEnabled() {
        return prefs.getBoolean(CHIME, true);
    }

    public void setChimeEnabled(boolean enabled) {
        prefs.edit().putBoolean(CHIME, enabled).apply();
    }

    public String chimeDate() {
        return prefs.getString(CHIME_DATE, "");
    }

    public void setChimeDate(String date) {
        prefs.edit().putString(CHIME_DATE, date == null ? "" : date).apply();
    }

    public boolean dayCloseEnabled() {
        return prefs.getBoolean(DAY_CLOSE, false);
    }

    public void setDayCloseEnabled(boolean enabled) {
        prefs.edit().putBoolean(DAY_CLOSE, enabled).apply();
    }

    public int dayCloseMin() {
        return prefs.getInt(DAY_CLOSE_MIN, 21 * 60);
    }

    public void setDayCloseMin(int minutes) {
        prefs.edit().putInt(DAY_CLOSE_MIN, minutes).apply();
    }

    public BackupData.Preferences exportPreferences() {
        BackupData.Preferences out = new BackupData.Preferences();
        out.name = name();
        out.goal = goal();
        out.tone = tone();
        out.theme = theme();
        out.skipWelcome = skipWelcome();
        out.ttsRate = ttsRate();
        out.ttsVoice = ttsVoice();
        out.quietEnabled = quietEnabled();
        out.quietStartMin = quietStartMin();
        out.quietEndMin = quietEndMin();
        out.daysMask = daysMask();
        out.includeUserQuotes = includeUserQuotes();
        out.preferExact = preferExact();
        return out;
    }

    public void applyImportedPreferences(BackupData.Preferences incoming) {
        if (incoming == null) {
            return;
        }
        prefs.edit()
                .putString(NAME, incoming.name == null ? "" : UserQuoteRules.normalize(incoming.name))
                .putString(GOAL, incoming.goal == null ? "" : incoming.goal)
                .putString(TONE, incoming.tone == null ? "" : incoming.tone)
                .putString(THEME, incoming.theme == null ? "system" : incoming.theme)
                .putBoolean(SKIP_WELCOME, incoming.skipWelcome)
                .putFloat(TTS_RATE, incoming.ttsRate)
                .putString(TTS_VOICE, incoming.ttsVoice == null ? "" : incoming.ttsVoice)
                .putBoolean(QUIET_ENABLED, incoming.quietEnabled)
                .putInt(QUIET_START, incoming.quietStartMin)
                .putInt(QUIET_END, incoming.quietEndMin)
                .putInt(DAYS, incoming.daysMask)
                .putBoolean(INCLUDE_USER, incoming.includeUserQuotes)
                .putBoolean(PREFER_EXACT, incoming.preferExact)
                .putBoolean(ONBOARDING, true)
                .apply();
        applyTheme();
    }

    public void clear() {
        prefs.edit().clear().apply();
        applyTheme();
    }

    private void migrateLegacy() {
        PreferenceMigration.Input input = new PreferenceMigration.Input();
        input.newSchemaPresent = prefs.contains(ONBOARDING) || prefs.contains(NAME) || prefs.contains(STEP);
        if (input.newSchemaPresent) {
            return;
        }
        SharedPreferences legacy = context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        input.legacyName = first(prefs, legacy, "nombre", "name", "user_name");
        input.legacyGoal = first(prefs, legacy, "pregunta1", "objetivo", "goal");
        input.legacyTone = first(prefs, legacy, "pregunta2", "estilo", "tone");
        PreferenceMigration.Output output = PreferenceMigration.migrate(input);
        if (!output.shouldWrite) {
            return;
        }
        prefs.edit()
                .putString(NAME, output.name)
                .putString(GOAL, output.goal)
                .putString(TONE, output.tone)
                .putBoolean(ONBOARDING, output.onboardingComplete)
                .apply();
    }

    private static String first(SharedPreferences primary, SharedPreferences secondary, String... keys) {
        for (String key : keys) {
            String value = primary.getString(key, "");
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
            value = secondary.getString(key, "");
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return "";
    }
}
