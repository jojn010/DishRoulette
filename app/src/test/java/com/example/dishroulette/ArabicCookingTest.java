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
 @Test public void minuteTimersUseWholeDurationsAndRoundUpFractions(){
  assertEquals(2,KitchenScreens.suggestedMinutes("Simmer for 1.5 minutes."));
  assertEquals(3,KitchenScreens.suggestedMinutes("Simmer for 1.5 to 2.5 minutes."));
  assertEquals(2,KitchenScreens.suggestedMinutes("اطبخ لمدة ١٫٥ دقيقة."));
  assertEquals(15,KitchenScreens.suggestedMinutes("اطبخ لمدة ۱۵ دقيقة."));
  assertEquals(240,KitchenScreens.suggestedMinutes("Simmer for 1000 minutes."));
  assertEquals(0,KitchenScreens.suggestedMinutes("Wait item5 minutes."));
 }
 @Test public void crossLanguagePantryNamesShareTheSameCanonicalIngredient(){
  assertEquals(RecipeCatalog.parseIngredients("rice,tomato"),RecipeCatalog.parseIngredients("أرز، بندورة"));
 }
}
