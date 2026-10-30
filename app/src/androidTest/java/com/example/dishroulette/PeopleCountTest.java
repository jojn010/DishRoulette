package com.example.dishroulette;

import android.graphics.Bitmap;
import android.view.View;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.gson.Gson;
import java.lang.reflect.Field;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class PeopleCountTest {
    private void readable(TextView t){
        assertNotNull(t.getLayout());
        for(int n=0;n<t.getLayout().getLineCount();n++)assertEquals(0,t.getLayout().getEllipsisCount(n));
        assertTrue(t.getLayout().getHeight()<=t.getHeight()-t.getCompoundPaddingTop()-t.getCompoundPaddingBottom());
    }
    private void capture(String name)throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();Thread.sleep(250);
        Bitmap b=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(b);
        File dir=InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("people-count");
        try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();
    }
    @Test public void bilingualCountPersistsAndReachesRecipeGeneration()throws Exception {
        android.content.SharedPreferences p=InterfaceSettings.prefs(InstrumentationRegistry.getInstrumentation().getTargetContext());
        for(String language:new String[]{"en","ar"}) {
            p.edit().putString(InterfaceSettings.LANGUAGE,language).putString("spin_profile","{}").putBoolean("reduce_motion",true).commit();
            try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
                scenario.onActivity(a->{
                    assertEquals(2,a.profile().servings);
                    a.findViewById(R.id.fewerPeople).performClick();assertEquals(1,a.profile().servings);assertFalse(a.findViewById(R.id.fewerPeople).isEnabled());
                    for(int i=1;i<12;i++)a.findViewById(R.id.morePeople).performClick();
                    assertEquals(12,a.profile().servings);assertFalse(a.findViewById(R.id.morePeople).isEnabled());
                    for(int i=12;i>6;i--)a.findViewById(R.id.fewerPeople).performClick();
                    View count=a.findViewById(R.id.peopleCount);View row=(View)count.getParent();row.requestRectangleOnScreen(new android.graphics.Rect(0,0,row.getWidth(),row.getHeight()),true);
                });
                capture(language+"-six-people");
                scenario.onActivity(a->{
                    readable(a.findViewById(R.id.peopleCount));
                    for(int id:new int[]{R.id.fewerPeople,R.id.morePeople}) {
                        TextView b=a.findViewById(id);readable(b);assertTrue(b.getHeight()>=48*b.getResources().getDisplayMetrics().density);assertNotNull(b.getContentDescription());
                    }
                    assertEquals(language.equals("ar")?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR,a.findViewById(R.id.peopleCount).getLayoutDirection());
                    assertEquals(6,SpinProfile.load(p,new Gson()).servings);
                });
                scenario.recreate();
                scenario.onActivity(a->{
                    assertEquals(6,a.profile().servings);
                    try {
                        Field f=MainActivity.class.getDeclaredField("loadout");f.setAccessible(true);
                        ((IngredientLoadout)f.get(a)).restore(Arrays.asList("Rice","Tomato"),0);
                        Field api=MainActivity.class.getDeclaredField("recipeApi");api.setAccessible(true);
                        api.set(a,(RecipeApi)request->{assertEquals(6,request.servings);assertEquals(language,request.language);return new DualInterfaceTest.LocalReply(request);});
                    }catch(ReflectiveOperationException e){throw new AssertionError(e);}
                    a.findViewById(R.id.simpleGenerate).performClick();
                });Thread.sleep(800);
                scenario.onActivity(a->{
                    assertEquals(View.VISIBLE,a.findViewById(R.id.cardMealResult).getVisibility());
                    try{Field f=MainActivity.class.getDeclaredField("currentRecipe");f.setAccessible(true);RecipeCatalog.Recipe r=(RecipeCatalog.Recipe)f.get(a);assertEquals(6,r.servings);assertTrue(KitchenScreens.meta(a,r).contains(a.getString(R.string.recipe_metadata,r.minutes,6)));}catch(Exception e){throw new AssertionError(e);}
                });
            }
        }
        // Old settings gain the default without losing exclusions or goal.
        p.edit().putString("spin_profile","{\"goal\":\"Bulking\",\"allergies\":[\"Sesame\"]}").commit();
        SpinProfile legacy=SpinProfile.load(p,new Gson());assertEquals(2,legacy.servings);assertEquals("Bulking",legacy.goal);assertTrue(legacy.allergies.contains("Sesame"));
        p.edit().putString("spin_profile","{}").commit();
    }
}
