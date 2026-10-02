package com.example.impulso.data.catalog;

import android.content.Context;
import android.util.Log;

import com.example.impulso.domain.CatalogLoader;
import com.example.impulso.domain.Quote;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CatalogRepository {
    private final Context app;
    private List<Quote> quotes = List.of();
    private boolean loaded;

    public CatalogRepository(Context context) {
        this.app = context.getApplicationContext();
    }

    public List<Quote> quotes() {
        ensure();
        return quotes;
    }

    public Quote find(String id) {
        if (id == null) {
            return null;
        }
        for (Quote quote : quotes()) {
            if (id.equals(quote.id)) {
                return quote;
            }
        }
        return null;
    }

    private void ensure() {
        if (loaded) {
            return;
        }
        synchronized (this) {
            if (loaded) {
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(
                    app.getAssets().open("catalog.json"), StandardCharsets.UTF_8)) {
                CatalogLoader.Loaded loadedCatalog = CatalogLoader.load(reader);
                quotes = loadedCatalog.quotes;
                if (!loadedCatalog.problems.isEmpty()) {
                    Log.w("Impulso", "Problemas de catálogo: " + loadedCatalog.problems.size());
                }
            } catch (Exception exception) {
                Log.e("Impulso", "No se pudo leer el catálogo");
                quotes = List.of();
            }
            loaded = true;
        }
    }
}
