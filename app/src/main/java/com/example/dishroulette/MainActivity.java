package com.example.dishroulette;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AlertDialog;
import com.example.dishroulette.core.IngredientExclusions;
import com.example.dishroulette.core.SpinOptions;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.dishroulette.databinding.ActivityMainBinding;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private IngredientLoadout loadout;
    private SimpleHomeView simpleHome;
    private android.widget.Button resultPreferences;
    private boolean simple;
    SpinProfile profile(){return profile;}
    String meal(){return meal;}
    boolean isSimple(){return simple;}
    void generate(boolean another){spin(another);}
    void chooseGoal(String goal){selectGoal(goal);}
    void chooseGenerationMode(String mode){if(spinning)return;profile.mode=mode;profile.save(preferences,gson);updateMode();}
    void cancelGeneration(){if(pendingCall!=null){pendingCall.cancel();pendingCall=null;}pendingReveal=null;loadout.cancelFire();setSpinning(false);}
    void changeLanguage(String language){changeInterface(simple?"simple":"advanced",language);}
    void changeInterface(String mode,String language){
        if(spinning)return;
        if(mode.equals(simple?"simple":"advanced")&&language.equals(InterfaceSettings.language(this)))return;
        preferences.edit().putString(InterfaceSettings.MODE,mode).putString(InterfaceSettings.LANGUAGE,language).apply();recreate();
    }
    @Override protected void attachBaseContext(android.content.Context base){super.attachBaseContext(InterfaceSettings.localized(base));}

    private SharedPreferences preferences;
    private final Gson gson = new Gson();
    private RecipeApi recipeApi;
    private Call<RecipeSuggestion> pendingCall;
    private final Set<String> savedIds = new HashSet<>();
    private String meal = "Any meal";
    private RecipeCatalog.Recipe currentRecipe;
    private Set<String> resultPantry = new HashSet<>();
    private boolean spinning;
    private boolean firingFinished;
    private Runnable pendingReveal;
    private MechanicalAudio mechanicalSound;
    private KitchenScreens kitchenScreens;
    private final android.os.Handler presentation = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable waitingTick;
    private int waitingPhase;
    private boolean compactLayout;
    private final List<android.widget.Button> ammoButtons = new ArrayList<>();
    boolean hasExclusions() { return profile.hasExclusions(); }


    private void afterFiring(Runnable reveal) {
        if (firingFinished) reveal.run();
        else pendingReveal = reveal;
    }

    private SpinProfile profile;

    @Override
    protected void onCreate(Bundle state) {
        simple=InterfaceSettings.simple(this);
        setTheme(simple?R.style.Theme_DishRoulette_Simple:R.style.Theme_DishRoulette);
        super.onCreate(state);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.getRoot().setLayoutDirection(InterfaceSettings.language(this).equals("ar")?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
        if(simple){loadout=new NativeIngredientLoadout();binding.advancedHome.setVisibility(View.GONE);binding.simpleHome.setVisibility(View.VISIBLE);}
        else {WeaponInputView weapon=new WeaponInputView(this,null);weapon.setId(R.id.tabletop);binding.sceneHost.addView(weapon,new android.widget.FrameLayout.LayoutParams(-1,-1));loadout=weapon;}

        androidx.core.view.WindowInsetsControllerCompat bars =
                new androidx.core.view.WindowInsetsControllerCompat(getWindow(), binding.getRoot());
        bars.setAppearanceLightStatusBars(simple);
        bars.setAppearanceLightNavigationBars(simple);
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets padding = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            view.setPadding(padding.left, padding.top, padding.right, padding.bottom);
            configureLayout(insets.isVisible(WindowInsetsCompat.Type.ime()));
            return insets;
        });
        preferences = getSharedPreferences("dishroulette_ui", MODE_PRIVATE);
        profile = SpinProfile.load(preferences, gson);
        meal = preferences.getString("selected_meal", "Any meal");
        savedIds.addAll(preferences.getStringSet("saved_recipes", new HashSet<>()));
        if (state != null) {
            meal = state.getString("meal", "Any meal");
            currentRecipe = decodeRecipe(state.getString("recipe_json"));
            resultPantry = RecipeCatalog.parseIngredients(state.getString("result_pantry", ""));
        }
        recipeApi = RecipeApi.create();
        if(!simple){mechanicalSound = new MechanicalAudio(this);((WeaponInputView)loadout).setAudio(mechanicalSound);}
        if (state == null) {
            try {
                String[] slots=gson.fromJson(preferences.getString("loaded_ammunition","[]"),String[].class);
                loadout.restore(slots==null?null:java.util.Arrays.asList(slots),preferences.getInt("loaded_chamber",0));
            } catch(RuntimeException ignored) { loadout.restore(null,0); }
        }
        if (state != null) loadout.restore(state.getStringArrayList("ammunition"), state.getInt("active_chamber", 0));
        loadout.setChangedListener(this::updateMagazineStatus);

        kitchenScreens = new KitchenScreens(this,binding.secondaryScreen,preferences,new KitchenScreens.Actions(){
            public void open(RecipeCatalog.Recipe recipe){ currentRecipe=recipe;resultPantry=readPantry();renderRecipe(); }
            public void remove(RecipeCatalog.Recipe recipe){ savedIds.remove(recipe.id);persistSaved(recipe,true);updateSaveButton(); }
        });
        getOnBackPressedDispatcher().addCallback(this,new androidx.activity.OnBackPressedCallback(true){
            @Override public void handleOnBackPressed(){
                if(kitchenScreens.isOpen())kitchenScreens.close();
                else if(binding.cardMealResult.getVisibility()==View.VISIBLE)binding.cardMealResult.setVisibility(View.GONE);
                else {setEnabled(false);getOnBackPressedDispatcher().onBackPressed();setEnabled(true);}
            }
        });
        loadout.refreshExperience();
        createChips();
        setupActions();
        updateMode();
        updateFilterSummary();
        updateSpinCounter();
        if(simple){simpleHome=new SimpleHomeView(this,(NativeIngredientLoadout)loadout);binding.simpleHome.addView(simpleHome,new android.widget.FrameLayout.LayoutParams(-1,-1));}
        configureLayout(false);
        UiTypography.apply(binding.getRoot());
        if (currentRecipe != null && (state == null || !state.getBoolean("was_firing"))) renderRecipe();
        if (state != null && state.getBoolean("was_firing"))
            binding.tvSceneHelp.setText(R.string.shot_interrupted);
        if(state!=null){
            if("settings".equals(state.getString("secondary_screen")))showSettings();
            else if("saved".equals(state.getString("secondary_screen")))showSavedRecipes();
            else if("cooking".equals(state.getString("secondary_screen"))){
                RecipeCatalog.Recipe cooking=decodeRecipe(state.getString("cooking_recipe"));
                if(cooking!=null&&!profile.blocks(cooking)&&!profile.requiresReview)kitchenScreens.cook(cooking);
            }
            if(!state.getBoolean("result_visible",true))binding.cardMealResult.setVisibility(View.GONE);
        }
    }

    private void createChips() {
        for (String mode : new String[]{"pantry", "discover"}) {
            Chip chip = selectableChip(getString(mode.equals("pantry") ? R.string.pantry_mode : R.string.discover_mode));
            binding.modeChips.addView(chip);
            chip.setChecked(profile.mode.equals(mode));
            chip.setOnCheckedChangeListener((button, checked) -> {
                if (checked) {
                    profile.mode = mode;
                    profile.save(preferences, gson);
                    updateMode();
                }
            });
        }
        for (String label : SpinOptions.MEALS) {
            Chip chip = selectableChip(label);
            chip.setId(View.generateViewId());
            chip.setText(UiLabels.option(this,label));
            chip.setCheckable(true);
            chip.setEnsureMinTouchTargetSize(true);
            chip.setCheckedIconVisible(false);
            // The selected meal is restored explicitly; generated IDs change on recreation.
            chip.setSaveEnabled(false);
            binding.mealChips.addView(chip);
            chip.setChecked(label.equals(meal));
            chip.setOnCheckedChangeListener((button, checked) -> {
                if (checked) {meal = label;preferences.edit().putString("selected_meal",meal).apply();updateFilterSummary();}
            });
        }
    }

    private void setupActions() {
        binding.tvLoadState.setOnClickListener(v -> showLoadoutManager());
        binding.tvMagazineStatus.setOnClickListener(v -> showLoadoutManager());
        binding.btnPreviousLoad.setOnClickListener(v -> loadout.page(-1));
        binding.btnNextLoad.setOnClickListener(v -> loadout.page(1));
        binding.btnClearChamber.setOnClickListener(v -> loadout.clearChamber());
        binding.btnClearAll.setOnClickListener(v -> loadout.clearAll());
        binding.btnCloseResult.setOnClickListener(v -> binding.cardMealResult.setVisibility(View.GONE));
        binding.btnBalanced.setOnClickListener(v -> selectGoal("Balanced"));
        binding.btnCut.setOnClickListener(v -> selectGoal("Cutting"));
        binding.btnBulk.setOnClickListener(v -> selectGoal("Bulking"));
        binding.btnCancelGeneration.setOnClickListener(v -> {
            if(pendingCall!=null){pendingCall.cancel();pendingCall=null;}
            pendingReveal=null; loadout.cancelFire(); setSpinning(false);
        });
        binding.btnSpin.setOnClickListener(view -> spin(false));
        binding.btnDislike.setOnClickListener(view -> spin(true));
        binding.btnLike.setOnClickListener(view -> {
            if (currentRecipe == null) return;
            boolean removed = !savedIds.add(currentRecipe.id);
            if (removed) savedIds.remove(currentRecipe.id);
            persistSaved(currentRecipe, removed);
            updateSaveButton();
            Snackbar.make(binding.getRoot(), removed ? R.string.removed_feedback
                    : R.string.saved_feedback, Snackbar.LENGTH_SHORT).show();
        });
        binding.btnRecipe.setOnClickListener(view -> {
            if (currentRecipe != null) showRecipe(currentRecipe);
        });
        binding.btnSaved.setOnClickListener(view -> showSavedRecipes());
        binding.btnFilters.setOnClickListener(view -> showSettings());
    }

    private void selectGoal(String goal) {
        if (spinning || loadout.isLoading()) return;
        profile.goal = goal;
        profile.save(preferences, gson);
        updateFilterSummary();
    }

    private void updateMagazineStatus() {
        IngredientMagazine magazine = loadout.magazine();
        preferences.edit().putString("loaded_ammunition",gson.toJson(magazine.save())).putInt("loaded_chamber",magazine.active()).apply();
        boolean discovery=profile.mode.equals("discover");
        binding.tvLoadState.setText(discovery?R.string.auto_load:compactLayout?R.string.manage:magazine.count()==0?R.string.state_empty:magazine.count()>=20?R.string.state_full:R.string.state_ready);
        binding.tvLoadState.setTextColor(getColor(magazine.count()>0||discovery?R.color.accent:R.color.muted));
        if(ammoButtons.isEmpty()){
            for(int i=0;i<6;i++){
                final int chamber=i;
                android.widget.Button b=new androidx.appcompat.widget.AppCompatButton(this);
                UiTypography.apply(b);b.setTextSize(12);b.setAllCaps(false);b.setSingleLine(true);b.setEllipsize(android.text.TextUtils.TruncateAt.END);b.setMaxWidth(Math.round(240*getResources().getDisplayMetrics().density));int pad=Math.round(12*getResources().getDisplayMetrics().density);b.setPadding(pad,0,pad,0);b.setTextColor(UiColors.color(this,R.color.ink));b.setBackgroundResource(R.drawable.table_control);
                b.setOnClickListener(v->loadout.select(loadout.magazine().page()*6+chamber));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-1);lp.rightMargin=Math.round(6*getResources().getDisplayMetrics().density);binding.ammoTray.addView(b,lp);ammoButtons.add(b);
            }
        }
        String[] names=magazine.visible();
        for(int i=0;i<6;i++){
            android.widget.Button b=ammoButtons.get(i);String name=names[i].trim();
            b.setText(String.format(Locale.ROOT,"%02d  %s",magazine.page()*6+i+1,name.isEmpty()?getString(R.string.empty_slot):name));
            b.setContentDescription(getString(R.string.cartridge_a11y,magazine.page()*6+i+1,name.isEmpty()?getString(R.string.state_empty):name));
            b.setSelected(i==magazine.active()%6);b.setEnabled(!spinning&&!discovery);
        }
        binding.ammoScroll.setVisibility(discovery||compactLayout?View.GONE:View.VISIBLE);
        binding.tvMagazineStatus.setText(discovery?getString(R.string.chef_choice):compactLayout?getString(R.string.loaded_count,magazine.count()):getString(R.string.load_status,magazine.page()+1,magazine.count()));
        if(simpleHome!=null)simpleHome.refresh();
    }

    private void configureLayout(boolean keyboardVisible) {
        if(binding==null||simple)return;
        boolean phone=getResources().getConfiguration().screenWidthDp<840;
        compactLayout=phone&&(getResources().getConfiguration().screenHeightDp<680||keyboardVisible);
        binding.goalRail.setVisibility(compactLayout?View.GONE:View.VISIBLE);
        binding.tvBrandCaption.setVisibility(compactLayout?View.GONE:View.VISIBLE);
        binding.tvWeaponName.setVisibility(compactLayout?View.GONE:View.VISIBLE);
        binding.tvWeaponDetail.setVisibility(compactLayout?View.GONE:View.VISIBLE);
        binding.tvSceneHelp.setVisibility(compactLayout?View.GONE:View.VISIBLE);
        binding.tvSpinCounter.setVisibility(compactLayout?View.GONE:View.VISIBLE);
        View navigation=(View)binding.btnPreviousLoad.getParent();navigation.setVisibility(compactLayout?View.GONE:View.VISIBLE);
        boolean shortKeyboard=phone&&keyboardVisible&&getResources().getConfiguration().screenHeightDp<680;
        View header=(View)binding.tvTitle.getParent().getParent();header.setVisibility(shortKeyboard?View.GONE:View.VISIBLE);
        android.view.ViewGroup.LayoutParams hp=header.getLayoutParams();hp.height=Math.round((compactLayout?52:60)*getResources().getDisplayMetrics().density);header.setLayoutParams(hp);
        View status=(View)binding.tvMagazineStatus.getParent();status.setVisibility(shortKeyboard?View.GONE:View.VISIBLE);status.setMinimumHeight(Math.round((compactLayout?48:0)*getResources().getDisplayMetrics().density));
        binding.tvLoadState.setContentDescription(getString(R.string.manage_ingredients));
        if(profile!=null)updateMagazineStatus();
    }
    private void showLoadoutManager(){
        if(spinning||loadout.isLoading()||profile.mode.equals("discover"))return;
        loadout.inspect();
        List<String> slots=loadout.magazine().save();
        List<String> labels=new ArrayList<>();List<Integer> indices=new ArrayList<>();
        int empty=-1;
        for(int i=0;i<slots.size();i++){
            if(slots.get(i).trim().isEmpty()){if(empty<0)empty=i;continue;}
            indices.add(i);labels.add(String.format(Locale.ROOT,"%02d / %s",i+1,slots.get(i)));
        }
        indices.add(empty<0?slots.size():empty);labels.add(getString(R.string.add_ingredient));
        indices.add(-1);labels.add(getString(R.string.remove_selected));indices.add(-2);labels.add(getString(R.string.clear_ingredients));
        new MaterialAlertDialogBuilder(this).setTitle(getString(R.string.loadout_title))
            .setItems(labels.toArray(new String[0]),(d,which)->{
                int selection=indices.get(which);
                if(selection==-1)loadout.clearChamber();
                else if(selection==-2)loadout.clearAll();
                else loadout.select(selection);
            }).setPositiveButton(R.string.close,null).show();
    }

    private Set<String> readPantry() {
        Set<String> pantry = new java.util.LinkedHashSet<>();
        for (String ingredient : loadout.magazine().ingredients())
            pantry.addAll(RecipeCatalog.parseIngredients(ingredient));
        return pantry;
    }

    private void spin(boolean excludeCurrent) {
        if (spinning || loadout.isLoading()) return;
        if (profile.requiresReview) { showGenerationError(getString(R.string.review_filters)); return; }
        String ingredientError = loadout.magazine().validationError();
        if (profile.mode.equals("pantry") && ingredientError != null) { showGenerationError(IngredientValidation.message(this,ingredientError)); return; }
        Set<String> pantry = profile.mode.equals("discover") ? new HashSet<>() : readPantry();
        if (profile.mode.equals("pantry") && pantry.isEmpty()) {
            android.widget.Toast.makeText(this, R.string.ingredient_error, android.widget.Toast.LENGTH_LONG).show();
            loadout.focusCartridge();
            return;
        }
        Set<String> conflicts = IngredientExclusions.conflicts(pantry, profile.allergies, profile.customExclusions);
        if (!conflicts.isEmpty()) {
            showGenerationError(getString(R.string.allergy_conflict, localizedNames(conflicts)));
            return;
        }
        if (profile.hasExclusions() && IngredientExclusions.hasUncertainIngredients(pantry)) {
            showGenerationError(getString(R.string.uncertain_ingredients));
            return;
        }
        if (!BuildConfig.DEBUG) {
            showGenerationError(getString(R.string.service_not_deployed));
            return;
        }
        setSpinning(true);
        binding.cardMealResult.setVisibility(View.GONE);
        firingFinished = false;
        pendingReveal = null;
        loadout.fire(() -> {
            firingFinished = true;
            binding.tvSceneHelp.setText(R.string.waiting_for_recipe);
            if(spinning) startWaiting();
            if (pendingReveal != null) {
                Runnable reveal = pendingReveal;
                pendingReveal = null;
                reveal.run();
            }
        });
        String avoid = excludeCurrent && currentRecipe != null ? currentRecipe.title : "";
        SpinProfile requestProfile = gson.fromJson(gson.toJson(profile), SpinProfile.class);
        List<RecipeApi.Favorite> favorites = new ArrayList<>();
        if (profile.useFavorites) for (RecipeCatalog.Recipe recipe : eligibleFavorites()) favorites.add(new RecipeApi.Favorite(recipe));
        pendingCall = recipeApi.suggest(new RecipeApi.Request(pantry, meal, avoid, requestProfile, favorites, InterfaceSettings.language(this)));
        pendingCall.enqueue(new Callback<RecipeSuggestion>() {
            @Override public void onResponse(Call<RecipeSuggestion> call, Response<RecipeSuggestion> response) {
                if (call.isCanceled() || isDestroyed()) return;
                pendingCall = null;
                afterFiring(() -> {
                    if (isDestroyed()) return;
                    setSpinning(false);
                    if (!response.isSuccessful()) {
                        showGenerationError(apiError(response));
                        return;
                    }
                    try {
                        if (response.body() == null) throw new IllegalArgumentException();
                        currentRecipe = response.body().toRecipe(pantry, requestProfile);
                        currentRecipe.language=InterfaceSettings.language(MainActivity.this);
                        resultPantry = new HashSet<>(pantry);
                        updateSpinCounter();
                        preferences.edit().putInt("spin_count", preferences.getInt("spin_count", 0) + 1).apply();
                        updateSpinCounter();
                        renderRecipe();
                        binding.cardMealResult.setAlpha(0f);
                        binding.cardMealResult.setTranslationY(48f * getResources().getDisplayMetrics().density);
                        binding.cardMealResult.animate().alpha(1f).translationY(0f).setDuration(Experience.duration(MainActivity.this,480)).start();

                    } catch (IllegalArgumentException e) {
                        showGenerationError(getString(R.string.invalid_recipe));
                    }
                });
            }
            @Override public void onFailure(Call<RecipeSuggestion> call, Throwable error) {
                if (call.isCanceled() || isDestroyed()) return;
                pendingCall = null;
                afterFiring(() -> {
                    if (isDestroyed()) return;
                    setSpinning(false);
                    // A transport timeout is not evidence that Gemini itself timed out.
                    // Only the backend's TIMEOUT response is presented as a generation timeout.
                    showGenerationError(getString(R.string.connection_error));
                });
            }
        });
    }

    private String apiError(Response<?> response) {
        String code = "";
        if (response.errorBody() != null) {
            try (okhttp3.ResponseBody body = response.errorBody()) {
                code = JsonParser.parseString(body.string()).getAsJsonObject().get("code").getAsString();
            } catch (Exception ignored) { /* Fall back to a safe message, not raw server errors. */ }
        }
        switch (code) {
            case "KEY_NOT_CONFIGURED": return getString(R.string.key_missing);
            case "PROVIDER_UNAVAILABLE": return getString(R.string.provider_unavailable);
            case "PROVIDER_CONFIGURATION": return getString(R.string.provider_configuration);
            case "NO_RECIPE": return getString(R.string.no_filtered_recipe);
            case "INVALID_FILTERS": return getString(R.string.preferences_invalid);
            case "ALLERGY_CONFLICT": case "ALLERGY_REJECTED": return getString(R.string.allergy_rejected);
            case "UNCERTAIN_INGREDIENTS": return getString(R.string.uncertain_ingredients);
            case "INVALID_INGREDIENTS": return getString(R.string.invalid_ingredients);
            case "TIMEOUT": return getString(R.string.generation_timeout);
            case "RATE_LIMITED": return getString(R.string.rate_limited);
            default: return getString(response.code() == 429 ? R.string.rate_limited : R.string.invalid_recipe);
        }
    }

    private void showGenerationError(String message) {
        new MaterialAlertDialogBuilder(this).setTitle(R.string.kitchen_error)
                .setMessage(message).setPositiveButton(simple?R.string.home:R.string.loadout_back,(d,w)->loadout.inspect()).show();
    }
    private void startWaiting(){
        if(simple)return;
        stopWaiting();binding.cookingStatus.setVisibility(View.VISIBLE);waitingPhase=0;
        String[] messages={getString(R.string.loading_preheat),getString(R.string.loading_mix),getString(R.string.loading_chef),getString(R.string.loading_prepare)};
        waitingTick=new Runnable(){@Override public void run(){
            binding.tvCookingStatus.setText(messages[Math.min(waitingPhase++,messages.length-1)]);
            presentation.postDelayed(this,4200);
        }};waitingTick.run();
    }
    private void stopWaiting(){
        if(waitingTick!=null)presentation.removeCallbacks(waitingTick);waitingTick=null;binding.cookingStatus.setVisibility(View.GONE);
    }

    private void persistSaved(RecipeCatalog.Recipe recipe, boolean removed) {
        SharedPreferences.Editor editor = preferences.edit()
                .putStringSet("saved_recipes", new HashSet<>(savedIds));
        if (removed) editor.remove("recipe_json_" + recipe.id).remove("liked_at_" + recipe.id);
        else editor.putString("recipe_json_" + recipe.id, gson.toJson(recipe))
                .putLong("liked_at_" + recipe.id, System.currentTimeMillis());
        editor.apply();
        updateFilterSummary();
    }

    private RecipeCatalog.Recipe decodeRecipe(String json) {
        if (json == null) return null;
        try {
            RecipeCatalog.Recipe recipe = gson.fromJson(json, RecipeCatalog.Recipe.class);
            if (recipe == null || recipe.id == null || recipe.title == null || recipe.meal == null
                    || recipe.description == null || recipe.ingredients == null || recipe.method == null
                    || recipe.keys == null || recipe.keys.contains(null)
                    || recipe.minutes < 1 || recipe.servings < 1) return null;
            return recipe;
        } catch (RuntimeException ignored) { return null; }
    }

    private void setSpinning(boolean busy) {
        spinning = busy;
        if(!busy)stopWaiting();
        if (busy) binding.tvSceneHelp.setText(R.string.firing_recipe);
        else updateMode();
        if (!busy) {
            loadout.finish();

        }
        binding.btnSpin.setEnabled(!busy);
        binding.btnDislike.setEnabled(!busy);
        binding.btnFilters.setEnabled(!busy);
        for (int i = 0; i < binding.modeChips.getChildCount(); i++) binding.modeChips.getChildAt(i).setEnabled(!busy);
        loadout.lock(busy);
        binding.btnPreviousLoad.setEnabled(!busy);
        binding.btnNextLoad.setEnabled(!busy);
        binding.btnClearChamber.setEnabled(!busy);
        binding.btnClearAll.setEnabled(!busy);
        binding.btnBalanced.setEnabled(!busy);
        binding.btnCut.setEnabled(!busy);
        binding.btnBulk.setEnabled(!busy);
        binding.btnSpin.setText(busy ? R.string.spinning : R.string.spin);
        binding.spinProgress.setVisibility(View.GONE);
        updateMagazineStatus();
        if(simpleHome!=null)simpleHome.setBusy(busy);
        for (int i = 0; i < binding.mealChips.getChildCount(); i++) {
            binding.mealChips.getChildAt(i).setEnabled(!busy);
        }
    }

    private void renderRecipe() {
        binding.cardMealResult.setVisibility(View.VISIBLE);
        binding.btnCloseResult.setText(simple?R.string.back_home:R.string.kitchen_back_to_loadout);
        binding.btnDislike.setText(simple?R.string.try_another:R.string.another);
        if(simple) styleSimpleResult();
        UiTypography.apply(binding.cardMealResult);
        loadout.inspect();
        binding.cardMealResult.scrollTo(0,0);
        binding.tvResultEyebrow.setText(currentRecipe.language==null||!currentRecipe.language.equals(InterfaceSettings.language(this))?R.string.saved_original_language:currentRecipe.aiGenerated?R.string.ai_result_eyebrow:R.string.result_eyebrow);
        binding.tvMealTitle.setText(currentRecipe.title);
        binding.tvMealDescription.setText(currentRecipe.description);
        binding.tvMealMeta.setText(UiLabels.option(this,currentRecipe.meal)+"  /  "+KitchenScreens.meta(this,currentRecipe));
        binding.tvMatch.setText(currentRecipe.discovery ? getString(R.string.shopping_recipe)
                : getString(R.string.match_summary, currentRecipe.matches(resultPantry), currentRecipe.keys.size()));
        List<String> missing = currentRecipe.missing(resultPantry);
        if (currentRecipe.discovery) {
            binding.tvMissing.setText(R.string.shopping_note);
        } else if (currentRecipe.aiGenerated) {
            List<String> extras = currentRecipe.additionalIngredients;
            binding.tvMissing.setText(extras == null || extras.isEmpty() ? getString(R.string.no_extras)
                    : getString(R.string.staples, localizedNames(extras)));
        } else {
            binding.tvMissing.setText(missing.isEmpty() ? getString(R.string.all_ingredients)
                    : getString(R.string.missing, android.text.TextUtils.join(", ", missing)));
        }
        binding.tvMealMeta.setVisibility(View.VISIBLE);
        binding.tvMatch.setVisibility(View.VISIBLE);
        binding.tvMissing.setVisibility(View.VISIBLE);
        binding.resultActions.setVisibility(View.VISIBLE);
        String explanation = currentRecipe.whyThisDish == null ? "" : currentRecipe.whyThisDish;
        String fit = currentRecipe.goalFit == null ? "" : currentRecipe.goalFit;
        String personalization = getString(R.string.personalization_details,
                explanation.isEmpty() ? "" : getString(R.string.why_recipe, explanation),
                fit.isEmpty() ? "" : getString(R.string.goal_fit, fit));
        binding.tvPersonalization.setText(personalization.trim());
        binding.tvPersonalization.setVisibility(explanation.isEmpty() && fit.isEmpty() ? View.GONE : View.VISIBLE);
        boolean blocked = profile.requiresReview || profile.blocks(currentRecipe);
        binding.tvAllergyWarning.setText(blocked ? R.string.allergy_rejected : R.string.allergy_note);
        binding.tvAllergyWarning.setVisibility(profile.hasExclusions() ? View.VISIBLE : View.GONE);
        binding.btnRecipe.setEnabled(!blocked);
        updateSaveButton();
    }

    private String localizedNames(java.util.Collection<String> names){List<String> display=new ArrayList<>();for(String value:names)display.add(UiLabels.option(this,value));return android.text.TextUtils.join(", ",display);}
    private void styleSimpleResult(){
        android.view.ViewGroup page=(android.view.ViewGroup)binding.cardMealResult.getChildAt(0);
        for(int i=0;i<page.getChildCount();i++){
            View v=page.getChildAt(i);
            if(v instanceof TextView){TextView t=(TextView)v;t.setLetterSpacing(0);t.setTypeface(android.graphics.Typeface.create("sans",android.graphics.Typeface.NORMAL));}
        }
        binding.tvMealTitle.setTypeface(android.graphics.Typeface.create(InterfaceSettings.language(this).equals("ar")?"sans":"serif",android.graphics.Typeface.NORMAL));
        binding.btnCloseResult.setBackgroundColor(UiColors.color(this,R.color.surface));
        binding.tvResultEyebrow.setText(R.string.fresh_recipe);
        binding.tvResultSection.setText(R.string.recipe_ready);
        binding.tvResultFooter.setVisibility(View.GONE);
        binding.btnCloseResult.setAllCaps(false);binding.btnRecipe.setAllCaps(false);binding.btnRecipe.setText(R.string.view_recipe);binding.btnRecipe.setCornerRadius(Math.round(16*getResources().getDisplayMetrics().density));binding.btnLike.setCornerRadius(Math.round(16*getResources().getDisplayMetrics().density));
        if(resultPreferences==null){resultPreferences=new androidx.appcompat.widget.AppCompatButton(this);resultPreferences.setText(R.string.edit_preferences);resultPreferences.setAllCaps(false);resultPreferences.setTextColor(UiColors.color(this,R.color.accent));resultPreferences.setBackgroundColor(UiColors.color(this,R.color.surface));resultPreferences.setMinHeight(Math.round(52*getResources().getDisplayMetrics().density));resultPreferences.setOnClickListener(v->{binding.cardMealResult.setVisibility(View.GONE);showFilters();});binding.resultActions.addView(resultPreferences,new LinearLayout.LayoutParams(-1,-2));}
        binding.simpleRecipeDetails.setVisibility(View.VISIBLE);binding.simpleRecipeDetails.removeAllViews();
        recipeDetailText(getString(R.string.ingredients_title),true);recipeDetailText(currentRecipe.ingredients,false);recipeDetailText(getString(R.string.method_title),true);recipeDetailText(currentRecipe.method,false);
    }
    private void recipeDetailText(String text,boolean title){
        TextView view=new TextView(this);view.setText(text);view.setTextSize(title?21:16);view.setTextColor(UiColors.color(this,title?R.color.ink:R.color.muted));view.setLineSpacing(6,1);androidx.core.view.ViewCompat.setAccessibilityHeading(view,title);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=Math.round(16*getResources().getDisplayMetrics().density);binding.simpleRecipeDetails.addView(view,lp);
    }
    void showSettings(){if(!spinning)kitchenScreens.settings();}

    private void updateSaveButton() {
        boolean saved = currentRecipe != null && savedIds.contains(currentRecipe.id);
        binding.btnLike.setText(saved ? R.string.saved_generic : R.string.save_generic);
        binding.btnLike.setContentDescription(getString(saved ? R.string.remove_recipe : R.string.save_recipe));
    }

    private void updateSpinCounter() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());
        if (!today.equals(preferences.getString("spin_day", ""))) {
            preferences.edit().putString("spin_day", today).putInt("spin_count", 0).apply();
        }
        int count = preferences.getInt("spin_count", 0);
        binding.tvSpinCounter.setText(getResources().getQuantityString(R.plurals.spin_count, count, count));
    }

    void showSavedRecipes() {
        List<RecipeCatalog.Recipe> saved = new ArrayList<>();
        for (String id : savedIds) {
            RecipeCatalog.Recipe recipe = decodeRecipe(preferences.getString("recipe_json_" + id, null));
            if (recipe == null) recipe = RecipeCatalog.find(id);
            if (recipe != null) saved.add(recipe);
        }
        saved.sort((a, b) -> a.title.compareToIgnoreCase(b.title));
        kitchenScreens.saved(saved);
    }

    private void showRecipe(RecipeCatalog.Recipe recipe) {
        if (profile.blocks(recipe) || profile.requiresReview) {
            MaterialAlertDialogBuilder blocked = new MaterialAlertDialogBuilder(this).setTitle(recipe.title)
                    .setMessage(profile.requiresReview ? R.string.review_filters : R.string.allergy_rejected)
                    .setPositiveButton(R.string.close, null);
            if (savedIds.contains(recipe.id)) blocked.setNeutralButton(R.string.remove_recipe, (dialog, which) -> {
                savedIds.remove(recipe.id); persistSaved(recipe, true); updateSaveButton();
            });
            blocked.show(); return;
        }
        kitchenScreens.cook(recipe);
    }

    private Chip selectableChip(String label) {
        Chip chip = new Chip(this);
        chip.setId(View.generateViewId());
        chip.setText(label);UiTypography.apply(chip);
        chip.setTextSize(12);
        chip.setChipCornerRadius(3*getResources().getDisplayMetrics().density);
        chip.setChipStrokeWidth(getResources().getDisplayMetrics().density);
        android.content.res.ColorStateList colors=new android.content.res.ColorStateList(
            new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},
            new int[]{UiColors.color(this,R.color.accent),UiColors.color(this,R.color.surface)});
        chip.setChipBackgroundColor(colors);
        chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(UiColors.color(this,R.color.line)));
        chip.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{UiColors.color(this,R.color.on_accent),UiColors.color(this,R.color.muted)}));
        chip.setCheckedIconVisible(false);
        chip.setCheckable(true);
        chip.setEnsureMinTouchTargetSize(true);
        chip.setSaveEnabled(false);
        return chip;
    }

    private void updateMode() {
        boolean discovery=profile.mode.equals("discover");
        loadout.setDiscovery(discovery);
        binding.tvSceneHelp.setText(discovery?R.string.discovery_help:R.string.ammo_help);
        binding.btnClearChamber.setVisibility(discovery?View.GONE:View.VISIBLE);
        binding.btnClearAll.setVisibility(discovery?View.GONE:View.VISIBLE);
        binding.btnPreviousLoad.setVisibility(discovery?View.GONE:View.VISIBLE);
        binding.btnNextLoad.setVisibility(discovery?View.GONE:View.VISIBLE);
        updateMagazineStatus();
    }

    private List<RecipeCatalog.Recipe> eligibleFavorites() {
        List<RecipeCatalog.Recipe> recipes = new ArrayList<>();
        Set<String> titles = new HashSet<>();
        List<String> ids = new ArrayList<>(savedIds);
        ids.sort((a, b) -> {
            int compare = Long.compare(preferences.getLong("liked_at_" + b, 0), preferences.getLong("liked_at_" + a, 0));
            return compare != 0 ? compare : a.compareTo(b);
        });
        for (String id : ids) {
            RecipeCatalog.Recipe recipe = decodeRecipe(preferences.getString("recipe_json_" + id, null));
            if (recipe == null) recipe = RecipeCatalog.find(id);
            if (recipe != null && !profile.blocks(recipe) && titles.add(recipe.title.toLowerCase(Locale.ROOT))) recipes.add(recipe);
            if (recipes.size() == 8) break;
        }
        return recipes;
    }

    private void updateFilterSummary() {
        loadout.setGoal(profile.goal);
        binding.btnBalanced.setSelected(profile.goal.equals("Balanced"));
        binding.btnCut.setSelected(profile.goal.equals("Cutting"));
        binding.btnBulk.setSelected(profile.goal.equals("Bulking"));
        binding.tvWeaponName.setText(profile.goal.equals("Balanced")?R.string.kitchen_the_classic:profile.goal.equals("Cutting")?R.string.weapon_precision:R.string.weapon_heavy);
        binding.tvWeaponDetail.setText(profile.goal.equals("Balanced")?R.string.weapon_classic_detail:profile.goal.equals("Cutting")?R.string.weapon_cut_detail:R.string.weapon_bulk_detail);
        int count = eligibleFavorites().size();
        String taste = !profile.useFavorites ? getString(R.string.taste_off) : count == 0 ? getString(R.string.taste_empty)
                : getResources().getQuantityString(R.plurals.taste_count, count, count);
        String summary = UiLabels.option(this,meal)+" · "+getString(R.string.filter_summary, UiLabels.option(this,profile.goal), UiLabels.option(this,profile.cuisine), taste);
        List<String> exclusions = new ArrayList<>();for(String value:profile.allergies)exclusions.add(UiLabels.option(this,value)); exclusions.addAll(profile.customExclusions);
        if (profile.requiresReview) summary = getString(R.string.review_filters);
        if (!exclusions.isEmpty()) summary += "\n" + getString(R.string.exclusions_summary, android.text.TextUtils.join(", ", exclusions));
        binding.tvFilterSummary.setText(summary);
        if(simpleHome!=null)simpleHome.refresh();
    }

    void showFilters() {
        // Resolve controls from this dialog's layout, independently of generated bindings.
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_preferences, null, false);
        UiTypography.apply(dialogView);
        boolean drawerWasVisible=binding.settingsDrawer.getVisibility()==View.VISIBLE;
        android.view.ViewGroup settingsHome = (android.view.ViewGroup) binding.settingsDrawer.getParent();
        settingsHome.removeView(binding.settingsDrawer);
        LinearLayout dialogContent = (LinearLayout) ((android.view.ViewGroup) dialogView).getChildAt(0);
        dialogContent.addView(binding.settingsDrawer, 0);
        binding.settingsDrawer.setVisibility(View.VISIBLE);
        MaterialSwitch switchMotion = dialogView.findViewById(R.id.switchMotion);
        MaterialSwitch switchSound = dialogView.findViewById(R.id.switchSound);
        switchMotion.setChecked(preferences.getBoolean("reduce_motion",false));
        switchSound.setChecked(preferences.getBoolean("mechanical_sound",true));
        switchSound.setVisibility(simple?View.GONE:View.VISIBLE);
        MaterialSwitch switchFavorites = dialogView.findViewById(R.id.switchFavorites);
        TextView tvTasteSummary = dialogView.findViewById(R.id.tvTasteSummary);
        ChipGroup goalChips = dialogView.findViewById(R.id.goalChips);
        MaterialAutoCompleteTextView cuisineDropdown = dialogView.findViewById(R.id.cuisineDropdown);
        ChipGroup allergyChips = dialogView.findViewById(R.id.allergyChips);
        TextInputEditText etCustomExclusions = dialogView.findViewById(R.id.etCustomExclusions);
        TextInputLayout customLayout = dialogView.findViewById(R.id.customLayout);
        SpinProfile draft = gson.fromJson(gson.toJson(profile), SpinProfile.class);
        switchFavorites.setChecked(draft.useFavorites);
        int count = eligibleFavorites().size();
        tvTasteSummary.setText(count == 0 ? getString(R.string.taste_empty)
                : getResources().getQuantityString(R.plurals.taste_count, count, count));
        for (String goal : SpinOptions.GOALS) {
            Chip chip = selectableChip(UiLabels.option(this,goal));
            goalChips.addView(chip); chip.setChecked(goal.equals(draft.goal));
            chip.setOnCheckedChangeListener((button, checked) -> { if (checked) draft.goal = goal; });
        }
        List<String> cuisines=new ArrayList<>();for(String value:SpinOptions.CUISINES)cuisines.add(UiLabels.option(this,value));
        cuisineDropdown.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_dropdown_item_1line,cuisines));
        cuisineDropdown.setText(UiLabels.option(this,draft.cuisine),false);
        cuisineDropdown.setOnItemClickListener((parent,view,position,id)->draft.cuisine=SpinOptions.CUISINES.get(position));
        for (String allergy : SpinOptions.ALLERGENS) {
            Chip chip = selectableChip(UiLabels.option(this,allergy));
            allergyChips.addView(chip); chip.setChecked(draft.allergies.contains(allergy));
            chip.setOnCheckedChangeListener((button, checked) -> {
                if (checked && !draft.allergies.contains(allergy)) draft.allergies.add(allergy);
                else if (!checked) draft.allergies.remove(allergy);
            });
        }
        etCustomExclusions.setText(android.text.TextUtils.join(", ", draft.customExclusions));
        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setTitle(R.string.filter_title)
                .setView(dialogView).setPositiveButton(R.string.apply_filters, null)
                .setNegativeButton(R.string.cancel, null).create();
        dialog.setOnDismissListener(unused -> {
            dialogContent.removeView(binding.settingsDrawer);
            binding.settingsDrawer.setVisibility(drawerWasVisible?View.VISIBLE:View.GONE);
            settingsHome.addView(binding.settingsDrawer);
        });
        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            try {
                Editable custom = etCustomExclusions.getText();
                draft.customExclusions = IngredientExclusions.parseCustom(custom == null ? "" : custom.toString());
                int cuisineIndex=cuisines.indexOf(cuisineDropdown.getText().toString());
                if(cuisineIndex<0)throw new IllegalArgumentException();
                draft.cuisine=SpinOptions.CUISINES.get(cuisineIndex);
                if (!SpinOptions.CUISINES.contains(draft.cuisine)) throw new IllegalArgumentException();
            } catch (IllegalArgumentException e) {
                customLayout.setError(getString(R.string.custom_invalid)); return;
            }
            draft.mode = profile.mode; // The source chips update the live profile inside this drawer.
            draft.useFavorites = switchFavorites.isChecked();
            draft.requiresReview = false;
            preferences.edit().putBoolean("reduce_motion",switchMotion.isChecked()).putBoolean("mechanical_sound",switchSound.isChecked()).apply();
            loadout.refreshExperience();
            profile = draft;
            profile.save(preferences, gson);
            updateFilterSummary();
            if (currentRecipe != null && binding.cardMealResult.getVisibility()==View.VISIBLE) renderRecipe();
            dialog.dismiss();
        }));
        dialog.show();
    }

    @Override protected void onResume() { super.onResume(); loadout.resume(); }
    @Override protected void onPause() { loadout.pause(); super.onPause(); }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("was_firing", spinning);
        state.putBoolean("result_visible",binding.cardMealResult.getVisibility()==View.VISIBLE);
        state.putString("secondary_screen",kitchenScreens.screen());
        if(kitchenScreens.cookingRecipe()!=null)state.putString("cooking_recipe",gson.toJson(kitchenScreens.cookingRecipe()));
        state.putStringArrayList("ammunition", loadout.magazine().save());
        state.putInt("active_chamber", loadout.magazine().active());
        state.putString("meal", meal);
        if (currentRecipe != null) state.putString("recipe_json", gson.toJson(currentRecipe));
        state.putString("result_pantry", android.text.TextUtils.join(",", resultPantry));
        super.onSaveInstanceState(state);
    }

    @Override protected void onDestroy() {
        // Cancel delivery and release the Activity when leaving or rotating the screen.
        pendingReveal = null;
        stopWaiting();kitchenScreens.dispose();
        loadout.dispose();
        if (mechanicalSound != null) mechanicalSound.close();
        if (pendingCall != null) pendingCall.cancel();
        super.onDestroy();
    }
}
