package com.example.dishroulette;

import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class InspectionGestureTest {
    private long downTime;
    private Object field(Object value,String name){try{Field f=value.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(value);}catch(Exception e){throw new AssertionError(e);}}
    private WeaponInputView weapon(MainActivity a){return a.findViewById(R.id.tabletop);}
    private View surface(MainActivity a){return (View)field(weapon(a),"surface");}
    private WeaponRenderer renderer(MainActivity a){return (WeaponRenderer)field(weapon(a),"renderer");}
    private WeaponGestureController controller(MainActivity a){return (WeaponGestureController)field(weapon(a),"gestures");}
    private InspectionState state(MainActivity a){return controller(a).state;}
    @Before public void defaults(){InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences("dishroulette_ui",0).edit().remove("loaded_ammunition").remove("loaded_chamber").putBoolean("reduce_motion",false).putString("selected_meal","Any meal").putString(InterfaceSettings.MODE,"advanced").putString(InterfaceSettings.LANGUAGE,"en").putString("spin_profile","{}").commit();}
    private void event(MainActivity a,int action,float x,float y,int source){
        long time=SystemClock.uptimeMillis();if(action==MotionEvent.ACTION_DOWN)downTime=time;
        MotionEvent e=MotionEvent.obtain(downTime,time,action,x,y,0);e.setSource(source);surface(a).dispatchTouchEvent(e);e.recycle();
    }
    private float[] center(MainActivity a,int slot){return new float[]{((float[])field(renderer(a),"hitX"))[slot]*surface(a).getWidth(),((float[])field(renderer(a),"hitY"))[slot]*surface(a).getHeight()};}
    private void mouseOrbit(MainActivity a){View s=surface(a);float x=s.getWidth()*.84f,y=s.getHeight()*.80f;event(a,0,x,y,InputDevice.SOURCE_MOUSE);event(a,2,x-150,y-80,InputDevice.SOURCE_MOUSE);event(a,1,x-150,y-80,InputDevice.SOURCE_MOUSE);}
    private void capture(String name)throws Exception{
        Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(bitmap);
        File dir=InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("inspection");assertNotNull(dir);
        try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}bitmap.recycle();
    }
    @Test public void cylinderFollowsHeldDragReversesAndSettlesWithoutEditing()throws Exception{
        final float[] point=new float[2];
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->weapon(a).restore(Arrays.asList("Chicken","Rice","Eggs","","",""),0));Thread.sleep(650);
            scenario.onActivity(a->{float[] p=center(a,0);point[0]=p[0];point[1]=p[1];assertTrue(renderer(a).pickCylinder(p[0]/surface(a).getWidth(),p[1]/surface(a).getHeight()));event(a,0,p[0],p[1],InputDevice.SOURCE_TOUCHSCREEN);});
            Thread.sleep(50);
            scenario.onActivity(a->{event(a,2,point[0],point[1]-38,InputDevice.SOURCE_TOUCHSCREEN);assertTrue(state(a).cylinderAngle>5);assertTrue(state(a).cylinderAngle<59);assertEquals(0,state(a).yaw,.001f);assertFalse(((EditText)a.findViewById(R.id.ammunitionEditor)).hasFocus());});
            final float[] first=new float[1];scenario.onActivity(a->first[0]=state(a).cylinderAngle);Thread.sleep(50);
            scenario.onActivity(a->{event(a,2,point[0],point[1]-22,InputDevice.SOURCE_TOUCHSCREEN);assertTrue(state(a).cylinderAngle<first[0]);event(a,2,point[0],point[1]-65,InputDevice.SOURCE_TOUCHSCREEN);});Thread.sleep(250);
            scenario.onActivity(a->event(a,1,point[0],point[1]-65,InputDevice.SOURCE_TOUCHSCREEN));Thread.sleep(700);
            scenario.onActivity(a->{assertEquals(0,state(a).cylinderAngle%60,.01f);assertEquals(InspectionState.chamber(state(a).cylinderAngle),weapon(a).magazine().active()%6);assertEquals(Arrays.asList("Chicken","Rice","Eggs","","",""),weapon(a).magazine().save());assertFalse(((EditText)a.findViewById(R.id.ammunitionEditor)).hasFocus());});capture("01-cylinder-inspected");
        }
    }
    private void multi(MainActivity a,int action,float x0,float y0,float x1,float y1){
        MotionEvent.PointerProperties p0=new MotionEvent.PointerProperties(),p1=new MotionEvent.PointerProperties();p0.id=0;p1.id=1;p0.toolType=1;p1.toolType=1;
        MotionEvent.PointerCoords c0=new MotionEvent.PointerCoords(),c1=new MotionEvent.PointerCoords();c0.x=x0;c0.y=y0;c1.x=x1;c1.y=y1;c0.pressure=c1.pressure=1;c0.size=c1.size=.1f;
        MotionEvent event=MotionEvent.obtain(downTime,SystemClock.uptimeMillis(),action,2,new MotionEvent.PointerProperties[]{p0,p1},new MotionEvent.PointerCoords[]{c0,c1},0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);surface(a).dispatchTouchEvent(event);event.recycle();
    }
    @Test public void secondFingerSettlesHeldCylinderEvenBeforePinchThreshold()throws Exception{
        final float[] p=new float[2];
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            Thread.sleep(550);
            scenario.onActivity(a->{float[] center=center(a,0);p[0]=center[0];p[1]=center[1];event(a,0,p[0],p[1],InputDevice.SOURCE_TOUCHSCREEN);});Thread.sleep(40);
            scenario.onActivity(a->{event(a,2,p[0],p[1]-42,InputDevice.SOURCE_TOUCHSCREEN);assertTrue(Math.abs(state(a).cylinderAngle)>1);multi(a,261,p[0],p[1]-42,p[0]+30,p[1]-42);multi(a,262,p[0],p[1]-42,p[0]+30,p[1]-42);event(a,1,p[0],p[1]-42,InputDevice.SOURCE_TOUCHSCREEN);});Thread.sleep(650);
            scenario.onActivity(a->{assertEquals(0,state(a).cylinderAngle%60,.01f);assertFalse(controller(a).isSettling());assertFalse(((EditText)a.findViewById(R.id.ammunitionEditor)).hasFocus());});
        }
    }
    @Test public void mouseOrbitPinchPointerLiftWheelAndDoubleTapReset()throws Exception{
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            Thread.sleep(500);scenario.onActivity(a->mouseOrbit(a));Thread.sleep(350);
            scenario.onActivity(a->{assertTrue(Math.abs(state(a).yaw)>1);assertTrue(Math.abs(state(a).pitch)>1);assertFalse(((EditText)a.findViewById(R.id.ammunitionEditor)).hasFocus());
                View s=surface(a);float x=s.getWidth()/2f,y=s.getHeight()/2f;event(a,0,x-80,y,InputDevice.SOURCE_TOUCHSCREEN);multi(a,261,x-80,y,x+80,y);
            });Thread.sleep(150);
            scenario.onActivity(a->{View s=surface(a);float x=s.getWidth()/2f,y=s.getHeight()/2f;multi(a,2,x-140,y,x+140,y);});Thread.sleep(80);
            scenario.onActivity(a->{View s=surface(a);float x=s.getWidth()/2f,y=s.getHeight()/2f;multi(a,2,x-220,y,x+220,y);});Thread.sleep(80);
            scenario.onActivity(a->{View s=surface(a);float x=s.getWidth()/2f,y=s.getHeight()/2f;multi(a,2,x-290,y,x+290,y);multi(a,2,x-400,y,x+400,y);multi(a,262,x-400,y,x+400,y);
                float yaw=state(a).yaw;event(a,2,x-300,y-150,InputDevice.SOURCE_TOUCHSCREEN);assertEquals(yaw,state(a).yaw,.001f);event(a,1,x-300,y-150,InputDevice.SOURCE_TOUCHSCREEN);assertTrue("pinch zoom="+state(a).zoom,state(a).zoom<.95f);
            });Thread.sleep(400);capture("02-orbit-and-pinch");
            scenario.onActivity(a->{View s=surface(a);event(a,0,s.getWidth()*.95f,25,InputDevice.SOURCE_TOUCHSCREEN);event(a,1,s.getWidth()*.95f,25,InputDevice.SOURCE_TOUCHSCREEN);});Thread.sleep(80);
            scenario.onActivity(a->{View s=surface(a);event(a,0,s.getWidth()*.95f,25,InputDevice.SOURCE_TOUCHSCREEN);event(a,1,s.getWidth()*.95f,25,InputDevice.SOURCE_TOUCHSCREEN);});Thread.sleep(500);
            scenario.onActivity(a->{assertEquals(0,state(a).yaw,.001f);assertEquals(0,state(a).pitch,.001f);assertEquals(1,state(a).zoom,.001f);
                MotionEvent.PointerProperties prop=new MotionEvent.PointerProperties();prop.id=0;prop.toolType=MotionEvent.TOOL_TYPE_MOUSE;MotionEvent.PointerCoords coords=new MotionEvent.PointerCoords();coords.x=200;coords.y=200;coords.setAxisValue(MotionEvent.AXIS_VSCROLL,2);
                long now=SystemClock.uptimeMillis();MotionEvent wheel=MotionEvent.obtain(now,now,MotionEvent.ACTION_SCROLL,1,new MotionEvent.PointerProperties[]{prop},new MotionEvent.PointerCoords[]{coords},0,0,1,1,0,0,InputDevice.SOURCE_MOUSE,0);assertTrue(surface(a).dispatchGenericMotionEvent(wheel));wheel.recycle();assertTrue(state(a).zoom<1);
            });
        }
    }
    @Test public void readableFocusEditingAutomaticAdvanceAndBothOtherWeapons()throws Exception{
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->weapon(a).restore(Arrays.asList("Chicken Breast","Sweet Potatoes","Greek Yogurt","Rice","Beef","Eggs"),0));Thread.sleep(500);
            scenario.onActivity(a->{float[] p=center(a,0);event(a,0,p[0],p[1],InputDevice.SOURCE_TOUCHSCREEN);event(a,1,p[0],p[1],InputDevice.SOURCE_TOUCHSCREEN);});Thread.sleep(900);
            scenario.onActivity(a->{assertTrue("Native ingredient editor must receive cartridge taps; active="+weapon(a).magazine().active(),((EditText)a.findViewById(R.id.ammunitionEditor)).hasFocus());assertEquals(1f,((EditText)a.findViewById(R.id.ammunitionEditor)).getAlpha(),.001f);assertEquals(1024,((Bitmap)field(renderer(a),"labelBitmap")).getWidth());});capture("03-readable-cartridge");
            scenario.onActivity(a->{EditText e=a.findViewById(R.id.ammunitionEditor);android.view.inputmethod.InputConnection input=e.onCreateInputConnection(new EditorInfo());input.finishComposingText();e.selectAll();input.commitText("Turkey Breast",1);e.onCreateInputConnection(new EditorInfo()).performEditorAction(EditorInfo.IME_ACTION_NEXT);});Thread.sleep(850);
            scenario.onActivity(a->{assertEquals(1,weapon(a).magazine().active());assertTrue("Ingredients="+weapon(a).magazine().ingredients(),weapon(a).magazine().ingredients().contains("Turkey Breast"));assertEquals("Sweet Potatoes",((EditText)a.findViewById(R.id.ammunitionEditor)).getText().toString());});capture("04-follow-next-chamber");
            scenario.onActivity(a->{a.findViewById(R.id.btnCut).performClick();});Thread.sleep(500);scenario.onActivity(a->{mouseOrbit(a);});Thread.sleep(600);
            scenario.onActivity(a->{assertTrue(Math.abs(state(a).yaw)>1);weapon(a).select(0);});Thread.sleep(600);capture("05-rifle-inspection");
            scenario.onActivity(a->{a.findViewById(R.id.btnBulk).performClick();});Thread.sleep(500);scenario.onActivity(a->{mouseOrbit(a);});Thread.sleep(600);scenario.onActivity(a->{float[] p=center(a,0);event(a,0,p[0],p[1],InputDevice.SOURCE_TOUCHSCREEN);event(a,1,p[0],p[1],InputDevice.SOURCE_TOUCHSCREEN);});Thread.sleep(850);scenario.onActivity(a->assertTrue(((EditText)a.findViewById(R.id.ammunitionEditor)).hasFocus()));capture("06-shotgun-inspection");
            scenario.onActivity(a->{float yaw=state(a).yaw,angle=state(a).cylinderAngle;weapon(a).lock(true);mouseOrbit(a);assertEquals(yaw,state(a).yaw,.001f);assertEquals(angle,state(a).cylinderAngle,.001f);weapon(a).lock(false);});
        }
    }
}
