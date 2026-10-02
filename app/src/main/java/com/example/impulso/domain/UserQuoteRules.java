package com.example.impulso.domain;

public final class UserQuoteRules {
    public static final int MAX_LENGTH = 500;

    private UserQuoteRules() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim();
    }

    public static boolean isValid(String normalized) {
        if (normalized == null || normalized.isEmpty()) {
            return false;
        }
        int count = normalized.codePointCount(0, normalized.length());
        return count >= 1 && count <= MAX_LENGTH;
    }

    public static boolean isValidName(String name) {
        if (name == null || name.isEmpty()) {
            return true;
        }
        return name.codePointCount(0, name.length()) <= 40;
    }
}
