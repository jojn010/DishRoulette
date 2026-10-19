package com.example.dishroulette;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import java.util.List;

/** Owns the accessible native ingredient editor, mechanical loading and interactive scene. */
public final class WeaponInputView extends FrameLayout {
    private final IngredientMagazine magazine = new IngredientMagazine();
    private final GLSurfaceView surface;
    private final WeaponRenderer renderer = new WeaponRenderer();
    private final EditText keyboard;
    private final WeaponGestureController gestures;
    private boolean editing, pendingCommit;
    private boolean discovery;
    private final android.widget.LinearLayout editorBar;
    private final android.widget.Button loadButton;
    private boolean syncing, locked, loading;
    private String goal = "Balanced";
    private ValueAnimator animation, transition, focusAnimation;
    private boolean focusTarget;
    private Runnable changed;
    private MechanicalAudio audio;
    public WeaponInputView(Context c, AttributeSet attrs) {
        super(c, attrs);
        surface = new GLSurfaceView(c) {
            @Override public void queueEvent(Runnable task) { super.queueEvent(task); requestRender(); }
            @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
                super.onSizeChanged(w, h, oldw, oldh);
                if (w > 0 && h > 0) {
                    int pixels = Math.min(1080, w);
                    getHolder().setFixedSize(pixels, Math.max(1, h * pixels / w));
                }
            }
        };
        surface.setEGLContextClientVersion(2);
        surface.setEGLConfigChooser(8, 8, 8, 0, 16, 0);
        surface.setPreserveEGLContextOnPause(true);
        surface.setRenderer(renderer);
        surface.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        renderer.requestFrame = surface::requestRender;
        LayoutParams sceneParams = new LayoutParams(-1, -1); sceneParams.bottomMargin = dp(64);
        addView(surface, sceneParams);
        keyboard = new EditText(c);
        keyboard.setId(R.id.ammunitionEditor);
        keyboard.setSingleLine(true);
        keyboard.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        keyboard.setImeOptions(EditorInfo.IME_ACTION_NEXT | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        keyboard.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)});
        editorBar = new android.widget.LinearLayout(c);
        editorBar.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        editorBar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        editorBar.setPadding(dp(16), dp(4), dp(16), dp(4));
        keyboard.setBackgroundResource(R.drawable.console_panel);
        keyboard.setTextColor(c.getColor(R.color.ink));
        keyboard.setHintTextColor(c.getColor(R.color.muted));
        keyboard.setTextSize(15);
        keyboard.setPadding(dp(14),0,dp(8),0);
        keyboard.setHint("Ingredient / tap to load");
        keyboard.setCursorVisible(true);
        keyboard.setSaveEnabled(false);
        keyboard.setContentDescription("Active ingredient. Enter a name, then press Next or Load.");
        editorBar.addView(keyboard, new android.widget.LinearLayout.LayoutParams(0, dp(52), 1));
        loadButton = new androidx.appcompat.widget.AppCompatButton(c);
        loadButton.setText("+ LOAD"); loadButton.setTextSize(12); loadButton.setTextColor(c.getColor(R.color.accent));
        loadButton.setBackgroundResource(R.drawable.table_control);
        loadButton.setContentDescription("Load ingredient and select next cartridge");
        android.widget.LinearLayout.LayoutParams loadParams = new android.widget.LinearLayout.LayoutParams(dp(80), dp(52));
        loadParams.leftMargin=dp(6); editorBar.addView(loadButton, loadParams);
        addView(editorBar,new LayoutParams(-1,dp(64),android.view.Gravity.BOTTOM));
        loadButton.setOnClickListener(v -> commit());
        keyboard.setOnFocusChangeListener((v, focused) -> { if(focused) editing=true; });
        keyboard.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!syncing && !locked && !loading) { magazine.edit(s.toString()); publish(); }
            }
            @Override public void afterTextChanged(Editable e) { }
        });
        keyboard.setOnEditorActionListener((v, action, event) -> {
            if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) commit();
                return true;
            }
            if (action == EditorInfo.IME_ACTION_NEXT || action == EditorInfo.IME_ACTION_DONE || action == EditorInfo.IME_ACTION_GO) { commit(); return true; }
            return false;
        });
        gestures = new WeaponGestureController(c, renderer, new WeaponGestureController.Listener() {
            @Override public void poseChanged(InspectionState pose) {
                float yaw=pose.yaw,pitch=pose.pitch,zoom=pose.zoom;
                surface.queueEvent(() -> {renderer.inspectYaw=yaw;renderer.inspectPitch=pitch;renderer.inspectZoom=zoom;});
            }
            @Override public void cylinderChanged(float angle,boolean manipulating) {surface.queueEvent(() -> renderer.setCylinderAngle(angle,manipulating));}
            @Override public void chamberSettled(int chamber) {
                magazine.select(magazine.page()*6+chamber);syncKeyboard();publish();
            }
            @Override public void editChamber(int chamber) {select(magazine.page()*6+chamber);}
            @Override public void inspectionStarted() {hideKeyboardForInspection();}
            @Override public void cameraReset() {hideKeyboardForInspection();setPresentationFocus(false);}
            @Override public void rotationFinished() {if(pendingCommit && editing && !locked){pendingCommit=false;commit();}}
            @Override public void detent() {if(audio!=null)audio.detent();}
        });
        surface.setOnTouchListener(gestures);
        surface.setOnGenericMotionListener(gestures);
        surface.setFocusableInTouchMode(true);
        setFocusableInTouchMode(true);requestFocus();
        publish();
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    public void setDiscovery(boolean value) {
        discovery=value;
        keyboard.setHint(value ? "Chef’s choice / no ingredients needed" : "Ingredient / tap to load");
        keyboard.setEnabled(!value && !locked); loadButton.setEnabled(!value && !locked);
        editorBar.setAlpha(value ? .65f : 1f);
        if(value) hideKeyboardForInspection();
        surface.queueEvent(() -> renderer.discovery=value);
    }
    public void refreshExperience() { boolean reduce=Experience.reduced(getContext()); surface.queueEvent(() -> renderer.reducedMotion=reduce); }
    public void inspect() { hideKeyboardForInspection(); setPresentationFocus(false); }
    public void setAudio(MechanicalAudio value) { audio = value; }
    public IngredientMagazine magazine() { return magazine; }
    public boolean isLoading() { return loading || gestures.isSettling(); }
    public void setChangedListener(Runnable value) { changed = value; publish(); }
    public void restore(List<String> slots, int active) { magazine.restore(slots, active); syncKeyboard(); publish(); if(goal.equals("Balanced"))gestures.align(magazine.active()%6,false); }
    private void publish() {
        String[] text = magazine.visible(); int active = magazine.active() % 6;
        surface.queueEvent(() -> renderer.setIngredients(text, active));
        if (changed != null) changed.run();
    }
    private void syncKeyboard() {
        syncing = true; keyboard.setText(magazine.text()); keyboard.setSelection(keyboard.length()); syncing = false;
    }
    public void select(int slot) {
        if (locked || loading || discovery) return;
        magazine.select(slot); syncKeyboard(); publish();
        if(goal.equals("Balanced"))gestures.align(slot%6,true);
        // Focus restores a useful side angle while preserving a modest inspection tilt.
        gestures.state.yaw=Math.max(-40,Math.min(40,gestures.state.yaw));
        gestures.state.pitch=Math.max(-30,Math.min(30,gestures.state.pitch));
        float yaw=gestures.state.yaw,pitch=gestures.state.pitch;
        surface.queueEvent(() -> {renderer.inspectYaw=yaw;renderer.inspectPitch=pitch;});
        focusCartridge();
    }
    public void page(int direction) {
        if (locked || loading) return;
        int next = Math.max(0, magazine.page() + direction);
        select(next * 6);
    }
    public void clearChamber() { if (!locked && !loading) { magazine.edit(""); syncKeyboard(); publish(); focusCartridge(); } }
    public void clearAll() {
        if (locked || loading) return;
        hideKeyboardForInspection();
        // Finish any detent before clearing so its callback cannot restore the old selection.
        gestures.setEnabled(false);
        restore(null, 0);
        gestures.state.resetCamera();
        surface.queueEvent(() -> {renderer.inspectYaw=0;renderer.inspectPitch=0;renderer.inspectZoom=1;});
        setPresentationFocus(false);
        gestures.setEnabled(true);
    }
    public void focusCartridge() {
        if (locked || loading || discovery) return;
        editing=true;
        setPresentationFocus(true);
        showKeyboard();
    }
    private void setPresentationFocus(boolean focused) {
        if (focusTarget == focused) return;
        focusTarget = focused;
        if (focusAnimation != null) focusAnimation.cancel();
        float start = presentationFocus;
        focusAnimation = ValueAnimator.ofFloat(start, focused ? .25f : 0f);
        focusAnimation.setDuration(Experience.duration(getContext(),320));
        focusAnimation.setInterpolator(new android.view.animation.DecelerateInterpolator());
        focusAnimation.addUpdateListener(a -> {
            presentationFocus = (float) a.getAnimatedValue();
            float value = presentationFocus;
            surface.queueEvent(() -> renderer.focus = value);
        });
        focusAnimation.start();
    }
    private float presentationFocus;
    private void showKeyboard() {
        keyboard.requestFocus();
        InputMethodManager ime = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (ime != null) ime.showSoftInput(keyboard, InputMethodManager.SHOW_IMPLICIT);
    }
    public boolean commit() {
        if (discovery || locked || loading || magazine.text().trim().isEmpty()) return false;
        if (gestures.isSettling()) {pendingCommit=editing;return editing;}
        String error = magazine.validationError();
        if (error != null) { Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show(); return false; }
        // Freeze this page until its geometry settles, then publish the new active slot/page.
        if (!magazine.commit()) return false;
        loading = true; gestures.setEnabled(false); keyboard.setEnabled(false);
        if (audio != null) audio.load(goal);
        animation = ValueAnimator.ofFloat(0, 1);
        animation.setDuration(Experience.duration(getContext(),goal.equals("Bulking") ? 640 : 540));
        animation.setInterpolator(new android.view.animation.LinearInterpolator());
        animation.addUpdateListener(a -> { float t = (float) a.getAnimatedValue(); surface.queueEvent(() -> renderer.load(t)); });
        animation.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator a) {
                loading = false; keyboard.setEnabled(!locked && !discovery); loadButton.setEnabled(!locked && !discovery);
                if(goal.equals("Balanced"))gestures.automaticAdvance();
                surface.queueEvent(renderer::settleLoad);
                gestures.setEnabled(!locked);
                syncKeyboard(); publish();
                if (!locked && editing) {showKeyboard();}
            }
        });
        animation.start(); return true;
    }
    public void setGoal(String value) {
        if (goal.equals(value)) return;
        goal = value;
        setPresentationFocus(false);
        if (animation != null && loading) animation.end();
        if (transition != null) transition.cancel();
        surface.queueEvent(() -> renderer.setGoal(value));
        if(value.equals("Balanced"))gestures.align(magazine.active()%6,false);
        transition = ValueAnimator.ofFloat(1, 0); transition.setDuration(Experience.duration(getContext(),420));
        transition.addUpdateListener(a -> { float t=(float)a.getAnimatedValue(); surface.queueEvent(() -> renderer.transition=t); });
        transition.start();
    }
    public void lock(boolean value) { locked = value; gestures.setEnabled(!value && !loading); keyboard.setEnabled(!value && !loading && !discovery); loadButton.setEnabled(!value && !loading && !discovery); }
    private void hideKeyboardForInspection() {
        editing=false;pendingCommit=false;keyboard.clearFocus();
        InputMethodManager ime=(InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if(ime!=null)ime.hideSoftInputFromWindow(keyboard.getWindowToken(),0);
    }
    public void fire(Runnable complete) {
        lock(true);
        InputMethodManager ime = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (ime != null) ime.hideSoftInputFromWindow(keyboard.getWindowToken(), 0);
        keyboard.clearFocus();
        setPresentationFocus(false);
        surface.queueEvent(() -> renderer.waiting=true);
        animation = ValueAnimator.ofFloat(0,1); animation.setDuration(Experience.reduced(getContext()) ? 250 : 2600);
        animation.setInterpolator(new android.view.animation.LinearInterpolator());
        final boolean[] sounded = {false};
        animation.addUpdateListener(a -> {
            float t=(float)a.getAnimatedValue();surface.queueEvent(() -> renderer.shot=t);
            if (t >= .29f && !sounded[0]) { sounded[0] = true; if (audio != null) audio.fire(); }
        });
        animation.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;
            @Override public void onAnimationCancel(Animator a) { cancelled=true; }
            @Override public void onAnimationEnd(Animator a) { surface.queueEvent(() -> renderer.shot=-1); if(!cancelled) complete.run(); }
        });
        animation.start();
    }
    public void cancelFire() { if(animation!=null){animation.removeAllListeners();animation.cancel();} finish(); }
    public void finish() { lock(false); surface.queueEvent(() -> { renderer.waiting=false;renderer.shot=-1; }); }
    public void resume() {
        surface.onResume();
        gestures.resume();
        if (animation != null && animation.isPaused()) animation.resume();
        if (transition != null && transition.isPaused()) transition.resume();
        if (focusAnimation != null && focusAnimation.isPaused()) focusAnimation.resume();
    }
    public void pause() {
        gestures.pause();
        if (animation != null && animation.isRunning()) animation.pause();
        if (transition != null && transition.isRunning()) transition.pause();
        if (focusAnimation != null && focusAnimation.isRunning()) focusAnimation.pause();
        surface.onPause();
    }
    public void dispose() {
        locked=true;
        gestures.dispose();
        if (animation!=null) { animation.removeAllListeners(); animation.cancel(); }
        if (transition!=null) transition.cancel();
        if (focusAnimation!=null) focusAnimation.cancel();
    }
}
