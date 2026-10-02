package com.example.impulso.ui;

import android.app.Activity;

import com.example.impulso.R;
import com.example.impulso.data.prefs.AppPreferences;

public final class Themes {
    private Themes() {
    }

    public static void apply(Activity activity) {
        if ("illustration".equals(new AppPreferences(activity).theme())) {
            activity.setTheme(R.style.Theme_Impulso_Illustration);
        }
    }
}
