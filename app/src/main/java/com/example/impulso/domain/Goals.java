package com.example.impulso.domain;

import java.util.Set;

public final class Goals {
    public static final String AUTOESTIMA = "autoestima";
    public static final String CALMA = "calma";
    public static final String CONSTANCIA = "constancia";
    public static final String ENFOQUE = "enfoque";
    public static final String CAMBIOS = "cambios";
    public static final String GRATITUD = "gratitud";

    public static final String[] IDS = {
            AUTOESTIMA, CALMA, CONSTANCIA, ENFOQUE, CAMBIOS, GRATITUD
    };

    private static final Set<String> SET = Set.of(IDS);

    private Goals() {
    }

    public static boolean isValid(String goal) {
        return goal != null && SET.contains(goal);
    }
}
