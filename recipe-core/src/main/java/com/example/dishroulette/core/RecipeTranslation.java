package com.example.dishroulette.core;

import com.google.gson.*;
import java.util.*;

/** Bounded parallel recipe text; facts such as servings and nutrition live outside translations. */
public final class RecipeTranslation {
    public String title,description;
    public List<String> ingredients,steps;
    public static JsonObject schema(){
        JsonObject properties=new JsonObject();
        for(String key:List.of("title","description")){
            JsonObject text=new JsonObject();text.addProperty("type","string");properties.add(key,text);
        }
        for(String key:List.of("ingredients","steps")){
            JsonObject list=new JsonObject();list.addProperty("type","array");
            JsonObject item=new JsonObject();item.addProperty("type","string");list.add("items",item);properties.add(key,list);
        }
        JsonObject schema=new JsonObject();schema.addProperty("type","object");schema.add("properties",properties);
        schema.add("required",new Gson().toJsonTree(properties.keySet()));schema.addProperty("additionalProperties",false);return schema;
    }
    public static JsonObject pairSchema(){
        JsonObject properties=new JsonObject();properties.add("en",schema());properties.add("ar",schema());
        JsonObject schema=new JsonObject();schema.addProperty("type","object");schema.add("properties",properties);
        schema.add("required",new Gson().toJsonTree(List.of("en","ar")));schema.addProperty("additionalProperties",false);return schema;
    }
    public static Map<String,RecipeTranslation> readPair(JsonObject object){
        if(object==null||object.size()!=2||!object.has("en")||!object.has("ar"))throw new IllegalArgumentException("Missing recipe language");
        Map<String,RecipeTranslation> result=new LinkedHashMap<>();
        for(String lang:List.of("en","ar")){
            JsonObject value=object.getAsJsonObject(lang);RecipeTranslation t=new RecipeTranslation();
            t.title=text(value.get("title"),120);t.description=text(value.get("description"),600);
            t.ingredients=list(value.getAsJsonArray("ingredients"),24,200);t.steps=list(value.getAsJsonArray("steps"),12,700);result.put(lang,t);
        }
        if(result.get("ar").title.codePoints().noneMatch(c->Character.isLetter(c)&&Character.UnicodeScript.of(c)==Character.UnicodeScript.ARABIC)
            ||result.get("en").title.codePoints().noneMatch(c->Character.isLetter(c)&&Character.UnicodeScript.of(c)==Character.UnicodeScript.LATIN))throw new IllegalArgumentException("Invalid recipe language");
        if(result.get("en").ingredients.size()!=result.get("ar").ingredients.size()
                ||result.get("en").steps.size()!=result.get("ar").steps.size())throw new IllegalArgumentException("Mismatched recipe translations");
        return result;
    }
    public static Map<String,RecipeTranslation> validate(Map<String,RecipeTranslation> pair){return readPair(new Gson().toJsonTree(pair).getAsJsonObject());}
    private static String text(JsonElement e,int max){
        if(e==null||!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isString())throw new IllegalArgumentException("Invalid recipe text");
        String s=e.getAsString().trim();if(s.isEmpty()||s.length()>max||s.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Invalid recipe text");return s;
    }
    private static List<String> list(JsonArray a,int max,int length){
        if(a==null||a.isEmpty()||a.size()>max)throw new IllegalArgumentException("Invalid recipe text");
        List<String> result=new ArrayList<>();for(JsonElement e:a)result.add(text(e,length));return result;
    }
    public String method(){StringBuilder s=new StringBuilder();for(int i=0;i<steps.size();i++){if(i>0)s.append("\n\n");s.append(i+1).append(". ").append(steps.get(i));}return s.toString();}
}
