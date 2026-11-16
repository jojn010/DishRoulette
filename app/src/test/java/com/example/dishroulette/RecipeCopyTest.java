package com.example.dishroulette;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Protect every persisted recipe field when the language-copy implementation changes.
 */
public class RecipeCopyTest {
    @Test
    public void languageCopyPreservesAllFactsAndOwnsItsMutableLists() {
        Gson gson = new Gson();
        RecipeCatalog.Recipe source = gson.fromJson(gson.toJson(RecipeCatalog.ALL.get(0)), RecipeCatalog.Recipe.class);
        source.language = "en";
        source.caloriesPerServing = 300;
        source.proteinGramsPerServing = 15.0;
        source.carbohydratesGramsPerServing = 40.0;
        source.fatGramsPerServing = 8.0;
        source.fiberGramsPerServing = 4.0;
        source.sugarGramsPerServing = 2.0;
        source.sodiumMilligramsPerServing = 200.0;
        source.goal = "Balanced";
        source.cuisine = "Italian";
        source.whyThisDish = "Your favorite flavors";
        source.goalFit = "Balanced cooking";
        source.discovery = true;
        source.shared = true;
        source.aiGenerated = true;
        source.additionalIngredients = new ArrayList<>(Arrays.asList("salt", "water"));
        JsonObject expected = gson.toJsonTree(source).getAsJsonObject();
        com.example.dishroulette.core.RecipeTranslation ar = source.translations.get("ar");
        expected.addProperty("title", ar.title);
        expected.addProperty("description", ar.description);
        expected.addProperty("ingredients", String.join("\n", ar.ingredients));
        expected.addProperty("method", ar.method());
        expected.addProperty("language", "ar");
        expected.addProperty("whyThisDish", "");
        expected.addProperty("goalFit", "");
        RecipeCatalog.Recipe copy = source.forLanguage("ar");
        assertEquals(expected, gson.toJsonTree(copy));
        copy.additionalIngredients.clear();
        copy.keys.clear();
        copy.translations.get("en").ingredients.clear();
        assertEquals(2, source.additionalIngredients.size());
        assertFalse(source.keys.isEmpty());
        assertFalse(source.translations.get("en").ingredients.isEmpty());
    }

    @Test
    public void preferenceDraftCannotMutateOriginalLists() {
        RecipePreferences original = new RecipePreferences();
        original.allergies.add("Sesame");
        original.customExclusions.add("garlic");
        RecipePreferences draft = original.copy();
        assertEquals(new Gson().toJsonTree(original), new Gson().toJsonTree(draft));
        draft.allergies.clear();
        draft.customExclusions.clear();
        draft.servings = 8;
        assertEquals(2, original.servings);
        assertEquals(Arrays.asList("Sesame"), original.allergies);
        assertEquals(Arrays.asList("garlic"), original.customExclusions);
    }
}
