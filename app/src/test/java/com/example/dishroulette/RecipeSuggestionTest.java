package com.example.dishroulette;

import com.google.gson.Gson;

import org.junit.Test;

import static org.junit.Assert.*;

public class RecipeSuggestionTest {
    private final Gson gson = new Gson();

    private RecipeSuggestion suggestion() {
        return gson.fromJson("{\"title\":\"Scrambled eggs\",\"meal\":\"Breakfast\",\"description\":\"Simple eggs.\","
            + "\"minutes\":10,\"servings\":1,\"ingredients\":[\"2 eggs\"],\"steps\":[\"Cook until set.\"],"
            + "\"pantryUsed\":[\"eggs\"],\"additionalIngredients\":[]}", RecipeSuggestion.class);
    }

    @Test
    public void generatedRecipeSurvivesPersistence() {
        RecipeCatalog.Recipe recipe = suggestion().toRecipe(RecipeCatalog.parseIngredients("eggs"));
        RecipeCatalog.Recipe restored = gson.fromJson(gson.toJson(recipe), RecipeCatalog.Recipe.class);
        assertTrue(restored.aiGenerated);
        assertEquals(recipe.id, restored.id);
        assertEquals("1. Cook until set.", restored.method);
        assertEquals(1, restored.matches(RecipeCatalog.parseIngredients("eggs")));
        assertTrue(restored.additionalIngredients.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingSteps() {
        RecipeSuggestion result = suggestion();
        result.steps = null;
        result.toRecipe(RecipeCatalog.parseIngredients("eggs"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInventedPantryIngredient() {
        suggestion().toRecipe(RecipeCatalog.parseIngredients("rice"));
    }

    @Test
    public void snackDessertAndGoalSurviveConversionAndSaving() {
        for (String meal : new String[]{"Snack", "Dessert"}) {
            RecipeSuggestion result = suggestion();
            result.meal = meal;
            RecipePreferences profile = new RecipePreferences();
            profile.goal = "Cutting";
            RecipeCatalog.Recipe recipe = result.toRecipe(RecipeCatalog.parseIngredients("eggs"), profile);
            RecipeCatalog.Recipe restored = gson.fromJson(gson.toJson(recipe), RecipeCatalog.Recipe.class);
            assertEquals(meal, restored.meal);
            assertEquals("Cutting", restored.goal);
        }
    }

    @Test
    public void nutritionSurvivesConversionAndSavingWithoutMultiplyingByServings() {
        RecipeSuggestion result = suggestion();
        result.servings = 2;
        result.caloriesPerServing = 210;
        result.proteinGramsPerServing = 12.5;
        result.carbohydratesGramsPerServing = 20.0;
        result.fatGramsPerServing = 9.0;
        result.fiberGramsPerServing = 2.0;
        result.sugarGramsPerServing = 1.5;
        result.sodiumMilligramsPerServing = 300.0;
        RecipeCatalog.Recipe recipe = result.toRecipe(RecipeCatalog.parseIngredients("eggs"));
        RecipeCatalog.Recipe restored = gson.fromJson(gson.toJson(recipe), RecipeCatalog.Recipe.class);
        assertEquals(Integer.valueOf(210), restored.caloriesPerServing);
        assertEquals(12.5, restored.proteinGramsPerServing, 0.001);
        assertEquals(20.0, restored.carbohydratesGramsPerServing, 0.001);
        assertEquals(9.0, restored.fatGramsPerServing, 0.001);
        assertEquals(2.0, restored.fiberGramsPerServing, 0.001);
        assertEquals(1.5, restored.sugarGramsPerServing, 0.001);
        assertEquals(300.0, restored.sodiumMilligramsPerServing, 0.001);
        assertNull(suggestion().toRecipe(RecipeCatalog.parseIngredients("eggs")).caloriesPerServing);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsPartialNutrition() {
        RecipeSuggestion result = suggestion();
        result.caloriesPerServing = 210;
        result.toRecipe(RecipeCatalog.parseIngredients("eggs"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidAdditionalNutrient() {
        RecipeSuggestion result = suggestion();
        result.caloriesPerServing = 210;
        result.proteinGramsPerServing = 12.5;
        result.sodiumMilligramsPerServing = Double.NaN;
        result.toRecipe(RecipeCatalog.parseIngredients("eggs"));
    }
}
