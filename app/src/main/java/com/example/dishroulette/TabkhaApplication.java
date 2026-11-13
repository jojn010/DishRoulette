package com.example.dishroulette;

/** Apply the saved DayNight choice before any activity or dialog is created. */
public final class TabkhaApplication extends android.app.Application {
    @Override public void onCreate(){super.onCreate();AppearanceSettings.apply(this);}
}
