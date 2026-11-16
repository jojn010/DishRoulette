package com.example.dishroulette;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import retrofit2.Response;

import static org.junit.Assert.*;

/**
 * Opt-in checks of the actual Android networking and JSON conversion against Gemini.
 */
@RunWith(AndroidJUnit4.class)
public class LiveRecipeConnectionTest {
    private void verify(RecipePreferences profile, Set<String> pantry) throws Exception {
        // Ordinary test runs must not make paid API calls.
        Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("liveRecipe")));
        java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Response<RecipeSuggestion>> result =
            new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<Throwable> error =
            new java.util.concurrent.atomic.AtomicReference<>();
        RecipeApi.create(InstrumentationRegistry.getInstrumentation().getTargetContext()).suggest(new RecipeApi.Request(
                pantry, "Any meal", "", profile, Collections.emptyList(),
                InstrumentationRegistry.getArguments().getString("recipeLanguage", "en")))
            .enqueue(new retrofit2.Callback<RecipeSuggestion>() {
                public void onResponse(retrofit2.Call<RecipeSuggestion> call, Response<RecipeSuggestion> response) {
                    result.set(response);
                    done.countDown();
                }

                public void onFailure(retrofit2.Call<RecipeSuggestion> call, Throwable failure) {
                    error.set(failure);
                    done.countDown();
                }
            });
        assertTrue(done.await(70, java.util.concurrent.TimeUnit.SECONDS));
        if (error.get() != null)
            throw new AssertionError(error.get());
        Response<RecipeSuggestion> response = result.get();
        assertNotNull(response);
        String failure = "HTTP " + response.code();
        if (response.errorBody() != null) {
            try (okhttp3.ResponseBody body = response.errorBody()) {
                failure += " " + body.string();
            }
        }
        assertTrue(failure, response.isSuccessful());
        assertNotNull(response.body());
        assertEquals(profile.servings, response.body().servings.intValue());
        if (profile.servings == 6) {
            java.io.File file = new java.io.File(
                InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),
                "six-person-validation.json");
            try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                new com.google.gson.Gson().toJson(response.body(), writer);
            }
        }
        RecipeCatalog.Recipe bilingual = response.body().toRecipe(pantry, profile);
        assertFalse(bilingual.title.isEmpty());
        assertTrue("Generated recipe must carry both languages", bilingual.bilingual());
        assertEquals("ar", bilingual.forLanguage("ar").language);
        assertEquals("en", bilingual.forLanguage("en").language);
    }

    @Test
    public void legacyTranslation() throws Exception {
        Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("liveRecipe")));
        java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<RecipeCatalog.Recipe> result =
            new java.util.concurrent.atomic.AtomicReference<>();
        RecipeCatalog.Recipe original =
            new RecipeCatalog.Recipe("legacy-translation-check", "Tomato rice", "Dinner", 20, 2, "Warm tomato rice.",
                "rice,tomato", "1 cup rice\n2 tomatoes", "1. Simmer for 15 minutes.\n\n2. Serve warm.");
        original.language = "en";
        new RecipeTranslationClient().translate(InstrumentationRegistry.getInstrumentation().getTargetContext(),
            original, new RecipeTranslationClient.Result() {
                public void ready(RecipeCatalog.Recipe recipe) {
                    result.set(recipe);
                    done.countDown();
                }

                public void failed() {
                    done.countDown();
                }
            });
        assertTrue(done.await(70, java.util.concurrent.TimeUnit.SECONDS));
        assertNotNull("Legacy translation failed", result.get());
        RecipeCatalog.Recipe translated = result.get();
        assertTrue(translated.bilingual());
        assertEquals(original.id, translated.id);
        assertEquals(original.ingredients, translated.forLanguage("en").ingredients);
        assertEquals(original.method, translated.forLanguage("en").method);
        assertEquals(original.servings, translated.servings);
        assertEquals(original.minutes, translated.minutes);
    }

    @Test
    public void sixPeople() throws Exception {
        RecipePreferences profile = new RecipePreferences();
        profile.servings = 6;
        verify(profile, new HashSet<>(Arrays.asList("rice", "tomato", "eggs")));
    }

    @Test
    public void pantryRecipe() throws Exception {
        verify(new RecipePreferences(), new HashSet<>(Arrays.asList("eggs", "tomato", "bread")));
    }

    @Test
    public void cuisineDiscovery() throws Exception {
        RecipePreferences profile = new RecipePreferences();
        profile.mode = "discover";
        profile.cuisine = "Italian";
        profile.goal = "Bulking";
        verify(profile, Collections.emptySet());
    }
}
