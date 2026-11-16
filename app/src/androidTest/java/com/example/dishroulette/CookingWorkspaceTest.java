package com.example.dishroulette;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Exercise the actual cooking workspace with full nutrition, RTL and persisted progress.
 */
public class CookingWorkspaceTest {
    private KitchenScreens screens(MainActivity a) {
        try {
            Field f = MainActivity.class.getDeclaredField("kitchenScreens");
            f.setAccessible(true);
            return (KitchenScreens) f.get(a);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private TextView text(View v, String label) {
        if (v instanceof TextView && label.contentEquals(((TextView) v).getText()))
            return (TextView) v;
        if (v instanceof ViewGroup)
            for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) {
                TextView t = text(((ViewGroup) v).getChildAt(i), label);
                if (t != null)
                    return t;
            }
        return null;
    }

    private void capture(String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Thread.sleep(250);
        Bitmap b = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(b);
        File dir =
            InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("cooking-workspace");
        try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
            b.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        b.recycle();
    }

    private void readable(View v) {
        if (v.getVisibility() != View.VISIBLE)
            return;
        if (v instanceof TextView) {
            TextView t = (TextView) v;
            if (t.getLayout() != null) {
                for (int i = 0; i < t.getLayout().getLineCount(); i++)
                    assertEquals("Ellipsis: " + t.getText(), 0, t.getLayout().getEllipsisCount(i));
                assertTrue("Clipping: " + t.getText(), t.getLayout().getHeight() <=
                    t.getHeight() - t.getCompoundPaddingTop() - t.getCompoundPaddingBottom());
            }
            if (t instanceof android.widget.Button || t instanceof CheckBox)
                assertTrue("Touch target", t.getHeight() >= 48 * t.getResources().getDisplayMetrics().density);
        }
        if (v instanceof ViewGroup)
            for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++)
                readable(((ViewGroup) v).getChildAt(i));
    }

    @Test
    public void bilingualStepCardNutritionAndProgress() throws Exception {
        boolean before = "true".equals(InstrumentationRegistry.getArguments().getString("captureOnly"));
        for (String lang : new String[]{"en", "ar"}) {
            boolean ar = lang.equals("ar");
            InterfaceSettings.prefs(InstrumentationRegistry.getInstrumentation().getTargetContext()).edit()
                .putString(InterfaceSettings.LANGUAGE, lang).putBoolean("reduce_motion", true)
                .putString("spin_profile", "{}").commit();
            RecipeCatalog.Recipe recipe =
                new RecipeCatalog.Recipe("workspace-" + lang, ar ? "أرز بالبندورة" : "Tomato rice", "Dinner", 20, 2, "",
                    "rice", ar ? "كوب أرز\nحبتان بندورة" : "1 cup rice\n2 tomatoes",
                    ar ? "١. اغسل الأرز وقطّع البندورة.\n\n٢. اطبخ الأرز لمدة ١٥ دقيقة." : "1. Rinse the rice and chop tomatoes.\n\n2. Cook the rice for 15 minutes.");
            recipe.language = lang;
            recipe.caloriesPerServing = 320;
            recipe.proteinGramsPerServing = 12.5;
            recipe.carbohydratesGramsPerServing = 45.0;
            recipe.fatGramsPerServing = 9.0;
            recipe.fiberGramsPerServing = 4.0;
            recipe.sugarGramsPerServing = 3.0;
            recipe.sodiumMilligramsPerServing = 300.0;
            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                scenario.onActivity(a -> {
                    InterfaceSettings.prefs(a).edit().remove("cook_step_" + recipe.id)
                        .remove("cook_done_" + recipe.id + "_0").commit();
                    screens(a).cook(recipe);
                });
                capture(lang + "-step");
                if (before)
                    continue;
                scenario.onActivity(a -> {
                    View root = a.findViewById(R.id.secondaryScreen);
                    readable(root);
                    assertEquals(ar ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR, root.getLayoutDirection());
                    TextView nutrition = text(root, a.getString(R.string.saved_show_nutrition));
                    assertNotNull(nutrition);
                    nutrition.performClick();
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(a -> {
                    View root = a.findViewById(R.id.secondaryScreen);
                    readable(root);
                    assertNotNull(text(root, a.getString(R.string.saved_hide_nutrition)));
                    TextView details = root.findViewWithTag("cooking-nutrition");
                    assertEquals(View.VISIBLE, details.getVisibility());
                    assertTrue(details.getText().toString().contains(a.getString(R.string.nutrition_variation)));
                    assertTrue(details.getText().toString().contains("~~~"));
                    text(root, a.getString(R.string.saved_hide_nutrition)).performClick();
                    ((CheckBox) text(root, a.getString(R.string.step_complete))).setChecked(true);
                    text(root, a.getString(R.string.next_step)).performClick();
                });
                capture(lang + "-timer");
                scenario.onActivity(a -> {
                    assertEquals(View.VISIBLE, a.findViewById(R.id.cookingTimerDial).getVisibility());
                    readable(a.findViewById(R.id.secondaryScreen));
                    screens(a).close();
                    screens(a).cook(recipe);
                    assertEquals(1, InterfaceSettings.prefs(a).getInt("cook_step_" + recipe.id, -1));
                    assertTrue(InterfaceSettings.prefs(a).getBoolean("cook_done_" + recipe.id + "_0", false));
                });
            }
        }
    }
}
