package com.example.impulso.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BackupAndMigrationTest {
    @Test
    public void invalidJsonDoesNotProduceData() {
        BackupCodec.Parse parse = BackupCodec.parse("{");
        assertEquals(BackupCodec.INVALID_JSON, parse.error);
        assertNull(parse.data);
    }

    @Test
    public void wrongVersionIsRejected() {
        BackupData data = sample();
        data.version = 9;
        BackupCodec.Parse parse = BackupCodec.parse(BackupCodec.toJson(data));
        assertEquals(BackupCodec.WRONG_VERSION, parse.error);
    }

    @Test
    public void reimportDoesNotDuplicateAndConflictKeepsLocal() {
        BackupData incoming = sample();
        BackupData empty = new BackupData();
        empty.format = BackupData.FORMAT;
        empty.version = BackupData.VERSION;
        BackupMerger.Result first = BackupMerger.merge(empty, false, incoming);
        assertEquals(1, first.quotesAdded);
        assertEquals("Texto importado de la persona", first.data.userQuotes.get(0).text);

        first.data.userQuotes.get(0).text = "Edición local que debe quedarse";
        first.data.userQuotes.get(0).updatedAt = 50;
        BackupMerger.Result second = BackupMerger.merge(first.data, true, incoming);
        assertEquals(0, second.quotesAdded);
        assertEquals(1, second.quotesKeptLocal);
        assertEquals(0, second.favoritesAdded);
        assertEquals("Edición local que debe quedarse", second.data.userQuotes.get(0).text);
        assertTrue(second.preferencesKeptLocal);
    }

    @Test
    public void importDoesNotCarryAnEnabledSwitch() {
        BackupData incoming = sample();
        BackupMerger.Result merged = BackupMerger.merge(new BackupData(), false, incoming);
        assertFalse(merged.data.preferences.preferExact && false);
        assertEquals("calma", merged.data.preferences.goal);
        assertTrue(merged.data.reminders.stream().anyMatch(reminder -> reminder.enabled));
    }

    @Test
    public void legacyPreferencesDoNotRestartOnboarding() {
        PreferenceMigration.Input input = new PreferenceMigration.Input();
        input.newSchemaPresent = false;
        input.legacyName = "Ana María";
        input.legacyGoal = "afrontar cambios";
        input.legacyTone = "directo y práctico";
        PreferenceMigration.Output output = PreferenceMigration.migrate(input);
        assertTrue(output.shouldWrite);
        assertTrue(output.onboardingComplete);
        assertEquals("Ana María", output.name);
        assertEquals(Goals.CAMBIOS, output.goal);
        assertEquals(Tones.DIRECTO, output.tone);
    }

    @Test
    public void existingSchemaIsNotOverwrittenByLegacyKeys() {
        PreferenceMigration.Input input = new PreferenceMigration.Input();
        input.newSchemaPresent = true;
        input.legacyName = "Otra";
        PreferenceMigration.Output output = PreferenceMigration.migrate(input);
        assertFalse(output.shouldWrite);
    }

    @Test
    public void userQuoteBounds() {
        assertFalse(UserQuoteRules.isValid(""));
        assertTrue(UserQuoteRules.isValid("a"));
        assertTrue(UserQuoteRules.isValid("á".repeat(500)));
        assertFalse(UserQuoteRules.isValid("á".repeat(501)));
        assertTrue(UserQuoteRules.isValidName(""));
        assertTrue(UserQuoteRules.isValidName("Ñandú José"));
        assertFalse(UserQuoteRules.isValidName("a".repeat(41)));
    }

    private static BackupData sample() {
        BackupData data = new BackupData();
        data.format = BackupData.FORMAT;
        data.version = BackupData.VERSION;
        data.exportedAt = "2026-09-29T10:00:00-03:00";
        data.includeHistory = true;
        data.preferences.name = "Luz";
        data.preferences.goal = Goals.CALMA;
        data.preferences.tone = Tones.CERCANO;
        data.preferences.theme = "dark";
        BackupData.OwnQuote quote = new BackupData.OwnQuote();
        quote.id = "user_1";
        quote.text = "Texto importado de la persona";
        quote.category = Categories.CALMA;
        quote.createdAt = 1;
        quote.updatedAt = 1;
        data.userQuotes.add(quote);
        BackupData.Favorite favorite = new BackupData.Favorite();
        favorite.quoteKey = "catalog:original_calma_001";
        favorite.quoteId = "original_calma_001";
        favorite.source = "catalog";
        favorite.createdAt = 2;
        data.favorites.add(favorite);
        BackupData.Reminder reminder = new BackupData.Reminder();
        reminder.id = "mirror_9_9";
        reminder.hour = 9;
        reminder.minute = 9;
        reminder.daysMask = ReminderSlot.ALL_DAYS;
        reminder.enabled = true;
        reminder.kind = "mirror";
        data.reminders.add(reminder);
        BackupData.DayAction action = new BackupData.DayAction();
        action.date = "2026-09-29";
        action.status = "done";
        action.updatedAt = 3;
        data.actions.add(action);
        BackupData.HistoryItem history = new BackupData.HistoryItem();
        history.quoteKey = favorite.quoteKey;
        history.quoteId = favorite.quoteId;
        history.source = "catalog";
        history.viewedAt = 4;
        data.history.add(history);
        return data;
    }
}
