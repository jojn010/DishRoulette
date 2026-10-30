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
 @Test public void arabicDualMinuteDurationsProduceTwoMinuteTimers(){
  assertEquals(2,KitchenScreens.suggestedMinutes("اترك الخليط لمدة دقيقتين ثم قلّب."));
  assertEquals(2,KitchenScreens.suggestedMinutes("مدة الانتظار دقيقتان."));
  assertEquals(2,KitchenScreens.suggestedMinutes("اطبخ لمدة ٢ دقيقتين."));
  assertEquals(0,KitchenScreens.suggestedMinutes("هذه كلمة دقيقتينغيرصحيحة."));
  assertEquals(5,KitchenScreens.suggestedMinutes("اطبخ لمدة 5 دقائق ثم استرح دقيقتين."));
 }
 @Test public void minuteTimersUseWholeDurationsAndRoundUpFractions(){
  assertEquals(2,KitchenScreens.suggestedMinutes("Simmer for 1.5 minutes."));
  assertEquals(3,KitchenScreens.suggestedMinutes("Simmer for 1.5 to 2.5 minutes."));
  assertEquals(2,KitchenScreens.suggestedMinutes("اطبخ لمدة ١٫٥ دقيقة."));
  assertEquals(15,KitchenScreens.suggestedMinutes("اطبخ لمدة ۱۵ دقيقة."));
  assertEquals(240,KitchenScreens.suggestedMinutes("Simmer for 1000 minutes."));
  assertEquals(0,KitchenScreens.suggestedMinutes("Wait item5 minutes."));
 }
 @Test public void hourTimersIncludeAdjacentMinutesInBothLanguages(){
  assertEquals(60,KitchenScreens.suggestedMinutes("Bake for 1 hour."));
  assertEquals(90,KitchenScreens.suggestedMinutes("Bake for 1 hour 30 minutes."));
  assertEquals(90,KitchenScreens.suggestedMinutes("Simmer for 1 hr and 30 mins."));
  assertEquals(120,KitchenScreens.suggestedMinutes("Rest for 1–2 hours."));
  assertEquals(90,KitchenScreens.suggestedMinutes("Bake for 1.5 hours."));
  assertEquals(60,KitchenScreens.suggestedMinutes("اطبخ لمدة ساعة."));
  assertEquals(90,KitchenScreens.suggestedMinutes("اطبخ لمدة ساعة و٣٠ دقيقة."));
  assertEquals(120,KitchenScreens.suggestedMinutes("اطبخ لمدة ساعتين."));
  assertEquals(135,KitchenScreens.suggestedMinutes("مدة الطهي ساعتان و١٥ دقيقة."));
  assertEquals(90,KitchenScreens.suggestedMinutes("اطبخ لمدة ١٫٥ ساعة."));
  assertEquals(180,KitchenScreens.suggestedMinutes("اطبخ لمدة ۲ إلى ۳ ساعات."));
 }
 @Test public void hourTimersKeepFirstSeparateDurationAndExistingBounds(){
  assertEquals(60,KitchenScreens.suggestedMinutes("Bake for 1 hour, then rest for 10 minutes."));
  assertEquals(5,KitchenScreens.suggestedMinutes("Stir for 5 minutes then bake for 1 hour."));
  assertEquals(240,KitchenScreens.suggestedMinutes("Simmer for 1000 hours."));
  assertEquals(240,KitchenScreens.suggestedMinutes("Bake for 4 hours and 30 minutes."));
  assertEquals(0,KitchenScreens.suggestedMinutes("Wait item5 hours."));
  assertEquals(0,KitchenScreens.suggestedMinutes("Use the hourly schedule."));
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
