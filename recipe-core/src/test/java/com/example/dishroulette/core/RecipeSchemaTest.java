package com.example.dishroulette.core;

import com.google.gson.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RecipeSchemaTest {
    @Test public void generatedCuisineCannotBeBlankAndSelectedCuisineIsEnforced() throws Exception {
        for (String cuisine : new String[]{"Any cuisine", "Italian"}) {
            JsonObject request = JsonParser.parseString("{\"ingredients\":[\"eggs\"],\"meal\":\"Any meal\"}").getAsJsonObject();
            request.addProperty("cuisine", cuisine);
            JsonArray values = RecipeRules.payload(request).getAsJsonObject("generationConfig")
                    .getAsJsonObject("responseFormat").getAsJsonObject("text").getAsJsonObject("schema")
                    .getAsJsonObject("properties").getAsJsonObject("cuisine").getAsJsonArray("enum");
            for (JsonElement value : values) {
                assertFalse(value.getAsString().isEmpty());
                assertFalse(value.getAsString().equals("Any cuisine"));
            }
            if (cuisine.equals("Italian")) {
                assertEquals(1, values.size());
                assertEquals("Italian", values.get(0).getAsString());
            }
        }
    }
}
