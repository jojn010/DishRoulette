package com.example.dishroulette;

import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.example.dishroulette.core.IngredientExclusions;
import com.example.dishroulette.core.RecipeOptions;

import java.util.*;

/**
 * Local user choices. No calorie target or inferred medical profile is stored.
 */
public final class RecipePreferences {
    @SerializedName("servings")
    public int servings = 2;
    @SerializedName("mode")
    public String mode = "pantry";
    @SerializedName("goal")
    public String goal = "Balanced";
    @SerializedName("cuisine")
    public String cuisine = "Any cuisine";
    @SerializedName("useFavorites")
    public boolean useFavorites = true;
    @SerializedName("allergies")
    public List<String> allergies = new ArrayList<>();
    @SerializedName("customExclusions")
    public List<String> customExclusions = new ArrayList<>();
    @SerializedName("requiresReview")
    public boolean requiresReview;

    private static RecipePreferences invalidProfile() {
        RecipePreferences p = new RecipePreferences();
        p.requiresReview = true;
        return p;
    }

    public boolean hasExclusions() {
        return !allergies.isEmpty() || !customExclusions.isEmpty();
    }

    public static RecipePreferences load(SharedPreferences preferences, Gson gson) {
        try {
            RecipePreferences p = gson.fromJson(preferences.getString("spin_profile", "{}"), RecipePreferences.class);
            if (p == null || !("pantry".equals(p.mode) || "discover".equals(p.mode))
                || !RecipeOptions.GOALS.contains(p.goal) || !RecipeOptions.CUISINES.contains(p.cuisine)
                || p.allergies == null || !RecipeOptions.ALLERGENS.containsAll(p.allergies)
                || p.customExclusions == null)
                return invalidProfile();
            // Recover only the new count; retain all existing dietary choices.
            if (p.servings < 1 || p.servings > 12)
                p.servings = 2;
            p.customExclusions = IngredientExclusions.parseCustom(String.join(",", p.customExclusions));
            return p;
        } catch (RuntimeException ignored) {
            return invalidProfile();
        }
    }

    /**
     * Snapshot mutable choices before a request or preferences dialog edits them.
     */
    public RecipePreferences copy() {
        RecipePreferences copy = new RecipePreferences();
        copy.servings = servings;
        copy.mode = mode;
        copy.goal = goal;
        copy.cuisine = cuisine;
        copy.useFavorites = useFavorites;
        copy.requiresReview = requiresReview;
        copy.allergies = new ArrayList<>(allergies);
        copy.customExclusions = new ArrayList<>(customExclusions);
        return copy;
    }

    public void save(SharedPreferences preferences, Gson gson) {
        preferences.edit().putString("spin_profile", gson.toJson(this)).apply();
    }

    public boolean blocks(RecipeCatalog.Recipe recipe) {
        if (!hasExclusions())
            return false;
        if (recipe.bilingual())
            for (com.example.dishroulette.core.RecipeTranslation text : recipe.translations.values()) {
                java.util.List<String> check = new java.util.ArrayList<>(text.ingredients);
                check.addAll(text.steps);
                check.add(text.title);
                check.add(text.description);
                if (!IngredientExclusions.conflicts(check, allergies, customExclusions).isEmpty()
                    || IngredientExclusions.hasUncertainIngredients(text.ingredients))
                    return true;
            }
        return
            !IngredientExclusions.conflicts(Arrays.asList(recipe.title, recipe.ingredients, recipe.method), allergies,
                customExclusions).isEmpty()
                || IngredientExclusions.hasUncertainIngredients(Collections.singletonList(recipe.ingredients));
    }
}
