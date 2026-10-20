package com.example.dishroulette;
import android.content.Context;
final class IngredientValidation {
 private IngredientValidation(){}
 static String message(Context c,String error){
  if(error==null)return null;
  int id=error.startsWith("One ingredient")?R.string.ingredient_single:error.startsWith("That ingredient")?R.string.ingredient_duplicate:error.startsWith("Use at most")?R.string.ingredient_length:R.string.ingredient_limit;
  return c.getString(id);
 }
}
