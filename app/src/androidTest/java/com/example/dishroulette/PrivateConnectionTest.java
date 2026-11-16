package com.example.dishroulette;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.JsonParser;

import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

public class PrivateConnectionTest {
    @Test
    public void privateBootstrapConnectsWithoutKeyEntry() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // This check applies only when explicitly running against an installed private owner APK.
        assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("privateConnection")));
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            GeminiCredentials.Value saved = new GeminiCredentials(context).read();
            assertTrue("Automatic connection import failed", saved != null);
            try (InputStream in = context.getAssets().open("private-gemini-setup.json")) {
                String expected = JsonParser.parseReader(new java.io.InputStreamReader(in,
                    java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().get("key").getAsString();
                assertTrue("Imported connection differs from bundled connection", expected.equals(saved.key));
            }
            CountDownLatch done = new CountDownLatch(1);
            AtomicBoolean success = new AtomicBoolean();
            java.util.concurrent.atomic.AtomicReference<String> diagnostic =
                new java.util.concurrent.atomic.AtomicReference<>("No response");
            RecipeApi.Request request = new RecipeApi.Request(Collections.singleton("eggs"),
                "Any meal", "", new RecipePreferences(), Collections.emptyList(), "en");
            RecipeApi.create(context).suggest(request).enqueue(new Callback<RecipeSuggestion>() {
                public void onResponse(Call<RecipeSuggestion> call, Response<RecipeSuggestion> response) {
                    success.set(response.isSuccessful() && response.body() != null
                        && com.example.dishroulette.core.NutritionEstimate.valid(
                        response.body().caloriesPerServing, response.body().proteinGramsPerServing)
                        && com.example.dishroulette.core.NutritionEstimate.validNutrient(
                        response.body().carbohydratesGramsPerServing, 1000)
                        && com.example.dishroulette.core.NutritionEstimate.validNutrient(
                        response.body().fatGramsPerServing, 500)
                        && com.example.dishroulette.core.NutritionEstimate.validNutrient(
                        response.body().fiberGramsPerServing, 200)
                        && com.example.dishroulette.core.NutritionEstimate.validNutrient(
                        response.body().sugarGramsPerServing, 1000)
                        && com.example.dishroulette.core.NutritionEstimate.validNutrient(
                        response.body().sodiumMilligramsPerServing, 20000));
                    diagnostic.set(response.isSuccessful() ? "Missing or invalid nutrient" : "HTTP " + response.code());
                    if (!response.isSuccessful()) {
                        try {
                            String code =
                                JsonParser.parseString(response.errorBody().string()).getAsJsonObject().get("code")
                                    .getAsString();
                            if (code.matches("[A-Z_]+"))
                                diagnostic.set(diagnostic.get() + " " + code);
                        } catch (Exception ignored) {
                        }
                    }
                    done.countDown();
                }

                public void onFailure(Call<RecipeSuggestion> call, Throwable failure) {
                    done.countDown();
                }
            });
            assertTrue("Generation timed out", done.await(75, TimeUnit.SECONDS));
            assertTrue("Direct on-device recipe generation failed: " + diagnostic.get(), success.get());
        }
    }
}
