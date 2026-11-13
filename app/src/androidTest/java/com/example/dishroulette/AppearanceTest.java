package com.example.dishroulette;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.core.graphics.ColorUtils;
import java.io.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Run on the emulator: includes reversible device night-mode changes. */
public class AppearanceTest {
    private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
    private void idle(){InstrumentationRegistry.getInstrumentation().waitForIdleSync();}
    private void choose(ActivityScenario<MainActivity> scenario,int id){
        scenario.onActivity(a->{a.showSettings();a.findViewById(id).performClick();});idle();
    }
    private void resolved(ActivityScenario<MainActivity> scenario,boolean dark)throws Exception{
        boolean[] matches={false};
        for(int i=0;i<30;i++){
            idle();scenario.onActivity(a->matches[0]=AppearanceSettings.dark(a)==dark);
            if(matches[0])break;Thread.sleep(100);
        }
        assertTrue("Theme did not resolve",matches[0]);
        scenario.onActivity(a->{
            int surface=UiColors.color(a,R.color.surface),ink=UiColors.color(a,R.color.ink),muted=UiColors.color(a,R.color.muted);
            assertTrue("Body contrast",ColorUtils.calculateContrast(ink,surface)>=7);
            assertTrue("Secondary text contrast",ColorUtils.calculateContrast(muted,surface)>=4.5);
            assertTrue("Button contrast",ColorUtils.calculateContrast(UiColors.color(a,R.color.on_accent),UiColors.color(a,R.color.accent))>=4.5);
        });
    }
    private void capture(String name)throws Exception{
        idle();Thread.sleep(700);Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(bitmap);
        File dir=context.getExternalFilesDir("appearance");
        try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
    }
    private void shell(String command)throws Exception{
        try(android.os.ParcelFileDescriptor descriptor=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
            FileInputStream stream=new FileInputStream(descriptor.getFileDescriptor())){stream.readAllBytes();}
    }
    @Test public void lightAndDarkPersistAndRenderEnglishAndArabic()throws Exception{
        String original=AppearanceSettings.choice(context),language=InterfaceSettings.language(context);
        try{
            for(String lang:new String[]{"en","ar"}){
                InterfaceSettings.prefs(context).edit().putString(InterfaceSettings.LANGUAGE,lang).commit();
                InstrumentationRegistry.getInstrumentation().runOnMainSync(()->AppearanceSettings.choose(context,"light"));
                try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
                    resolved(scenario,false);capture(lang+"-light-home");
                    choose(scenario,R.id.themeDark);resolved(scenario,true);assertEquals("dark",AppearanceSettings.choice(context));capture(lang+"-dark-settings");
                    scenario.recreate();resolved(scenario,true);
                    scenario.onActivity(a->a.getOnBackPressedDispatcher().onBackPressed());capture(lang+"-dark-home");
                    scenario.onActivity(a->{a.showSettings();a.findViewById(R.id.settingsRecipePreferences).performClick();});capture(lang+"-dark-preferences");
                    androidx.test.espresso.Espresso.pressBack();
                    choose(scenario,R.id.themeLight);resolved(scenario,false);capture(lang+"-light-settings");
                }
            }
        }finally{
            InterfaceSettings.prefs(context).edit().putString(InterfaceSettings.LANGUAGE,language).commit();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->AppearanceSettings.choose(context,original));
        }
    }
    @Test public void systemTracksDeviceAndExplicitLightOverridesDarkDevice()throws Exception{
        String original=AppearanceSettings.choice(context);
        int device=((android.app.UiModeManager)context.getSystemService(Context.UI_MODE_SERVICE)).getNightMode();
        try{
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->AppearanceSettings.choose(context,"system"));
            try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
                shell("cmd uimode night yes");resolved(scenario,true);
                choose(scenario,R.id.themeLight);resolved(scenario,false);
                choose(scenario,R.id.themeSystem);resolved(scenario,true);assertEquals("system",AppearanceSettings.choice(context));
                shell("cmd uimode night no");resolved(scenario,false);
                scenario.recreate();resolved(scenario,false);assertEquals("system",AppearanceSettings.choice(context));
            }
        }finally{
            shell("cmd uimode night "+(device==2?"yes":device==0?"auto":"no"));
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->AppearanceSettings.choose(context,original));
        }
    }
}
