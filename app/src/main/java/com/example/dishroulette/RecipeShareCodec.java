package com.example.dishroulette;

import com.google.gson.*;
import com.example.dishroulette.core.NutritionEstimate;
import com.example.dishroulette.core.SpinOptions;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Versioned, bounded offline recipe snapshots. Never serializes a profile, credential or local id. */
final class RecipeShareCodec {
    static final String PREFIX="TABKHA1:";
    static final int MAX_CODE=48000, MAX_JSON=32000;
    private static final Gson GSON=new GsonBuilder().disableHtmlEscaping().create();
    private RecipeShareCodec(){}
    private static JsonObject snapshot(RecipeCatalog.Recipe r){
        JsonObject o=new JsonObject();
        o.addProperty("title",r.title);o.addProperty("description",r.description);o.addProperty("meal",r.meal);
        o.addProperty("minutes",r.minutes);o.addProperty("servings",r.servings);
        o.addProperty("ingredients",r.ingredients);o.addProperty("method",r.method);
        o.addProperty("language",r.language==null?"":r.language);o.addProperty("cuisine",r.cuisine==null?"":r.cuisine);
        o.addProperty("aiGenerated",r.aiGenerated);
        if(r.caloriesPerServing!=null)o.addProperty("caloriesPerServing",r.caloriesPerServing);
        if(r.proteinGramsPerServing!=null)o.addProperty("proteinGramsPerServing",r.proteinGramsPerServing);
        if(r.carbohydratesGramsPerServing!=null)o.addProperty("carbohydratesGramsPerServing",r.carbohydratesGramsPerServing);
        if(r.fatGramsPerServing!=null)o.addProperty("fatGramsPerServing",r.fatGramsPerServing);
        if(r.fiberGramsPerServing!=null)o.addProperty("fiberGramsPerServing",r.fiberGramsPerServing);
        if(r.sugarGramsPerServing!=null)o.addProperty("sugarGramsPerServing",r.sugarGramsPerServing);
        if(r.sodiumMilligramsPerServing!=null)o.addProperty("sodiumMilligramsPerServing",r.sodiumMilligramsPerServing);
        return o;
    }
    static String encode(RecipeCatalog.Recipe r){
        JsonObject o=snapshot(r);validate(o);
        byte[] json=GSON.toJson(o).getBytes(StandardCharsets.UTF_8);
        if(json.length>MAX_JSON)throw invalid();
        try{
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(GZIPOutputStream zip=new GZIPOutputStream(out)){zip.write(json);}
            String code=PREFIX+Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
            if(code.length()>MAX_CODE)throw invalid();return code;
        }catch(IOException e){throw invalid();}
    }
    static RecipeCatalog.Recipe decode(String code){
        if(code==null||code.length()>MAX_CODE)throw invalid();
        code=code.trim();if(!code.startsWith(PREFIX))throw invalid();
        try{
            byte[] compressed=Base64.getUrlDecoder().decode(code.substring(PREFIX.length()));
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(GZIPInputStream zip=new GZIPInputStream(new ByteArrayInputStream(compressed))){
                byte[] chunk=new byte[1024];int n;
                while((n=zip.read(chunk))!=-1){if(out.size()+n>MAX_JSON)throw invalid();out.write(chunk,0,n);}
            }
            String json=new String(out.toByteArray(),StandardCharsets.UTF_8);
            // Reject excessive nesting before Gson sees untrusted JSON.
            boolean quoted=false,escape=false;int depth=0;
            for(char c:json.toCharArray()){
                if(escape){escape=false;continue;}if(quoted&&c=='\\'){escape=true;continue;}
                if(c=='"'){quoted=!quoted;continue;}
                if(!quoted){if(c=='{'||c=='['){if(++depth>4)throw invalid();}else if(c=='}'||c==']')depth--;}
            }
            JsonObject o=JsonParser.parseString(json).getAsJsonObject();
            RecipeCatalog.Recipe r=validate(o);
            // Hash the canonical allowlisted snapshot, independent of sender ids or unknown fields.
            byte[] hash=MessageDigest.getInstance("SHA-256").digest(GSON.toJson(snapshot(r)).getBytes(StandardCharsets.UTF_8));
            StringBuilder id=new StringBuilder("shared_");for(byte b:hash)id.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
            RecipeCatalog.Recipe copy=new RecipeCatalog.Recipe(id.toString(),r.title,r.meal,r.minutes,r.servings,r.description,"",r.ingredients,r.method);
            copy.language=r.language;copy.cuisine=r.cuisine;copy.aiGenerated=r.aiGenerated;copy.shared=true;copy.discovery=true;
            copy.caloriesPerServing=r.caloriesPerServing;copy.proteinGramsPerServing=r.proteinGramsPerServing;
            copy.carbohydratesGramsPerServing=r.carbohydratesGramsPerServing;copy.fatGramsPerServing=r.fatGramsPerServing;
            copy.fiberGramsPerServing=r.fiberGramsPerServing;copy.sugarGramsPerServing=r.sugarGramsPerServing;copy.sodiumMilligramsPerServing=r.sodiumMilligramsPerServing;
            return copy;
        }catch(Exception e){throw invalid();}
    }
    private static RecipeCatalog.Recipe validate(JsonObject o){
        String meal=text(o,"meal",30,false);if(!SpinOptions.MEALS.contains(meal)||meal.equals("Any meal"))throw invalid();
        RecipeCatalog.Recipe r=new RecipeCatalog.Recipe("",text(o,"title",120,false),meal,integer(o,"minutes",1,240),integer(o,"servings",1,12),text(o,"description",600,false),"",text(o,"ingredients",6000,false),text(o,"method",12000,false));
        r.language=text(o,"language",2,true);if(!r.language.isEmpty()&&!r.language.equals("en")&&!r.language.equals("ar"))throw invalid();
        r.cuisine=text(o,"cuisine",80,true);
        JsonPrimitive ai=o.getAsJsonPrimitive("aiGenerated");if(ai==null||!ai.isBoolean())throw invalid();r.aiGenerated=ai.getAsBoolean();
        if(o.has("caloriesPerServing")||o.has("proteinGramsPerServing")){
            r.caloriesPerServing=integer(o,"caloriesPerServing",1,5000);r.proteinGramsPerServing=number(o,"proteinGramsPerServing",500);
            if(!NutritionEstimate.valid(r.caloriesPerServing,r.proteinGramsPerServing))throw invalid();
        }
        for(Map.Entry<String,Integer> entry:Map.of("carbohydratesGramsPerServing",1000,"fatGramsPerServing",500,"fiberGramsPerServing",200,"sugarGramsPerServing",1000,"sodiumMilligramsPerServing",20000).entrySet()){
            if(!o.has(entry.getKey()))continue;if(r.caloriesPerServing==null)throw invalid();double value=number(o,entry.getKey(),entry.getValue());
            switch(entry.getKey()){
                case "carbohydratesGramsPerServing":r.carbohydratesGramsPerServing=value;break;
                case "fatGramsPerServing":r.fatGramsPerServing=value;break;
                case "fiberGramsPerServing":r.fiberGramsPerServing=value;break;
                case "sugarGramsPerServing":r.sugarGramsPerServing=value;break;
                case "sodiumMilligramsPerServing":r.sodiumMilligramsPerServing=value;break;
            }
        }
        return r;
    }
    private static String text(JsonObject o,String key,int max,boolean empty){
        JsonPrimitive p=o.getAsJsonPrimitive(key);if(p==null||!p.isString())throw invalid();String s=p.getAsString();
        if(s.length()>max||(!empty&&s.trim().isEmpty()))throw invalid();
        for(char c:s.toCharArray())if(Character.isISOControl(c)&&c!='\n'&&c!='\r'&&c!='\t')throw invalid();return s;
    }
    private static int integer(JsonObject o,String key,int min,int max){JsonPrimitive p=o.getAsJsonPrimitive(key);if(p==null||!p.isNumber())throw invalid();int value=p.getAsBigDecimal().intValueExact();if(value<min||value>max)throw invalid();return value;}
    private static double number(JsonObject o,String key,int max){JsonPrimitive p=o.getAsJsonPrimitive(key);if(p==null||!p.isNumber())throw invalid();double n=p.getAsDouble();if(!NutritionEstimate.validNutrient(n,max))throw invalid();return n;}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("Invalid Tabkha recipe code");}
}
