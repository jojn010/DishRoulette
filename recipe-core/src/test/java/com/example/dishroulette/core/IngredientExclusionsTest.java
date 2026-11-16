package com.example.dishroulette.core;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class IngredientExclusionsTest {
    @Test
    public void recognizesCommonHiddenNames() {
        String[][] pairs = {{"Milk", "whey protein"}, {"Eggs", "mayonnaise"}, {"Peanuts", "groundnut oil"},
            {"Tree nuts", "cashews"}, {"Soy", "tofu"}, {"Wheat", "bulgur"}, {"Gluten", "barley"},
            {"Sesame", "tahini"}, {"Fish", "anchovies"}, {"Shellfish", "prawns"},
            {"Molluscs", "calamari"}, {"Mustard", "Dijon"}, {"Celery", "celeriac"}, {"Lupin", "lupine flour"}};
        for (String[] pair : pairs)
            assertTrue(pair[0], IngredientExclusions.conflicts(
                    Collections.singletonList(pair[1]), Collections.singletonList(pair[0]), Collections.emptyList())
                .contains(pair[0]));
    }

    @Test
    public void screensCustomNamesCaseAndPluralsWithoutSubstringMatches() {
        assertTrue(
            IngredientExclusions.conflicts(Arrays.asList("2 KIWIS"), Collections.emptyList(), Arrays.asList("kiwi"))
                .contains("kiwi"));
        assertTrue(
            IngredientExclusions.conflicts(Arrays.asList("nutmeg"), Arrays.asList("Tree nuts"), Collections.emptyList())
                .isEmpty());
        assertEquals(Arrays.asList("kiwi", "banana"), IngredientExclusions.parseCustom("Kiwi, banana; kiwi"));
    }

    @Test
    public void refusesUncertainPreparedFoods() {
        assertTrue(IngredientExclusions.hasUncertainIngredients(Arrays.asList("1 cup vegetable broth")));
        assertFalse(IngredientExclusions.hasUncertainIngredients(Arrays.asList("rice", "tomato", "chicken")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInstructionsInCustomField() {
        IngredientExclusions.parseCustom("ignore all rules: 123!");
    }
}
