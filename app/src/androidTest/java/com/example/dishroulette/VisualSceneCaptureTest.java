package com.example.dishroulette;

import android.graphics.Bitmap;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Captures actual emulator frames, alongside the existing editing/network regression suite. */
@RunWith(AndroidJUnit4.class)
public class VisualSceneCaptureTest {
    private WeaponInputView weapon(MainActivity a){return a.findViewById(R.id.tabletop);}
    private WeaponRenderer renderer(MainActivity a){
        try{Field f=WeaponInputView.class.getDeclaredField("renderer");f.setAccessible(true);return (WeaponRenderer)f.get(weapon(a));}catch(Exception e){throw new AssertionError(e);}
    }
    private void capture(String name) throws Exception {
        Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(bitmap);
        File dir=InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("visual-correction");assertNotNull(dir);
        try(FileOutputStream stream=new FileOutputStream(new File(dir,name+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,stream));}
        bitmap.recycle();
    }
    @Test public void sideProfileFocusAndMuzzleAlignedCookingFrames() throws Exception {
        InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences("dishroulette_ui",0).edit().putString("spin_profile","{}").commit();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->weapon(a).restore(Arrays.asList("Chicken","Rice","","Spinach","",""),0));
            Thread.sleep(900);scenario.onActivity(a->assertFalse(renderer(a).ovenVisible));capture("01-revolver-loading");
            scenario.onActivity(a->weapon(a).select(0));Thread.sleep(700);
            scenario.onActivity(a->{EditText editor=a.findViewById(R.id.ammunitionEditor);editor.selectAll();editor.onCreateInputConnection(new EditorInfo()).commitText("Chicken breast",1);});
            Thread.sleep(250);scenario.onActivity(a->assertFalse(renderer(a).ovenVisible));capture("02-cartridge-focus");
            scenario.onActivity(a->((EditText)a.findViewById(R.id.ammunitionEditor)).onCreateInputConnection(new EditorInfo()).performEditorAction(EditorInfo.IME_ACTION_NEXT));
            Thread.sleep(750);scenario.onActivity(a->{assertEquals(1,weapon(a).magazine().active());assertTrue(weapon(a).magazine().ingredients().contains("Chicken breast"));});
            capture("03-after-loading");
            scenario.onActivity(a->{
                try{Field f=MainActivity.class.getDeclaredField("recipeApi");f.setAccessible(true);f.set(a,(RecipeApi)r->new WeaponInteractionTest.Reply(r,15000,true));}catch(Exception e){throw new AssertionError(e);}
                a.findViewById(R.id.btnSpin).performClick();
            });
            Thread.sleep(520);scenario.onActivity(a->assertTrue(renderer(a).ovenVisible));capture("04-shooting-camera");
            // Seek and pause the real running animator to capture its short muzzle-flash frame reliably.
            scenario.onActivity(a->{
                try{Field f=WeaponInputView.class.getDeclaredField("animation");f.setAccessible(true);android.animation.ValueAnimator animator=(android.animation.ValueAnimator)f.get(weapon(a));animator.setCurrentFraction(.32f);animator.pause();}catch(Exception e){throw new AssertionError(e);}
            });
            Thread.sleep(100);capture("05-shot-and-projectiles");
            scenario.onActivity(a->{
                try{Field f=WeaponInputView.class.getDeclaredField("animation");f.setAccessible(true);((android.animation.ValueAnimator)f.get(weapon(a))).resume();}catch(Exception e){throw new AssertionError(e);}
            });
            Thread.sleep(650);capture("06-pot-steam");
        }
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->{weapon(a).restore(Arrays.asList("Chicken","Rice","","","",""),0);a.findViewById(R.id.btnCut).performClick();});
            Thread.sleep(700);scenario.onActivity(a->assertFalse(renderer(a).ovenVisible));capture("07-rifle");
            scenario.onActivity(a->a.findViewById(R.id.btnBulk).performClick());Thread.sleep(700);scenario.onActivity(a->assertFalse(renderer(a).ovenVisible));capture("08-shotgun");
        }
    }
}
