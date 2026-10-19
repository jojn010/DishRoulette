package com.example.dishroulette;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Full-screen native recipe library and cooking workspace; progress uses existing local storage. */
final class KitchenScreens {
    interface Actions {
        void open(RecipeCatalog.Recipe recipe);
        void remove(RecipeCatalog.Recipe recipe);
    }
    private final MainActivity activity;
    private final FrameLayout host;
    private final SharedPreferences prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String screen = "";
    private RecipeCatalog.Recipe cooking;
    private Runnable timerTick;
    private LinearLayout body;
    private final Actions actions;

    KitchenScreens(MainActivity activity, FrameLayout host, SharedPreferences prefs, Actions actions) {
        this.activity=activity;this.host=host;this.prefs=prefs;this.actions=actions;
    }
    String screen() { return screen; }
    RecipeCatalog.Recipe cookingRecipe() { return cooking; }
    boolean isOpen() { return host.getVisibility()==View.VISIBLE; }
    void close() { stopTick(); host.removeAllViews(); host.setVisibility(View.GONE); screen=""; }
    void dispose() { stopTick(); }
    private void stopTick() { if(timerTick!=null)handler.removeCallbacks(timerTick);timerTick=null; }
    private int dp(int n) { return Math.round(n*activity.getResources().getDisplayMetrics().density); }
    private void base(String label, String title, Runnable back) {
        stopTick();host.removeAllViews();host.setVisibility(View.VISIBLE);
        ScrollView scroll=new ScrollView(activity);scroll.setFillViewport(true);
        body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(24),dp(16),dp(24),dp(28));
        // Limit long lines on tablets while allowing comfortable phone typography.
        FrameLayout page=new FrameLayout(activity);
        FrameLayout.LayoutParams width=new FrameLayout.LayoutParams(Math.min(activity.getResources().getDisplayMetrics().widthPixels,dp(720)),-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        page.addView(body,width);scroll.addView(page);host.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        button(body,"← BACK",false,back);
        text(body,label,11,R.color.accent,"monospace",24);
        text(body,title,34,R.color.ink,"serif",12);
        line(body);
    }
    private void line(LinearLayout parent) { View v=new View(activity);v.setBackgroundColor(activity.getColor(R.color.line));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));lp.topMargin=dp(20);lp.bottomMargin=dp(12);parent.addView(v,lp); }
    private TextView text(LinearLayout parent,String value,int size,int color,String font,int margin) {
        TextView t=new TextView(activity);t.setText(value);t.setTextSize(size);t.setTextColor(activity.getColor(color));t.setTypeface(Typeface.create(font,Typeface.NORMAL));t.setLineSpacing(dp(3),1);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(margin);parent.addView(t,lp);return t;
    }
    private Button button(LinearLayout parent,String title,boolean primary,Runnable click) {
        Button b=new androidx.appcompat.widget.AppCompatButton(activity);b.setText(title);b.setTextSize(13);b.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));b.setTextColor(activity.getColor(primary?R.color.on_accent:R.color.ink));
        b.setBackgroundResource(R.drawable.table_control);if(primary)b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(activity.getColor(R.color.accent)));
        b.setMinHeight(dp(52));b.setPadding(dp(14),dp(8),dp(14),dp(8));b.setOnClickListener(v->click.run());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(10);parent.addView(b,lp);return b;
    }
    void saved(List<RecipeCatalog.Recipe> recipes) {
        screen="saved";cooking=null;
        base("THE RECIPE ARCHIVE / "+recipes.size()+" SAVED","Good meals.\nWorth keeping.",this::close);
        if(recipes.isEmpty()){
            text(body,"Your first favorite starts at the table.",22,R.color.ink,"sans-serif-condensed",28);
            text(body,"Save a dish after your next shot. Your recipes stay on this device, ready whenever you are.",16,R.color.muted,"sans",12);
            button(body,"RETURN TO LOADOUT →",true,this::close);return;
        }
        int index=0;
        for(RecipeCatalog.Recipe recipe:recipes){
            text(body,String.format(Locale.ROOT,"%02d / %s",++index,recipe.meal.toUpperCase(Locale.ROOT)),11,R.color.accent,"monospace",18);
            text(body,recipe.title,25,R.color.ink,"sans-serif-condensed",8);
            text(body,recipe.description,15,R.color.muted,"sans",8);
            text(body,meta(recipe),12,R.color.accent,"monospace",12);
            button(body,"OPEN RECIPE →",true,()->{close();actions.open(recipe);});
            button(body,"REMOVE FROM SAVED",false,()->{
                actions.remove(recipe);List<RecipeCatalog.Recipe> remaining=new ArrayList<>(recipes);remaining.remove(recipe);saved(remaining);
            });line(body);
        }
    }
    static String meta(RecipeCatalog.Recipe r) {
        String value=r.minutes+" MIN  /  "+r.servings+(r.servings==1?" SERVING":" SERVINGS");
        if(r.cuisine!=null&&!r.cuisine.trim().isEmpty())value+="\n"+r.cuisine;
        if(r.goal!=null&&!r.goal.trim().isEmpty())value+="  /  "+r.goal;
        return value;
    }
    static List<String> steps(RecipeCatalog.Recipe recipe) {
        List<String> result=new ArrayList<>();
        for(String s:recipe.method.split("\\n\\s*\\n|\\n(?=\\d+[.)]\\s)")){
            String clean=s.trim().replaceFirst("^\\d+[.)]\\s*","");if(!clean.isEmpty())result.add(clean);
        }
        return result;
    }
    static int suggestedMinutes(String step) {
        Matcher m=Pattern.compile("(?i)(\\d{1,3})(?:\\s*[-–]\\s*(\\d{1,3}))?\\s*(?:minutes?|mins?)\\b").matcher(step);
        if(!m.find())return 0;
        return Math.max(1,Math.min(240,Integer.parseInt(m.group(2)==null?m.group(1):m.group(2))));
    }
    void cook(RecipeCatalog.Recipe recipe) {
        screen="cooking";cooking=recipe;
        List<String> steps=steps(recipe);
        int selected=Math.max(0,Math.min(steps.size()-1,prefs.getInt("cook_step_"+recipe.id,0)));
        base("03 / LET’S COOK",recipe.title,this::close);
        text(body,meta(recipe),12,R.color.accent,"monospace",8);
        LinearLayout checklist=new LinearLayout(activity);checklist.setOrientation(LinearLayout.VERTICAL);
        Button checklistToggle=button(body,"INGREDIENT CHECKLIST  +",false,()->{});
        checklistToggle.setContentDescription("Show or hide ingredient checklist");
        body.addView(checklist);checklist.setVisibility(View.GONE);
        checklistToggle.setOnClickListener(v->{boolean open=checklist.getVisibility()!=View.VISIBLE;checklist.setVisibility(open?View.VISIBLE:View.GONE);checklistToggle.setText(open?"INGREDIENT CHECKLIST  −":"INGREDIENT CHECKLIST  +");});
        int ingredient=0;
        for(String value:recipe.ingredients.split("\\n")){
            if(value.trim().isEmpty())continue;
            String key="cook_ingredient_"+recipe.id+"_"+ingredient++;
            CheckBox check=new androidx.appcompat.widget.AppCompatCheckBox(activity);check.setText(value);check.setTextSize(17);check.setTextColor(activity.getColor(R.color.ink));check.setMinHeight(dp(52));check.setPadding(0,dp(5),0,dp(5));check.setChecked(prefs.getBoolean(key,false));
            check.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean(key,on).apply());checklist.addView(check,new LinearLayout.LayoutParams(-1,-2));
        }
        if(activity.hasExclusions())text(body,activity.getString(R.string.allergy_note),13,R.color.muted,"sans",12);
        line(body);
        TextView progress=text(body,"",11,R.color.accent,"monospace",12);progress.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        ProgressBar bar=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(steps.size());bar.setProgressTintList(android.content.res.ColorStateList.valueOf(activity.getColor(R.color.accent)));LinearLayout.LayoutParams barLp=new LinearLayout.LayoutParams(-1,dp(4));barLp.topMargin=dp(14);body.addView(bar,barLp);
        TextView stepNumber=text(body,"",32,R.color.accent,"serif",24);
        TextView instruction=text(body,"",21,R.color.ink,"sans",14);
        CheckBox done=new androidx.appcompat.widget.AppCompatCheckBox(activity);done.setText("Mark this step complete");done.setTextSize(16);done.setTextColor(activity.getColor(R.color.ink));done.setMinHeight(dp(56));body.addView(done);
        TextView timer=text(body,"",18,R.color.accent,"monospace",8);timer.setContentDescription("Cooking timer");
        LinearLayout timerActions=new LinearLayout(activity);timerActions.setOrientation(LinearLayout.VERTICAL);body.addView(timerActions);
        Button startTimer=button(timerActions,"",false,()->{});
        Button resetTimer=button(timerActions,"RESET TIMER",false,()->{});
        text(timerActions,"Timer is shown here. No background alarm is scheduled.",12,R.color.muted,"sans",8);
        LinearLayout nav=new LinearLayout(activity);nav.setOrientation(LinearLayout.HORIZONTAL);body.addView(nav);
        Button previous=button(nav,"← PREVIOUS",false,()->{}),next=button(nav,"NEXT STEP →",true,()->{});
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,-2,1);left.topMargin=dp(18);left.rightMargin=dp(6);previous.setLayoutParams(left);
        LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,-2,1);right.topMargin=dp(18);next.setLayoutParams(right);
        TextView finish=text(body,"",19,R.color.accent,"serif",20);
        button(body,"BACK TO RECIPE OVERVIEW",false,this::close);
        final int[] current={selected};
        final Runnable[] render={null};
        render[0]=()->{
            int step=current[0];String completeKey="cook_done_"+recipe.id+"_"+step;
            prefs.edit().putInt("cook_step_"+recipe.id,step).apply();
            int completed=0;for(int i=0;i<steps.size();i++)if(prefs.getBoolean("cook_done_"+recipe.id+"_"+i,false))completed++;
            progress.setText("STEP "+(step+1)+" OF "+steps.size()+"  /  "+completed+" COMPLETE");bar.setProgress(completed);
            stepNumber.setText(String.format(Locale.ROOT,"%02d /",step+1));instruction.setText(steps.get(step));
            done.setOnCheckedChangeListener(null);done.setChecked(prefs.getBoolean(completeKey,false));
            done.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean(completeKey,on).apply();render[0].run();});
            previous.setEnabled(step>0);next.setEnabled(step<steps.size()-1);
            finish.setText(completed==steps.size()?"Service is ready. Enjoy your meal.":"");
            int minutes=suggestedMinutes(steps.get(step));timerActions.setVisibility(minutes>0?View.VISIBLE:View.GONE);timer.setVisibility(minutes>0?View.VISIBLE:View.GONE);
            stopTick();
            if(minutes>0){
                String prefix="cook_timer_"+recipe.id+"_"+step;
                timerTick=new Runnable(){@Override public void run(){
                    long deadline=prefs.getLong(prefix+"_end",0),remaining=deadline>0?Math.max(0,deadline-System.currentTimeMillis()):prefs.getLong(prefix+"_left",minutes*60000L);
                    timer.setText(String.format(Locale.ROOT,"TIMER  %02d:%02d%s",(remaining+999)/60000,((remaining+999)/1000)%60,deadline>0&&remaining==0?" / READY":""));
                    startTimer.setText(deadline>0&&remaining>0?"PAUSE TIMER":remaining==0?"RESTART "+minutes+" MIN TIMER":"START / RESUME TIMER");
                    if(deadline>0&&remaining==0)prefs.edit().putLong(prefix+"_end",0).putLong(prefix+"_left",0).apply();
                    handler.postDelayed(this,1000);
                }};timerTick.run();
                startTimer.setOnClickListener(v->{
                    long end=prefs.getLong(prefix+"_end",0);SharedPreferences.Editor e=prefs.edit();
                    if(end>System.currentTimeMillis())e.putLong(prefix+"_left",end-System.currentTimeMillis()).putLong(prefix+"_end",0);
                    else {long leftMs=prefs.getLong(prefix+"_left",minutes*60000L);if(leftMs<=0)leftMs=minutes*60000L;e.putLong(prefix+"_end",System.currentTimeMillis()+leftMs);}
                    e.apply();render[0].run();
                });
                resetTimer.setOnClickListener(v->{prefs.edit().remove(prefix+"_end").remove(prefix+"_left").apply();render[0].run();});
            }
        };
        previous.setOnClickListener(v->{if(current[0]>0){current[0]--;render[0].run();}});
        next.setOnClickListener(v->{if(current[0]<steps.size()-1){current[0]++;render[0].run();}});
        render[0].run();
    }
}
