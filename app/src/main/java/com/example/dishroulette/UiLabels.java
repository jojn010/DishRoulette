package com.example.dishroulette;
import android.content.Context;
import androidx.core.text.BidiFormatter;
final class UiLabels {
 private UiLabels(){}
 static String option(Context c,String value){if(value==null)return "";switch(value){
 case "Any meal":return c.getString(R.string.option_any_meal);
 case "Breakfast":return c.getString(R.string.option_breakfast);
 case "Lunch":return c.getString(R.string.option_lunch);
 case "Dinner":return c.getString(R.string.option_dinner);
 case "Snack":return c.getString(R.string.option_snack);
 case "Dessert":return c.getString(R.string.option_dessert);
 case "Balanced":return c.getString(R.string.option_balanced);
 case "Cutting":return c.getString(R.string.option_cutting);
 case "Bulking":return c.getString(R.string.option_bulking);
 case "Mediterranean":return c.getString(R.string.option_mediterranean);
 case "Any cuisine":return c.getString(R.string.option_any_cuisine);
 case "Palestinian":return c.getString(R.string.option_palestinian);
 case "Lebanese":return c.getString(R.string.option_lebanese);
 case "Turkish":return c.getString(R.string.option_turkish);
 case "Moroccan":return c.getString(R.string.option_moroccan);
 case "Egyptian":return c.getString(R.string.option_egyptian);
 case "Italian":return c.getString(R.string.option_italian);
 case "Greek":return c.getString(R.string.option_greek);
 case "Mexican":return c.getString(R.string.option_mexican);
 case "Indian":return c.getString(R.string.option_indian);
 case "Japanese":return c.getString(R.string.option_japanese);
 case "Chinese":return c.getString(R.string.option_chinese);
 case "Korean":return c.getString(R.string.option_korean);
 case "Thai":return c.getString(R.string.option_thai);
 case "Ethiopian":return c.getString(R.string.option_ethiopian);
 case "French":return c.getString(R.string.option_french);
 case "Spanish":return c.getString(R.string.option_spanish);
 case "American":return c.getString(R.string.option_american);
 case "Milk":return c.getString(R.string.option_milk);
 case "Eggs":return c.getString(R.string.option_eggs);
 case "Peanuts":return c.getString(R.string.option_peanuts);
 case "Tree nuts":return c.getString(R.string.option_tree_nuts);
 case "Soy":return c.getString(R.string.option_soy);
 case "Wheat":return c.getString(R.string.option_wheat);
 case "Gluten":return c.getString(R.string.option_gluten);
 case "Sesame":return c.getString(R.string.option_sesame);
 case "Fish":return c.getString(R.string.option_fish);
 case "Shellfish":return c.getString(R.string.option_shellfish);
 case "Molluscs":return c.getString(R.string.option_molluscs);
 case "Mustard":return c.getString(R.string.option_mustard);
 case "Celery":return c.getString(R.string.option_celery);
 case "Lupin":return c.getString(R.string.option_lupin);
 case "water":return c.getString(R.string.option_water);
 case "salt":return c.getString(R.string.option_salt);
 case "pepper":return c.getString(R.string.option_pepper);
 case "cooking oil":return c.getString(R.string.option_cooking_oil);
 default:return BidiFormatter.getInstance(c.getResources().getConfiguration().getLocales().get(0)).unicodeWrap(value);}}
}
