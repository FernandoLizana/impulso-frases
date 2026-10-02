package com.example.impulso.domain;

public final class Decks {
    public static final String ALL = "todo";
    public static final String REST = "descanso";
    public static final String CRAFT = "oficio";
    public static final String INNER = "interior";
    public static final String OWN = "propias";

    private Decks() {
    }

    public static boolean contains(String deck, String category) {
        if (category == null) {
            return false;
        }
        return switch (deck) {
            case REST -> Categories.CALMA.equals(category) || Categories.DESCANSO.equals(category);
            case CRAFT -> Categories.ENFOQUE.equals(category)
                    || Categories.PROPOSITO.equals(category)
                    || Categories.CONSTANCIA.equals(category);
            case INNER -> Categories.AUTOESTIMA.equals(category)
                    || Categories.CONFIANZA.equals(category)
                    || Categories.GRATITUD.equals(category)
                    || Categories.RELACIONES.equals(category);
            default -> true;
        };
    }
}
