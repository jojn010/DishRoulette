package com.example.dishroulette;
import org.junit.Test;
import static org.junit.Assert.*;
public class ArabicCookingTest {
 @Test public void minuteTimersReadArabicAndWesternDigitsAndRanges(){
  assertEquals(15,KitchenScreens.suggestedMinutes("اطبخ الأرز لمدة ١٥ دقيقة."));
  assertEquals(7,KitchenScreens.suggestedMinutes("اطبخ لمدة 5 إلى 7 دقائق."));
  assertEquals(12,KitchenScreens.suggestedMinutes("Simmer for 10–12 minutes."));
  assertEquals(0,KitchenScreens.suggestedMinutes("اغسل المكونات جيدًا."));
 }
 @Test public void crossLanguagePantryNamesShareTheSameCanonicalIngredient(){
  assertEquals(RecipeCatalog.parseIngredients("rice,tomato"),RecipeCatalog.parseIngredients("أرز، بندورة"));
 }
}
