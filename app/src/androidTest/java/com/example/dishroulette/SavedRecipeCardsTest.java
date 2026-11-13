package com.example.dishroulette;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Bilingual library frames and interaction checks using test-only recipe fixtures. */
public class SavedRecipeCardsTest {
    private KitchenScreens screens(MainActivity a){try{Field f=MainActivity.class.getDeclaredField("kitchenScreens");f.setAccessible(true);return (KitchenScreens)f.get(a);}catch(Exception e){throw new AssertionError(e);}}
    private ViewGroup host(MainActivity a){try{Field f=KitchenScreens.class.getDeclaredField("host");f.setAccessible(true);return (ViewGroup)f.get(screens(a));}catch(Exception e){throw new AssertionError(e);}}
    private TextView text(View v,String label){if(v instanceof TextView&&label.contentEquals(((TextView)v).getText()))return (TextView)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){TextView t=text(((ViewGroup)v).getChildAt(i),label);if(t!=null)return t;}return null;}
    private void capture(String name)throws Exception{
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();Thread.sleep(350);
        Bitmap b=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(b);
        File dir=InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("saved-cards");
        try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,out));}b.recycle();
    }
    private void readable(View v){
        if(v.getVisibility()!=View.VISIBLE)return;
        if(v instanceof TextView){TextView t=(TextView)v;assertNotNull(t.getLayout());
            for(int i=0;i<t.getLayout().getLineCount();i++)assertEquals("Ellipsis: "+t.getText(),0,t.getLayout().getEllipsisCount(i));
            assertTrue("Clipping: "+t.getText(),t.getLayout().getHeight()<=t.getHeight()-t.getCompoundPaddingTop()-t.getCompoundPaddingBottom());
            if(t instanceof android.widget.Button){assertTrue(t.getHeight()>=48*t.getResources().getDisplayMetrics().density);assertTrue(t.isImportantForAccessibility());}
        }
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)readable(((ViewGroup)v).getChildAt(i));
    }
    private void translations(RecipeCatalog.Recipe recipe,String enTitle,String arTitle){
        com.example.dishroulette.core.RecipeTranslation en=new com.example.dishroulette.core.RecipeTranslation();
        en.title=enTitle;en.description="A simple dish to share.";en.ingredients=java.util.List.of("1 can chickpeas");en.steps=java.util.List.of("Mix and serve.");
        com.example.dishroulette.core.RecipeTranslation ar=new com.example.dishroulette.core.RecipeTranslation();
        ar.title=arTitle;ar.description="وجبة بسيطة للمشاركة.";ar.ingredients=java.util.List.of("علبة حمص");ar.steps=java.util.List.of("اخلط المكونات وقدّمها.");
        recipe.translations=java.util.Map.of("en",en,"ar",ar);
    }
    @Test public void bilingualLibrary()throws Exception{
        for(String language:new String[]{"en","ar"}){
            boolean ar=language.equals("ar");
            InterfaceSettings.prefs(InstrumentationRegistry.getInstrumentation().getTargetContext()).edit().putString(InterfaceSettings.LANGUAGE,language).putBoolean("reduce_motion",true).putString("spin_profile","{}").commit();
            RecipeCatalog.Recipe first=new RecipeCatalog.Recipe("card-first",ar?"أرز بالبندورة":"Tomato rice","Dinner",20,2,ar?"وجبة دافئة وبسيطة من الأرز والبندورة.":"A warm, simple tomato rice dish.","rice","1 cup rice","Cook rice for 15 minutes.");
            first.caloriesPerServing=320;first.proteinGramsPerServing=12.5;first.carbohydratesGramsPerServing=45.0;first.fatGramsPerServing=9.0;first.fiberGramsPerServing=4.0;first.sugarGramsPerServing=3.0;first.sodiumMilligramsPerServing=300.0;
            RecipeCatalog.Recipe second=new RecipeCatalog.Recipe("card-second",ar?"سلطة الحمص بالليمون":"Lemony chickpea salad","Lunch",15,2,ar?"خضار مقرمشة وحمص مع صلصة الليمون.":"Crisp vegetables, chickpeas and a bright lemon dressing.","chickpeas","1 can chickpeas","Mix and serve.");
            first.language=language;second.language=language;
            translations(second,"Lemony chickpea salad","سلطة الحمص بالليمون");
            try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
                scenario.onActivity(a->screens(a).saved(Arrays.asList(first,second)));
                capture(language+"-library");
                scenario.onActivity(a->{
                    assertEquals(ar?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR,host(a).getLayoutDirection());readable(host(a));
                    assertEquals(View.GONE,text(host(a),a.getString(R.string.nutrition_unavailable)).getVisibility());
                    text(host(a),a.getString(R.string.saved_show_nutrition)).performClick();
                });
                capture(language+"-expanded");
                scenario.onActivity(a->{
                    readable(host(a));
                    java.text.NumberFormat n=java.text.NumberFormat.getNumberInstance(java.util.Locale.forLanguageTag(language));
                    String sodium=a.getString(R.string.nutrition_sodiumMilligrams,n.format(300));
                    TextView details=text(host(a),KitchenScreens.meta(a,first).substring(KitchenScreens.meta(a,first).indexOf(a.getString(R.string.nutrition_estimate,n.format(320),n.format(12.5)))));
                    assertNotNull(details);assertEquals(View.VISIBLE,details.getVisibility());assertTrue(details.getText().toString().contains(sodium));assertTrue(details.getText().toString().contains(a.getString(R.string.nutrition_variation)));
                    text(host(a),a.getString(R.string.saved_hide_nutrition)).performClick();assertEquals(View.GONE,details.getVisibility());
                    text(host(a),a.getString(R.string.remove_saved)).performClick();
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(a->{assertNull(text(host(a),first.title));assertNotNull(text(host(a),second.title));text(host(a),a.getString(R.string.open_recipe)).performClick();});
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(a->assertEquals(second.title,((TextView)a.findViewById(R.id.tvMealTitle)).getText().toString()));
            }
        }
    }
}
