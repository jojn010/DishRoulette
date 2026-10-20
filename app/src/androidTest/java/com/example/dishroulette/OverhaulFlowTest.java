package com.example.dishroulette;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.view.inputmethod.EditorInfo;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

/** Real activity/GL capture, using deterministic responses only inside this test APK. */
@RunWith(AndroidJUnit4.class)
public class OverhaulFlowTest {
    private SharedPreferences prefs(){return InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences("dishroulette_ui",0);}
    @Before public void defaults(){SharedPreferences.Editor fixture=prefs().edit();for(String key:prefs().getAll().keySet())if(key.startsWith("cook_")&&key.contains("overhaul_test_recipe"))fixture.remove(key);fixture.commit();prefs().edit().remove("loaded_ammunition").remove("loaded_chamber").putString("selected_meal","Any meal").putBoolean("reduce_motion",false).putString(InterfaceSettings.MODE,"advanced").putString(InterfaceSettings.LANGUAGE,"en").putString("spin_profile","{}").commit();}
    private WeaponInputView weapon(MainActivity a){return a.findViewById(R.id.tabletop);}
    private KitchenScreens screens(MainActivity a){try{Field f=MainActivity.class.getDeclaredField("kitchenScreens");f.setAccessible(true);return (KitchenScreens)f.get(a);}catch(Exception e){throw new AssertionError(e);}}
    private void capture(String name)throws Exception{
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();Thread.sleep(450);
        Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(bitmap);
        File dir=InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("v1.0.1");assertNotNull(dir);
        try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
    }
    private TextView text(View root,String value){
        if(root instanceof TextView && ((TextView)root).getText().toString().equals(value))return (TextView)root;
        if(root instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)root;for(int i=0;i<group.getChildCount();i++){TextView found=text(group.getChildAt(i),value);if(found!=null)return found;}}
        return null;
    }
    private RecipeCatalog.Recipe recipe(){
        RecipeCatalog.Recipe r=new RecipeCatalog.Recipe("overhaul_test_recipe","Charred tomato & rice skillet","Dinner",25,2,"A warm skillet of golden rice, tomatoes and softly cooked eggs.","rice,tomato,eggs","1 cup rice\n2 tomatoes\n2 eggs\n1 tbsp cooking oil","1. Rinse the rice and dice the tomatoes.\n\n2. Simmer the rice for 15 minutes.\n\n3. Add tomatoes and eggs; cook until the eggs are fully set.");r.cuisine="Mediterranean";r.goal="Balanced";return r;
    }
    @Test public void readableLoadoutModesGoalsAndPersistentAmmunition()throws Exception{
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            Thread.sleep(1200);capture("01-empty-loadout");
            scenario.onActivity(a->{EditText e=a.findViewById(R.id.ammunitionEditor);assertEquals(1,e.getAlpha(),.001);e.setText("Rice");e.onEditorAction(EditorInfo.IME_ACTION_NEXT);});Thread.sleep(700);
            scenario.onActivity(a->{assertEquals(1,weapon(a).magazine().count());weapon(a).restore(Arrays.asList("Rice","Tomato","Eggs","Garlic","",""),4);weapon(a).inspect();});Thread.sleep(600);capture("02-loaded-revolver");
            scenario.recreate();scenario.onActivity(a->{assertEquals(4,weapon(a).magazine().count());a.findViewById(R.id.btnCut).performClick();});Thread.sleep(700);capture("03-precision");
            scenario.onActivity(a->a.findViewById(R.id.btnBulk).performClick());Thread.sleep(700);capture("04-heavyweight");
            scenario.onActivity(a->{ChipGroup modes=a.findViewById(R.id.modeChips);((Chip)modes.getChildAt(1)).setChecked(true);assertFalse(((EditText)a.findViewById(R.id.ammunitionEditor)).isEnabled());assertEquals(4,weapon(a).magazine().count());});Thread.sleep(500);capture("05-surprise");
            scenario.onActivity(a->{ChipGroup modes=a.findViewById(R.id.modeChips);((Chip)modes.getChildAt(0)).setChecked(true);assertEquals(4,weapon(a).magazine().count());a.findViewById(R.id.btnFilters).performClick();});Thread.sleep(400);capture("06-preferences");
        }
    }
    @Test public void cookingChecklistStepsTimerAndRecreation()throws Exception{
        RecipeCatalog.Recipe r=recipe();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->{screens(a).cook(r);text(a.findViewById(R.id.secondaryScreen),a.getString(R.string.checklist_closed)).performClick();TextView check=text(a.findViewById(R.id.secondaryScreen),"1 cup rice");assertNotNull(check);check.performClick();text(a.findViewById(R.id.secondaryScreen),a.getString(R.string.next_step)).performClick();});Thread.sleep(500);capture("08-cooking");
            scenario.onActivity(a->{assertNotNull(text(a.findViewById(R.id.secondaryScreen),a.getString(R.string.start_timer)));text(a.findViewById(R.id.secondaryScreen),a.getString(R.string.start_timer)).performClick();});
            scenario.recreate();Thread.sleep(500);
            scenario.onActivity(a->{assertEquals("cooking",screens(a).screen());assertTrue(((CheckBox)text(a.findViewById(R.id.secondaryScreen),"1 cup rice")).isChecked());assertNotNull(text(a.findViewById(R.id.secondaryScreen),a.getString(R.string.pause_timer)));screens(a).saved(Collections.singletonList(r));});Thread.sleep(500);capture("09-saved-archive");
            scenario.onActivity(a->{text(a.findViewById(R.id.secondaryScreen),a.getString(R.string.open_recipe)).performClick();assertEquals(View.VISIBLE,a.findViewById(R.id.cardMealResult).getVisibility());});Thread.sleep(500);capture("07-recipe-reveal");
        }
    }
    @Test public void waitingAndCancelKeepIngredientsAndUnlockInput()throws Exception{
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->{weapon(a).restore(Arrays.asList("Rice","Tomato"),0);try{Field f=MainActivity.class.getDeclaredField("recipeApi");f.setAccessible(true);f.set(a,(RecipeApi)r->new WeaponInteractionTest.Reply(r,30000,true));}catch(Exception e){throw new AssertionError(e);}a.findViewById(R.id.btnSpin).performClick();});
            Thread.sleep(3500);capture("10-cooking-wait");
            scenario.onActivity(a->{assertEquals(View.VISIBLE,a.findViewById(R.id.cookingStatus).getVisibility());a.findViewById(R.id.btnCancelGeneration).performClick();assertTrue(a.findViewById(R.id.btnSpin).isEnabled());assertEquals(2,weapon(a).magazine().count());assertEquals(View.GONE,a.findViewById(R.id.cookingStatus).getVisibility());});
        }
    }
    @Test public void liveGeminiRecipeCanBeSavedAndCooked() throws Exception {
        org.junit.Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("liveRecipe")));
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->{weapon(a).restore(Arrays.asList("Rice","Tomato","Eggs"),0);a.findViewById(R.id.btnSpin).performClick();});
            final boolean[] ready={false};
            for(int i=0;i<66&&!ready[0];i++){Thread.sleep(1000);scenario.onActivity(a->ready[0]=a.findViewById(R.id.cardMealResult).getVisibility()==View.VISIBLE);}
            assertTrue("Live Gemini request should reveal a recipe",ready[0]);capture("11-live-gemini-recipe");
            scenario.onActivity(a->{a.findViewById(R.id.btnLike).performClick();a.findViewById(R.id.btnRecipe).performClick();assertEquals("cooking",screens(a).screen());});Thread.sleep(800);capture("12-live-cooking");
        }
    }
    @Test public void compactIngredientManagerEditsAndRemovesTheChosenCartridge() throws Exception {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->{weapon(a).restore(Arrays.asList("Rice","Tomato"),0);a.findViewById(R.id.tvLoadState).performClick();});
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText("02 / Tomato")).perform(androidx.test.espresso.action.ViewActions.click());
            Thread.sleep(500);
            scenario.onActivity(a->{assertEquals("Tomato",((EditText)a.findViewById(R.id.ammunitionEditor)).getText().toString());a.findViewById(R.id.tvLoadState).performClick();});
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText(R.string.remove_selected)).perform(androidx.test.espresso.action.ViewActions.click());
            scenario.onActivity(a->{assertEquals(1,weapon(a).magazine().count());assertTrue(weapon(a).magazine().ingredients().contains("Rice"));});
        }
    }
    @Test public void reducedMotionRevealsAnEarlyRecipeWithoutTheLongFiringSequence() throws Exception {
        prefs().edit().putBoolean("reduce_motion",true).commit();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->{weapon(a).restore(Arrays.asList("Rice","Tomato"),0);try{Field f=MainActivity.class.getDeclaredField("recipeApi");f.setAccessible(true);f.set(a,(RecipeApi)r->new WeaponInteractionTest.Reply(r,100,false));}catch(Exception e){throw new AssertionError(e);}a.findViewById(R.id.btnSpin).performClick();});
            Thread.sleep(1000);
            scenario.onActivity(a->{assertEquals(View.VISIBLE,a.findViewById(R.id.cardMealResult).getVisibility());assertTrue(a.findViewById(R.id.btnSpin).isEnabled());});
        }
    }
}
