package com.example.dishroulette;

import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.example.dishroulette.core.IngredientExclusions;
import com.example.dishroulette.core.SpinOptions;
import java.util.*;

/** Local user choices. No calorie target or inferred medical profile is stored. */
public final class SpinProfile {
    @SerializedName("servings") public int servings = 2;
    @SerializedName("mode") public String mode = "pantry";
    @SerializedName("goal") public String goal = "Balanced";
    @SerializedName("cuisine") public String cuisine = "Any cuisine";
    @SerializedName("useFavorites") public boolean useFavorites = true;
    @SerializedName("allergies") public List<String> allergies = new ArrayList<>();
    @SerializedName("customExclusions") public List<String> customExclusions = new ArrayList<>();
    @SerializedName("requiresReview") public boolean requiresReview;
    private static SpinProfile invalidProfile() { SpinProfile p = new SpinProfile(); p.requiresReview = true; return p; }
    public boolean hasExclusions() { return !allergies.isEmpty() || !customExclusions.isEmpty(); }
    public static SpinProfile load(SharedPreferences preferences, Gson gson) {
        try {
            SpinProfile p = gson.fromJson(preferences.getString("spin_profile", "{}"), SpinProfile.class);
            if (p == null || !("pantry".equals(p.mode) || "discover".equals(p.mode))
                    || !SpinOptions.GOALS.contains(p.goal) || !SpinOptions.CUISINES.contains(p.cuisine)
                    || p.allergies == null || !SpinOptions.ALLERGENS.containsAll(p.allergies)
                    || p.customExclusions == null) return invalidProfile();
            // Recover only the new count; retain all existing dietary choices.
            if (p.servings < 1 || p.servings > 12) p.servings = 2;
            p.customExclusions = IngredientExclusions.parseCustom(String.join(",", p.customExclusions));
            return p;
        } catch (RuntimeException ignored) { return invalidProfile(); }
    }
    public void save(SharedPreferences preferences, Gson gson) {
        preferences.edit().putString("spin_profile", gson.toJson(this)).apply();
    }
    public boolean blocks(RecipeCatalog.Recipe recipe) {
        if (!hasExclusions()) return false;
        return !IngredientExclusions.conflicts(Arrays.asList(recipe.title, recipe.ingredients, recipe.method), allergies, customExclusions).isEmpty()
                || IngredientExclusions.hasUncertainIngredients(Collections.singletonList(recipe.ingredients));
    }
}
