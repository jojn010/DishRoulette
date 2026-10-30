package com.example.dishroulette;

import android.content.*;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.action.ViewActions.*;

public class RecipeSharingTest {
    private void capture(String name)throws Exception{
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();Thread.sleep(250);
        Bitmap b=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(b);
        File dir=InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("recipe-sharing");
        try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();
    }
    private void pasteAndReview(String code){
        onView(withId(R.id.importRecipe)).perform(scrollTo(),click());
        onView(withId(R.id.recipeCodeInput)).perform(scrollTo(),replaceText(code),closeSoftKeyboard());
        onView(withText(R.string.review_recipe)).perform(click());
    }
    @Test public void bilingualCopyReviewRotateSaveDeduplicateAndCookOffline()throws Exception{
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        android.content.SharedPreferences prefs=InterfaceSettings.prefs(context);
        for(String language:new String[]{"en","ar"}){
            prefs.edit().putString(InterfaceSettings.LANGUAGE,language).putString("spin_profile","{}").putBoolean("reduce_motion",true).commit();
            RecipeCatalog.Recipe recipe=new RecipeCatalog.Recipe("share-test",language.equals("ar")?"أرز للأصحاب":"Rice for friends","Dinner",25,6,language.equals("ar")?"وجبة بسيطة للمشاركة.":"A simple dish to share.","rice",language.equals("ar")?"٣ أكواب أرز\n٦ حبات بندورة":"3 cups rice\n6 tomatoes",language.equals("ar")?"١. اطبخ الأرز لمدة ١٥ دقيقة.\n\n٢. قدّمه دافئًا.":"1. Cook rice for 15 minutes.\n\n2. Serve warm.");recipe.language=language;
            String[] copied={null};
            try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
                scenario.onActivity(a->a.shareRecipe(recipe));capture(language+"-share");
                onView(withId(R.id.copyRecipeCode)).perform(click());
                scenario.onActivity(a->{copied[0]=((android.content.ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE)).getPrimaryClip().getItemAt(0).getText().toString();assertTrue(copied[0].startsWith("TABKHA1:"));a.showSavedRecipes();});
                pasteAndReview(copied[0]);capture(language+"-review");
                scenario.recreate();onView(withText(R.string.import_save)).perform(click());
                String id=RecipeShareCodec.decode(copied[0]).id;
                scenario.onActivity(a->{
                    assertTrue(prefs.getStringSet("saved_recipes",Collections.emptySet()).contains(id));
                    assertEquals(recipe.title,((TextView)a.findViewById(R.id.tvMealTitle)).getText().toString());
                    assertEquals(View.GONE,a.findViewById(R.id.tvMissing).getVisibility());
                    assertEquals(a.getString(R.string.shared_recipe_label),((TextView)a.findViewById(R.id.tvResultEyebrow)).getText().toString());
                    a.showSavedRecipes();
                });
                int count=prefs.getStringSet("saved_recipes",Collections.emptySet()).size();
                pasteAndReview(copied[0]);onView(withText(R.string.import_save)).perform(click());assertEquals(count,prefs.getStringSet("saved_recipes",Collections.emptySet()).size());
                scenario.onActivity(a->a.findViewById(R.id.btnRecipe).performClick());
                scenario.recreate();scenario.onActivity(a->{try{Field f=MainActivity.class.getDeclaredField("kitchenScreens");f.setAccessible(true);KitchenScreens k=(KitchenScreens)f.get(a);assertEquals("cooking",k.screen());assertEquals(6,k.cookingRecipe().servings);}catch(Exception e){throw new AssertionError(e);}});
            }
        }
    }
    @Test public void androidShareSheetOpensWithoutSending()throws Exception{
        InterfaceSettings.prefs(InstrumentationRegistry.getInstrumentation().getTargetContext()).edit().putString(InterfaceSettings.LANGUAGE,"en").commit();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->a.shareRecipe(RecipeCatalog.ALL.get(0)));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();Thread.sleep(250);
            android.app.UiAutomation ui=InstrumentationRegistry.getInstrumentation().getUiAutomation();
            boolean clicked=false;
            for(int attempt=0;attempt<30&&!clicked;attempt++){
                android.view.accessibility.AccessibilityNodeInfo dialog=ui.getRootInActiveWindow();
                if(dialog!=null)for(android.view.accessibility.AccessibilityNodeInfo node:dialog.findAccessibilityNodeInfosByText("Share recipe")){
                    if(node.isClickable()&&node.getClassName().toString().endsWith("Button")){clicked=node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);break;}
                }
                if(!clicked)Thread.sleep(100);
            }
            assertTrue("Share action unavailable",clicked);
            boolean chooser=false;
            for(int attempt=0;attempt<30&&!chooser;attempt++){
                android.view.accessibility.AccessibilityNodeInfo root=ui.getRootInActiveWindow();
                chooser=root!=null&&root.getPackageName()!=null&&root.getPackageName().toString().contains("intentresolver");
                if(!chooser)Thread.sleep(100);
            }
            assertTrue("Android share sheet unavailable",chooser);capture("android-share-sheet");
            InstrumentationRegistry.getInstrumentation().getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);
        }
    }
    @Test public void malformedImportDoesNotSaveAndExclusionsStillBlockCooking()throws Exception{
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();android.content.SharedPreferences p=InterfaceSettings.prefs(context);
        p.edit().putString(InterfaceSettings.LANGUAGE,"en").putString("spin_profile","{}").commit();
        Set<String> before=new HashSet<>(p.getStringSet("saved_recipes",Collections.emptySet()));
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(MainActivity::showSavedRecipes);pasteAndReview("TABKHA1:broken");
            onView(withId(R.id.recipeCodeInput)).check(androidx.test.espresso.assertion.ViewAssertions.matches(isDisplayed()));
            assertEquals(before,p.getStringSet("saved_recipes",Collections.emptySet()));
            onView(withText(R.string.cancel)).perform(click());
            scenario.onActivity(a->{a.profile().allergies.add("Sesame");RecipeCatalog.Recipe r=new RecipeCatalog.Recipe("blocked","Tahini rice","Dinner",20,2,"Sesame dish","","rice\ntahini","Cook rice and add tahini.");assertFalse(a.saveSharedRecipe(RecipeShareCodec.decode(RecipeShareCodec.encode(r))));});
            assertEquals(before,p.getStringSet("saved_recipes",Collections.emptySet()));
        }
    }
}
