package com.example.dishroulette.core;

/**
 * Sanity bounds for optional per-serving model estimates, never measured nutrition.
 */
public final class NutritionEstimate {
    private NutritionEstimate() {
    }

    public static boolean validNutrient(Double value, double maximum) {
        return value != null && Double.isFinite(value) && value >= 0 && value <= maximum;
    }

    public static boolean valid(Integer calories, Double protein) {
        return calories != null && protein != null && calories >= 1 && calories <= 5000
            && Double.isFinite(protein) && protein >= 0 && protein <= 500;
    }
}
