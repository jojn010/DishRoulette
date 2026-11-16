package com.example.dishroulette;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Changing a preference must not destroy the Home control a keyboard user is focused on.
 */
public class HomeFocusStabilityTest {
    private TextView findText(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText()))
            return (TextView) view;
        if (view instanceof ViewGroup)
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                TextView found = findText(((ViewGroup) view).getChildAt(i), text);
                if (found != null)
                    return found;
            }
        return null;
    }

    @Test
    public void servingAndGoalUpdatesPreserveFocusedControlsInBothLanguages() {
        for (String language : new String[]{"en", "ar"}) {
            InterfaceSettings.prefs(InstrumentationRegistry.getInstrumentation().getTargetContext()).edit()
                .putString(InterfaceSettings.LANGUAGE, language).putString("loaded_ammunition", "[\"Rice\"]")
                .putString("simple_input_draft", "").remove("simple_input_edit_slot").putString("spin_profile", "{}")
                .commit();
            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                scenario.onActivity(activity -> {
                    View home = activity.findViewById(R.id.simpleHome);
                    TextView rice = findText(home, "Rice");
                    assertNotNull(rice);
                    assertTrue(rice.requestFocusFromTouch());
                    activity.chooseServings(3);
                    assertTrue("Ingredient focus lost after changing servings", rice.isFocused());
                    TextView goal = findText(home,
                        UiLabels.option(activity, "Cutting") + "\n" + activity.getString(R.string.cut_description));
                    assertNotNull(goal);
                    assertTrue(goal.requestFocusFromTouch());
                    activity.chooseGoal("Cutting");
                    assertTrue("Goal focus lost after selection", goal.isFocused());
                    assertEquals("Cutting", activity.profile().goal);
                });
            }
        }
    }
}
