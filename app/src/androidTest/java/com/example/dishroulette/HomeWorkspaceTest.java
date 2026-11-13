package com.example.dishroulette;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.button.MaterialButton;
import java.io.File;
import java.io.FileOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Checks the visible Home controls at the emulator's configured font scale. */
@RunWith(AndroidJUnit4.class)
public class HomeWorkspaceTest {
    private TextView text(View v,String label){
        if(v instanceof TextView && label.contentEquals(((TextView)v).getText()))return (TextView)v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){
            TextView found=text(((ViewGroup)v).getChildAt(i),label);if(found!=null)return found;
        }
        return null;
    }
    private ScrollView scroll(View v){
        if(v instanceof ScrollView)return (ScrollView)v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){
            ScrollView found=scroll(((ViewGroup)v).getChildAt(i));if(found!=null)return found;
        }
        return null;
    }
    private void readable(TextView v){
        assertNotNull(v);assertNotNull(v.getLayout());
        for(int i=0;i<v.getLayout().getLineCount();i++)assertEquals("Truncated: "+v.getText(),0,v.getLayout().getEllipsisCount(i));
        assertTrue("Vertical clipping: "+v.getText()+" layout="+v.getLayout().getHeight()+" height="+v.getHeight()+" padding="+(v.getCompoundPaddingTop()+v.getCompoundPaddingBottom()),v.getLayout().getHeight()<=v.getHeight()-v.getCompoundPaddingTop()-v.getCompoundPaddingBottom());
        View target=v.getParent() instanceof NavigationButton?(View)v.getParent():v;
        assertTrue("Small touch target: "+v.getText(),target.getHeight()/v.getResources().getDisplayMetrics().density>=48);
        assertTrue(target.isImportantForAccessibility());
    }
    private void capture(String name)throws Exception{
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();Thread.sleep(300);
        Bitmap b=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(b);
        File dir=InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("home-workspace");
        try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,out));}b.recycle();
    }
    @Test public void bilingualWorkspaceControlsRemainReadableAndNavigationHasSelectedHome()throws Exception{
        for(String language:new String[]{"en","ar"}){
            android.content.Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
            InterfaceSettings.prefs(c).edit().putString(InterfaceSettings.LANGUAGE,language).putString("loaded_ammunition","[]").putString("simple_input_draft","").putString("spin_profile","{}").putBoolean("reduce_motion",true).commit();
            try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
                scenario.onActivity(a->{
                    View root=a.findViewById(R.id.simpleHome);
                    assertEquals(language.equals("ar")?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR,root.getLayoutDirection());
                    for(int id:new int[]{R.string.home,R.string.saved,R.string.settings})readable(text(root,a.getString(id)));
                    View home=(View)text(root,a.getString(R.string.home)).getParent();assertTrue(home.isSelected());home.performClick();assertTrue(home.isSelected());
                    readable(a.findViewById(R.id.simpleGenerate));readable(a.findViewById(R.id.photoScan));readable(a.findViewById(R.id.simpleIngredientInput));
                    assertNull(a.findViewById(R.id.geminiConnect));
                });
                capture(language+"-top");
                scenario.onActivity(a->{View scan=a.findViewById(R.id.photoScan);scan.getParent().requestChildFocus(scan,scan);scroll(a.findViewById(R.id.simpleHome)).scrollTo(0,scan.getBottom());});
                capture(language+"-workspace");
            }
        }
    }
}
