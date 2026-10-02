package com.example.impulso.domain;

import java.util.Set;

public final class Categories {
    public static final String AUTOESTIMA = "autoestima";
    public static final String CALMA = "calma";
    public static final String CONSTANCIA = "constancia";
    public static final String ENFOQUE = "enfoque";
    public static final String CAMBIOS = "cambios";
    public static final String GRATITUD = "gratitud";
    public static final String RESILIENCIA = "resiliencia";
    public static final String DESCANSO = "descanso";
    public static final String RELACIONES = "relaciones";
    public static final String CREATIVIDAD = "creatividad";
    public static final String PROPOSITO = "proposito";
    public static final String CONFIANZA = "confianza";

    public static final String[] IDS = {
            AUTOESTIMA, CALMA, CONSTANCIA, ENFOQUE, CAMBIOS, GRATITUD,
            RESILIENCIA, DESCANSO, RELACIONES, CREATIVIDAD, PROPOSITO, CONFIANZA
    };

    private static final Set<String> SET = Set.of(IDS);

    private Categories() {
    }

    public static boolean isValid(String id) {
        return id != null && SET.contains(id);
    }
}
