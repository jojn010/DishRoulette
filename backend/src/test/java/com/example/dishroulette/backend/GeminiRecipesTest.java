package com.example.dishroulette.backend;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.Test;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class GeminiRecipesTest {
    private static JsonObject json(String s) { return JsonParser.parseString(s).getAsJsonObject(); }
    private static JsonObject request() { return json("{\"ingredients\":[\"eggs\"],\"meal\":\"Breakfast\",\"avoidTitle\":\"\"}"); }
    private static JsonObject recipe() {
        return json("""
                {"possible":true,"title":"Scrambled eggs","meal":"Breakfast","description":"A quick breakfast.",
                 "cuisine":"French","whyThisDish":"Uses your eggs.","goalFit":"Balanced breakfast.",
                 "minutes":10,"servings":1,"ingredients":["2 eggs","1 tsp cooking oil"],
                 "steps":["Cook the eggs until set."],"pantryUsed":["eggs"],"additionalIngredients":["cooking oil"]}
                """);
    }
    @Test public void availablePantryDoesNotRequireEveryIngredient() throws Exception {
        JsonObject input = request();
        input.add("ingredients", json("{\"items\":[\"eggs\",\"rice\",\"banana\",\"milk\"]}").get("items"));
        JsonObject result = GeminiRecipes.validateRecipe(recipe(), input);
        assertEquals(json("{\"items\":[\"eggs\"]}").get("items"), result.get("pantryUsed"));
        // Using the whole pantry is also valid when it fits the selected dish.
        assertEquals(recipe().get("pantryUsed"),
                GeminiRecipes.validateRecipe(recipe(), request()).get("pantryUsed"));
    }
    @Test public void rejectsMissingInputsAndOversizeValues() throws Exception {
        for (String body : new String[]{"{}", "{\"ingredients\":[],\"meal\":\"Breakfast\"}",
                "{\"ingredients\":[\"eggs\"],\"meal\":\"anything\"}"}) {
            try { GeminiRecipes.validateRequest(json(body)); fail(); }
            catch (GeminiRecipes.ApiFailure e) { assertEquals(400, e.status); }
        }
        assertEquals("Breakfast", GeminiRecipes.validateRequest(request()).get("meal").getAsString());
    }
    @Test public void rejectsUnlistedIngredientsAndIncompleteRecipes() throws Exception {
        JsonObject recipe = recipe(); recipe.add("pantryUsed", JsonParser.parseString("[\"chicken\"]"));
        try { GeminiRecipes.validateRecipe(recipe, request()); fail(); }
        catch (GeminiRecipes.ApiFailure e) { assertEquals("INVALID_RECIPE", e.code); }
        recipe = recipe(); recipe.remove("steps");
        try { GeminiRecipes.validateRecipe(recipe, request()); fail(); }
        catch (GeminiRecipes.ApiFailure e) { assertEquals(502, e.status); }
    }
    @Test public void handlesNoDishAndMissingKey() throws Exception {
        JsonObject noRecipe = recipe(); noRecipe.addProperty("possible", false);
        try { GeminiRecipes.validateRecipe(noRecipe, request()); fail(); }
        catch (GeminiRecipes.ApiFailure e) { assertEquals("NO_RECIPE", e.code); }
        try { new GeminiRecipes("", "gemini-3.5-flash-lite").suggest(request()); fail(); }
        catch (GeminiRecipes.ApiFailure e) { assertEquals("KEY_NOT_CONFIGURED", e.code); }
    }
    @Test public void localHttpRoundTripUsesPrivateHeaderAndReturnsValidatedRecipe() throws Exception {
        HttpServer provider = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> auth = new AtomicReference<>();
        AtomicReference<JsonObject> payload = new AtomicReference<>();
        provider.createContext("/", exchange -> {
            auth.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            payload.set(json(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            JsonObject part = new JsonObject(); part.addProperty("text", recipe().toString());
            JsonArray parts = new JsonArray(); parts.add(part);
            JsonObject content = new JsonObject(); content.add("parts", parts);
            JsonObject candidate = new JsonObject(); candidate.addProperty("finishReason", "STOP"); candidate.add("content", content);
            JsonArray candidates = new JsonArray(); candidates.add(candidate);
            JsonObject result = new JsonObject(); result.add("candidates", candidates);
            byte[] data = result.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, data.length); exchange.getResponseBody().write(data); exchange.close();
        });
        provider.start();
        try (RecipeServer backend = new RecipeServer(0, new GeminiRecipes("test-secret",
                URI.create("http://127.0.0.1:" + provider.getAddress().getPort() + "/")))) {
            backend.start();
            HttpResponse<String> response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                    URI.create("http://127.0.0.1:" + backend.port() + "/recipes/suggest"))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(request().toString())).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertEquals("Scrambled eggs", json(response.body()).get("title").getAsString());
            assertEquals("test-secret", auth.get());
            assertFalse(response.body().contains("test-secret"));
            assertEquals("APPLICATION_JSON", payload.get().getAsJsonObject("generationConfig")
                    .getAsJsonObject("responseFormat").getAsJsonObject("text").get("mimeType").getAsString());
        } finally { provider.stop(0); }
    }
    @Test public void mapsQuotaAndBlockedResponses() throws Exception {
        HttpServer provider = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        provider.createContext("/", exchange -> { exchange.sendResponseHeaders(429, -1); exchange.close(); });
        provider.start();
        try {
            GeminiRecipes service = new GeminiRecipes("test-secret", URI.create("http://127.0.0.1:" + provider.getAddress().getPort()));
            try { service.suggest(request()); fail(); }
            catch (GeminiRecipes.ApiFailure e) { assertEquals(429, e.status); assertEquals("RATE_LIMITED", e.code); }
        } finally { provider.stop(0); }
    }
}
