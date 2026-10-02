package com.example.impulso.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Elige una frase después de filtrar. No reintenta al azar.
 * Evita las últimas {@link #RECENT_WINDOW} distintas cuando aún queda otra.
 * Si no quedan, reincorpora primero la vista hace más tiempo.
 */
public final class QuoteSelector {
    public static final int RECENT_WINDOW = 30;

    private QuoteSelector() {
    }

    public static Quote select(
            List<Quote> catalog,
            String goal,
            String tone,
            List<String> recentNewestFirst,
            String avoidId,
            long seed) {
        return select(catalog, goal, tone, recentNewestFirst, avoidId, seed, List.of(), Decks.ALL);
    }

    public static Quote select(
            List<Quote> catalog,
            String goal,
            String tone,
            List<String> recentNewestFirst,
            String avoidId,
            long seed,
            List<Quote> extras) {
        return select(catalog, goal, tone, recentNewestFirst, avoidId, seed, extras, Decks.ALL);
    }

    public static Quote select(
            List<Quote> catalog,
            String goal,
            String tone,
            List<String> recentNewestFirst,
            String avoidId,
            long seed,
            List<Quote> extras,
            String deck) {
        List<Quote> base = catalog == null ? List.of() : catalog;
        if (Decks.OWN.equals(deck)) {
            base = List.of();
        } else if (deck != null && !Decks.ALL.equals(deck)) {
            List<Quote> scoped = new ArrayList<>();
            for (Quote quote : base) {
                if (Decks.contains(deck, quote.category)) {
                    scoped.add(quote);
                }
            }
            if (!scoped.isEmpty()) {
                base = scoped;
            }
        }
        List<Quote> extra = extras == null ? List.of() : extras;
        if (base.isEmpty() && extra.isEmpty()) {
            return null;
        }

        boolean spiritual = Tones.ESPIRITUAL.equals(tone);
        List<Quote> toneFiltered = new ArrayList<>();
        for (Quote quote : base) {
            if (quote == null || quote.id == null) {
                continue;
            }
            if (spiritual) {
                if (Tones.ESPIRITUAL.equals(quote.tone)) {
                    toneFiltered.add(quote);
                }
            } else if (!Tones.ESPIRITUAL.equals(quote.tone)) {
                toneFiltered.add(quote);
            }
        }
        if (!spiritual) {
            for (Quote quote : extra) {
                if (quote != null && quote.id != null) {
                    toneFiltered.add(quote);
                }
            }
        }
        if (toneFiltered.isEmpty()) {
            return null;
        }

        List<String> preferred = GoalToneMapping.categoriesFor(goal);
        List<Quote> pool = new ArrayList<>();
        if (!preferred.isEmpty()) {
            for (Quote quote : toneFiltered) {
                if (quote.category != null && preferred.contains(quote.category)) {
                    pool.add(quote);
                }
            }
        }
        if (pool.isEmpty()) {
            pool.addAll(toneFiltered);
        }

        List<Quote> fresh = withoutRecent(pool, recentNewestFirst);
        if (!fresh.isEmpty()) {
            List<Quote> candidates = avoidConsecutive(fresh, avoidId);
            candidates.sort((a, b) -> a.id.compareTo(b.id));
            int index = (int) Math.floorMod(seed, candidates.size());
            return candidates.get(index);
        }

        Quote oldest = oldestSeen(pool, recentNewestFirst, avoidId);
        if (oldest != null) {
            return oldest;
        }
        List<Quote> fallback = avoidConsecutive(pool, avoidId);
        fallback.sort((a, b) -> a.id.compareTo(b.id));
        if (fallback.isEmpty()) {
            return null;
        }
        return fallback.get((int) Math.floorMod(seed, fallback.size()));
    }

    private static List<Quote> withoutRecent(List<Quote> pool, List<String> recentNewestFirst) {
        if (recentNewestFirst == null || recentNewestFirst.isEmpty()) {
            return new ArrayList<>(pool);
        }
        Set<String> blocked = new LinkedHashSet<>();
        int limit = Math.min(RECENT_WINDOW, recentNewestFirst.size());
        for (int i = 0; i < limit; i++) {
            blocked.add(recentNewestFirst.get(i));
        }
        List<Quote> fresh = new ArrayList<>();
        for (Quote quote : pool) {
            if (!blocked.contains(quote.id)) {
                fresh.add(quote);
            }
        }
        return fresh;
    }

    private static Quote oldestSeen(List<Quote> pool, List<String> recentNewestFirst, String avoidId) {
        if (recentNewestFirst == null || recentNewestFirst.isEmpty()) {
            return null;
        }
        Map<String, Quote> byId = new HashMap<>();
        for (Quote quote : pool) {
            byId.put(quote.id, quote);
        }
        Quote oldestOther = null;
        Quote oldestAny = null;
        for (int i = recentNewestFirst.size() - 1; i >= 0; i--) {
            Quote quote = byId.get(recentNewestFirst.get(i));
            if (quote == null) {
                continue;
            }
            if (oldestAny == null) {
                oldestAny = quote;
            }
            if (oldestOther == null && (avoidId == null || !avoidId.equals(quote.id))) {
                oldestOther = quote;
            }
        }
        return oldestOther != null ? oldestOther : oldestAny;
    }

    private static List<Quote> avoidConsecutive(List<Quote> candidates, String avoidId) {
        if (avoidId == null || candidates.size() <= 1) {
            return new ArrayList<>(candidates);
        }
        List<Quote> filtered = new ArrayList<>();
        for (Quote quote : candidates) {
            if (!avoidId.equals(quote.id)) {
                filtered.add(quote);
            }
        }
        return filtered.isEmpty() ? new ArrayList<>(candidates) : filtered;
    }
}
