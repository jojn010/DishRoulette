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
}
