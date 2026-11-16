package com.example.dishroulette;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class RecipeCatalogTest {
    @Test
    public void normalizesAliasesAndDeduplicates() {
        assertEquals(new HashSet<>(java.util.Arrays.asList("eggs", "tomato", "rice")),
            RecipeCatalog.parseIngredients(" Egg, eggs; TOMATOES\ncooked rice, "));
    }

    @Test
    public void emptyAndUnknownPantryProduceNoMatches() {
        assertTrue(RecipeCatalog.matching(RecipeCatalog.parseIngredients(null), "Any meal").isEmpty());
        assertTrue(RecipeCatalog.matching(RecipeCatalog.parseIngredients("dragonfruit"), "Any meal").isEmpty());
    }

    @Test
    public void respectsMealFilterAndReportsMissingIngredients() {
        Set<String> pantry = RecipeCatalog.parseIngredients("eggs, bread");
        List<RecipeCatalog.Recipe> recipes = RecipeCatalog.matching(pantry, "Breakfast");
        assertEquals(1, recipes.size());
        assertEquals("eggs_toast", recipes.get(0).id);
        assertEquals(2, recipes.get(0).matches(pantry));
        assertTrue(recipes.get(0).missing(pantry).isEmpty());
        assertTrue(RecipeCatalog.matching(pantry, "Lunch").stream()
            .allMatch(recipe -> recipe.meal.equals("Lunch")));
        RecipeCatalog.Recipe rice = RecipeCatalog.find("egg_rice");
        assertNotNull(rice);
        assertEquals(java.util.Arrays.asList("rice", "peas"), rice.missing(pantry));
    }

    @Test
    public void catalogIdsAreUniqueAndRecipesAreComplete() {
        Set<String> ids = new HashSet<>();
        for (RecipeCatalog.Recipe recipe : RecipeCatalog.ALL) {
            assertTrue(ids.add(recipe.id));
            assertFalse(recipe.keys.isEmpty());
            assertFalse(recipe.ingredients.isEmpty());
            assertFalse(recipe.method.isEmpty());
            assertTrue(recipe.minutes > 0);
            assertTrue(recipe.servings > 0);
        }
        assertNull(RecipeCatalog.find("missing"));
    }
}
