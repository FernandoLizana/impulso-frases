package com.example.impulso.domain;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CatalogLoader {
    public static final int EXPECTED_VERSION = 1;
    public static final int MIN_QUOTES = 68;
    public static final int MIN_PER_CATEGORY = 1;
    public static final String AUTHORSHIP = "attributed";

    public static final class Loaded {
        public final int catalogVersion;
        public final List<Quote> quotes;
        public final List<String> problems;

        public Loaded(int catalogVersion, List<Quote> quotes, List<String> problems) {
            this.catalogVersion = catalogVersion;
            this.quotes = quotes;
            this.problems = problems;
        }
    }

    private CatalogLoader() {
    }

    public static Loaded load(Reader reader) {
        List<String> problems = new ArrayList<>();
        List<Quote> quotes = new ArrayList<>();
        int version = 0;
        try {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject()) {
                problems.add("root");
                return new Loaded(0, List.of(), problems);
            }
            JsonObject root = rootElement.getAsJsonObject();
            version = root.has("catalogVersion") ? root.get("catalogVersion").getAsInt() : 0;
            if (version != EXPECTED_VERSION) {
                problems.add("version");
            }
            if (!root.has("quotes") || !root.get("quotes").isJsonArray()) {
                problems.add("quotes");
                return new Loaded(version, List.of(), problems);
            }
            JsonArray array = root.getAsJsonArray("quotes");
            Set<String> ids = new HashSet<>();
            Set<String> texts = new HashSet<>();
            Map<String, Integer> perCategory = new HashMap<>();
            Map<String, Set<String>> tonesPerCategory = new HashMap<>();
            for (String category : Categories.IDS) {
                perCategory.put(category, 0);
                tonesPerCategory.put(category, new HashSet<>());
            }
            for (JsonElement element : array) {
                String problem = readQuote(element, quotes, ids, texts, perCategory, tonesPerCategory);
                if (problem != null) {
                    problems.add(problem);
                }
            }
            if (quotes.size() < MIN_QUOTES) {
                problems.add("count:" + quotes.size());
            }
            Set<String> tonesInCatalog = new HashSet<>();
            for (String category : Categories.IDS) {
                int count = perCategory.get(category);
                if (count < MIN_PER_CATEGORY) {
                    problems.add("category:" + category + ":" + count);
                }
                tonesInCatalog.addAll(tonesPerCategory.get(category));
            }
            for (String tone : Tones.IDS) {
                if (!tonesInCatalog.contains(tone)) {
                    problems.add("tone:" + tone);
                }
            }
        } catch (RuntimeException exception) {
            problems.add("invalid_json");
        }
        return new Loaded(version, List.copyOf(quotes), List.copyOf(problems));
    }

    private static String readQuote(
            JsonElement element,
            List<Quote> quotes,
            Set<String> ids,
            Set<String> texts,
            Map<String, Integer> perCategory,
            Map<String, Set<String>> tonesPerCategory) {
        if (!element.isJsonObject()) {
            return "item";
        }
        JsonObject object = element.getAsJsonObject();
        String id = text(object, "id");
        String body = text(object, "text");
        String author = text(object, "author");
        String authorship = text(object, "authorship");
        String category = text(object, "category");
        String tone = text(object, "tone");
        String reflection = text(object, "reflection");
        String micro = text(object, "microAction");
        String locale = text(object, "locale");
        int itemVersion = object.has("catalogVersion") && object.get("catalogVersion").isJsonPrimitive()
                ? object.get("catalogVersion").getAsInt() : 0;
        if (id == null || !id.matches("frase_[a-z]+_[0-9]{3}")) {
            return "id:" + id;
        }
        if (!ids.add(id)) {
            return "duplicate_id:" + id;
        }
        if (!id.startsWith("frase_" + category + "_")) {
            return "id_category:" + id;
        }
        if (body == null || body.length() < 20 || body.length() > 280) {
            return "text:" + id;
        }
        if (!texts.add(body.toLowerCase())) {
            return "duplicate_text:" + id;
        }
        if (author == null || author.trim().isEmpty() || !AUTHORSHIP.equals(authorship)) {
            return "author:" + id;
        }
        if (object.has("sourceUrl") && !object.get("sourceUrl").isJsonNull()) {
            return "source:" + id;
        }
        if (!Categories.isValid(category) || !Tones.isValid(tone)) {
            return "taxonomy:" + id;
        }
        if (reflection == null || reflection.length() < 10 || micro == null || micro.length() < 10) {
            return "action:" + id;
        }
        if (!"es".equals(locale) || itemVersion != EXPECTED_VERSION) {
            return "meta:" + id;
        }
        if (!object.has("tags") || !object.get("tags").isJsonArray() || object.getAsJsonArray("tags").isEmpty()) {
            return "tags:" + id;
        }
        List<String> tags = new ArrayList<>();
        for (JsonElement tag : object.getAsJsonArray("tags")) {
            if (!tag.isJsonPrimitive()) {
                return "tags:" + id;
            }
            tags.add(tag.getAsString());
        }
        String english = text(object, "english");
        quotes.add(new Quote(id, body, author, authorship, null, category, tone, tags, reflection, micro, locale, itemVersion, english));
        perCategory.put(category, perCategory.get(category) + 1);
        tonesPerCategory.get(category).add(tone);
        return null;
    }

    private static String text(JsonObject object, String name) {
        if (!object.has(name) || object.get(name).isJsonNull() || !object.get(name).isJsonPrimitive()) {
            return null;
        }
        return object.get(name).getAsString();
    }
}
