package com.example.dishroulette;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import com.example.dishroulette.core.IngredientExclusions;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

/** Typed, validated response from our backend; never accepts executable model output. */
public final class RecipeSuggestion {
    @SerializedName("title") public String title;
    @SerializedName("meal") public String meal;
    @SerializedName("description") public String description;
    @SerializedName("minutes") public Integer minutes;
    @SerializedName("servings") public Integer servings;
    @SerializedName("ingredients") public List<String> ingredients;
    @SerializedName("steps") public List<String> steps;
    @SerializedName("pantryUsed") public List<String> pantryUsed;
    @SerializedName("additionalIngredients") public List<String> additionalIngredients;

    @SerializedName("cuisine") public String cuisine;
    @SerializedName("whyThisDish") public String whyThisDish;
    @SerializedName("goalFit") public String goalFit;
    public RecipeCatalog.Recipe toRecipe(Set<String> pantry) { return toRecipe(pantry, new SpinProfile()); }
    public RecipeCatalog.Recipe toRecipe(Set<String> pantry, SpinProfile profile) {
        if (!text(title, 120) || !text(description, 600) || !text(meal, 30)
                || (!com.example.dishroulette.core.SpinOptions.MEALS.contains(meal) || meal.equals("Any meal"))
                || minutes == null || minutes < 1 || minutes > 240
                || servings == null || servings < 1 || servings > 12
                || !list(ingredients, 1, 24, 200) || !list(steps, 1, 12, 700)
                || !list(pantryUsed, profile.mode.equals("discover") ? 0 : 1, 20, 80) || !pantry.containsAll(pantryUsed)
                || !list(additionalIngredients, 0, profile.mode.equals("discover") ? 24 : 4, 200)) {
            throw new IllegalArgumentException("Invalid recipe response");
        }
        StringBuilder method = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            if (i > 0) method.append("\n\n");
            method.append(i + 1).append(". ").append(steps.get(i));
        }
        RecipeCatalog.Recipe recipe = new RecipeCatalog.Recipe("ai_" + UUID.randomUUID(),
                title, meal, minutes, servings, description, String.join(",", pantryUsed),
                String.join("\n", ingredients), method.toString());
        recipe.goal = profile.goal;
        recipe.cuisine = cuisine == null ? "" : cuisine;
        recipe.whyThisDish = whyThisDish == null ? "" : whyThisDish;
        recipe.goalFit = goalFit == null ? "" : goalFit;
        recipe.discovery = profile.mode.equals("discover");
        if (profile.blocks(recipe)) throw new IllegalArgumentException("Excluded ingredient");
        recipe.aiGenerated = true;
        recipe.additionalIngredients = additionalIngredients;
        return recipe;
    }
    private static boolean text(String s, int max) { return s != null && !s.trim().isEmpty() && s.length() <= max; }
    private static boolean list(List<String> values, int min, int max, int length) {
        if (values == null || values.size() < min || values.size() > max) return false;
        for (String value : values) if (!text(value, length)) return false;
        return true;
    }
}
