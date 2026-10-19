package com.example.dishroulette;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.GestureDetector;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

/** Pointer arbitration only. No ingredient strings or list mutations live in this controller. */
final class WeaponGestureController implements View.OnTouchListener,View.OnGenericMotionListener {
    interface Listener {
        void poseChanged(InspectionState pose);
        void cylinderChanged(float angle,boolean manipulating);
        void chamberSettled(int chamber);
        void editChamber(int chamber);
        void inspectionStarted();
        void cameraReset();
        void detent();
        void rotationFinished();
    }
    final InspectionState state=new InspectionState();
    private final WeaponRenderer picker;
    private final Listener listener;
    private final GestureDetector taps;
    private final ScaleGestureDetector scale;
    private final int slop;
    private View view;
    private VelocityTracker velocity;
    private ValueAnimator settlement;
    private int pointer=-1;
    private float downX,downY,lastX,lastY,dragScale;
    private boolean enabled=true,cylinderCandidate,dragging,pinching,suppressSingle,settling,doubleTap;
    WeaponGestureController(Context context,WeaponRenderer picker,Listener listener){
        this.picker=picker;this.listener=listener;slop=ViewConfiguration.get(context).getScaledTouchSlop();
        taps=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
            @Override public boolean onDown(MotionEvent e){return true;}
            @Override public boolean onSingleTapConfirmed(MotionEvent e){
                if(!enabled||view==null||suppressSingle||dragging)return false;
                int hit=picker.hit(e.getX()/view.getWidth(),e.getY()/view.getHeight());
                if(hit>=0)listener.editChamber(hit);
                return true;
            }
            @Override public boolean onDoubleTap(MotionEvent e){doubleTap=true;return true;}
            @Override public boolean onDoubleTapEvent(MotionEvent e){
                if(e.getActionMasked()!=MotionEvent.ACTION_UP)return true;
                if(!enabled||view==null||!doubleTap||suppressSingle||dragging||pinching)return false;
                doubleTap=false;
                int hit=picker.hit(e.getX()/view.getWidth(),e.getY()/view.getHeight());
                if(hit>=0)listener.editChamber(hit);
                else{state.resetCamera();listener.cameraReset();listener.poseChanged(state);}
                return true;
            }
        });
        scale=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override public boolean onScaleBegin(ScaleGestureDetector d){
                if(!enabled)return false;
                pinching=true;suppressSingle=true;
                if(dragging&&cylinderCandidate)settle(0);
                dragging=false;listener.inspectionStarted();return true;
            }
            @Override public boolean onScale(ScaleGestureDetector d){state.scale(d.getScaleFactor());listener.poseChanged(state);return true;}
            @Override public void onScaleEnd(ScaleGestureDetector d){pinching=false;}
        });
        scale.setQuickScaleEnabled(false); // Double tap is reserved for reset, including mouse double clicks.
    }
    boolean isSettling(){return settling;}
    void setEnabled(boolean value){
        enabled=value;
        if(!value){cancelPointers();if(settlement!=null&&settling)settlement.end();}
    }
    void align(int chamber,boolean animate){
        cancelSettlement();
        float target=InspectionState.alignedAngle(state.cylinderAngle,chamber);
        if(!animate){state.cylinderAngle=target;listener.cylinderChanged(target,false);return;}
        animateTo(target,false);
    }
    void automaticAdvance(){state.cylinderAngle-=60;}
    @Override public boolean onTouch(View target,MotionEvent event){
        view=target;
        if(!enabled)return true;
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN){
            pointer=event.getPointerId(0);downX=lastX=event.getX();downY=lastY=event.getY();dragging=false;suppressSingle=false;pinching=false;
            cylinderCandidate=picker.pickCylinder(downX/view.getWidth(),downY/view.getHeight());
            dragScale=picker.cylinderDegreesPerPixel(view.getWidth());
            if(cylinderCandidate)cancelSettlement();
            if(velocity!=null)velocity.recycle();velocity=VelocityTracker.obtain();
        }
        if(velocity!=null)velocity.addMovement(event);
        scale.onTouchEvent(event);taps.onTouchEvent(event);
        if(action==MotionEvent.ACTION_POINTER_DOWN){
            // A second finger owns the stream immediately, even below Android's pinch threshold.
            if(dragging&&cylinderCandidate)settle(0);
            dragging=false;suppressSingle=true;listener.inspectionStarted();return true;
        }
        if(action==MotionEvent.ACTION_MOVE&&!pinching&&!suppressSingle&&pointer>=0){
            int index=event.findPointerIndex(pointer);if(index<0)return true;
            float x=event.getX(index),y=event.getY(index);
            if(!dragging&&Math.hypot(x-downX,y-downY)>slop){dragging=true;listener.inspectionStarted();view.getParent().requestDisallowInterceptTouchEvent(true);}
            if(dragging){
                if(cylinderCandidate){state.cylinderAngle-=(y-lastY)*dragScale;listener.cylinderChanged(state.cylinderAngle,true);}
                else{state.orbit((x-lastX)*180/view.getWidth(),(y-lastY)*125/view.getHeight());listener.poseChanged(state);}
            }
            lastX=x;lastY=y;
        }
        if(action==MotionEvent.ACTION_POINTER_UP){
            int remaining=event.getActionIndex()==0?1:0;
            if(remaining<event.getPointerCount()){pointer=event.getPointerId(remaining);lastX=event.getX(remaining);lastY=event.getY(remaining);}
            suppressSingle=true;
        }
        if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){
            if(dragging&&cylinderCandidate&&!suppressSingle){
                float angularVelocity=0;
                if(action!=MotionEvent.ACTION_CANCEL&&velocity!=null){velocity.computeCurrentVelocity(1000);angularVelocity=-velocity.getYVelocity(pointer)*dragScale;}
                settle(angularVelocity);
            }
            if(velocity!=null){velocity.recycle();velocity=null;}
            pointer=-1;dragging=false;pinching=false;
            view.getParent().requestDisallowInterceptTouchEvent(false);
            if(action==MotionEvent.ACTION_UP)view.performClick();
        }
        return true;
    }
    private void settle(float velocity){animateTo(InspectionState.restingAngle(state.cylinderAngle,velocity),true);}
    private void animateTo(float target,boolean select){
        cancelSettlement();
        float start=state.cylinderAngle;
        if(Math.abs(target-start)<.01f){listener.cylinderChanged(target,false);if(select){listener.chamberSettled(InspectionState.chamber(target));listener.detent();}listener.rotationFinished();return;}
        settling=true;
        settlement=ValueAnimator.ofFloat(start,target);
        settlement.setDuration(Math.min(430,Math.max(200,(int)(180+Math.abs(target-start)*1.4f))));
        settlement.setInterpolator(new android.view.animation.OvershootInterpolator(.45f));
        settlement.addUpdateListener(a->{state.cylinderAngle=(float)a.getAnimatedValue();listener.cylinderChanged(state.cylinderAngle,true);});
        settlement.addListener(new AnimatorListenerAdapter(){
            private boolean cancelled;
            @Override public void onAnimationCancel(Animator a){cancelled=true;}
            @Override public void onAnimationEnd(Animator a){settling=false;if(cancelled)return;state.cylinderAngle=target;listener.cylinderChanged(target,false);if(select){listener.chamberSettled(InspectionState.chamber(target));listener.detent();}listener.rotationFinished();}
        });settlement.start();
    }
    @Override public boolean onGenericMotion(View target,MotionEvent event){
        if(!enabled||!event.isFromSource(InputDevice.SOURCE_CLASS_POINTER)||event.getAction()!=MotionEvent.ACTION_SCROLL)return false;
        float wheel=event.getAxisValue(MotionEvent.AXIS_VSCROLL);if(wheel==0)return false;
        listener.inspectionStarted();state.scale((float)Math.exp(wheel*.13f));listener.poseChanged(state);return true;
    }
    private void cancelSettlement(){if(settlement!=null)settlement.cancel();settling=false;}
    private void cancelPointers(){
        if(velocity!=null){velocity.recycle();velocity=null;}pointer=-1;dragging=false;pinching=false;suppressSingle=true;
        long time=android.os.SystemClock.uptimeMillis();MotionEvent cancel=MotionEvent.obtain(time,time,MotionEvent.ACTION_CANCEL,0,0,0);taps.onTouchEvent(cancel);scale.onTouchEvent(cancel);cancel.recycle();
    }
    void pause(){cancelPointers();if(settlement!=null&&settling)settlement.pause();}
    void resume(){if(settlement!=null&&settlement.isPaused())settlement.resume();}
    void dispose(){enabled=false;cancelPointers();cancelSettlement();}
}
