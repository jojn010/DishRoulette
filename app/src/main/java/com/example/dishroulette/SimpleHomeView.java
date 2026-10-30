package com.example.dishroulette;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.List;

/** A native, lightweight recipe discovery home. It never references the renderer or 3D meshes. */
final class SimpleHomeView extends LinearLayout {
    private final MainActivity activity;
    private final NativeIngredientLoadout loadout;
    private final EditText input;
    private final TextView ingredientCount,empty,summary,loadingTitle;
    private final LinearLayout pantry,loading,goals;
    private final ChipGroup ingredients;
    private final MaterialButton pantryMode,surpriseMode,generate,add,language;
    private boolean editing,busy,syncing;
    private final ScrollView scroll;
    private int editIndex;
    private final ImageView foodPhoto;
    private final MaterialButton scan,clear;
    private final TextView editHint,peopleCount;
    private final MaterialButton fewerPeople,morePeople;
    SimpleHomeView(MainActivity a,NativeIngredientLoadout loadout){
        super(a);activity=a;this.loadout=loadout;setOrientation(VERTICAL);setBackgroundColor(color(R.color.background));
        LinearLayout header=row();header.setGravity(Gravity.CENTER_VERTICAL);header.setPadding(dp(22),0,dp(22),0);
        TextView logo=label(header,activity.getString(R.string.app_name),23,R.color.ink,0);logo.setTypeface(Typeface.create("serif",Typeface.BOLD));logo.setLayoutParams(new LayoutParams(0,-2,1));logo.setGravity(isArabic()?Gravity.RIGHT:Gravity.LEFT);UiIcons.text(logo,R.drawable.ic_bowl);
        language=button(header,InterfaceSettings.language(a).equals("ar")?"EN":"العربية",false,()->a.changeLanguage(InterfaceSettings.language(a).equals("ar")?"en":"ar"));language.setContentDescription(a.getString(R.string.language));language.setLayoutParams(new LayoutParams(-2,dp(48)));addView(header,new LayoutParams(-1,dp(64)));
        scroll=new ScrollView(a);scroll.setFillViewport(true);addView(scroll,new LayoutParams(-1,0,1));
        FrameLayout frame=new FrameLayout(a);scroll.addView(frame);
        LinearLayout page=column();page.setPadding(dp(24),dp(16),dp(24),dp(28));
        int max=Math.min(a.getResources().getDisplayMetrics().widthPixels,dp(1000));frame.addView(page,new FrameLayout.LayoutParams(max,-2,Gravity.TOP|Gravity.CENTER_HORIZONTAL));
        label(page,a.getString(R.string.simple_eyebrow),11,R.color.accent,8);
        TextView hero=label(page,a.getString(R.string.simple_hero),30,R.color.ink,12);hero.setTypeface(UiTypography.face(a,"serif",Typeface.NORMAL));androidx.core.view.ViewCompat.setAccessibilityHeading(hero,true);
        label(page,a.getString(R.string.simple_intro),15,R.color.muted,10);
        FrameLayout photoCard=new FrameLayout(a);android.graphics.drawable.GradientDrawable photoShape=panel(20,R.color.surface);photoCard.setBackground(photoShape);photoCard.setClipToOutline(true);LayoutParams photoLp=new LayoutParams(-1,dp(150));photoLp.topMargin=dp(18);page.addView(photoCard,photoLp);
        foodPhoto=new ImageView(a);foodPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);foodPhoto.setImageResource(R.drawable.pantry_photo);foodPhoto.setContentDescription(a.getString(R.string.food_photo_description));photoCard.addView(foodPhoto,new FrameLayout.LayoutParams(-1,-1));
        TextView photoCaption=new TextView(a);photoCaption.setText(R.string.food_photo_caption);photoCaption.setTextSize(11);photoCaption.setTextColor(color(R.color.ink));photoCaption.setPadding(dp(12),dp(5),dp(12),dp(5));photoCaption.setBackgroundColor(androidx.core.graphics.ColorUtils.setAlphaComponent(color(R.color.background),238));UiTypography.apply(photoCaption);FrameLayout.LayoutParams cap=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.START);cap.setMargins(dp(10),0,dp(10),dp(10));photoCard.addView(photoCaption,cap);
        LinearLayout modeRail=row();LayoutParams modeLp=new LayoutParams(-1,-2);modeLp.topMargin=dp(24);page.addView(modeRail,modeLp);
        pantryMode=button(modeRail,a.getString(R.string.pantry_mode),false,()->a.chooseGenerationMode("pantry"));
        surpriseMode=button(modeRail,a.getString(R.string.discover_mode),false,()->a.chooseGenerationMode("discover"));
        UiIcons.button(pantryMode,R.drawable.ic_bowl);UiIcons.button(surpriseMode,R.drawable.ic_shuffle);
        LayoutParams one=new LayoutParams(0,-2,1);one.setMarginEnd(dp(8));pantryMode.setLayoutParams(one);surpriseMode.setLayoutParams(new LayoutParams(0,-2,1));
        LinearLayout columns=new LinearLayout(a);boolean wide=a.getResources().getConfiguration().screenWidthDp>=840;columns.setOrientation(wide?HORIZONTAL:VERTICAL);page.addView(columns,new LayoutParams(-1,-2));
        LinearLayout left=column();columns.addView(left,new LayoutParams(wide?0:-1,-2,wide?1.4f:0));
        LinearLayout workspace=column();workspace.setPadding(dp(18),dp(18),dp(18),dp(14));workspace.setBackground(panel(22,R.color.surface));
        LayoutParams workspaceLp=new LayoutParams(-1,-2);workspaceLp.topMargin=dp(18);left.addView(workspace,workspaceLp);
        pantry=column();workspace.addView(pantry,new LayoutParams(-1,-2));
        ingredientCount=label(pantry,"",20,R.color.ink,0);ingredientCount.setTypeface(UiTypography.face(a,"sans-serif",Typeface.BOLD));
        androidx.core.view.ViewCompat.setAccessibilityHeading(ingredientCount,true);ingredientCount.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        label(pantry,a.getString(R.string.simple_pantry_description),14,R.color.muted,6);
        LinearLayout entry=row();LayoutParams entryLp=new LayoutParams(-1,-2);entryLp.topMargin=dp(16);pantry.addView(entry,entryLp);
        input=new androidx.appcompat.widget.AppCompatEditText(a);input.setId(R.id.simpleIngredientInput);input.setSingleLine();UiTypography.apply(input);input.setTextSize(16);input.setTextColor(color(R.color.ink));input.setHintTextColor(color(R.color.muted));input.setHint(R.string.ingredient_hint);input.setContentDescription(a.getString(R.string.add_ingredient));input.setPadding(dp(14),dp(8),dp(14),dp(8));input.setBackground(panel(12,R.color.background));input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)});input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        entry.addView(input,new LayoutParams(0,dp(56),1));add=button(entry,a.getString(R.string.add),true,()->commitPending());LayoutParams addLp=new LayoutParams(-2,dp(56));addLp.setMarginStart(dp(8));add.setLayoutParams(addLp);
        input.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void afterTextChanged(Editable e){}public void onTextChanged(CharSequence s,int st,int before,int count){input.setError(null);if(!syncing)InterfaceSettings.prefs(a).edit().putString("simple_input_draft",s.toString()).apply();}});
        input.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_DONE||event!=null&&event.getKeyCode()==android.view.KeyEvent.KEYCODE_ENTER&&event.getAction()==android.view.KeyEvent.ACTION_DOWN){commitPending();return true;}return false;});
        ingredients=new ChipGroup(a);ingredients.setChipSpacingHorizontal(dp(6));ingredients.setChipSpacingVertical(dp(4));pantry.addView(ingredients,new LayoutParams(-1,-2));
        empty=label(pantry,a.getString(R.string.ingredients_empty)+"\n"+a.getString(R.string.ingredients_empty_detail),15,R.color.muted,16);
        editHint=label(pantry,a.getString(R.string.tap_to_edit),12,R.color.muted,8);
        clear=button(pantry,a.getString(R.string.clear_all),false,()->{loadout.clearAll();editing=false;setDraft("");});
        clear.setStrokeWidth(0);clear.setBackgroundTintList(ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
        scan=button(workspace,a.getString(R.string.photo_scan),false,a::scanIngredients);scan.setId(R.id.photoScan);UiIcons.button(scan,R.drawable.ic_camera);
        scan.setBackgroundTintList(ColorStateList.valueOf(color(R.color.background)));
        LinearLayout right=column();right.setPadding(wide?dp(24):0,dp(24),0,0);columns.addView(right,new LayoutParams(wide?0:-1,-2,wide?1:0));
        androidx.core.view.ViewCompat.setAccessibilityHeading(label(right,a.getString(R.string.recipe_preferences),21,R.color.ink,0),true);
        TextView peopleHeading=label(right,a.getString(R.string.people_heading),18,R.color.ink,18);
        androidx.core.view.ViewCompat.setAccessibilityHeading(peopleHeading,true);
        label(right,a.getString(R.string.people_help),14,R.color.muted,4);
        LinearLayout people=row();people.setGravity(Gravity.CENTER_VERTICAL);right.addView(people,new LayoutParams(-1,-2));
        fewerPeople=button(people,"−",false,()->a.chooseServings(a.profile().servings-1));fewerPeople.setId(R.id.fewerPeople);fewerPeople.setContentDescription(a.getString(R.string.fewer_people));fewerPeople.setLayoutParams(new LayoutParams(dp(56),-2));
        peopleCount=label(people,"",18,R.color.ink,0);peopleCount.setId(R.id.peopleCount);peopleCount.setGravity(Gravity.CENTER);peopleCount.setLayoutParams(new LayoutParams(0,-2,1));peopleCount.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        morePeople=button(people,"+",false,()->a.chooseServings(a.profile().servings+1));morePeople.setId(R.id.morePeople);morePeople.setContentDescription(a.getString(R.string.more_people));morePeople.setLayoutParams(new LayoutParams(dp(56),-2));
        summary=label(right,"",14,R.color.muted,10);
        goals=column();right.addView(goals,new LayoutParams(-1,-2));
        button(right,a.getString(R.string.edit_preferences),false,a::showFilters);
        generate=button(page,a.getString(R.string.find_recipe),true,()->{if(busy){a.cancelGeneration();return;}if(!activity.profile().mode.equals("pantry")||commitPending())a.generate(false);});generate.setId(R.id.simpleGenerate);generate.setTextSize(17);LayoutParams genLp=new LayoutParams(-1,dp(60));genLp.topMargin=dp(24);generate.setLayoutParams(genLp);
        loading=column();loading.setPadding(dp(18),dp(18),dp(18),dp(18));loading.setBackground(panel(18,R.color.surface));LayoutParams waitLp=new LayoutParams(-1,-2);waitLp.topMargin=dp(16);page.addView(loading,waitLp);
        loadingTitle=label(loading,a.getString(R.string.generating),21,R.color.ink,0);loadingTitle.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);label(loading,a.getString(R.string.generating_detail),14,R.color.muted,8);
        ProgressBar progress=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(true);loading.addView(progress,new LayoutParams(-1,dp(8)));button(loading,a.getString(R.string.cancel_generation),false,a::cancelGeneration);loading.setVisibility(GONE);
        page.removeView(generate);LinearLayout actionDock=row();actionDock.setPadding(dp(24),dp(10),dp(24),dp(4));actionDock.setBackgroundColor(color(R.color.background));generate.setMinHeight(dp(60));actionDock.addView(generate,new LayoutParams(-1,-2));addView(actionDock,new LayoutParams(-1,-2));
        page.removeView(loading);page.addView(loading,0);
        LinearLayout navigation=row();navigation.setPadding(dp(20),dp(4),dp(20),dp(8));navigation.setBackgroundColor(color(R.color.background));addView(navigation,new LayoutParams(-1,-2));
        for(int index=0;index<3;index++){final int i=index;MaterialButton b=button(navigation,a.getString(index==0?R.string.home:index==1?R.string.saved:R.string.settings),false,()->{if(i==1)a.showSavedRecipes();else if(i==2)a.showSettings();else if(Experience.reduced(a))scroll.scrollTo(0,0);else scroll.smoothScrollTo(0,0);});UiIcons.button(b,index==0?R.drawable.ic_home:index==1?R.drawable.ic_bookmark:R.drawable.ic_settings);b.setIconGravity(MaterialButton.ICON_GRAVITY_TOP);b.setIconPadding(dp(2));b.setTextSize(12);b.setStrokeWidth(0);b.setCornerRadius(dp(22));b.setSelected(index==0);
            b.setBackgroundTintList(ColorStateList.valueOf(index==0?color(R.color.surface):android.graphics.Color.TRANSPARENT));
            if(index==0)b.setTypeface(UiTypography.face(a,"sans-serif",Typeface.BOLD));
            b.setPadding(dp(4),dp(6),dp(4),dp(6));b.setMinHeight(Math.round(dp(64)*Math.max(1f,getResources().getConfiguration().fontScale)));LayoutParams navLp=new LayoutParams(0,-2,1);navLp.setMarginStart(dp(3));navLp.setMarginEnd(dp(3));b.setLayoutParams(navLp);}
        input.setText(InterfaceSettings.prefs(a).getString("simple_input_draft",""));loadout.setFocusListener(this::editCurrent);refresh();setFocusableInTouchMode(true);requestFocus();
    }
    private boolean isArabic(){return InterfaceSettings.language(activity).equals("ar");}
    private int color(int id){return UiColors.color(activity,id);}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout row(){LinearLayout l=new LinearLayout(activity);l.setOrientation(HORIZONTAL);return l;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(activity);l.setOrientation(VERTICAL);return l;}
    private GradientDrawable panel(int radius,int color){GradientDrawable d=new GradientDrawable();d.setColor(color(color));d.setCornerRadius(dp(radius));d.setStroke(dp(1),color(R.color.line));return d;}
    private TextView label(LinearLayout parent,String value,int size,int color,int top){TextView t=new TextView(activity);t.setText(value);t.setTextSize(size);t.setTextColor(color(color));t.setLineSpacing(dp(3),1);t.setTextDirection(TEXT_DIRECTION_FIRST_STRONG);UiTypography.apply(t);LayoutParams lp=new LayoutParams(-1,-2);lp.topMargin=dp(top);parent.addView(t,lp);return t;}
    private MaterialButton button(LinearLayout parent,String text,boolean primary,Runnable action){MaterialButton b=new MaterialButton(activity);b.setText(text);UiTypography.apply(b);b.setTextSize(14);b.setAllCaps(false);b.setCornerRadius(dp(14));b.setMinHeight(dp(52));b.setTextColor(color(primary?R.color.on_accent:R.color.accent));b.setBackgroundTintList(ColorStateList.valueOf(color(primary?R.color.accent:R.color.surface)));b.setStrokeColor(ColorStateList.valueOf(color(R.color.line)));b.setStrokeWidth(primary?0:dp(1));b.setPadding(dp(12),dp(6),dp(12),dp(6));b.setOnClickListener(v->action.run());parent.addView(b,new LayoutParams(-1,-2));return b;}
    private void setDraft(String value){syncing=true;input.setText(value);syncing=false;InterfaceSettings.prefs(activity).edit().putString("simple_input_draft",value).apply();}
    private void editCurrent(){editing=true;editIndex=loadout.magazine().active();setDraft(loadout.magazine().text());add.setText(R.string.update_ingredient);input.requestFocus();input.setSelection(input.length());((android.view.inputmethod.InputMethodManager)activity.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).showSoftInput(input,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);}
    boolean commitPending(){
        if(busy)return false;String value=input.getText().toString().trim();if(value.isEmpty())return true;
        IngredientMagazine m=loadout.magazine();List<String> backup=m.save();int selected=m.active();int slot=editing?editIndex:0;
        if(!editing){while(slot<backup.size()&&!backup.get(slot).trim().isEmpty())slot++;}
        m.select(slot);m.edit(value);String error=m.validationError();if(error!=null){m.restore(backup,selected);input.setError(IngredientValidation.message(activity,error));return false;}
        m.commit();editing=false;setDraft("");add.setText(R.string.add);loadout.publish();return true;
    }
    void refresh(){
        if(input==null)return;scan.setEnabled(!busy);boolean discovery=activity.profile().mode.equals("discover");
        if(!Boolean.valueOf(discovery).equals(foodPhoto.getTag())){foodPhoto.setImageResource(discovery?R.drawable.inspiration_photo:R.drawable.pantry_photo);foodPhoto.setContentDescription(activity.getString(discovery?R.string.food_inspiration_description:R.string.food_photo_description));foodPhoto.setTag(discovery);}pantry.setVisibility(discovery?GONE:VISIBLE);
        pantryMode.setChecked(!discovery);surpriseMode.setChecked(discovery);pantryMode.setBackgroundTintList(ColorStateList.valueOf(color(!discovery?R.color.accent:R.color.surface)));pantryMode.setTextColor(color(!discovery?R.color.on_accent:R.color.ink));surpriseMode.setBackgroundTintList(ColorStateList.valueOf(color(discovery?R.color.accent:R.color.surface)));surpriseMode.setTextColor(color(discovery?R.color.on_accent:R.color.ink));
        ingredientCount.setText(activity.getString(R.string.ingredients_count,loadout.magazine().count()));empty.setVisibility(loadout.magazine().count()==0?VISIBLE:GONE);
        editHint.setVisibility(loadout.magazine().count()==0?GONE:VISIBLE);clear.setVisibility(loadout.magazine().count()==0?GONE:VISIBLE);clear.setEnabled(!busy);
        ingredients.removeAllViews();List<String> slots=loadout.magazine().save();
        for(int i=0;i<slots.size();i++){if(slots.get(i).trim().isEmpty())continue;final int slot=i;String name=slots.get(i);Chip chip=new Chip(activity);chip.setText(name);UiTypography.apply(chip);chip.setTextSize(15);chip.setChipIconResource(R.drawable.ic_leaf);chip.setChipIconVisible(true);chip.setChipIconTint(ColorStateList.valueOf(color(R.color.accent)));chip.setCloseIconVisible(true);chip.setChipBackgroundColor(ColorStateList.valueOf(color(R.color.background)));chip.setTextColor(color(R.color.ink));chip.setCloseIconContentDescription(activity.getString(R.string.remove_ingredient_a11y,name));chip.setContentDescription(activity.getString(R.string.edit_ingredient_a11y,name));chip.setOnClickListener(v->loadout.select(slot));chip.setOnCloseIconClickListener(v->{loadout.magazine().select(slot);loadout.clearChamber();if(editing&&editIndex==slot){editing=false;setDraft("");}});chip.setEnabled(!busy);ingredients.addView(chip);}
        peopleCount.setText(activity.getString(R.string.people_count,activity.profile().servings));
        fewerPeople.setEnabled(!busy&&activity.profile().servings>1);morePeople.setEnabled(!busy&&activity.profile().servings<12);
        summary.setText(discovery?activity.getString(R.string.simple_surprise_description):activity.getString(R.string.preferences_summary,UiLabels.option(activity,activity.meal()),UiLabels.option(activity,activity.profile().cuisine),UiLabels.option(activity,activity.profile().goal)));
        goals.removeAllViews();String[] values={"Balanced","Cutting","Bulking"};int[] descriptions={R.string.balanced_description,R.string.cut_description,R.string.bulk_description};
        for(int i=0;i<values.length;i++){final String goal=values[i];MaterialButton b=button(goals,UiLabels.option(activity,goal)+"\n"+activity.getString(descriptions[i]),goal.equals(activity.profile().goal),()->activity.chooseGoal(goal));UiIcons.button(b,i==0?R.drawable.ic_bowl:i==1?R.drawable.ic_leaf:R.drawable.ic_flame);b.setEnabled(!busy);}
        UiIcons.button(generate,discovery?R.drawable.ic_sparkle:R.drawable.ic_bowl);
        generate.setText(busy?activity.getString(R.string.cancel_generation):activity.getString(discovery?R.string.surprise_action:R.string.find_recipe));
    }
    void setBusy(boolean value){busy=value;input.setEnabled(!value);add.setEnabled(!value);language.setEnabled(!value);pantryMode.setEnabled(!value);surpriseMode.setEnabled(!value);generate.setEnabled(true);loading.setVisibility(value?VISIBLE:GONE);if(value){if(Experience.reduced(activity))scroll.scrollTo(0,0);else scroll.smoothScrollTo(0,0);}refresh();}
}
