package com.example.impulso.domain;

import java.util.ArrayList;
import java.util.List;

/** Otra frase, de otra categoría, que no sustituye a la del día. */
public final class TensionQuote {
    private TensionQuote() {
    }

    public static Quote pick(List<Quote> catalog, Quote daily, String tone, long seed) {
        if (catalog == null || daily == null || daily.id == null) {
            return null;
        }
        boolean spiritual = Tones.ESPIRITUAL.equals(tone);
        List<Quote> pool = new ArrayList<>();
        for (Quote quote : catalog) {
            if (quote == null || quote.id == null || quote.id.equals(daily.id)) {
                continue;
            }
            if (daily.category != null && daily.category.equals(quote.category)) {
                continue;
            }
            if (spiritual) {
                if (!Tones.ESPIRITUAL.equals(quote.tone)) {
                    continue;
                }
            } else if (Tones.ESPIRITUAL.equals(quote.tone)) {
                continue;
            }
            pool.add(quote);
        }
        if (pool.isEmpty()) {
            return null;
        }
        pool.sort((a, b) -> a.id.compareTo(b.id));
        return pool.get((int) Math.floorMod(seed, pool.size()));
    }
}
