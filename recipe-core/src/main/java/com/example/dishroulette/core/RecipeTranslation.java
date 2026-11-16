package com.example.dishroulette.core;

import com.google.gson.*;

import java.util.*;

/**
 * Bounded parallel recipe text; facts such as servings and nutrition live outside translations.
 */
public final class RecipeTranslation {
    public String title, description;
    public List<String> ingredients, steps;

    public static JsonObject schema() {
        JsonObject properties = new JsonObject();
        for (String key : List.of("title", "description")) {
            JsonObject text = new JsonObject();
            text.addProperty("type", "string");
            properties.add(key, text);
        }
        for (String key : List.of("ingredients", "steps")) {
            JsonObject list = new JsonObject();
            list.addProperty("type", "array");
            JsonObject item = new JsonObject();
            item.addProperty("type", "string");
            list.add("items", item);
            properties.add(key, list);
        }
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", properties);
        schema.add("required", new Gson().toJsonTree(properties.keySet()));
        schema.addProperty("additionalProperties", false);
        return schema;
    }

    public static JsonObject pairSchema() {
        JsonObject properties = new JsonObject();
        properties.add("en", schema());
        properties.add("ar", schema());
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", properties);
        schema.add("required", new Gson().toJsonTree(List.of("en", "ar")));
        schema.addProperty("additionalProperties", false);
        return schema;
    }

    public static Map<String, RecipeTranslation> readPair(JsonObject object) {
        if (object == null || object.size() != 2 || !object.has("en") || !object.has("ar"))
            throw new IllegalArgumentException("Missing recipe language");
        Map<String, RecipeTranslation> result = new LinkedHashMap<>();
        for (String lang : List.of("en", "ar")) {
            JsonObject value = object.getAsJsonObject(lang);
            RecipeTranslation t = new RecipeTranslation();
            t.title = text(value.get("title"), 120);
            t.description = text(value.get("description"), 600);
            t.ingredients = list(value.getAsJsonArray("ingredients"), 24, 200);
            t.steps = list(value.getAsJsonArray("steps"), 12, 700);
            result.put(lang, t);
        }
        validatePairShape(result);
        return result;
    }

    /**
     * Validate typed data directly; rendering must not serialize a recipe to JSON.
     */
    public static Map<String, RecipeTranslation> validate(Map<String, RecipeTranslation> pair) {
        if (pair == null || pair.size() != 2 || !pair.containsKey("en") || !pair.containsKey("ar"))
            throw new IllegalArgumentException("Missing recipe language");
        Map<String, RecipeTranslation> result = new LinkedHashMap<>();
        for (String language : List.of("en", "ar")) {
            RecipeTranslation source = pair.get(language);
            if (source == null)
                throw new IllegalArgumentException("Missing recipe language");
            RecipeTranslation copy = new RecipeTranslation();
            copy.title = checkedText(source.title, 120);
            copy.description = checkedText(source.description, 600);
            copy.ingredients = checkedList(source.ingredients, 24, 200);
            copy.steps = checkedList(source.steps, 12, 700);
            result.put(language, copy);
        }
        validatePairShape(result);
        return result;
    }

    private static void validatePairShape(Map<String, RecipeTranslation> result) {
        if (result.get("ar").title.codePoints()
            .noneMatch(c -> Character.isLetter(c) && Character.UnicodeScript.of(c) == Character.UnicodeScript.ARABIC)
            || result.get("en").title.codePoints()
            .noneMatch(c -> Character.isLetter(c) && Character.UnicodeScript.of(c) == Character.UnicodeScript.LATIN))
            throw new IllegalArgumentException("Invalid recipe language");
        if (result.get("en").ingredients.size() != result.get("ar").ingredients.size()
            || result.get("en").steps.size() != result.get("ar").steps.size())
            throw new IllegalArgumentException("Mismatched recipe translations");
    }

    private static String checkedText(String value, int max) {
        if (value == null)
            throw new IllegalArgumentException("Invalid recipe text");
        String clean = value.trim();
        if (clean.isEmpty() || clean.length() > max || clean.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Invalid recipe text");
        return clean;
    }

    private static List<String> checkedList(List<String> values, int max, int length) {
        if (values == null || values.isEmpty() || values.size() > max)
            throw new IllegalArgumentException("Invalid recipe text");
        List<String> result = new ArrayList<>(values.size());
        for (String value : values)
            result.add(checkedText(value, length));
        return result;
    }

    private static String text(JsonElement e, int max) {
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("Invalid recipe text");
        return checkedText(e.getAsString(), max);
    }

    private static List<String> list(JsonArray a, int max, int length) {
        if (a == null || a.isEmpty() || a.size() > max)
            throw new IllegalArgumentException("Invalid recipe text");
        List<String> result = new ArrayList<>();
        for (JsonElement e : a)
            result.add(text(e, length));
        return result;
    }

    public String method() {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            if (i > 0)
                s.append("\n\n");
            s.append(i + 1).append(". ").append(steps.get(i));
        }
        return s.toString();
    }
}
