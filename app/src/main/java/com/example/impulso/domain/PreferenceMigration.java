package com.example.impulso.domain;

/**
 * Migración de preferencias antiguas. Este repositorio no traía datos previos;
 * si aparecen claves heredadas, no se reinicia el cuestionario.
 */
public final class PreferenceMigration {
    public static final class Input {
        public boolean newSchemaPresent;
        public String legacyName;
        public String legacyGoal;
        public String legacyTone;
    }

    public static final class Output {
        public final boolean shouldWrite;
        public final String name;
        public final String goal;
        public final String tone;
        public final boolean onboardingComplete;

        private Output(boolean shouldWrite, String name, String goal, String tone, boolean onboardingComplete) {
            this.shouldWrite = shouldWrite;
            this.name = name;
            this.goal = goal;
            this.tone = tone;
            this.onboardingComplete = onboardingComplete;
        }
    }

    private PreferenceMigration() {
    }

    public static Output migrate(Input input) {
        if (input == null || input.newSchemaPresent) {
            return new Output(false, "", "", "", false);
        }
        String name = cleanName(input.legacyName);
        String goal = mapGoal(input.legacyGoal);
        String tone = mapTone(input.legacyTone);
        if (name.isEmpty() && goal.isEmpty() && tone.isEmpty()) {
            return new Output(false, "", "", "", false);
        }
        return new Output(true, name, goal, tone, true);
    }

    private static String cleanName(String raw) {
        String name = UserQuoteRules.normalize(raw);
        if (!UserQuoteRules.isValidName(name)) {
            return "";
        }
        return name;
    }

    static String mapGoal(String raw) {
        String value = UserQuoteRules.normalize(raw).toLowerCase();
        return switch (value) {
            case "autoestima", "confianza en mi", "confianza en mí" -> Goals.AUTOESTIMA;
            case "calma", "tranquilidad" -> Goals.CALMA;
            case "constancia", "habitos", "hábitos" -> Goals.CONSTANCIA;
            case "enfoque", "concentracion", "concentración" -> Goals.ENFOQUE;
            case "cambios", "afrontar cambios", "cambio" -> Goals.CAMBIOS;
            case "gratitud" -> Goals.GRATITUD;
            default -> Goals.isValid(value) ? value : "";
        };
    }

    static String mapTone(String raw) {
        String value = UserQuoteRules.normalize(raw).toLowerCase();
        return switch (value) {
            case "cercano", "cercano y amable", "amable" -> Tones.CERCANO;
            case "directo", "directo y practico", "directo y práctico", "practico", "práctico" -> Tones.DIRECTO;
            case "espiritual", "reflexivo", "reflexivo y espiritual", "simbolico", "simbólico" -> Tones.ESPIRITUAL;
            default -> Tones.isValid(value) ? value : "";
        };
    }
}
