package com.example.dishroulette;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import java.util.Locale;

/** Native app settings, independent from recipe preferences and canonical API values. */
final class InterfaceSettings {
    static final String STORE="dishroulette_ui", MODE="interface_mode", LANGUAGE="interface_language";
    private InterfaceSettings() { }
    static SharedPreferences prefs(Context c){return c.getSharedPreferences(STORE,0);}
    static boolean simple(Context c){
        SharedPreferences p=prefs(c);
        // Migrate legacy installations without changing recipes, ingredients or language.
        if (!"simple".equals(p.getString(MODE,""))) {
            p.edit().putString(MODE,"simple").apply();
        }
        return true;
    }
    static String language(Context c){
        String saved=prefs(c).getString(LANGUAGE,"");
        if(saved.equals("ar")||saved.equals("en"))return saved;
        android.os.LocaleList locales=android.content.res.Resources.getSystem().getConfiguration().getLocales();
        for(int i=0;i<locales.size();i++){String l=locales.get(i).getLanguage();if(l.equals("ar")||l.equals("en"))return l;}
        return "en";
    }
    static Context localized(Context base){
        Configuration config=new Configuration(base.getResources().getConfiguration());
        Locale locale=new Locale(language(base));config.setLocale(locale);config.setLayoutDirection(locale);
        return base.createConfigurationContext(config);
    }
}
