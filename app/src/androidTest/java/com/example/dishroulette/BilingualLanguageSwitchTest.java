package com.example.dishroulette;

import android.content.Context;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import static org.junit.Assert.*;

/** Emulator fixture: the same imported recipe follows Settings through recreation and cooking. */
public class BilingualLanguageSwitchTest {
    @Test public void importedRecipeChangesLanguageWithoutChangingIdentityOrCookingProgress()throws Exception{
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        InterfaceSettings.prefs(c).edit().putString(InterfaceSettings.LANGUAGE,"en").putString("spin_profile","{}").commit();
        RecipeCatalog.Recipe imported=RecipeShareCodec.decode(RecipeShareCodec.encode(RecipeCatalog.ALL.get(5)));
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->{assertTrue(a.saveSharedRecipe(imported));assertEquals(imported.title,((TextView)a.findViewById(R.id.tvMealTitle)).getText().toString());a.changeLanguage("ar");});
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(a->{assertEquals(imported.forLanguage("ar").title,((TextView)a.findViewById(R.id.tvMealTitle)).getText().toString());
                a.findViewById(R.id.btnRecipe).performClick();
                InterfaceSettings.prefs(a).edit().putInt("cook_step_"+imported.id,2).putBoolean("cook_done_"+imported.id+"_0",true).commit();
                a.changeLanguage("en");});
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(a->{try{
                java.lang.reflect.Field field=MainActivity.class.getDeclaredField("kitchenScreens");field.setAccessible(true);KitchenScreens kitchen=(KitchenScreens)field.get(a);
                assertEquals("cooking",kitchen.screen());assertEquals(imported.id,kitchen.cookingRecipe().id);assertEquals("en",kitchen.cookingRecipe().language);
                assertEquals(imported.method,kitchen.cookingRecipe().method);
                assertEquals(2,InterfaceSettings.prefs(a).getInt("cook_step_"+imported.id,-1));
                assertTrue(InterfaceSettings.prefs(a).getBoolean("cook_done_"+imported.id+"_0",false));
            }catch(ReflectiveOperationException e){throw new AssertionError(e);}});
        }
    }
}
