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
    @Test public void generatedRecipeSurvivesPersistence() {
        RecipeCatalog.Recipe recipe = suggestion().toRecipe(RecipeCatalog.parseIngredients("eggs"));
        RecipeCatalog.Recipe restored = gson.fromJson(gson.toJson(recipe), RecipeCatalog.Recipe.class);
        assertTrue(restored.aiGenerated);
        assertEquals(recipe.id, restored.id);
        assertEquals("1. Cook until set.", restored.method);
        assertEquals(1, restored.matches(RecipeCatalog.parseIngredients("eggs")));
        assertTrue(restored.additionalIngredients.isEmpty());
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsMissingSteps() {
        RecipeSuggestion result = suggestion(); result.steps = null;
        result.toRecipe(RecipeCatalog.parseIngredients("eggs"));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsInventedPantryIngredient() {
        suggestion().toRecipe(RecipeCatalog.parseIngredients("rice"));
    }
    @Test public void snackDessertAndGoalSurviveConversionAndSaving() {
        for(String meal:new String[]{"Snack","Dessert"}){
            RecipeSuggestion result=suggestion();result.meal=meal;
            SpinProfile profile=new SpinProfile();profile.goal="Cutting";
            RecipeCatalog.Recipe recipe=result.toRecipe(RecipeCatalog.parseIngredients("eggs"),profile);
            RecipeCatalog.Recipe restored=gson.fromJson(gson.toJson(recipe),RecipeCatalog.Recipe.class);
            assertEquals(meal,restored.meal);assertEquals("Cutting",restored.goal);
        }
    }
}
