package com.example.dishroulette;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import com.example.dishroulette.core.IngredientExclusions;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

/** Typed, validated response from our backend; never accepts executable model output. */
public final class RecipeSuggestion {
    @SerializedName("translations") public java.util.Map<String,com.example.dishroulette.core.RecipeTranslation> translations;
    @SerializedName("title") public String title;
    @SerializedName("meal") public String meal;
    @SerializedName("description") public String description;
    @SerializedName("minutes") public Integer minutes;
    @SerializedName("servings") public Integer servings;
    @SerializedName("caloriesPerServing") public Integer caloriesPerServing;
    @SerializedName("proteinGramsPerServing") public Double proteinGramsPerServing;
    @SerializedName("carbohydratesGramsPerServing") public Double carbohydratesGramsPerServing;
    @SerializedName("fatGramsPerServing") public Double fatGramsPerServing;
    @SerializedName("fiberGramsPerServing") public Double fiberGramsPerServing;
    @SerializedName("sugarGramsPerServing") public Double sugarGramsPerServing;
    @SerializedName("sodiumMilligramsPerServing") public Double sodiumMilligramsPerServing;

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
        if ((caloriesPerServing != null || proteinGramsPerServing != null)
                && !com.example.dishroulette.core.NutritionEstimate.valid(caloriesPerServing, proteinGramsPerServing)) {
            throw new IllegalArgumentException("Invalid nutrition estimate");
        }
        if (carbohydratesGramsPerServing != null && (!com.example.dishroulette.core.NutritionEstimate.validNutrient(carbohydratesGramsPerServing, 1000) || caloriesPerServing == null)) throw new IllegalArgumentException("Invalid nutrition estimate");
        if (fatGramsPerServing != null && (!com.example.dishroulette.core.NutritionEstimate.validNutrient(fatGramsPerServing, 500) || caloriesPerServing == null)) throw new IllegalArgumentException("Invalid nutrition estimate");
        if (fiberGramsPerServing != null && (!com.example.dishroulette.core.NutritionEstimate.validNutrient(fiberGramsPerServing, 200) || caloriesPerServing == null)) throw new IllegalArgumentException("Invalid nutrition estimate");
        if (sugarGramsPerServing != null && (!com.example.dishroulette.core.NutritionEstimate.validNutrient(sugarGramsPerServing, 1000) || caloriesPerServing == null)) throw new IllegalArgumentException("Invalid nutrition estimate");
        if (sodiumMilligramsPerServing != null && (!com.example.dishroulette.core.NutritionEstimate.validNutrient(sodiumMilligramsPerServing, 20000) || caloriesPerServing == null)) throw new IllegalArgumentException("Invalid nutrition estimate");
        StringBuilder method = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            if (i > 0) method.append("\n\n");
            method.append(i + 1).append(". ").append(steps.get(i));
        }
        RecipeCatalog.Recipe recipe = new RecipeCatalog.Recipe("ai_" + UUID.randomUUID(),
                title, meal, minutes, servings, description, String.join(",", pantryUsed),
                String.join("\n", ingredients), method.toString());
        if(translations!=null)recipe.translations=com.example.dishroulette.core.RecipeTranslation.validate(translations);
        recipe.caloriesPerServing = caloriesPerServing;
        recipe.proteinGramsPerServing = proteinGramsPerServing;
        recipe.carbohydratesGramsPerServing = carbohydratesGramsPerServing;
        recipe.fatGramsPerServing = fatGramsPerServing;
        recipe.fiberGramsPerServing = fiberGramsPerServing;
        recipe.sugarGramsPerServing = sugarGramsPerServing;
        recipe.sodiumMilligramsPerServing = sodiumMilligramsPerServing;

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
