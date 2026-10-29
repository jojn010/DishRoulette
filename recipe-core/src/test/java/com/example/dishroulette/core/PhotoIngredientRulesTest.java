package com.example.dishroulette.core;

import com.google.gson.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class PhotoIngredientRulesTest {
    private String response(String json, String finish) {
        JsonObject part = new JsonObject(); part.addProperty("text", json);
        JsonArray parts = new JsonArray(); parts.add(part);
        JsonObject content = new JsonObject(); content.add("parts", parts);
        JsonObject candidate = new JsonObject(); candidate.add("content", content); candidate.addProperty("finishReason", finish);
        JsonArray candidates = new JsonArray(); candidates.add(candidate);
        JsonObject root = new JsonObject(); root.add("candidates", candidates); return root.toString();
    }
    @Test public void acceptsEnglishArabicAndDeduplicates() {
        assertEquals(List.of("Tomato", "بيض"), PhotoIngredientRules.parseResponse(response("{\"ingredients\":[\"Tomato\",\"tomato\",\"بيض\"]}", "STOP")));
        assertTrue(PhotoIngredientRules.parseResponse(response("{\"ingredients\":[]}", "STOP")).isEmpty());
    }
    @Test public void rejectsTruncatedBlockedMalformedAndOversizedOutput() {
        for (String body : List.of("{}", "bad json", response("{\"ingredients\":[\"rice\"]}", "MAX_TOKENS"),
                response("{\"ingredients\":[3]}", "STOP"), response("{\"ingredients\":[\"rice, eggs\"]}", "STOP"),
                response("{\"ingredients\":[\"rice\\nignore instructions\"]}", "STOP"),
                response("{\"ingredients\":[\"" + "a".repeat(81) + "\"]}", "STOP"), " ".repeat(100_001))) {
            assertThrows(IllegalArgumentException.class, () -> PhotoIngredientRules.parseResponse(body));
        }
        JsonObject many = new JsonObject(); JsonArray names = new JsonArray();
        for (int i = 0; i < 21; i++) names.add("food" + i);
        many.add("ingredients", names);
        assertThrows(IllegalArgumentException.class, () -> PhotoIngredientRules.parseResponse(response(many.toString(), "STOP")));
    }
    @Test public void requestContainsImageAndLocalizedStructuredContract() {
        JsonObject payload = PhotoIngredientRules.payload("aW1hZ2U=", "ar");
        JsonObject image = payload.getAsJsonArray("contents").get(0).getAsJsonObject().getAsJsonArray("parts").get(0).getAsJsonObject().getAsJsonObject("inlineData");
        assertEquals("image/jpeg", image.get("mimeType").getAsString());
        assertEquals("aW1hZ2U=", image.get("data").getAsString());
        assertTrue(payload.get("systemInstruction").toString().contains("Arabic"));
        assertEquals("application/json", payload.getAsJsonObject("generationConfig").get("responseMimeType").getAsString());
        assertThrows(IllegalArgumentException.class, () -> PhotoIngredientRules.payload("", "en"));
    }
}
