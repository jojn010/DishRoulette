package com.example.dishroulette;

import android.content.Context;

/**
 * Convert domain validation failures into the current interface language.
 */
final class IngredientValidation {
    private IngredientValidation() {
    }

    static String message(Context context, PantryEntries.ValidationError error) {
        if (error == null)
            return null;
        int message = switch (error) {
            case MULTIPLE_INGREDIENTS -> R.string.ingredient_single;
            case DUPLICATE -> R.string.ingredient_duplicate;
            case NAME_TOO_LONG -> R.string.ingredient_length;
            case SERVICE_LIMIT -> R.string.ingredient_limit;
        };
        return context.getString(message);
    }
}
