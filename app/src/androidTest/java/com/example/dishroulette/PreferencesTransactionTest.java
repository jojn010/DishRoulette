package com.example.dishroulette;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.allOf;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.*;

/**
 * Meal selection follows the same Apply/Cancel contract as all other recipe preferences.
 */
public class PreferencesTransactionTest {
    @Test
    public void mealChangesApplyTogetherAndCancelDiscardsThemInBothLanguages() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        for (String language : new String[]{"en", "ar"}) {
            InterfaceSettings.prefs(context).edit().putString(InterfaceSettings.LANGUAGE, language)
                .putString("selected_meal", "Any meal").putString("spin_profile", "{}").commit();
            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                String[] dinner = {null}, lunch = {null};
                scenario.onActivity(a -> {
                    dinner[0] = UiLabels.option(a, "Dinner");
                    lunch[0] = UiLabels.option(a, "Lunch");
                    a.showFilters();
                });
                onView(allOf(withText(dinner[0]), withParent(withId(R.id.mealChips)))).perform(click());
                onView(withId(android.R.id.button2)).perform(click());
                scenario.onActivity(a -> {
                    assertEquals("Any meal", a.meal());
                    a.showFilters();
                });
                onView(allOf(withText(lunch[0]), withParent(withId(R.id.mealChips)))).perform(click());
                onView(withId(android.R.id.button1)).perform(click());
                scenario.onActivity(a -> {
                    assertEquals("Lunch", a.meal());
                    assertEquals("Lunch", InterfaceSettings.prefs(a).getString("selected_meal", ""));
                });
                scenario.recreate();
                scenario.onActivity(a -> assertEquals("Lunch", a.meal()));
            }
        }
    }
}
