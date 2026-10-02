package com.example.impulso.domain;

import java.util.Set;

public final class Tones {
    public static final String CERCANO = "cercano";
    public static final String DIRECTO = "directo";
    public static final String ESPIRITUAL = "espiritual";

    public static final String[] IDS = {CERCANO, DIRECTO, ESPIRITUAL};
    private static final Set<String> SET = Set.of(IDS);

    private Tones() {
    }

    public static boolean isValid(String tone) {
        return tone != null && SET.contains(tone);
    }
}
