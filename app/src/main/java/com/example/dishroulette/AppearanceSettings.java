package com.example.dishroulette;

import android.content.Context;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

final class AppearanceSettings {
    private AppearanceSettings() {
    }

    static String choice(Context context) {
        String value = InterfaceSettings.prefs(context).getString("appearance", "system");
        return "light".equals(value) || "dark".equals(value) ? value : "system";
    }

    static void apply(Context context) {
        String value = choice(context);
        AppCompatDelegate.setDefaultNightMode("light".equals(value) ? AppCompatDelegate.MODE_NIGHT_NO :
            "dark".equals(value) ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    static void choose(Context context, String value) {
        if (!"light".equals(value) && !"dark".equals(value) && !"system".equals(value))
            return;
        InterfaceSettings.prefs(context).edit().putString("appearance", value).apply();
        apply(context);
    }

    static boolean dark(Context context) {
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES;
    }
}
