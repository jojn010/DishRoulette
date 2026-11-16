package com.example.dishroulette.backend;

import com.google.gson.*;
import com.example.dishroulette.core.RecipeRules;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Stateless Gemini adapter. Credentials and raw upstream errors never reach the Android app.
 */
final class GeminiRecipes {
    private static final Gson GSON = new Gson();
    private final String apiKey;
    private final URI endpoint;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    GeminiRecipes(String apiKey, String model) {
        this(apiKey, URI.create("https://generativelanguage.googleapis.com/v1beta/models/"
            + checkedModel(model) + ":generateContent"));
    }

    // Package-private endpoint injection permits tests against a local mock, never a runtime override.
    GeminiRecipes(String apiKey, URI endpoint) {
        this.apiKey = apiKey;
        this.endpoint = endpoint;
    }

    private static String checkedModel(String model) {
        if (model == null || !model.matches("gemini-[a-zA-Z0-9.-]+"))
            throw new IllegalArgumentException("Invalid model name");
        return model;
    }

    static final class ApiFailure extends Exception {
        final int status;
        final String code;

        ApiFailure(int status, String code) {
            super(code);
            this.status = status;
            this.code = code;
        }
    }

    static JsonObject validateRequest(JsonObject input) throws ApiFailure {
        try {
            return RecipeRules.validateRequest(input);
        } catch (RecipeRules.ApiFailure e) {
            throw new ApiFailure(e.status, e.code);
        }
    }

    static JsonObject validateRecipe(JsonObject result, JsonObject request) throws ApiFailure {
        try {
            return RecipeRules.validateRecipe(result, request);
        } catch (RecipeRules.ApiFailure e) {
            throw new ApiFailure(e.status, e.code);
        }
    }

    JsonObject suggest(JsonObject input) throws ApiFailure {
        JsonObject request = validateRequest(input);
        if (apiKey == null || apiKey.isBlank())
            throw new ApiFailure(503, "KEY_NOT_CONFIGURED");
        try {
            HttpRequest http = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(50))
                .header("x-goog-api-key", apiKey).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(RecipeRules.payload(request)))).build();
            HttpResponse<String> response =
                client.send(http, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            if (status == 429)
                throw new ApiFailure(429, "RATE_LIMITED");
            if (status == 400 || status == 401 || status == 403 || status == 404)
                throw new ApiFailure(503, "PROVIDER_CONFIGURATION");
            if (status != 200)
                throw new ApiFailure(502, "PROVIDER_UNAVAILABLE");
            return RecipeRules.parseResponse(response.body(), request);
        } catch (RecipeRules.ApiFailure e) {
            throw new ApiFailure(e.status, e.code);
        } catch (HttpTimeoutException e) {
            throw new ApiFailure(504, "TIMEOUT");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiFailure(503, "PROVIDER_UNAVAILABLE");
        } catch (IOException e) {
            throw new ApiFailure(502, "PROVIDER_UNAVAILABLE");
        }
    }
}
