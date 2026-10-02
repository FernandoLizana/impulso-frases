package com.example.impulso.domain;

import java.util.List;

/**
 * Prioridad de categorías según la primera respuesta del cuestionario.
 * autoestima → autoestima y confianza
 * calma → calma y descanso
 * constancia → constancia y resiliencia
 * enfoque → enfoque y propósito
 * cambios (afrontar cambios) → cambios y resiliencia
 * gratitud → gratitud y relaciones
 * Un objetivo vacío no prioriza categorías: entran todas las que respeten el tono.
 * El tono espiritual solo usa frases de ese tono, y solo si la persona lo eligió.
 */
public final class GoalToneMapping {
    private GoalToneMapping() {
    }

    public static List<String> categoriesFor(String goal) {
        if (goal == null) {
            return List.of();
        }
        return switch (goal) {
            case Goals.AUTOESTIMA -> List.of(Categories.AUTOESTIMA, Categories.CONFIANZA);
            case Goals.CALMA -> List.of(Categories.CALMA, Categories.DESCANSO);
            case Goals.CONSTANCIA -> List.of(Categories.CONSTANCIA, Categories.RESILIENCIA);
            case Goals.ENFOQUE -> List.of(Categories.ENFOQUE, Categories.PROPOSITO);
            case Goals.CAMBIOS -> List.of(Categories.CAMBIOS, Categories.RESILIENCIA);
            case Goals.GRATITUD -> List.of(Categories.GRATITUD, Categories.RELACIONES);
            default -> List.of();
        };
    }
}
