package com.example.dishroulette;

import android.content.Context;
import android.provider.Settings;

/**
 * One motion policy for native transitions.
 */
final class Experience {
    private Experience() {
    }

    static boolean reduced(Context context) {
        return context.getSharedPreferences("dishroulette_ui", 0).getBoolean("reduce_motion", false)
            || Settings.Global.getFloat(context.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0;
    }

    static long duration(Context context, long normal) {
        return reduced(context) ? 0 : normal;
    }
}
