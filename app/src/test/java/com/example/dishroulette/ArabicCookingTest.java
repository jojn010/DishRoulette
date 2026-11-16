package com.example.dishroulette;

import org.junit.Test;

import static org.junit.Assert.*;

public class ArabicCookingTest {
    @Test
    public void minuteTimersReadArabicAndWesternDigitsAndRanges() {
        assertEquals(15, CookingInstructions.suggestedMinutes("اطبخ الأرز لمدة ١٥ دقيقة."));
        assertEquals(7, CookingInstructions.suggestedMinutes("اطبخ لمدة 5 إلى 7 دقائق."));
        assertEquals(12, CookingInstructions.suggestedMinutes("Simmer for 10–12 minutes."));
        assertEquals(0, CookingInstructions.suggestedMinutes("اغسل المكونات جيدًا."));
    }

    @Test
    public void arabicDualMinuteDurationsProduceTwoMinuteTimers() {
        assertEquals(2, CookingInstructions.suggestedMinutes("اترك الخليط لمدة دقيقتين ثم قلّب."));
        assertEquals(2, CookingInstructions.suggestedMinutes("مدة الانتظار دقيقتان."));
        assertEquals(2, CookingInstructions.suggestedMinutes("اطبخ لمدة ٢ دقيقتين."));
        assertEquals(0, CookingInstructions.suggestedMinutes("هذه كلمة دقيقتينغيرصحيحة."));
        assertEquals(5, CookingInstructions.suggestedMinutes("اطبخ لمدة 5 دقائق ثم استرح دقيقتين."));
    }

    @Test
    public void minuteTimersUseWholeDurationsAndRoundUpFractions() {
        assertEquals(2, CookingInstructions.suggestedMinutes("Simmer for 1.5 minutes."));
        assertEquals(3, CookingInstructions.suggestedMinutes("Simmer for 1.5 to 2.5 minutes."));
        assertEquals(2, CookingInstructions.suggestedMinutes("اطبخ لمدة ١٫٥ دقيقة."));
        assertEquals(15, CookingInstructions.suggestedMinutes("اطبخ لمدة ۱۵ دقيقة."));
        assertEquals(240, CookingInstructions.suggestedMinutes("Simmer for 1000 minutes."));
        assertEquals(0, CookingInstructions.suggestedMinutes("Wait item5 minutes."));
    }

    @Test
    public void hourTimersIncludeAdjacentMinutesInBothLanguages() {
        assertEquals(60, CookingInstructions.suggestedMinutes("Bake for 1 hour."));
        assertEquals(90, CookingInstructions.suggestedMinutes("Bake for 1 hour 30 minutes."));
        assertEquals(90, CookingInstructions.suggestedMinutes("Simmer for 1 hr and 30 mins."));
        assertEquals(120, CookingInstructions.suggestedMinutes("Rest for 1–2 hours."));
        assertEquals(90, CookingInstructions.suggestedMinutes("Bake for 1.5 hours."));
        assertEquals(60, CookingInstructions.suggestedMinutes("اطبخ لمدة ساعة."));
        assertEquals(90, CookingInstructions.suggestedMinutes("اطبخ لمدة ساعة و٣٠ دقيقة."));
        assertEquals(120, CookingInstructions.suggestedMinutes("اطبخ لمدة ساعتين."));
        assertEquals(135, CookingInstructions.suggestedMinutes("مدة الطهي ساعتان و١٥ دقيقة."));
        assertEquals(90, CookingInstructions.suggestedMinutes("اطبخ لمدة ١٫٥ ساعة."));
        assertEquals(180, CookingInstructions.suggestedMinutes("اطبخ لمدة ۲ إلى ۳ ساعات."));
    }

    @Test
    public void hourTimersKeepFirstSeparateDurationAndExistingBounds() {
        assertEquals(60, CookingInstructions.suggestedMinutes("Bake for 1 hour, then rest for 10 minutes."));
        assertEquals(5, CookingInstructions.suggestedMinutes("Stir for 5 minutes then bake for 1 hour."));
        assertEquals(240, CookingInstructions.suggestedMinutes("Simmer for 1000 hours."));
        assertEquals(240, CookingInstructions.suggestedMinutes("Bake for 4 hours and 30 minutes."));
        assertEquals(0, CookingInstructions.suggestedMinutes("Wait item5 hours."));
        assertEquals(0, CookingInstructions.suggestedMinutes("Use the hourly schedule."));
    }

    @Test
    public void arabicTimersRecognizeVowelMarksWithoutChangingWordBoundaries() {
        assertEquals(15, CookingInstructions.suggestedMinutes("اطبخ لِمُدَّةِ ١٥ دَقِيقَةً."));
        assertEquals(2, CookingInstructions.suggestedMinutes("اتركه دَقِيقَتَيْنِ."));
        assertEquals(90, CookingInstructions.suggestedMinutes("اطبخ لِمُدَّةِ سَاعَةٍ وَ٣٠ دَقِيقَةً."));
        assertEquals(7, CookingInstructions.suggestedMinutes("اطبخ ٥ إِلَى ٧ دَقَائِقَ."));
        assertEquals(0, CookingInstructions.suggestedMinutes("هذه سَاعَةٌغيرصحيحة."));
    }

    @Test
    public void numberedInstructionsSplitAcrossLanguagesAndLineEndings() {
        assertSteps("١. اغسل الأرز.\n٢. اطبخ لمدة ١٥ دقيقة.", "اغسل الأرز.", "اطبخ لمدة ١٥ دقيقة.");
        assertSteps("۱) Wash rice.\r\n  ۲) Simmer.", "Wash rice.", "Simmer.");
        assertSteps("1. Wash.\n  2. Simmer.", "Wash.", "Simmer.");
    }

    @Test
    public void paragraphsAndWrappedInstructionsKeepTheirMeaning() {
        assertSteps("1. Add 1.5 cups.\nStir well.\n\n2. Serve.", "Add 1.5 cups.\nStir well.", "Serve.");
    }

    private void assertSteps(String method, String... expected) {
        RecipeCatalog.Recipe recipe = new RecipeCatalog.Recipe("test", "Test", "Lunch", 10, 1, "", "", "", method);
        assertEquals(java.util.Arrays.asList(expected), CookingInstructions.steps(recipe));
    }

    @Test
    public void crossLanguagePantryNamesShareTheSameCanonicalIngredient() {
        assertEquals(RecipeCatalog.parseIngredients("rice,tomato"), RecipeCatalog.parseIngredients("أرز، بندورة"));
    }
}
