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
 @Test public void numberedInstructionsSplitAcrossLanguagesAndLineEndings(){
  assertSteps("١. اغسل الأرز.\n٢. اطبخ لمدة ١٥ دقيقة.", "اغسل الأرز.", "اطبخ لمدة ١٥ دقيقة.");
  assertSteps("۱) Wash rice.\r\n  ۲) Simmer.", "Wash rice.", "Simmer.");
  assertSteps("1. Wash.\n  2. Simmer.", "Wash.", "Simmer.");
 }
 @Test public void paragraphsAndWrappedInstructionsKeepTheirMeaning(){
  assertSteps("1. Add 1.5 cups.\nStir well.\n\n2. Serve.", "Add 1.5 cups.\nStir well.", "Serve.");
 }
 private void assertSteps(String method, String... expected){
  RecipeCatalog.Recipe recipe=new RecipeCatalog.Recipe("test","Test","Lunch",10,1,"","","",method);
  assertEquals(java.util.Arrays.asList(expected),KitchenScreens.steps(recipe));
 }
 @Test public void crossLanguagePantryNamesShareTheSameCanonicalIngredient(){
  assertEquals(RecipeCatalog.parseIngredients("rice,tomato"),RecipeCatalog.parseIngredients("أرز، بندورة"));
 }
}
