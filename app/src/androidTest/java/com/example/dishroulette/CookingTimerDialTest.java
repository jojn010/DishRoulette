package com.example.dishroulette;

import android.content.SharedPreferences;
import android.view.View;
import android.widget.FrameLayout;
import androidx.test.core.app.ActivityScenario;
import org.junit.Test;
import static org.junit.Assert.*;

public class CookingTimerDialTest {
    @Test public void timerRespectsReducedMotionAndCancelsOnDetach() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> {
                SharedPreferences prefs = InterfaceSettings.prefs(a);
                boolean previous = prefs.getBoolean("reduce_motion", false);
                android.widget.FrameLayout host = a.findViewById(R.id.secondaryScreen);
                CookingTimerView dial = new CookingTimerView(a);
                int previousVisibility = host.getVisibility();
                host.setVisibility(View.VISIBLE);
                host.addView(dial);
                try {
                    prefs.edit().putBoolean("reduce_motion", true).commit();
                    dial.update(90000, 120000, true);
                    assertNull(sweep(dial));
                    prefs.edit().putBoolean("reduce_motion", false).commit();
                    dial.update(90000, 120000, true);
                    if (android.provider.Settings.Global.getFloat(a.getContentResolver(),
                            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0) {
                        assertNotNull(sweep(dial));
                    }
                    dial.update(90000, 120000, false);
                    assertNull(sweep(dial));
                    dial.update(90000, 120000, true);
                    host.removeView(dial);
                    assertNull(sweep(dial));
                } finally {
                    host.removeView(dial);
                    host.setVisibility(previousVisibility);
                    prefs.edit().putBoolean("reduce_motion", previous).commit();
                }
            });
        }
    }

    private Object sweep(CookingTimerView dial) {
        try {
            java.lang.reflect.Field field = CookingTimerView.class.getDeclaredField("sweep");
            field.setAccessible(true);
            return field.get(dial);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    @Test public void dialTracksPauseResumeResetCompletionAndReopening() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> {
                SharedPreferences prefs = a.getSharedPreferences("timer_dial_test", 0);
                prefs.edit().clear().commit();
                KitchenScreens screens = new KitchenScreens(a, (FrameLayout)a.findViewById(R.id.secondaryScreen), prefs,
                        new KitchenScreens.Actions() {
                            public void open(RecipeCatalog.Recipe r) { }
                            public void remove(RecipeCatalog.Recipe r) { }
                        });
                RecipeCatalog.Recipe recipe = new RecipeCatalog.Recipe("dial_test", "Rice", "Dinner", 10, 1,
                        "Rice", "rice", "1 cup rice", "Cook for 10 minutes.");
                String prefix = "cook_timer_dial_test_0";
                screens.cook(recipe);
                CookingTimerView dial = a.findViewById(R.id.cookingTimerDial);
                assertEquals(View.VISIBLE, dial.getVisibility());
                assertEquals(1f, dial.remainingFraction(), .001f);
                a.findViewById(R.id.cookingTimerStart).performClick();
                assertTrue(prefs.getLong(prefix+"_end", 0) > System.currentTimeMillis());
                a.findViewById(R.id.cookingTimerStart).performClick();
                assertEquals(0, prefs.getLong(prefix+"_end", -1));
                assertTrue(prefs.getLong(prefix+"_left", 0) > 0);
                prefs.edit().putLong(prefix+"_left", 300000).apply();
                screens.close();screens.cook(recipe);
                dial = a.findViewById(R.id.cookingTimerDial);
                assertEquals(.5f, dial.remainingFraction(), .001f);
                a.findViewById(R.id.cookingTimerReset).performClick();
                assertEquals(1f, dial.remainingFraction(), .001f);
                prefs.edit().putLong(prefix+"_end", System.currentTimeMillis()-1000).apply();
                screens.cook(recipe);
                dial = a.findViewById(R.id.cookingTimerDial);
                assertEquals(0f, dial.remainingFraction(), .001f);
                assertTrue(dial.getContentDescription().toString().contains(a.getString(R.string.timer_finished)));
                a.findViewById(R.id.cookingTimerStart).performClick();
                assertTrue(prefs.getLong(prefix+"_end", 0) > System.currentTimeMillis());
                RecipeCatalog.Recipe untimed = new RecipeCatalog.Recipe("no_timer", "Salad", "Dinner", 5, 1,
                        "Salad", "tomato", "1 tomato", "Chop and serve.");
                screens.cook(untimed);
                assertEquals(View.GONE, a.findViewById(R.id.cookingTimerDial).getVisibility());
                RecipeCatalog.Recipe arabicDual = new RecipeCatalog.Recipe("arabic_dual_timer", "بيض", "Dinner", 2, 1,
                        "بيض", "eggs", "بيض", "اترك الخليط لمدة دقيقتين ثم قلّب.");
                screens.cook(arabicDual);
                dial = a.findViewById(R.id.cookingTimerDial);
                assertEquals(View.VISIBLE, dial.getVisibility());
                assertEquals(1f, dial.remainingFraction(), .001f);
                a.findViewById(R.id.cookingTimerStart).performClick();
                long remaining = prefs.getLong("cook_timer_arabic_dual_timer_0_end", 0) - System.currentTimeMillis();
                assertTrue(remaining > 115000 && remaining <= 120000);
                screens.close();prefs.edit().clear().apply();
            });
        }
    }
}
