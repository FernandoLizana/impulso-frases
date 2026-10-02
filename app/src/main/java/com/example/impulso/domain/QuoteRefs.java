package com.example.impulso.domain;

public final class QuoteRefs {
    public static final String CATALOG = "catalog";
    public static final String USER = "user";

    private QuoteRefs() {
    }

    public static String key(String source, String id) {
        return source + ":" + id;
    }
}
