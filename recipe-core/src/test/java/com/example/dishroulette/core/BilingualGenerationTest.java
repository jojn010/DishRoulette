package com.example.dishroulette.core;
import com.google.gson.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class BilingualGenerationTest {
    private JsonObject json(String s){return JsonParser.parseString(s).getAsJsonObject();}
    private JsonObject request(){return json("{\"ingredients\":[\"rice\"],\"meal\":\"Dinner\",\"bilingual\":true,\"language\":\"en\"}");}
    private JsonObject result(){return json("""
        {"possible":true,"title":"Rice","description":"Warm rice","meal":"Dinner","cuisine":"Italian",
        "minutes":20,"servings":2,"ingredients":["1 cup rice"],"steps":["Cook for 15 minutes."],
        "whyThisDish":"Uses rice","goalFit":"Balanced","pantryUsed":["rice"],"additionalIngredients":[],
        "translations":{"en":{"title":"Rice","description":"Warm rice","ingredients":["1 cup rice"],"steps":["Cook for 15 minutes."]},
        "ar":{"title":"أرز","description":"أرز دافئ","ingredients":["1 كوب أرز"],"steps":["اطبخ لمدة 15 دقيقة."]}}}
        """);}
    @Test public void schemaRequiresBothLanguagesAndValidatedOutputRetainsThem()throws Exception{
        JsonObject schema=RecipeRules.payload(request()).getAsJsonObject("generationConfig").getAsJsonObject("responseFormat").getAsJsonObject("text").getAsJsonObject("schema");
        assertTrue(schema.getAsJsonObject("properties").has("translations"));
        assertEquals(2,schema.getAsJsonObject("properties").getAsJsonObject("translations").getAsJsonArray("required").size());
        assertTrue(RecipeRules.validateRecipe(result(),request()).getAsJsonObject("translations").has("ar"));
    }
    @Test public void rejectsMissingLanguageAndDifferentPrimaryRecipe()throws Exception{
        JsonObject bad=result();bad.remove("translations");reject(bad,request());
        bad=result();bad.getAsJsonObject("translations").remove("ar");reject(bad,request());
        bad=result();bad.getAsJsonObject("translations").getAsJsonObject("en").addProperty("title","Another dish");reject(bad,request());
    }
    @Test public void rejectsExcludedIngredientHiddenInOtherLanguage()throws Exception{
        JsonObject request=request();request.add("allergies",json("{\"x\":[\"Milk\"]}").get("x"));
        JsonObject bad=result();bad.getAsJsonObject("translations").getAsJsonObject("ar").getAsJsonArray("ingredients").set(0,new JsonPrimitive("1 كوب حليب"));
        try{RecipeRules.validateRecipe(bad,request);fail();}catch(RecipeRules.ApiFailure expected){assertEquals("ALLERGY_REJECTED",expected.code);}
    }
    private void reject(JsonObject result,JsonObject request)throws Exception{try{RecipeRules.validateRecipe(result,request);fail();}catch(RecipeRules.ApiFailure expected){assertEquals("INVALID_RECIPE",expected.code);}}
}
