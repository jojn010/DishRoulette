package com.example.dishroulette;

import android.graphics.Bitmap;
import android.view.*;
import android.widget.*;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;

import java.io.*;
import java.lang.reflect.Field;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Run against the emulator's configured window dimensions, in both languages.
 */
public class ResponsiveLayoutTest {
    private void capture(String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Thread.sleep(200);
        Bitmap b = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(b);
        File dir = InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("responsive");
        try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
            b.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        b.recycle();
    }

    private ScrollView scroll(View v) {
        if (v instanceof ScrollView)
            return (ScrollView) v;
        if (v instanceof ViewGroup)
            for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) {
                ScrollView s = scroll(((ViewGroup) v).getChildAt(i));
                if (s != null)
                    return s;
            }
        return null;
    }

    private void readable(View v, int width) {
        if (v.getVisibility() != View.VISIBLE)
            return;
        assertTrue("Horizontal overflow: " + v.getClass().getSimpleName() + " " + v.getWidth() + ">" + width,
            v.getWidth() <= width + 1);
        if (v instanceof TextView) {
            TextView t = (TextView) v;
            if (t.getText().length() > 0 && t.getLayout() != null) {
                for (int i = 0; i < t.getLayout().getLineCount(); i++)
                    assertEquals("Ellipsis: " + t.getText(), 0, t.getLayout().getEllipsisCount(i));
                assertTrue("Clipped: " + t.getText() + " layout=" + t.getLayout().getHeight() + " available=" +
                        (t.getHeight() - t.getCompoundPaddingTop() - t.getCompoundPaddingBottom()),
                    t.getLayout().getHeight() <=
                        t.getHeight() - t.getCompoundPaddingTop() - t.getCompoundPaddingBottom());
            }
        }
        if (v instanceof LinearLayout || v instanceof AdaptivePageFrame)
            for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) {
                View child = ((ViewGroup) v).getChildAt(i);
                if (child.getVisibility() == View.VISIBLE)
                    assertTrue(
                        "Child outside " + v.getClass().getSimpleName() + ": " + child.getClass().getSimpleName(),
                        child.getLeft() >= 0 && child.getRight() <= v.getWidth() + 1);
            }
        if (v instanceof ViewGroup)
            for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++)
                readable(((ViewGroup) v).getChildAt(i), width);
    }

    @Test
    public void bilingualHomeRecipeSavedSettingsAndCookingFitAvailableWindow() throws Exception {
        boolean captureOnly = "true".equals(InstrumentationRegistry.getArguments().getString("captureOnly"));
        android.content.Context c = InstrumentationRegistry.getInstrumentation().getTargetContext();
        for (String lang : new String[]{"en", "ar"}) {
            InterfaceSettings.prefs(c).edit().putString(InterfaceSettings.LANGUAGE, lang)
                .putString("spin_profile", "{}").putBoolean("reduce_motion", true).commit();
            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                capture(lang + "-home");
                scenario.onActivity(a -> {
                    if (!captureOnly) {
                        View home = a.findViewById(R.id.simpleHome);
                        readable(home, home.getWidth());
                        assertTrue("Home viewport squeezed by fixed bottom controls",
                            scroll(home).getHeight() >= 100 * home.getResources().getDisplayMetrics().density);
                    }
                });
                if (!captureOnly) {
                    if ("true".equals(InstrumentationRegistry.getArguments().getString("exerciseKeyboard"))) {
                        scenario.onActivity(a -> {
                            EditText input = a.findViewById(R.id.simpleIngredientInput);
                            input.requestFocus();
                            ((android.view.inputmethod.InputMethodManager) a.getSystemService(
                                android.content.Context.INPUT_METHOD_SERVICE)).showSoftInput(input,
                                android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                        });
                        Thread.sleep(800);
                        capture(lang + "-keyboard");
                        scenario.onActivity(a -> {
                            View input = a.findViewById(R.id.simpleIngredientInput);
                            android.graphics.Rect visible = new android.graphics.Rect();
                            assertTrue("Ingredient entry hidden by keyboard", input.getGlobalVisibleRect(visible));
                            assertTrue("Input clipped by keyboard", visible.height() >=
                                Math.min(input.getHeight(), 48 * input.getResources().getDisplayMetrics().density));
                            ((android.view.inputmethod.InputMethodManager) a.getSystemService(
                                android.content.Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
                                input.getWindowToken(), 0);
                            input.clearFocus();
                        });
                        Thread.sleep(400);
                    }
                    scenario.onActivity(a -> scroll(a.findViewById(R.id.simpleHome)).fullScroll(View.FOCUS_DOWN));
                    capture(lang + "-home-bottom");
                    scenario.onActivity(a -> {
                        View generate = a.findViewById(R.id.simpleGenerate);
                        generate.requestRectangleOnScreen(
                            new android.graphics.Rect(0, 0, generate.getWidth(), generate.getHeight()), true);
                    });
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                    scenario.onActivity(a -> {
                        View generate = a.findViewById(R.id.simpleGenerate);
                        android.graphics.Rect visible = new android.graphics.Rect();
                        assertTrue("Recipe action unreachable", generate.getGlobalVisibleRect(visible));
                        assertTrue("Recipe action clipped", visible.height() >= generate.getHeight() - 1);
                    });
                }
                RecipeCatalog.Recipe recipe = new RecipeCatalog.Recipe("responsive-fixture",
                    lang.equals("ar") ? "أرز بالبندورة للأصحاب" : "Tomato rice for everyone", "Dinner", 25, 6,
                    lang.equals("ar") ? "وجبة دافئة للمشاركة مع الأصحاب." : "A warm dish to share with friends.",
                    "rice", "3 cups rice\n6 tomatoes", lang.equals(
                    "ar") ? "١. اطبخ الأرز لمدة ١٥ دقيقة.\n\n٢. قدّمه دافئًا." : "1. Cook rice for 15 minutes.\n\n2. Serve warm.");
                recipe.language = lang;
                scenario.onActivity(a -> a.saveSharedRecipe(RecipeShareCodec.decode(RecipeShareCodec.encode(recipe))));
                capture(lang + "-recipe");
                scenario.onActivity(a -> {
                    if (!captureOnly) {
                        View result = a.findViewById(R.id.cardMealResult);
                        readable(result, a.findViewById(R.id.main).getWidth());
                        View page = (View) a.findViewById(R.id.tvMealTitle).getParent();
                        assertTrue("Recipe lines stretch across tablet", page.getWidth() <=
                            Math.min(result.getWidth(), 720 * a.getResources().getDisplayMetrics().density) + 1);
                        View parent = (View) page.getParent();
                        assertTrue("Recipe page not centered",
                            Math.abs(page.getLeft() * 2 + page.getWidth() - parent.getWidth()) <= 2);
                    }
                    a.findViewById(R.id.btnRecipe).performClick();
                });
                capture(lang + "-cooking");
                scenario.onActivity(a -> {
                    if (!captureOnly)
                        readable(a.findViewById(R.id.secondaryScreen), a.findViewById(R.id.main).getWidth());
                });
                scenario.recreate();
                scenario.onActivity(a -> {
                    try {
                        Field f = MainActivity.class.getDeclaredField("kitchenScreens");
                        f.setAccessible(true);
                        assertEquals("cooking", ((KitchenScreens) f.get(a)).screen());
                    } catch (Exception e) {
                        throw new AssertionError(e);
                    }
                    a.showSavedRecipes();
                });
                capture(lang + "-saved");
                scenario.onActivity(a -> {
                    if (!captureOnly)
                        readable(a.findViewById(R.id.secondaryScreen), a.findViewById(R.id.main).getWidth());
                    a.showSettings();
                });
                capture(lang + "-settings");
                scenario.onActivity(a -> {
                    if (!captureOnly)
                        readable(a.findViewById(R.id.secondaryScreen), a.findViewById(R.id.main).getWidth());
                });
                if ("true".equals(InstrumentationRegistry.getArguments().getString("exercisePreferences"))) {
                    scenario.onActivity(a -> a.findViewById(R.id.settingsRecipePreferences).performClick());
                    capture(lang + "-preferences");
                    androidx.test.espresso.Espresso.onView(
                            androidx.test.espresso.matcher.ViewMatchers.withId(android.R.id.button1))
                        .perform(androidx.test.espresso.action.ViewActions.click());
                    scenario.onActivity(a -> a.findViewById(R.id.settingsRecipePreferences).performClick());
                    androidx.test.espresso.Espresso.onView(
                            androidx.test.espresso.matcher.ViewMatchers.withText(R.string.cancel))
                        .perform(androidx.test.espresso.action.ViewActions.click());
                }
            }
        }
    }
}
