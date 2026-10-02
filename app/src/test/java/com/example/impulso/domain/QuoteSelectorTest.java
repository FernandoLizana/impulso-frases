package com.example.impulso.domain;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class QuoteSelectorTest {
    @Test
    public void emptyCatalogReturnsNull() {
        assertNull(QuoteSelector.select(List.of(), Goals.CALMA, Tones.CERCANO, List.of(), null, 1));
    }

    @Test
    public void singleQuoteIsReturned() {
        Quote only = quote("a", Categories.CALMA, Tones.CERCANO);
        assertEquals("a", QuoteSelector.select(List.of(only), Goals.CALMA, Tones.CERCANO, List.of(), null, 4).id);
    }

    @Test
    public void spiritualToneKeepsOnlySpiritualQuotes() {
        List<Quote> catalog = List.of(
                quote("c", Categories.CALMA, Tones.CERCANO),
                quote("s", Categories.CALMA, Tones.ESPIRITUAL));
        Quote selected = QuoteSelector.select(catalog, Goals.CALMA, Tones.ESPIRITUAL, List.of(), null, 1);
        assertEquals("s", selected.id);
    }

    @Test
    public void otherTonesExcludeSpiritualQuotes() {
        List<Quote> catalog = List.of(
                quote("s", Categories.CALMA, Tones.ESPIRITUAL),
                quote("c", Categories.CALMA, Tones.CERCANO));
        Quote selected = QuoteSelector.select(catalog, Goals.CALMA, Tones.CERCANO, List.of(), null, 1);
        assertEquals("c", selected.id);
    }

    @Test
    public void avoidsRecentWindowWhenAnotherExists() {
        List<Quote> catalog = new ArrayList<>();
        List<String> recent = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            catalog.add(quote("old" + i, Categories.CALMA, Tones.CERCANO));
            recent.add("old" + i);
        }
        catalog.add(quote("fresh", Categories.CALMA, Tones.CERCANO));
        Quote selected = QuoteSelector.select(catalog, Goals.CALMA, Tones.CERCANO, recent, null, 99);
        assertEquals("fresh", selected.id);
    }

    @Test
    public void reincludesOldestWhenEveryCandidateWasRecent() {
        List<Quote> catalog = List.of(
                quote("new", Categories.ENFOQUE, Tones.DIRECTO),
                quote("old", Categories.ENFOQUE, Tones.DIRECTO));
        List<String> recent = List.of("new", "old");
        Quote selected = QuoteSelector.select(catalog, Goals.ENFOQUE, Tones.DIRECTO, recent, null, 1);
        assertEquals("old", selected.id);
    }

    @Test
    public void doesNotRepeatConsecutiveWhenAnotherExists() {
        List<Quote> catalog = List.of(
                quote("a", Categories.GRATITUD, Tones.CERCANO),
                quote("b", Categories.GRATITUD, Tones.CERCANO));
        Quote selected = QuoteSelector.select(catalog, Goals.GRATITUD, Tones.CERCANO, List.of(), "a", 0);
        assertEquals("b", selected.id);
    }

    @Test
    public void prefersGoalCategories() {
        List<Quote> catalog = List.of(
                quote("other", Categories.CREATIVIDAD, Tones.CERCANO),
                quote("match", Categories.CAMBIOS, Tones.CERCANO));
        Quote selected = QuoteSelector.select(catalog, Goals.CAMBIOS, Tones.CERCANO, List.of(), null, 3);
        assertEquals("match", selected.id);
    }

    @Test
    public void expandsWhenPreferredCategoryHasNoToneMatch() {
        List<Quote> catalog = List.of(quote("wide", Categories.CREATIVIDAD, Tones.DIRECTO));
        Quote selected = QuoteSelector.select(catalog, Goals.CALMA, Tones.DIRECTO, List.of(), null, 2);
        assertEquals("wide", selected.id);
    }

    @Test
    public void spiritualWithoutMatchesReturnsNull() {
        List<Quote> catalog = List.of(quote("c", Categories.CALMA, Tones.CERCANO));
        assertNull(QuoteSelector.select(catalog, Goals.CALMA, Tones.ESPIRITUAL, List.of(), null, 1));
    }

    @Test
    public void dailySeedIsStable() {
        List<Quote> catalog = List.of(
                quote("a", Categories.CONFIANZA, Tones.CERCANO),
                quote("b", Categories.CONFIANZA, Tones.CERCANO),
                quote("c", Categories.CONFIANZA, Tones.CERCANO));
        Quote first = QuoteSelector.select(catalog, Goals.AUTOESTIMA, Tones.CERCANO, List.of(), null, 42);
        Quote second = QuoteSelector.select(catalog, Goals.AUTOESTIMA, Tones.CERCANO, List.of(), null, 42);
        assertNotNull(first);
        assertEquals(first.id, second.id);
        assertNotEquals(first.id, QuoteSelector.select(catalog, Goals.AUTOESTIMA, Tones.CERCANO, List.of(), first.id, 42).id);
    }

    private static Quote quote(String id, String category, String tone) {
        return new Quote(id, "Texto de prueba suficientemente largo", "Original de la app", "original",
                null, category, tone, List.of("prueba"), "Una reflexión breve.", "Haz un paso de un minuto.", "es", 1, null);
    }
}
