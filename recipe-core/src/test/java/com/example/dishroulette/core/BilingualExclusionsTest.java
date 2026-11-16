package com.example.dishroulette.core;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class BilingualExclusionsTest {
    @Test
    public void arabicAllergenFamiliesAreScreenedAlongsideEnglish() {
        String[][] cases =
            {{"Milk", "الحليب"}, {"Eggs", "بيضة"}, {"Peanuts", "الفول السوداني"}, {"Tree nuts", "لوز"}, {"Soy", "صويا"}, {"Wheat", "القمح"}, {"Gluten", "شعير"}, {"Sesame", "الطحينة"}, {"Fish", "سمك"}, {"Shellfish", "روبيان"}, {"Molluscs", "محار"}, {"Mustard", "الخردل"}, {"Celery", "كرفس"}, {"Lupin", "ترمس"}};
        for (String[] c : cases)
            assertTrue(Arrays.toString(c),
                IngredientExclusions.conflicts(Collections.singletonList(c[1]), Collections.singletonList(c[0]),
                    Collections.emptyList()).contains(c[0]));
    }

    @Test
    public void customExclusionsFollowKnownCrossLanguageFoodNames() {
        assertTrue(IngredientExclusions.conflicts(Collections.singletonList("شرائح الموز"), Collections.emptyList(),
            Collections.singletonList("banana")).contains("banana"));
        assertTrue(IngredientExclusions.conflicts(Collections.singletonList("chopped kiwi"), Collections.emptyList(),
            Collections.singletonList("كيوي")).contains("كيوي"));
        assertEquals("rice", FoodNames.canonical("أُرْز"));
        assertEquals(Arrays.asList("كيوي", "موز"), IngredientExclusions.parseCustom("كيوي، موز"));
    }

    @Test
    public void preparedFoodsRemainUncertainInArabic() {
        assertTrue(IngredientExclusions.hasUncertainIngredients(Arrays.asList("صلصة جاهزة")));
        assertFalse(IngredientExclusions.hasUncertainIngredients(Arrays.asList("أرز", "بندورة")));
    }
}
