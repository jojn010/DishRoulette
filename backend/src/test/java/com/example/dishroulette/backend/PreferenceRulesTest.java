package com.example.dishroulette.backend;

import com.google.gson.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class PreferenceRulesTest {
    private JsonObject json(String value) { return JsonParser.parseString(value).getAsJsonObject(); }
    private JsonObject request() { return json("""
        {"ingredients":[],"mode":"discover","meal":"Lunch","goal":"Cutting","cuisine":"Palestinian",
         "allergies":["Sesame"],"customExclusions":["kiwi"],
         "likedDishes":[{"title":"Tahini bowl","mainIngredients":["tahini"]},
                        {"title":"Lemon chicken","mainIngredients":["chicken","lemon"]}]}
        """); }
    private JsonObject recipe() { return json("""
        {"possible":true,"title":"Lemon chicken rice","meal":"Lunch","cuisine":"Palestinian",
         "description":"Chicken and rice.","minutes":30,"servings":2,
         "ingredients":["200 g chicken","1 cup rice","1 lemon"],
         "steps":["Cook the chicken thoroughly and serve with cooked rice."],
         "pantryUsed":[],"additionalIngredients":["chicken","rice","lemon"],
         "whyThisDish":"Uses lemon and chicken like a liked dish.","goalFit":"A balanced lighter preparation."}
        """); }
    @Test public void selectedPeopleCountSurvivesValidationAndRejectsWrongYield() throws Exception {
        for(int count:new int[]{1,6,12}) {
            JsonObject input=request();input.addProperty("servings",count);
            JsonObject clean=GeminiRecipes.validateRequest(input);assertEquals(count,clean.get("servings").getAsInt());
            JsonObject output=recipe();output.addProperty("servings",count);
            assertEquals(count,GeminiRecipes.validateRecipe(output,clean).get("servings").getAsInt());
            output.addProperty("servings",count==1?2:1);
            try{GeminiRecipes.validateRecipe(output,clean);fail("Wrong yield accepted");}
            catch(GeminiRecipes.ApiFailure e){assertEquals("INVALID_RECIPE",e.code);}
        }
        for(String value:new String[]{"0","13","2.5","null","true","\"6\""}) {
            JsonObject input=request();input.add("servings",JsonParser.parseString(value));
            try{GeminiRecipes.validateRequest(input);fail("Invalid count: "+value);}
            catch(GeminiRecipes.ApiFailure e){assertEquals("INVALID_FILTERS",e.code);}
        }
    }
    @Test public void discoveryNeedsNoPantryAndRemovesConflictingLikes() throws Exception {
        JsonObject request = GeminiRecipes.validateRequest(request());
        assertEquals("Cutting", request.get("goal").getAsString());
        assertEquals("Palestinian", request.get("cuisine").getAsString());
        assertEquals(1, request.getAsJsonArray("likedDishes").size());
        assertEquals("Lemon chicken", request.getAsJsonArray("likedDishes").get(0).getAsJsonObject().get("title").getAsString());
        assertEquals("Lemon chicken rice", GeminiRecipes.validateRecipe(recipe(), request).get("title").getAsString());
    }
    @Test public void rejectsAllergenInGarnishAndCustomExclusion() throws Exception {
        for (String step : new String[]{"Finish with tahini.", "Serve with sliced kiwis."}) {
            JsonObject recipe = recipe(); JsonArray steps = new JsonArray(); steps.add(step); recipe.add("steps", steps);
            try { GeminiRecipes.validateRecipe(recipe, request()); fail(); }
            catch (GeminiRecipes.ApiFailure e) { assertEquals("ALLERGY_REJECTED", e.code); }
        }
    }
    @Test public void refusesCuisineMismatchAndUnknownGoals() throws Exception {
        JsonObject recipe = recipe(); recipe.addProperty("cuisine", "Italian");
        try { GeminiRecipes.validateRecipe(recipe, request()); fail(); }
        catch (GeminiRecipes.ApiFailure e) { assertEquals("INVALID_RECIPE", e.code); }
        JsonObject request = request(); request.addProperty("goal", "Starvation");
        try { GeminiRecipes.validateRequest(request); fail(); }
        catch (GeminiRecipes.ApiFailure e) { assertEquals("INVALID_FILTERS", e.code); }
    }
    @Test public void checksPantryBeforeSendingToGoogle() throws Exception {
        JsonObject request = request(); request.addProperty("mode", "pantry");
        request.add("ingredients", JsonParser.parseString("[\"tahini\",\"rice\"]"));
        try { GeminiRecipes.validateRequest(request); fail(); }
        catch (GeminiRecipes.ApiFailure e) { assertEquals("ALLERGY_CONFLICT", e.code); }
        request.add("ingredients", JsonParser.parseString("[\"sauce\",\"rice\"]"));
        try { GeminiRecipes.validateRequest(request); fail(); }
        catch (GeminiRecipes.ApiFailure e) { assertEquals("UNCERTAIN_INGREDIENTS", e.code); }
    }
    @Test public void snackAndDessertMatchTheRequestedMealEndToEnd() throws Exception {
        for (String meal : new String[]{"Snack", "Dessert"}) {
            JsonObject request=request();request.addProperty("meal",meal);
            JsonObject response=recipe();response.addProperty("meal",meal);
            assertEquals(meal,GeminiRecipes.validateRecipe(response,request).get("meal").getAsString());
            response.addProperty("meal","Dinner");
            try { GeminiRecipes.validateRecipe(response,request);fail(); }
            catch(GeminiRecipes.ApiFailure e){assertEquals("INVALID_RECIPE",e.code);}
        }
    }
    @Test public void languageDefaultsAreCompatibleAndOnlySupportedLanguagesPass() throws Exception {
        JsonObject input=request();assertEquals("en",GeminiRecipes.validateRequest(input).get("language").getAsString());
        input.addProperty("language","ar");assertEquals("ar",GeminiRecipes.validateRequest(input).get("language").getAsString());
        input.addProperty("language","zz");try{GeminiRecipes.validateRequest(input);fail();}catch(GeminiRecipes.ApiFailure e){assertEquals("INVALID_FILTERS",e.code);}
    }
    @Test public void arabicGeneratedRecipesCannotEvadeEnglishAllergyOrCustomFilters() throws Exception {
        JsonObject input=request();input.addProperty("language","ar");
        JsonObject output=recipe();output.addProperty("title","أرز بالدجاج");
        output.add("ingredients",JsonParser.parseString("[\"١ كوب أرز\",\"١ ملعقة طحينة\"]"));
        try{GeminiRecipes.validateRecipe(output,input);fail();}catch(GeminiRecipes.ApiFailure e){assertEquals("ALLERGY_REJECTED",e.code);}
        output.add("ingredients",JsonParser.parseString("[\"١ كوب أرز\",\"كيوي\"]"));
        try{GeminiRecipes.validateRecipe(output,input);fail();}catch(GeminiRecipes.ApiFailure e){assertEquals("ALLERGY_REJECTED",e.code);}
    }
    @Test public void rejectsUnknownCuisineAndRepeatedTitle() throws Exception {
        JsonObject input=request();input.addProperty("cuisine","Any cuisine");
        JsonObject output=recipe();output.addProperty("cuisine","Invented cuisine");
        try { GeminiRecipes.validateRecipe(output,input);fail("Unknown cuisine accepted"); }
        catch(GeminiRecipes.ApiFailure e){assertEquals("INVALID_RECIPE",e.code);}
        output=recipe();input.addProperty("avoidTitle","  LEMON CHICKEN RICE  ");
        try { GeminiRecipes.validateRecipe(output,input);fail("Repeated title accepted"); }
        catch(GeminiRecipes.ApiFailure e){assertEquals("NO_RECIPE",e.code);}
    }
    @Test public void screensEveryDisplayedRecipeTextForExclusions() throws Exception {
        for(String field:new String[]{"description","whyThisDish","goalFit"}) {
            JsonObject output=recipe();output.addProperty(field,"Serve with tahini.");
            try { GeminiRecipes.validateRecipe(output,request());fail("Unchecked field: "+field); }
            catch(GeminiRecipes.ApiFailure e){assertEquals("ALLERGY_REJECTED",e.code);}
        }
    }
}
