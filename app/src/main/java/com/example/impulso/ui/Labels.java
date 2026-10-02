package com.example.impulso.ui;

import android.content.Context;

import com.example.impulso.R;
import com.example.impulso.domain.Categories;
import com.example.impulso.domain.Goals;
import com.example.impulso.domain.Tones;

import java.time.LocalTime;

public final class Labels {
    private Labels() {
    }

    public static String category(Context context, String id) {
        if (id == null || id.isEmpty()) {
            return context.getString(R.string.category_none);
        }
        int res = switch (id) {
            case Categories.AUTOESTIMA -> R.string.category_autoestima;
            case Categories.CALMA -> R.string.category_calma;
            case Categories.CONSTANCIA -> R.string.category_constancia;
            case Categories.ENFOQUE -> R.string.category_enfoque;
            case Categories.CAMBIOS -> R.string.category_cambios;
            case Categories.GRATITUD -> R.string.category_gratitud;
            case Categories.RESILIENCIA -> R.string.category_resiliencia;
            case Categories.DESCANSO -> R.string.category_descanso;
            case Categories.RELACIONES -> R.string.category_relaciones;
            case Categories.CREATIVIDAD -> R.string.category_creatividad;
            case Categories.PROPOSITO -> R.string.category_proposito;
            case Categories.CONFIANZA -> R.string.category_confianza;
            default -> 0;
        };
        return res == 0 ? id : context.getString(res);
    }

    public static String goal(Context context, String id) {
        int res = switch (id == null ? "" : id) {
            case Goals.AUTOESTIMA -> R.string.goal_autoestima;
            case Goals.CALMA -> R.string.goal_calma;
            case Goals.CONSTANCIA -> R.string.goal_constancia;
            case Goals.ENFOQUE -> R.string.goal_enfoque;
            case Goals.CAMBIOS -> R.string.goal_cambios;
            case Goals.GRATITUD -> R.string.goal_gratitud;
            default -> 0;
        };
        return res == 0 ? "" : context.getString(res);
    }

    public static String tone(Context context, String id) {
        int res = switch (id == null ? "" : id) {
            case Tones.CERCANO -> R.string.tone_cercano;
            case Tones.DIRECTO -> R.string.tone_directo;
            case Tones.ESPIRITUAL -> R.string.tone_espiritual;
            default -> 0;
        };
        return res == 0 ? "" : context.getString(res);
    }

    public static String greeting(Context context, String name) {
        int hour = LocalTime.now().getHour();
        boolean named = name != null && !name.trim().isEmpty();
        if (hour < 12) {
            return named ? context.getString(R.string.greeting_morning_named, name) : context.getString(R.string.greeting_morning);
        }
        if (hour < 19) {
            return named ? context.getString(R.string.greeting_afternoon_named, name) : context.getString(R.string.greeting_afternoon);
        }
        return named ? context.getString(R.string.greeting_evening_named, name) : context.getString(R.string.greeting_evening);
    }
}
