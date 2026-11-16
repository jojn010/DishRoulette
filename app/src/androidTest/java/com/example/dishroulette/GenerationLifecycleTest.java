package com.example.dishroulette;

import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Request;
import okio.Timeout;

import org.junit.Before;
import org.junit.Test;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.junit.Assert.*;

/**
 * Real native controls with delayed transport replies, including delivery after cancellation.
 */
public class GenerationLifecycleTest {
    @Before
    public void defaults() {
        InterfaceSettings.prefs(InstrumentationRegistry.getInstrumentation().getTargetContext()).edit()
            .putString(InterfaceSettings.LANGUAGE, "en").putString("spin_profile", "{}")
            .putString("loaded_ammunition", "[]").putString("simple_input_draft", "")
            .putBoolean("reduce_motion", true).commit();
    }

    private void installApi(MainActivity activity, List<PendingReply> calls) {
        try {
            Field api = MainActivity.class.getDeclaredField("recipeApi");
            api.setAccessible(true);
            api.set(activity, (RecipeApi) request -> {
                PendingReply reply = new PendingReply(request);
                calls.add(reply);
                return reply;
            });
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private void add(MainActivity activity, String ingredient) {
        EditText input = activity.findViewById(R.id.simpleIngredientInput);
        input.setText(ingredient);
        input.onEditorAction(EditorInfo.IME_ACTION_DONE);
    }

    @Test
    public void cancelThenRetryIgnoresTheOldReplyAndKeepsPantry() throws Exception {
        List<PendingReply> calls = new ArrayList<>();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> {
                installApi(a, calls);
                add(a, "Rice");
                add(a, "Tomato");
                a.findViewById(R.id.simpleGenerate).performClick();
                assertFalse(a.findViewById(R.id.simpleIngredientInput).isEnabled());
                assertEquals(2, calls.get(0).input.ingredients.size());
                a.findViewById(R.id.simpleGenerate).performClick();
                assertTrue(calls.get(0).isCanceled());
                assertTrue(a.findViewById(R.id.simpleIngredientInput).isEnabled());
                a.findViewById(R.id.simpleGenerate).performClick();
                calls.get(0).fail();
                assertFalse(a.findViewById(R.id.simpleIngredientInput).isEnabled());
                assertEquals(View.GONE, a.findViewById(R.id.cardMealResult).getVisibility());
                calls.get(1).succeed();
            });
            Thread.sleep(400);
            scenario.onActivity(a -> {
                assertEquals(View.VISIBLE, a.findViewById(R.id.cardMealResult).getVisibility());
                assertTrue(a.findViewById(R.id.simpleIngredientInput).isEnabled());
                assertTrue(InterfaceSettings.prefs(a).getString("loaded_ammunition", "").contains("Rice"));
            });
        }
    }

    @Test
    public void rotatingCancelsRequestAndRestoresEditablePantry() {
        List<PendingReply> calls = new ArrayList<>();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> {
                installApi(a, calls);
                add(a, "Rice");
                a.findViewById(R.id.simpleGenerate).performClick();
            });
            scenario.recreate();
            scenario.onActivity(a -> {
                assertTrue(calls.get(0).isCanceled());
                calls.get(0).fail();
                assertTrue(a.findViewById(R.id.simpleIngredientInput).isEnabled());
                assertEquals(View.GONE, a.findViewById(R.id.cardMealResult).getVisibility());
                assertTrue(InterfaceSettings.prefs(a).getString("loaded_ammunition", "").contains("Rice"));
            });
        }
    }

    private static final class PendingReply implements Call<RecipeSuggestion> {
        final RecipeApi.Request input;
        Callback<RecipeSuggestion> callback;
        boolean canceled, executed;

        PendingReply(RecipeApi.Request input) {
            this.input = input;
        }

        public void enqueue(Callback<RecipeSuggestion> callback) {
            this.callback = callback;
            executed = true;
        }

        void fail() {
            callback.onFailure(this, new IOException("delayed test failure"));
        }

        void succeed() {
            new NativeAppFlowTest.LocalReply(input).enqueue(new Callback<RecipeSuggestion>() {
                public void onResponse(Call<RecipeSuggestion> call, Response<RecipeSuggestion> response) {
                    callback.onResponse(PendingReply.this, response);
                }

                public void onFailure(Call<RecipeSuggestion> call, Throwable error) {
                    callback.onFailure(PendingReply.this, error);
                }
            });
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
            return executed;
        }

        public Call<RecipeSuggestion> clone() {
            return new PendingReply(input);
        }

        public Request request() {
            return new Request.Builder().url("https://example.invalid/test").build();
        }

        public Timeout timeout() {
            return Timeout.NONE;
        }
    }
}
