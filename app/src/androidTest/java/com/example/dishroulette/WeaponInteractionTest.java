package com.example.dishroulette;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.Request;
import okio.Timeout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class WeaponInteractionTest {
    @Before public void cleanProfile() {
        InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences("dishroulette_ui",0)
                .edit().putString("spin_profile","{}").commit();
    }
    private WeaponInputView weapon(MainActivity activity) { return activity.findViewById(R.id.tabletop); }
    private void type(MainActivity a,String text,boolean enter) {
        EditText editor=a.findViewById(R.id.ammunitionEditor);
        editor.requestFocus();
        InputConnection connection=editor.onCreateInputConnection(new EditorInfo());
        editor.selectAll();connection.commitText(text,1);
        if(enter) connection.performEditorAction(EditorInfo.IME_ACTION_NEXT);
    }
    private void waitForLoad() throws Exception { Thread.sleep(800); }
    @Test public void revolverSixChambersEditingPagingAndRecreation() throws Exception {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            String[] ingredients={"Chicken","Rice","Tomato","Onion","Garlic","Lemon"};
            for(String name:ingredients){scenario.onActivity(a -> type(a,name,true));waitForLoad();}
            scenario.onActivity(a -> {
                assertEquals(6,weapon(a).magazine().count());assertEquals(1,weapon(a).magazine().page());
                // Repeated Enter on the new blank chamber must not add or advance.
                type(a,"",true);assertEquals(6,weapon(a).magazine().active());
                weapon(a).select(0);type(a,"Tofu",true);
            });waitForLoad();
            scenario.onActivity(a -> {assertEquals(6,weapon(a).magazine().count());assertFalse(weapon(a).magazine().ingredients().contains("Chicken"));weapon(a).page(1);type(a,"Pepper",true);});waitForLoad();
            scenario.recreate();
            scenario.onActivity(a -> {assertEquals(7,weapon(a).magazine().count());weapon(a).select(0);assertEquals("Tofu",weapon(a).magazine().text());weapon(a).clearChamber();assertEquals(6,weapon(a).magazine().count());});
        }
    }
    @Test public void eachLongGunAdvancesAndKeepsText() throws Exception {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> {a.findViewById(R.id.btnCut).performClick();type(a,"Chicken",true);assertTrue(weapon(a).isLoading());});waitForLoad();
            scenario.onActivity(a -> {assertEquals(1,weapon(a).magazine().active());a.findViewById(R.id.btnBulk).performClick();type(a,"Rice",true);assertTrue(weapon(a).isLoading());});waitForLoad();
            scenario.onActivity(a -> {assertEquals(2,weapon(a).magazine().active());assertEquals(2,weapon(a).magazine().count());});
        }
    }
    @Test public void setupKeepsSourceAndMealWhenApplyingAndReopening() throws Exception {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> {
                a.findViewById(R.id.btnFilters).performClick();
                try {
                    Field f=MainActivity.class.getDeclaredField("binding");f.setAccessible(true);
                    com.example.dishroulette.databinding.ActivityMainBinding b=(com.example.dishroulette.databinding.ActivityMainBinding)f.get(a);
                    ((com.google.android.material.chip.Chip)b.modeChips.getChildAt(1)).setChecked(true);
                    ((com.google.android.material.chip.Chip)b.mealChips.getChildAt(3)).setChecked(true);
                } catch(Exception e) { throw new AssertionError(e); }
            });
            scenario.onActivity(a -> clickDialogButton(a, android.R.id.button1));
            scenario.onActivity(a -> {
                SpinProfile p=SpinProfile.load(a.getSharedPreferences("dishroulette_ui",0),new com.google.gson.Gson());
                assertEquals("discover",p.mode);
                a.findViewById(R.id.btnFilters).performClick();
            });
            scenario.onActivity(a -> clickDialogButton(a, android.R.id.button2));
        }
    }
    private void clickDialogButton(MainActivity activity, int id) {
        try {
            Field f=MainActivity.class.getDeclaredField("binding");f.setAccessible(true);
            com.example.dishroulette.databinding.ActivityMainBinding b=(com.example.dishroulette.databinding.ActivityMainBinding)f.get(activity);
            View button=b.settingsDrawer.getRootView().findViewById(id);
            assertNotNull(button);assertTrue(button.performClick());
        } catch(Exception e) { throw new AssertionError(e); }
    }
    private void api(MainActivity a,RecipeApi value) {
        try{Field field=MainActivity.class.getDeclaredField("recipeApi");field.setAccessible(true);field.set(a,value);}catch(Exception e){throw new AssertionError(e);}
    }
    @Test public void earlyRecipeWaitsForSmokeAndSendsCompleteLoadout() throws Exception {
        AtomicReference<RecipeApi.Request> sent=new AtomicReference<>();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> {
                weapon(a).restore(Arrays.asList("Chicken","Rice","Tomato","Onion","Garlic","Lemon","Pepper"),6);
                a.findViewById(R.id.btnCut).performClick();
                api(a,request->{sent.set(request);return new Reply(request,100,false);});
                a.findViewById(R.id.btnSpin).performClick();
            });Thread.sleep(600);
            scenario.onActivity(a->{assertEquals(7,sent.get().ingredients.size());assertEquals("Cutting",sent.get().goal);assertEquals(View.GONE,a.findViewById(R.id.cardMealResult).getVisibility());assertFalse(a.findViewById(R.id.btnSpin).isEnabled());});
            Thread.sleep(2600);
            scenario.onActivity(a->{assertEquals(View.VISIBLE,a.findViewById(R.id.cardMealResult).getVisibility());assertEquals("Test kitchen",((TextView)a.findViewById(R.id.tvMealTitle)).getText().toString());assertTrue(a.findViewById(R.id.btnSpin).isEnabled());});
        }
    }
    @Test public void slowFailureKeepsInputLockedThenAllowsRetry() throws Exception {
        AtomicReference<Reply> pending=new AtomicReference<>();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a->{type(a,"Rice",false);api(a,r->{Reply reply=new Reply(r,10000,true);pending.set(reply);return reply;});a.findViewById(R.id.btnSpin).performClick();});
            Thread.sleep(3200);
            scenario.onActivity(a->{assertFalse(a.findViewById(R.id.btnSpin).isEnabled());assertTrue(((TextView)a.findViewById(R.id.tvSceneHelp)).getText().toString().contains("WAITING"));pending.get().deliver();});
            scenario.onActivity(a->{assertTrue(a.findViewById(R.id.btnSpin).isEnabled());assertEquals(1,weapon(a).magazine().count());});
        }
    }
    static final class Reply implements Call<RecipeSuggestion> {
        final RecipeApi.Request request;final long delay;final boolean fail;boolean cancelled,executed,delivered;Callback<RecipeSuggestion> callback;
        Reply(RecipeApi.Request request,long delay,boolean fail){this.request=request;this.delay=delay;this.fail=fail;}
        @Override public void enqueue(Callback<RecipeSuggestion> callback){executed=true;this.callback=callback;new Handler(Looper.getMainLooper()).postDelayed(this::deliver,delay);}
        void deliver(){
            if(cancelled || delivered || callback==null)return;
            delivered=true;
            if(fail){callback.onFailure(this,new java.io.IOException("test offline"));return;}
            RecipeSuggestion r=new RecipeSuggestion();r.title="Test kitchen";r.description="A deterministic test recipe.";r.meal="Dinner";r.minutes=20;r.servings=2;r.ingredients=request.ingredients;r.pantryUsed=request.ingredients;r.additionalIngredients=Collections.emptyList();r.steps=Collections.singletonList("Cook thoroughly.");callback.onResponse(this,Response.success(r));
        }
        @Override public Response<RecipeSuggestion> execute(){throw new UnsupportedOperationException();}
        @Override public void cancel(){cancelled=true;}@Override public boolean isCanceled(){return cancelled;}
        @Override public boolean isExecuted(){return executed;}@Override public Call<RecipeSuggestion> clone(){return new Reply(request,delay,fail);}
        @Override public Request request(){return new Request.Builder().url("http://localhost/test").build();}
        @Override public Timeout timeout(){return Timeout.NONE;}
    }
}
