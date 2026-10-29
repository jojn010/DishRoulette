package com.example.dishroulette;

import android.content.SharedPreferences;
import android.view.View;
import android.widget.FrameLayout;
import androidx.test.core.app.ActivityScenario;
import org.junit.Test;
import static org.junit.Assert.*;

public class CookingTimerDialTest {
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
                screens.close();prefs.edit().clear().apply();
            });
        }
    }
}
