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
    void close() { host.setKeepScreenOn(false); stopTick(); host.removeAllViews(); host.setVisibility(View.GONE); screen=""; }
    void dispose() { host.setKeepScreenOn(false); stopTick(); }
    private void stopTick() { if(timerTick!=null)handler.removeCallbacks(timerTick);timerTick=null; }
    private int dp(int n) { return Math.round(n*activity.getResources().getDisplayMetrics().density); }
    private void base(String label, String title, Runnable back) {
        stopTick();host.setKeepScreenOn(false);host.removeAllViews();host.setVisibility(View.VISIBLE);
        ScrollView scroll=new ScrollView(activity);scroll.setFillViewport(true);
        body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(24),dp(16),dp(24),dp(28));
        // Limit long lines on tablets while allowing comfortable phone typography.
        FrameLayout page=new FrameLayout(activity);
        FrameLayout.LayoutParams width=new FrameLayout.LayoutParams(Math.min(activity.getResources().getDisplayMetrics().widthPixels,dp(720)),-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL);
        page.addView(body,width);scroll.addView(page);host.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        button(body,activity.getString(R.string.back),false,back);
        text(body,label,11,R.color.accent,"monospace",24);
        text(body,title,34,R.color.ink,"serif",12);
        line(body);
    }
    private void line(LinearLayout parent) { View v=new View(activity);v.setBackgroundColor(UiColors.color(activity,R.color.line));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));lp.topMargin=dp(20);lp.bottomMargin=dp(12);parent.addView(v,lp); }
    private TextView text(LinearLayout parent,String value,int size,int color,String font,int margin) {
        TextView t=new TextView(activity);t.setText(value);t.setTextSize(size);t.setTextColor(UiColors.color(activity,color));t.setTypeface(Typeface.create(InterfaceSettings.language(activity).equals("ar")||activity.isSimple()?"sans":font,Typeface.NORMAL));t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);UiTypography.apply(t);t.setLineSpacing(dp(3),1);androidx.core.view.ViewCompat.setAccessibilityHeading(t,size>=24);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(margin);parent.addView(t,lp);return t;
    }
    private Button button(LinearLayout parent,String title,boolean primary,Runnable click) {
        Button b=new androidx.appcompat.widget.AppCompatButton(activity);b.setText(title);UiTypography.apply(b);b.setTextSize(activity.isSimple()?15:13);b.setAllCaps(false);b.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));b.setTextColor(UiColors.color(activity,primary?R.color.on_accent:R.color.ink));
        b.setBackgroundResource(R.drawable.table_control);
        if(activity.isSimple()){
            android.graphics.drawable.GradientDrawable shape=new android.graphics.drawable.GradientDrawable();shape.setColor(UiColors.color(activity,primary?R.color.accent:R.color.surface));shape.setCornerRadius(dp(14));shape.setStroke(dp(1),UiColors.color(activity,R.color.line));b.setBackground(shape);
        }
        if(primary)b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(UiColors.color(activity,R.color.accent)));
        b.setMinHeight(dp(52));b.setPadding(dp(14),dp(8),dp(14),dp(8));b.setOnClickListener(v->click.run());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(10);UiTypography.apply(b);parent.addView(b,lp);return b;
    }
    void saved(List<RecipeCatalog.Recipe> recipes) {
        screen="saved";cooking=null;
        base(activity.getString(R.string.archive_label,recipes.size()),activity.getString(R.string.archive_title),this::close);
        if(recipes.isEmpty()){
            text(body,activity.getString(R.string.archive_empty_heading),22,R.color.ink,"sans-serif-condensed",28);
            text(body,activity.getString(R.string.archive_empty_detail),16,R.color.muted,"sans",12);
            button(body,activity.getString(R.string.home),true,this::close);return;
        }
        int index=0;
        for(RecipeCatalog.Recipe recipe:recipes){
            text(body,String.format(Locale.ROOT,"%02d / %s",++index,UiLabels.option(activity,recipe.meal)),11,R.color.accent,"monospace",18);
            text(body,recipe.title,25,R.color.ink,"sans-serif-condensed",8);
            text(body,recipe.description,15,R.color.muted,"sans",8);
            text(body,meta(activity,recipe),12,R.color.accent,"monospace",12);
            button(body,activity.getString(R.string.open_recipe),true,()->{close();actions.open(recipe);});
            button(body,activity.getString(R.string.remove_saved),false,()->{
                actions.remove(recipe);List<RecipeCatalog.Recipe> remaining=new ArrayList<>(recipes);remaining.remove(recipe);saved(remaining);
            });line(body);
        }
    }
    void settings(){
        screen="settings";cooking=null;
        base(activity.getString(R.string.settings),activity.getString(R.string.appearance),this::close);
        text(body,activity.getString(R.string.settings_description),16,R.color.muted,"sans",6);
        text(body,activity.getString(R.string.language),21,R.color.ink,"sans",24);
        text(body,activity.getString(R.string.language_description),14,R.color.muted,"sans",8);
        Button english=button(body,"English",InterfaceSettings.language(activity).equals("en"),()->activity.changeLanguage("en"));english.setId(R.id.settingsEnglish);
        Button arabic=button(body,"العربية",InterfaceSettings.language(activity).equals("ar"),()->activity.changeLanguage("ar"));arabic.setId(R.id.settingsArabic);
        line(body);
        Button preferences=button(body,activity.getString(R.string.recipe_preferences),false,activity::showFilters);preferences.setId(R.id.settingsRecipePreferences);
    }

    static String meta(android.content.Context context,RecipeCatalog.Recipe r) {
        String value=context.getString(R.string.recipe_metadata,r.minutes,r.servings);
        if(r.cuisine!=null&&!r.cuisine.trim().isEmpty())value+="\n"+UiLabels.option(context,r.cuisine);
        if(r.goal!=null&&!r.goal.trim().isEmpty())value+=" · "+UiLabels.option(context,r.goal);
        if (com.example.dishroulette.core.NutritionEstimate.valid(r.caloriesPerServing, r.proteinGramsPerServing)) {
            java.text.NumberFormat numbers = java.text.NumberFormat.getNumberInstance(
                    context.getResources().getConfiguration().getLocales().get(0));
            numbers.setMaximumFractionDigits(1);
            value += "\n" + context.getString(R.string.nutrition_estimate,
                    numbers.format(r.caloriesPerServing), numbers.format(r.proteinGramsPerServing));
            if (com.example.dishroulette.core.NutritionEstimate.validNutrient(r.carbohydratesGramsPerServing, 1000))
                value += "\n" + context.getString(R.string.nutrition_carbohydratesGrams, numbers.format(r.carbohydratesGramsPerServing));
            if (com.example.dishroulette.core.NutritionEstimate.validNutrient(r.fatGramsPerServing, 500))
                value += "\n" + context.getString(R.string.nutrition_fatGrams, numbers.format(r.fatGramsPerServing));
            if (com.example.dishroulette.core.NutritionEstimate.validNutrient(r.fiberGramsPerServing, 200))
                value += "\n" + context.getString(R.string.nutrition_fiberGrams, numbers.format(r.fiberGramsPerServing));
            if (com.example.dishroulette.core.NutritionEstimate.validNutrient(r.sugarGramsPerServing, 1000))
                value += "\n" + context.getString(R.string.nutrition_sugarGrams, numbers.format(r.sugarGramsPerServing));
            if (com.example.dishroulette.core.NutritionEstimate.validNutrient(r.sodiumMilligramsPerServing, 20000))
                value += "\n" + context.getString(R.string.nutrition_sodiumMilligrams, numbers.format(r.sodiumMilligramsPerServing));
            value += "\n" + context.getString(R.string.nutrition_variation);
        } else value += "\n" + context.getString(R.string.nutrition_unavailable);
        return value;
    }
    static List<String> steps(RecipeCatalog.Recipe recipe) {
        List<String> result=new ArrayList<>();
        for(String s:recipe.method.split("\\r?\\n\\s*\\r?\\n|\\r?\\n(?=[\\t ]*\\p{Nd}+[.)]\\s)")){
            String clean=s.trim().replaceFirst("^\\p{Nd}+[.)]\\s*","");if(!clean.isEmpty())result.add(clean);
        }
        return result;
    }
    private static final String DURATION_NUMBER = "[0-9٠-٩۰-۹]+(?:[.٫][0-9٠-٩۰-۹]+)?";
    private static final String DURATION_RANGE = "("+DURATION_NUMBER+")(?:\\s*(?:[-–]|إلى|to)\\s*("+DURATION_NUMBER+"))?";
    private static final String MINUTE_UNIT = "minutes?|mins?|دقيقة|دقائق|دقيقتين|دقيقتان";
    private static final Pattern DURATION = Pattern.compile("(?i)(?<![\\p{L}\\p{N}.,٫])(?:"
            +DURATION_RANGE+"\\s*(hours?|hrs?|ساعة|ساعات|ساعتين|ساعتان|"+MINUTE_UNIT+")"
            +"|(ساعة|ساعتين|ساعتان|دقيقتين|دقيقتان))(?![\\p{L}])");
    private static final Pattern ADJACENT_MINUTES = Pattern.compile("(?i)^(?:\\s+(?:and\\s+)?|\\s*و\\s*)(?:"
            +DURATION_RANGE+"\\s*(?:"+MINUTE_UNIT+")|(دقيقتين|دقيقتان))(?![\\p{L}])");

    static int suggestedMinutes(String step) {
        // Keep the first duration; only adjacent minutes extend an hour duration.
        Matcher m=DURATION.matcher(step);
        if(!m.find())return 0;
        String unit=m.group(3)==null?m.group(4):m.group(3);
        boolean hours=unit.toLowerCase(Locale.ROOT).startsWith("h")||unit.startsWith("سا");
        double amount=m.group(1)==null?(unit.startsWith("ساعت")||unit.startsWith("دقيقت")?2:1)
                :durationNumber(m.group(2)==null?m.group(1):m.group(2));
        double minutes=amount*(hours?60:1);
        if(hours){
            Matcher extra=ADJACENT_MINUTES.matcher(step.substring(m.end()));
            if(extra.find())minutes+=extra.group(1)==null?2:durationNumber(extra.group(2)==null?extra.group(1):extra.group(2));
        }
        return Math.max(1,(int)Math.min(240,Math.ceil(minutes)));
    }
    private static double durationNumber(String value) {
        StringBuilder normalized=new StringBuilder();
        for(char c:value.toCharArray())normalized.append(c=='.'||c=='٫'?'.':(char)('0'+Character.digit(c,10)));
        return Double.parseDouble(normalized.toString());
    }
    void cook(RecipeCatalog.Recipe recipe) {
        screen="cooking";cooking=recipe;
        List<String> steps=steps(recipe);
        int selected=Math.max(0,Math.min(steps.size()-1,prefs.getInt("cook_step_"+recipe.id,0)));
        base(activity.getString(R.string.cook_label),recipe.title,this::close);
        // Android only honors this while the cooking view is visible in the foreground.
        host.setKeepScreenOn(true);
        text(body,meta(activity,recipe),12,R.color.accent,"monospace",8);
        LinearLayout checklist=new LinearLayout(activity);checklist.setOrientation(LinearLayout.VERTICAL);
        Button checklistToggle=button(body,activity.getString(R.string.checklist_closed),false,()->{});
        checklistToggle.setContentDescription(activity.getString(R.string.checklist_a11y));
        body.addView(checklist);checklist.setVisibility(View.GONE);
        checklistToggle.setOnClickListener(v->{boolean open=checklist.getVisibility()!=View.VISIBLE;checklist.setVisibility(open?View.VISIBLE:View.GONE);checklistToggle.setText(open?activity.getString(R.string.checklist_open):activity.getString(R.string.checklist_closed));});
        int ingredient=0;
        for(String value:recipe.ingredients.split("\\n")){
            if(value.trim().isEmpty())continue;
            String key="cook_ingredient_"+recipe.id+"_"+ingredient++;
            CheckBox check=new androidx.appcompat.widget.AppCompatCheckBox(activity);check.setText(value);UiTypography.apply(check);check.setTextSize(17);check.setTextColor(UiColors.color(activity,R.color.ink));check.setMinHeight(dp(52));check.setPadding(0,dp(5),0,dp(5));check.setChecked(prefs.getBoolean(key,false));
            check.setOnCheckedChangeListener((v,on)->prefs.edit().putBoolean(key,on).apply());checklist.addView(check,new LinearLayout.LayoutParams(-1,-2));
        }
        if(activity.hasExclusions())text(body,activity.getString(R.string.allergy_note),13,R.color.muted,"sans",12);
        line(body);
        TextView progress=text(body,"",11,R.color.accent,"monospace",12);progress.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        ProgressBar bar=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(steps.size());bar.setProgressTintList(android.content.res.ColorStateList.valueOf(UiColors.color(activity,R.color.accent)));LinearLayout.LayoutParams barLp=new LinearLayout.LayoutParams(-1,dp(4));barLp.topMargin=dp(14);body.addView(bar,barLp);
        TextView stepNumber=text(body,"",32,R.color.accent,"serif",24);
        TextView instruction=text(body,"",21,R.color.ink,"sans",14);
        CheckBox done=new androidx.appcompat.widget.AppCompatCheckBox(activity);done.setText(activity.getString(R.string.step_complete));UiTypography.apply(done);done.setTextSize(16);done.setTextColor(UiColors.color(activity,R.color.ink));done.setMinHeight(dp(56));body.addView(done);
        CookingTimerView timer=new CookingTimerView(activity);timer.setId(R.id.cookingTimerDial);
        LinearLayout.LayoutParams timerLp=new LinearLayout.LayoutParams(Math.min(dp(260),Math.max(1,activity.getResources().getDisplayMetrics().widthPixels-dp(48))),dp(260));
        timerLp.gravity=Gravity.CENTER_HORIZONTAL;timerLp.topMargin=dp(16);
        body.addView(timer,timerLp);
        LinearLayout timerActions=new LinearLayout(activity);timerActions.setOrientation(LinearLayout.VERTICAL);body.addView(timerActions);
        Button startTimer=button(timerActions,"",false,()->{});startTimer.setId(R.id.cookingTimerStart);
        Button resetTimer=button(timerActions,activity.getString(R.string.reset_timer),false,()->{});resetTimer.setId(R.id.cookingTimerReset);
        text(timerActions,activity.getString(R.string.timer_notice),12,R.color.muted,"sans",8);
        LinearLayout nav=new LinearLayout(activity);nav.setOrientation(LinearLayout.HORIZONTAL);body.addView(nav);
        Button previous=button(nav,activity.getString(R.string.previous_step),false,()->{}),next=button(nav,activity.getString(R.string.next_step),true,()->{});
        LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,-2,1);left.topMargin=dp(18);left.rightMargin=dp(6);previous.setLayoutParams(left);
        LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,-2,1);right.topMargin=dp(18);next.setLayoutParams(right);
        TextView finish=text(body,"",19,R.color.accent,"serif",20);
        button(body,activity.getString(R.string.back_overview),false,this::close);
        final int[] current={selected};
        final Runnable[] render={null};
        render[0]=()->{
            int step=current[0];String completeKey="cook_done_"+recipe.id+"_"+step;
            prefs.edit().putInt("cook_step_"+recipe.id,step).apply();
            int completed=0;for(int i=0;i<steps.size();i++)if(prefs.getBoolean("cook_done_"+recipe.id+"_"+i,false))completed++;
            progress.setText(activity.getString(R.string.step_progress,step+1,steps.size(),completed));bar.setProgress(completed);
            stepNumber.setText(String.format(activity.getResources().getConfiguration().getLocales().get(0),"%02d /",step+1));instruction.setText(steps.get(step));
            done.setOnCheckedChangeListener(null);done.setChecked(prefs.getBoolean(completeKey,false));
            done.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean(completeKey,on).apply();render[0].run();});
            previous.setEnabled(step>0);next.setEnabled(step<steps.size()-1);
            finish.setText(completed==steps.size()?activity.getString(R.string.enjoy_meal):"");
            int minutes=suggestedMinutes(steps.get(step));timerActions.setVisibility(minutes>0?View.VISIBLE:View.GONE);timer.setVisibility(minutes>0?View.VISIBLE:View.GONE);
            stopTick();
            if(minutes>0){
                String prefix="cook_timer_"+recipe.id+"_"+step;
                timerTick=new Runnable(){@Override public void run(){
                    long deadline=prefs.getLong(prefix+"_end",0),remaining=deadline>0?Math.max(0,deadline-System.currentTimeMillis()):prefs.getLong(prefix+"_left",minutes*60000L);
                    timer.update(remaining,minutes*60000L,deadline>0 && remaining>0);
                    startTimer.setText(deadline>0&&remaining>0?activity.getString(R.string.pause_timer):remaining==0?activity.getString(R.string.restart_timer,minutes):activity.getString(R.string.start_timer));
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
