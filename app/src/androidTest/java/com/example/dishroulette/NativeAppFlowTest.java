package com.example.dishroulette;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.view.inputmethod.EditorInfo;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.util.*;
import java.io.*;

import retrofit2.*;
import okhttp3.Request;
import okio.Timeout;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class NativeAppFlowTest {
    private SharedPreferences prefs() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext()
            .getSharedPreferences(InterfaceSettings.STORE, 0);
    }

    @Before
    public void defaults() {
        prefs().edit().putString(InterfaceSettings.MODE, "simple").putString(InterfaceSettings.LANGUAGE, "en")
            .putString("spin_profile", "{}").putString("loaded_ammunition", "[]").putString("selected_meal", "Any meal")
            .putString("simple_input_draft", "").putBoolean("reduce_motion", false).commit();
    }

    private Object field(Object target, String name) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private PantryController pantryController(MainActivity a) {
        return (PantryController) field(a, "pantryController");
    }

    private void api(MainActivity a, RecipeApi api) {
        try {
            Field f = MainActivity.class.getDeclaredField("recipeApi");
            f.setAccessible(true);
            f.set(a, api);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private void add(MainActivity a, String name) {
        EditText input = a.findViewById(R.id.simpleIngredientInput);
        input.setText(name);
        input.onEditorAction(EditorInfo.IME_ACTION_DONE);
    }

    private TextView text(View root, String value) {
        if (root instanceof TextView && ((TextView) root).getText().toString().equals(value))
            return (TextView) root;
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) root;
            for (int i = 0; i < g.getChildCount(); i++) {
                TextView found = text(g.getChildAt(i), value);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    private void capture(String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Thread.sleep(450);
        Bitmap b = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        File dir = InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("v1.1.0");
        assertNotNull(dir);
        try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
            assertNotNull(b);
            b.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        b.recycle();
    }

    @Test
    public void simpleEnglishEditsValidatesGeneratesSavesAndCooksWithoutOpenGl() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> {
                assertTrue(pantryController(a) instanceof PantryController);
                add(a, "Eggs");
                add(a, "Rice");
                add(a, "rice");
                assertEquals(2, pantryController(a).entries().count());
                assertNotNull(((EditText) a.findViewById(R.id.simpleIngredientInput)).getError());
                ((EditText) a.findViewById(R.id.simpleIngredientInput)).setText("");
            });
            capture("01-simple-en-home");
            scenario.onActivity(a -> {
                text(a.findViewById(R.id.simpleHome), "Eggs").performClick();
                add(a, "Tomato");
                assertEquals(2, pantryController(a).entries().count());
                assertFalse(pantryController(a).entries().ingredients().contains("Eggs"));
                api(a, r -> {
                    assertEquals("en", r.language);
                    return new LocalReply(r);
                });
                a.findViewById(R.id.simpleGenerate).performClick();
            });
            Thread.sleep(700);
            scenario.onActivity(a -> {
                assertEquals(View.VISIBLE, a.findViewById(R.id.cardMealResult).getVisibility());
                a.findViewById(R.id.btnLike).performClick();
            });
            capture("02-simple-en-recipe");
            scenario.onActivity(a -> {
                a.findViewById(R.id.btnRecipe).performClick();
                assertEquals("cooking", ((KitchenScreens) field(a, "kitchenScreens")).screen());
            });
            capture("03-simple-en-cooking");
            scenario.onActivity(MainActivity::showSavedRecipes);
            capture("04-simple-en-saved");
        }
    }

    @Test
    public void editingSurvivesRotationAndUpdatesTheOriginalIngredient() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> {
                add(a, "Rice");
                add(a, "Tomato");
                text(a.findViewById(R.id.simpleHome), "Rice").performClick();
                ((EditText) a.findViewById(R.id.simpleIngredientInput)).setText("Tofu");
            });
            scenario.recreate();
            scenario.onActivity(a -> {
                add(a, "Tofu");
                assertEquals(2, pantryController(a).entries().count());
                assertTrue(pantryController(a).entries().ingredients().contains("Tofu"));
                assertFalse(pantryController(a).entries().ingredients().contains("Rice"));
                assertFalse(prefs().contains("simple_input_edit_slot"));
            });
        }
    }

    @Test
    public void arabicSimpleAcceptsArabicAndEnglishIngredientsAndShowsArabicRecipe() throws Exception {
        prefs().edit().putString(InterfaceSettings.LANGUAGE, "ar").commit();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> {
                assertEquals(View.LAYOUT_DIRECTION_RTL, a.findViewById(R.id.main).getLayoutDirection());
                assertEquals("شو بدك تطبخ اليوم؟", a.getString(R.string.simple_hero));
                add(a, "أرز");
                add(a, "Tomato");
                assertTrue(pantryController(a).entries().ingredients().contains("أرز"));
            });
            capture("05-simple-ar-home");
            scenario.onActivity(a -> {
                api(a, r -> {
                    assertEquals("ar", r.language);
                    assertTrue(r.ingredients.contains("rice"));
                    return new LocalReply(r);
                });
                a.findViewById(R.id.simpleGenerate).performClick();
            });
            Thread.sleep(700);
            scenario.onActivity(a -> {
                assertEquals("أرز بالبندورة", ((TextView) a.findViewById(R.id.tvMealTitle)).getText().toString());
                a.findViewById(R.id.btnLike).performClick();
            });
            capture("06-simple-ar-recipe");
            scenario.onActivity(a -> a.findViewById(R.id.btnRecipe).performClick());
            capture("07-simple-ar-cooking");
            scenario.recreate();
            scenario.onActivity(a -> {
                assertEquals("ar", InterfaceSettings.language(a));
                assertEquals("cooking", ((KitchenScreens) field(a, "kitchenScreens")).screen());
                assertEquals(2, pantryController(a).entries().count());
            });
        }
    }

    @Test
    public void settingsChangeLanguageAndKeepIngredientsAndFavorites() throws Exception {
        final Set<String>[] saved = new Set[]{null};
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> {
                add(a, "أرز");
                add(a, "Tomato");
                saved[0] = new HashSet<>(prefs().getStringSet("saved_recipes", Collections.emptySet()));
                a.showSettings();
                assertEquals(0, a.getResources().getIdentifier("settingsAdvanced", "id", a.getPackageName()));
                assertEquals(0, a.getResources().getIdentifier("settingsSimple", "id", a.getPackageName()));
                assertEquals(0, a.getResources().getIdentifier("settingsGemini", "id", a.getPackageName()));
                a.findViewById(R.id.settingsArabic).performClick();
            });
            Thread.sleep(1000);
            scenario.onActivity(a -> {
                assertEquals("ar", InterfaceSettings.language(a));
                assertEquals(2, pantryController(a).entries().count());
                assertEquals(saved[0], prefs().getStringSet("saved_recipes", Collections.emptySet()));
            });
            scenario.recreate();
            scenario.onActivity(a -> {
                a.findViewById(R.id.settingsEnglish).performClick();
            });
            Thread.sleep(900);
            scenario.onActivity(a -> {
                assertEquals("en", a.getResources().getConfiguration().getLocales().get(0).getLanguage());
                assertEquals(2, pantryController(a).entries().count());
            });
        }
    }

    @Test
    public void newInstallAndLegacyInstallsUseSimpleWithoutChangingLanguage() {
        android.content.Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        android.content.Context clean = new android.content.ContextWrapper(base) {
            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                return base.getSharedPreferences("dual_interface_isolated_test", mode);
            }
        };
        SharedPreferences p = clean.getSharedPreferences("", 0);
        p.edit().clear().commit();
        InterfaceSettings.migrateLegacyMode(clean);
        assertEquals("simple", p.getString(InterfaceSettings.MODE, ""));
        p.edit().putString(InterfaceSettings.MODE, "advanced").putString(InterfaceSettings.LANGUAGE, "ar").commit();
        InterfaceSettings.migrateLegacyMode(clean);
        assertEquals("simple", p.getString(InterfaceSettings.MODE, ""));
        assertEquals("ar", InterfaceSettings.language(clean));
        p.edit().clear().putString("spin_profile", "{}").commit();
        InterfaceSettings.migrateLegacyMode(clean);
        assertEquals("simple", p.getString(InterfaceSettings.MODE, ""));
        p.edit().clear().commit();
    }

    @Test
    public void legacyAdvancedChoiceMigratesAndSupportsArabicRecipesAndSurprise() throws Exception {
        prefs().edit().putString(InterfaceSettings.MODE, "advanced").putString(InterfaceSettings.LANGUAGE, "ar")
            .putBoolean("reduce_motion", true).commit();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> {
                pantryController(a).restore(Arrays.asList("أرز", "Tomato"), 0);
                api(a, r -> {
                    assertEquals("ar", r.language);
                    return new LocalReply(r);
                });
                a.findViewById(R.id.simpleGenerate).performClick();
            });
            Thread.sleep(1200);
            scenario.onActivity(a -> {
                assertEquals(View.VISIBLE, a.findViewById(R.id.cardMealResult).getVisibility());
                assertEquals("أرز بالبندورة", ((TextView) a.findViewById(R.id.tvMealTitle)).getText().toString());
            });
            capture("14-advanced-ar-recipe");
        }
        prefs().edit().putString(InterfaceSettings.MODE, "simple").commit();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> {
                a.chooseGenerationMode("discover");
                api(a, r -> {
                    assertEquals("discover", r.mode);
                    assertTrue(r.ingredients.isEmpty());
                    assertEquals("ar", r.language);
                    return new LocalReply(r);
                });
                a.findViewById(R.id.simpleGenerate).performClick();
            });
            Thread.sleep(700);
            scenario.onActivity(a -> {
                assertEquals(View.VISIBLE, a.findViewById(R.id.cardMealResult).getVisibility());
                assertEquals(2, pantryController(a).entries().count());
            });
            capture("15-simple-ar-surprise");
        }
    }

    @Test
    public void liveEnglishAndArabicRecipes() throws Exception {
        org.junit.Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("liveRecipe")));
        for (String language : Arrays.asList("en", "ar")) {
            prefs().edit().putString(InterfaceSettings.LANGUAGE, language).putString("loaded_ammunition", "[]")
                .putString("simple_input_draft", "").commit();
            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                scenario.onActivity(a -> {
                    add(a, "أرز");
                    add(a, "Tomato");
                    a.findViewById(R.id.simpleGenerate).performClick();
                });
                final boolean[] ready = {false};
                for (int n = 0; n < 70 && !ready[0]; n++) {
                    Thread.sleep(1000);
                    scenario.onActivity(
                        a -> ready[0] = a.findViewById(R.id.cardMealResult).getVisibility() == View.VISIBLE);
                }
                capture("live-" + language + "-status");
                assertTrue("Live recipe in " + language, ready[0]);
                scenario.onActivity(a -> {
                    String title = ((TextView) a.findViewById(R.id.tvMealTitle)).getText().toString();
                    assertFalse(title.isEmpty());
                    if (language.equals("ar"))
                        assertTrue(title.matches(".*[\\u0600-\\u06FF].*"));
                    a.findViewById(R.id.btnLike).performClick();
                });
                capture("12-live-" + language + "-recipe");
                scenario.onActivity(a -> a.findViewById(R.id.btnRecipe).performClick());
                capture("13-live-" + language + "-cooking");
            }
        }
    }

    static final class LocalReply implements Call<RecipeSuggestion> {
        final RecipeApi.Request request;
        boolean canceled;

        LocalReply(RecipeApi.Request r) {
            request = r;
        }

        public void enqueue(Callback<RecipeSuggestion> callback) {
            RecipeSuggestion s = new RecipeSuggestion();
            boolean ar = request.language.equals("ar");
            s.title = ar ? "أرز بالبندورة" : "Tomato rice";
            s.description = ar ? "وجبة دافئة وبسيطة من الأرز والبندورة." : "A warm, simple tomato rice dish.";
            s.meal = "Dinner";
            s.cuisine = "Italian";
            s.minutes = 20;
            s.servings = request.servings;
            s.ingredients = ar ? Arrays.asList("١ كوب أرز", "٢ حبة بندورة") : Arrays.asList("1 cup rice", "2 tomatoes");
            s.pantryUsed = request.ingredients;
            s.additionalIngredients = Collections.emptyList();
            s.steps = ar ? Arrays.asList("اغسل الأرز وقطّع البندورة.", "اطبخ الأرز لمدة ١٥ دقيقة.") : Arrays.asList(
                "Rinse the rice and chop tomatoes.", "Cook the rice for 15 minutes.");
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (!canceled)
                    callback.onResponse(this, Response.success(s));
            }, 100);
        }

        public Response<RecipeSuggestion> execute() {
            throw new UnsupportedOperationException();
        }

        public void cancel() {
            canceled = true;
        }

        public boolean isCanceled() {
            return canceled;
        }

        public boolean isExecuted() {
            return false;
        }

        public Call<RecipeSuggestion> clone() {
            return new LocalReply(request);
        }

        public Request request() {
            return new Request.Builder().url("http://localhost/test").build();
        }

        public Timeout timeout() {
            return Timeout.NONE;
        }
    }
}
