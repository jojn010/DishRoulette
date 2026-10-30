package com.example.dishroulette;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RecipeShareCodecTest {
    private RecipeCatalog.Recipe recipe(){
        RecipeCatalog.Recipe r=new RecipeCatalog.Recipe("owner-local-id","أرز بالبندورة","Dinner",25,6,"وجبة للأصحاب","rice,tomato","٣ أكواب أرز\n٦ حبات بندورة","١. اغسل الأرز.\n\n٢. اطبخ لمدة ١٥ دقيقة.");
        r.language="ar";r.cuisine="Palestinian";r.aiGenerated=true;r.goal="Bulking";r.whyThisDish="private taste history";r.goalFit="private preference";
        r.caloriesPerServing=420;r.proteinGramsPerServing=12.5;r.sodiumMilligramsPerServing=300.0;return r;
    }
    private String pack(String json)throws Exception{
        ByteArrayOutputStream out=new ByteArrayOutputStream();try(GZIPOutputStream zip=new GZIPOutputStream(out)){zip.write(json.getBytes(StandardCharsets.UTF_8));}
        return RecipeShareCodec.PREFIX+Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
    }
    private JsonObject json(String code)throws Exception{
        try(GZIPInputStream zip=new GZIPInputStream(new ByteArrayInputStream(Base64.getUrlDecoder().decode(code.substring(8))))){return JsonParser.parseString(new String(zip.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();}
    }
    private void invalid(String code){try{RecipeShareCodec.decode(code);fail("Accepted invalid code");}catch(IllegalArgumentException expected){}}
    @Test public void bilingualSnapshotPreservesRecipeWithoutOwnerStateAndDeduplicates()throws Exception{
        RecipeCatalog.Recipe source=recipe();String code=RecipeShareCodec.encode(source);JsonObject data=json(code);
        for(String key:new String[]{"id","keys","goal","whyThisDish","goalFit","allergies","key","model"})assertFalse(data.has(key));
        RecipeCatalog.Recipe imported=RecipeShareCodec.decode(code);assertEquals(source.title,imported.title);assertEquals(source.ingredients,imported.ingredients);assertEquals(source.method,imported.method);assertEquals(6,imported.servings);assertEquals(source.proteinGramsPerServing,imported.proteinGramsPerServing);assertEquals("ar",imported.language);assertTrue(imported.shared);assertTrue(imported.keys.isEmpty());
        assertEquals(code,RecipeShareCodec.encode(imported));assertEquals(imported.id,RecipeShareCodec.decode(code).id);
        data.addProperty("id","overwrite-owner-recipe");data.addProperty("key","not-a-real-key");assertEquals(imported.id,RecipeShareCodec.decode(pack(data.toString())).id);
        RecipeCatalog.Recipe english=RecipeCatalog.ALL.get(0);assertEquals(english.title,RecipeShareCodec.decode(RecipeShareCodec.encode(english)).title);
    }
    @Test public void rejectsBadVersionsCorruptionHugeInputAndDecompressionBombs()throws Exception{
        for(String code:new String[]{"","TABKHA2:abc","https://example.com","TABKHA1:!!!","TABKHA1:abcd", "x".repeat(48001)})invalid(code);
        String good=RecipeShareCodec.encode(recipe());invalid(good.substring(0,good.length()-7));
        invalid(pack(" ".repeat(32001)));invalid(pack("[".repeat(1000)+"]".repeat(1000)));
    }
    @Test public void rejectsInvalidYieldTypesTextAndPartialNutrition()throws Exception{
        JsonObject original=json(RecipeShareCodec.encode(recipe()));
        for(String field:new String[]{"servings","minutes","proteinGramsPerServing"}){
            JsonObject data=original.deepCopy();data.addProperty(field,"6");invalid(pack(data.toString()));
        }
        for(int count:new int[]{0,13}){JsonObject data=original.deepCopy();data.addProperty("servings",count);invalid(pack(data.toString()));}
        JsonObject data=original.deepCopy();data.addProperty("title", "bad\u0000title");invalid(pack(data.toString()));
        data=original.deepCopy();data.remove("proteinGramsPerServing");invalid(pack(data.toString()));
        data=original.deepCopy();data.addProperty("method", " ");invalid(pack(data.toString()));
    }
    @Test public void historicalRecipesDoNotInventNutrition(){
        RecipeCatalog.Recipe r=RecipeCatalog.ALL.get(0);RecipeCatalog.Recipe imported=RecipeShareCodec.decode(RecipeShareCodec.encode(r));assertNull(imported.caloriesPerServing);assertNull(imported.proteinGramsPerServing);assertEquals(r.servings,imported.servings);
    }
}
