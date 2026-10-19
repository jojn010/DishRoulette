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

/** Opt-in checks of the actual Android networking and JSON conversion against Gemini. */
@RunWith(AndroidJUnit4.class)
public class LiveRecipeConnectionTest {
    private void verify(SpinProfile profile, Set<String> pantry) throws Exception {
        // Ordinary test runs must not make paid API calls.
        Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("liveRecipe")));
        Response<RecipeSuggestion> response = RecipeApi.create().suggest(new RecipeApi.Request(
                pantry, "Any meal", "", profile, Collections.emptyList())).execute();
        String failure = "HTTP " + response.code();
        if (response.errorBody() != null) {
            try (okhttp3.ResponseBody body = response.errorBody()) { failure += " " + body.string(); }
        }
        assertTrue(failure, response.isSuccessful());
        assertNotNull(response.body());
        assertFalse(response.body().toRecipe(pantry, profile).title.isEmpty());
    }

    @Test public void pantryRecipe() throws Exception {
        verify(new SpinProfile(), new HashSet<>(Arrays.asList("eggs", "tomato", "bread")));
    }

    @Test public void cuisineDiscovery() throws Exception {
        SpinProfile profile = new SpinProfile();
        profile.mode = "discover";
        profile.cuisine = "Italian";
        profile.goal = "Bulking";
        verify(profile, Collections.emptySet());
    }
}
