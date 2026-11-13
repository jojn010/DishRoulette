package com.example.dishroulette;

import com.google.gson.*;
import com.example.dishroulette.core.RecipeTranslation;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class BilingualRecipeTest {
    @Test public void everyBundledRecipeHasParallelCompleteTextAndIdenticalFacts(){
        for(RecipeCatalog.Recipe source:RecipeCatalog.ALL){
            assertTrue(source.id,source.bilingual());
            RecipeCatalog.Recipe ar=source.forLanguage("ar"),en=ar.forLanguage("en");
            assertEquals("ar",ar.language);assertNotEquals(source.title,ar.title);
            assertEquals(source.title,en.title);assertEquals(source.ingredients,en.ingredients);assertEquals(source.method,en.method);
            assertEquals(source.id,ar.id);assertEquals(source.minutes,ar.minutes);assertEquals(source.servings,ar.servings);
            assertEquals(source.translations.get("en").steps.size(),source.translations.get("ar").steps.size());
        }
    }
    @Test public void sharingFromEitherLanguageHasStableIdentityAndImportsOffline(){
        RecipeCatalog.Recipe source=RecipeCatalog.ALL.get(5);
        String english=RecipeShareCodec.encode(source),arabic=RecipeShareCodec.encode(source.forLanguage("ar"));
        assertEquals(english,arabic);
        RecipeCatalog.Recipe imported=RecipeShareCodec.decode(arabic);
        assertTrue(imported.bilingual());assertTrue(imported.shared);
        assertEquals(source.translations.get("ar").title,imported.forLanguage("ar").title);
        assertEquals(source.title,imported.forLanguage("en").title);
        assertEquals(imported.id,imported.forLanguage("ar").id);
        assertEquals(english,RecipeShareCodec.encode(imported.forLanguage("ar")));
    }
    @Test public void metadataAndProgressIdentitySurviveLanguageChangesAndStorage(){
        RecipeCatalog.Recipe source=new Gson().fromJson(new Gson().toJson(RecipeCatalog.ALL.get(0)),RecipeCatalog.Recipe.class);
        source.caloriesPerServing=300;source.proteinGramsPerServing=15.0;source.sodiumMilligramsPerServing=50.0;
        RecipeCatalog.Recipe imported=RecipeShareCodec.decode(RecipeShareCodec.encode(source));
        RecipeCatalog.Recipe ar=imported.forLanguage("ar");
        assertEquals(imported.id,ar.id);assertEquals(imported.caloriesPerServing,ar.caloriesPerServing);
        assertEquals(imported.proteinGramsPerServing,ar.proteinGramsPerServing);assertEquals(imported.sodiumMilligramsPerServing,ar.sodiumMilligramsPerServing);
        RecipeCatalog.Recipe restored=new Gson().fromJson(new Gson().toJson(ar),RecipeCatalog.Recipe.class);
        assertTrue(restored.bilingual());assertEquals(source.title,restored.forLanguage("en").title);
    }
    @Test public void rejectsIncompleteMismatchedAndOversizedTranslationPairs(){
        JsonObject pair=new Gson().toJsonTree(RecipeCatalog.ALL.get(0).translations).getAsJsonObject();
        JsonObject wrongLanguage=pair.deepCopy();wrongLanguage.add("ar",pair.get("en"));reject(wrongLanguage);
        JsonObject incomplete=pair.deepCopy();incomplete.remove("ar");reject(incomplete);
        JsonObject bad=pair.deepCopy();bad.getAsJsonObject("ar").getAsJsonArray("steps").remove(0);reject(bad);
        bad=pair.deepCopy();bad.getAsJsonObject("ar").addProperty("title","x".repeat(121));reject(bad);
        bad=pair.deepCopy();bad.getAsJsonObject("ar").addProperty("description","bad\u0000text");reject(bad);
    }
    private void reject(JsonObject object){try{RecipeTranslation.readPair(object);fail("Accepted invalid translations");}catch(IllegalArgumentException expected){}}
    @Test public void legacyMonolingualRecipeRemainsReadableWithoutFabricatingTranslation(){
        RecipeCatalog.Recipe source=new RecipeCatalog.Recipe("old","Old rice","Dinner",20,2,"Warm rice","rice","1 cup rice","1. Cook for 15 minutes.");
        source.language="en";RecipeCatalog.Recipe imported=RecipeShareCodec.decode(RecipeShareCodec.encode(source));
        assertFalse(imported.bilingual());assertSame(imported,imported.forLanguage("ar"));assertEquals(source.method,imported.method);
    }
}
