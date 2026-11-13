package com.example.dishroulette;

import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import org.junit.Test;
import static org.junit.Assert.*;

public class UiPolishTest {
    private View findText(View v,String text){
        if(v instanceof TextView && ((TextView)v).getText().toString().equals(text))return v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){
            View match=findText(((ViewGroup)v).getChildAt(i),text);if(match!=null)return match;
        }
        return null;
    }
    @Test public void centeredNavigationAndPersistentSoundChoiceInBothLanguages(){
        android.content.Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String originalLanguage=InterfaceSettings.prefs(c).getString(InterfaceSettings.LANGUAGE,"en");
        boolean hadSound=InterfaceSettings.prefs(c).contains("ui_sounds"),originalSound=InterfaceSettings.prefs(c).getBoolean("ui_sounds",true);
        try { for(String lang:new String[]{"en","ar"}){
            InterfaceSettings.prefs(c).edit().putString(InterfaceSettings.LANGUAGE,lang).putBoolean("ui_sounds",true).commit();
            try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
                scenario.onActivity(a->{
                    View home=a.findViewById(R.id.simpleHome);
                    TextView navLabel=(TextView)findText(home,a.getString(R.string.home));assertNotNull(navLabel);
                    NavigationButton nav=(NavigationButton)navLabel.getParent();
                    assertEquals(Gravity.CENTER,nav.getGravity());assertTrue(nav.isClickable());assertTrue(nav.isSelected());
                    assertNotNull(((android.widget.ImageView)nav.getChildAt(0)).getDrawable());
                    for(int id:new int[]{R.string.pantry_mode,R.string.discover_mode}){
                        MaterialButton mode=(MaterialButton)findText(home,a.getString(id));
                        assertNotNull(mode);assertEquals(mode.getCurrentTextColor(),mode.getIconTint().getDefaultColor());
                    }
                    a.showSettings();
                    SwitchMaterial sounds=(SwitchMaterial)findText(a.findViewById(R.id.secondaryScreen),a.getString(R.string.ui_sounds));
                    assertNotNull(sounds);assertTrue(sounds.isChecked());sounds.performClick();
                    assertFalse(InterfaceSettings.prefs(a).getBoolean("ui_sounds",true));
                });
                scenario.recreate();
                scenario.onActivity(a->{a.showSettings();
                    SwitchMaterial sounds=(SwitchMaterial)findText(a.findViewById(R.id.secondaryScreen),a.getString(R.string.ui_sounds));
                    assertFalse(sounds.isChecked());sounds.performClick();assertTrue(sounds.isChecked());
                });
            }
        }} finally {
            android.content.SharedPreferences.Editor restore=InterfaceSettings.prefs(c).edit().putString(InterfaceSettings.LANGUAGE,originalLanguage);
            if(hadSound)restore.putBoolean("ui_sounds",originalSound);else restore.remove("ui_sounds");restore.commit();
        }
    }
}
