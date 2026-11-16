package com.example.dishroulette;

import android.view.View;

import androidx.test.core.app.ActivityScenario;

import org.junit.Test;

import static org.junit.Assert.*;

public class CookingScreenAwakeTest {
    @Test
    public void awakeOnlyWhileCookingAndClearedOnExitOrDispose() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                View host = activity.findViewById(R.id.secondaryScreen);
                KitchenScreens screens = new KitchenScreens(activity, (android.widget.FrameLayout) host,
                    activity.getSharedPreferences("awake_test", 0), new KitchenScreens.Actions() {
                    public void open(RecipeCatalog.Recipe recipe) {
                    }

                    public void remove(RecipeCatalog.Recipe recipe) {
                    }
                });
                RecipeCatalog.Recipe recipe = new RecipeCatalog.Recipe("awake_test", "Eggs", "Breakfast",
                    10, 1, "Eggs", "eggs", "2 eggs", "Cook eggs.");
                screens.cook(recipe);
                assertTrue(host.getKeepScreenOn());
                screens.settings();
                assertFalse(host.getKeepScreenOn());
                screens.cook(recipe);
                screens.saved(java.util.Collections.emptyList());
                assertFalse(host.getKeepScreenOn());
                screens.cook(recipe);
                screens.close();
                assertFalse(host.getKeepScreenOn());
                screens.cook(recipe);
                screens.dispose();
                assertFalse(host.getKeepScreenOn());
                screens.close();
            });
        }
    }
}
