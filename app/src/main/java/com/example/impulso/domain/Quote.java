package com.example.impulso.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Quote {
    public final String id;
    public final String text;
    public final String author;
    public final String authorship;
    public final String sourceUrl;
    public final String category;
    public final String tone;
    public final List<String> tags;
    public final String reflection;
    public final String microAction;
    public final String locale;
    public final int catalogVersion;
    public final String english;

    public Quote(
            String id,
            String text,
            String author,
            String authorship,
            String sourceUrl,
            String category,
            String tone,
            List<String> tags,
            String reflection,
            String microAction,
            String locale,
            int catalogVersion,
            String english) {
        this.id = id;
        this.text = text;
        this.author = author;
        this.authorship = authorship;
        this.sourceUrl = sourceUrl;
        this.category = category;
        this.tone = tone;
        this.tags = Collections.unmodifiableList(new ArrayList<>(tags == null ? List.of() : tags));
        this.reflection = reflection;
        this.microAction = microAction;
        this.locale = locale;
        this.catalogVersion = catalogVersion;
        String trimmed = english == null ? "" : english.trim();
        this.english = trimmed.isEmpty() ? null : trimmed;
    }
}
