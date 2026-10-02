package com.example.impulso.domain;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CatalogIntegrityTest {
    @Test
    public void bundledCatalogMeetsEditorialRules() throws Exception {
        Path path = Path.of("src/main/assets/catalog.json");
        assertTrue(Files.exists(path));
        CatalogLoader.Loaded loaded = CatalogLoader.load(Files.newBufferedReader(path));
        assertEquals(loaded.problems.toString(), 0, loaded.problems.size());
        assertTrue(loaded.quotes.size() >= CatalogLoader.MIN_QUOTES);
        boolean found = false;
        for (Quote quote : loaded.quotes) {
            if ("Neville Goddard".equals(quote.author)
                    && quote.text.startsWith("Simplemente atrévete")) {
                found = true;
            }
            assertEquals("attributed", quote.authorship);
        }
        assertTrue(found);
    }
}
