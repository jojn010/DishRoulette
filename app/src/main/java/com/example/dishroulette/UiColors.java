package com.example.dishroulette;

import android.content.Context;
import android.util.TypedValue;

/**
 * One palette lookup shared by programmatic screens and native XML theme attributes.
 */
final class UiColors {
    private UiColors() {
    }

    static int color(Context c, int id) {
        int attr = id == R.color.background ? R.attr.kitchenBackground : id == R.color.surface ? R.attr.kitchenSurface :
            id == R.color.ink ? R.attr.kitchenInk :
                id == R.color.muted ? R.attr.kitchenMuted : id == R.color.accent ? R.attr.kitchenAccent :
                    id == R.color.on_accent ? R.attr.kitchenOnAccent : id == R.color.line ? R.attr.kitchenLine : 0;
        if (attr == 0)
            return c.getColor(id);
        TypedValue value = new TypedValue();
        c.getTheme().resolveAttribute(attr, value, true);
        return value.data;
    }
}
