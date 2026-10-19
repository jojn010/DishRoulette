package com.example.dishroulette;

import com.google.gson.Gson;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class PersonalizationTest {
    @Test public void profileAndNewRecipeFieldsSurvivePersistence() {
        Gson gson = new Gson(); SpinProfile profile = new SpinProfile();
        profile.mode = "discover"; profile.cuisine = "Palestinian"; profile.goal = "Bulking";
        profile.allergies.add("Milk"); profile.customExclusions.add("kiwi");
        SpinProfile copy = gson.fromJson(gson.toJson(profile), SpinProfile.class);
        assertEquals(profile.cuisine, copy.cuisine); assertEquals(profile.allergies, copy.allergies);
        RecipeSuggestion suggestion = gson.fromJson("""
            {"title":"Lemon rice","meal":"Lunch","description":"A rice dish.","minutes":20,"servings":2,
             "ingredients":["1 cup rice","1 lemon"],"steps":["Cook rice, add lemon."],"pantryUsed":[],
             "additionalIngredients":["rice","lemon"],"cuisine":"Palestinian","whyThisDish":"A citrus flavor.","goalFit":"Heartier serving."}
            """, RecipeSuggestion.class);
        RecipeCatalog.Recipe result = suggestion.toRecipe(Collections.emptySet(), copy);
        assertTrue(result.discovery); assertTrue(result.keys.isEmpty()); assertEquals("Palestinian",result.cuisine);
        RecipeApi.Favorite favorite = new RecipeApi.Favorite(result);
        assertEquals(Arrays.asList("rice", "lemon"), favorite.mainIngredients);
    }
    @Test public void previouslySavedDishIsBlockedByNewExclusion() {
        SpinProfile profile = new SpinProfile(); profile.allergies.add("Eggs");
        assertTrue(profile.blocks(RecipeCatalog.find("eggs_toast")));
        profile.allergies.clear(); assertFalse(profile.blocks(RecipeCatalog.find("eggs_toast")));
    }
}
