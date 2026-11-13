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

import java.util.ArrayList;
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
    private NativeIngredientLoadout loadout;
    private SimpleHomeView simpleHome;
    PhotoIngredientsController photoIngredients;
    void scanIngredients() {
        if (!spinning && simpleHome.commitPending()) photoIngredients.open();
    }
    String addPhotoIngredients(String text) {
        List<String> additions = new ArrayList<>();
        for (String line : text.split("\\r?\\n")) if (!line.trim().isEmpty()) additions.add(line.trim());
        if (additions.isEmpty()) return getString(R.string.photo_review_empty);
        IngredientMagazine candidate = new IngredientMagazine();
        candidate.restore(loadout.magazine().save(), 0);
        Set<String> seen = new HashSet<>(readPantry());
        for (String name : additions) {
            Set<String> parsed = RecipeCatalog.parseIngredients(name);
            if (parsed.size() == 1 && seen.containsAll(parsed)) continue;
            int slot = 0; List<String> slots = candidate.save();
            while (slot < slots.size() && !slots.get(slot).trim().isEmpty()) slot++;
            candidate.select(slot); candidate.edit(name);
            String error = candidate.validationError();
            if (error != null) return IngredientValidation.message(this, error);
            seen.addAll(parsed);
        }
        loadout.restore(candidate.save(), 0);
        chooseGenerationMode("pantry");
        return null;
    }
    private RecipeSharingController sharing;
    private android.widget.Button resultShare;
    private android.widget.Button resultPreferences;
    SpinProfile profile(){return profile;}
    String meal(){return meal;}
    void generate(boolean another){spin(another);}
    void chooseServings(int count){if(spinning)return;profile.servings=Math.max(1,Math.min(12,count));profile.save(preferences,gson);refreshHome();}
    void chooseGoal(String goal){selectGoal(goal);}
    void chooseGenerationMode(String mode){if(spinning)return;profile.mode=mode;profile.save(preferences,gson);refreshHome();}
    void cancelGeneration(){if(pendingCall!=null){pendingCall.cancel();pendingCall=null;}setSpinning(false);}
    void changeLanguage(String language){
        if(spinning || language.equals(InterfaceSettings.language(this)))return;
        preferences.edit().putString(InterfaceSettings.LANGUAGE,language).apply();recreate();
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
    private UiAudio uiAudio;
    void playUiClick(){if(uiAudio!=null)uiAudio.click();}
    void playUiConfirm(){if(uiAudio!=null)uiAudio.confirm();}
    void playTimerComplete(){if(uiAudio!=null && hasWindowFocus())uiAudio.timerComplete();}
    private KitchenScreens kitchenScreens;
    boolean hasExclusions() { return profile.hasExclusions(); }


    private SpinProfile profile;

    @Override
    protected void onCreate(Bundle state) {
        InterfaceSettings.migrateLegacyMode(this);
        setTheme(R.style.Theme_DishRoulette_Simple);
        super.onCreate(state);
        uiAudio=new UiAudio(this);
        photoIngredients = new PhotoIngredientsController(this, state);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.getRoot().setLayoutDirection(InterfaceSettings.language(this).equals("ar")?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
        loadout=new NativeIngredientLoadout();

        androidx.core.view.WindowInsetsControllerCompat bars =
                new androidx.core.view.WindowInsetsControllerCompat(getWindow(), binding.getRoot());
        bars.setAppearanceLightStatusBars(!AppearanceSettings.dark(this));
        bars.setAppearanceLightNavigationBars(!AppearanceSettings.dark(this));
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets padding = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(padding.left, padding.top, padding.right, padding.bottom);
            if (simpleHome != null) simpleHome.setKeyboardVisible(insets.isVisible(WindowInsetsCompat.Type.ime()));
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
        GeminiCredentials.importSetup(this);
        recipeApi = RecipeApi.create(this);
        if (state == null) {
            try {
                String[] slots=gson.fromJson(preferences.getString("loaded_ammunition","[]"),String[].class);
                loadout.restore(slots==null?null:java.util.Arrays.asList(slots),preferences.getInt("loaded_chamber",0));
            } catch(RuntimeException ignored) { loadout.restore(null,0); }
        }
        if (state != null) loadout.restore(state.getStringArrayList("ammunition"), state.getInt("active_chamber", 0));
        loadout.setChangedListener(this::persistIngredients);

        kitchenScreens = new KitchenScreens(this,binding.secondaryScreen,preferences,new KitchenScreens.Actions(){
            public void open(RecipeCatalog.Recipe recipe){ sharing.prepare(recipe,translated->{currentRecipe=translated;resultPantry=readPantry();renderRecipe();},()->{currentRecipe=recipe;resultPantry=readPantry();renderRecipe();}); }
            public void remove(RecipeCatalog.Recipe recipe){ savedIds.remove(recipe.id);persistSaved(recipe,true);updateSaveButton(); }
        });
        sharing=new RecipeSharingController(this);
        getOnBackPressedDispatcher().addCallback(this,new androidx.activity.OnBackPressedCallback(true){
            @Override public void handleOnBackPressed(){
                if(kitchenScreens.isOpen())kitchenScreens.close();
                else if(binding.cardMealResult.getVisibility()==View.VISIBLE)binding.cardMealResult.setVisibility(View.GONE);
                else {setEnabled(false);getOnBackPressedDispatcher().onBackPressed();setEnabled(true);}
            }
        });
        setupActions();
        simpleHome = new SimpleHomeView(this, loadout);
        binding.simpleHome.addView(simpleHome, new android.widget.FrameLayout.LayoutParams(-1, -1));
        ViewCompat.requestApplyInsets(binding.getRoot());
        UiTypography.apply(binding.getRoot());
        if (currentRecipe != null && (state == null || !state.getBoolean("was_firing"))) renderRecipe();
        if(state!=null){
            if("settings".equals(state.getString("secondary_screen")))showSettings();
            else if("saved".equals(state.getString("secondary_screen")))showSavedRecipes();
            else if("cooking".equals(state.getString("secondary_screen"))){
                RecipeCatalog.Recipe cooking=decodeRecipe(state.getString("cooking_recipe"));
                if(cooking!=null&&!profile.blocks(cooking)&&!profile.requiresReview)kitchenScreens.cook(cooking.forLanguage(InterfaceSettings.language(this)));
            }
            if(!state.getBoolean("result_visible",true))binding.cardMealResult.setVisibility(View.GONE);
        }
    }

    @Override protected void onPostCreate(Bundle state) {
        super.onPostCreate(state);
        photoIngredients.restore();
        sharing.restore(state);
    }

    private void setupActions() {
        binding.btnCloseResult.setOnClickListener(v -> binding.cardMealResult.setVisibility(View.GONE));
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
    }

    private void selectGoal(String goal) {
        if (spinning) return;
        profile.goal = goal;
        profile.save(preferences, gson);
        refreshHome();
    }

    private void persistIngredients() {
        IngredientMagazine magazine = loadout.magazine();
        // Keep the original storage keys so existing installations retain their pantry.
        preferences.edit().putString("loaded_ammunition", gson.toJson(magazine.save()))
                .putInt("loaded_chamber", magazine.active()).apply();
        refreshHome();
    }

    private void refreshHome() {
        if (simpleHome != null) simpleHome.refresh();
    }

    private Set<String> readPantry() {
        Set<String> pantry = new java.util.LinkedHashSet<>();
        for (String ingredient : loadout.magazine().ingredients())
            pantry.addAll(RecipeCatalog.parseIngredients(ingredient));
        return pantry;
    }

    private void spin(boolean excludeCurrent) {
        if (spinning) return;
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
        setSpinning(true);
        binding.cardMealResult.setVisibility(View.GONE);
        String avoid = excludeCurrent && currentRecipe != null ? currentRecipe.title : "";
        SpinProfile requestProfile = gson.fromJson(gson.toJson(profile), SpinProfile.class);
        List<RecipeApi.Favorite> favorites = new ArrayList<>();
        if (profile.useFavorites) for (RecipeCatalog.Recipe recipe : eligibleFavorites()) favorites.add(new RecipeApi.Favorite(recipe));
        pendingCall = recipeApi.suggest(new RecipeApi.Request(pantry, meal, avoid, requestProfile, favorites, InterfaceSettings.language(this)));
        pendingCall.enqueue(new Callback<RecipeSuggestion>() {
            @Override public void onResponse(Call<RecipeSuggestion> call, Response<RecipeSuggestion> response) {
                if (call.isCanceled() || isDestroyed() || pendingCall != call) return;
                pendingCall = null;
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
                    renderRecipe();
                    binding.cardMealResult.setAlpha(0f);
                    binding.cardMealResult.setTranslationY(48f * getResources().getDisplayMetrics().density);
                    binding.cardMealResult.animate().alpha(1f).translationY(0f).setDuration(Experience.duration(MainActivity.this,480)).start();

                } catch (IllegalArgumentException e) {
                    showGenerationError(getString(R.string.invalid_recipe));
                }
            }
            @Override public void onFailure(Call<RecipeSuggestion> call, Throwable error) {
                if (call.isCanceled() || isDestroyed() || pendingCall != call) return;
                pendingCall = null;
                setSpinning(false);
                // A transport timeout is not evidence that Gemini itself timed out.
                // Only the backend's TIMEOUT response is presented as a generation timeout.
                showGenerationError(getString(R.string.connection_error));
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
        MaterialAlertDialogBuilder dialog=new MaterialAlertDialogBuilder(this).setTitle(R.string.kitchen_error).setMessage(message)
                .setPositiveButton(R.string.home, null);
        if(message.equals(getString(R.string.key_missing))||message.equals(getString(R.string.provider_configuration)))dialog.setNeutralButton(R.string.gemini_settings,(d,w)->showGeminiSettings());
        dialog.show();
    }
    private void persistSaved(RecipeCatalog.Recipe recipe, boolean removed) {
        SharedPreferences.Editor editor = preferences.edit()
                .putStringSet("saved_recipes", new HashSet<>(savedIds));
        if (removed) editor.remove("recipe_json_" + recipe.id).remove("liked_at_" + recipe.id);
        else editor.putString("recipe_json_" + recipe.id, gson.toJson(recipe))
                .putLong("liked_at_" + recipe.id, System.currentTimeMillis());
        editor.apply();
        refreshHome();
    }

    private RecipeCatalog.Recipe decodeRecipe(String json) {
        if (json == null) return null;
        try {
            RecipeCatalog.Recipe recipe = gson.fromJson(json, RecipeCatalog.Recipe.class);
            if (recipe == null || recipe.id == null || recipe.title == null || recipe.meal == null
                    || recipe.description == null || recipe.ingredients == null || recipe.method == null
                    || recipe.keys == null || recipe.keys.contains(null)
                    || recipe.minutes < 1 || recipe.servings < 1) return null;
            RecipeCatalog.Recipe bundled=RecipeCatalog.find(recipe.id);
            if(!recipe.bilingual()&&bundled!=null&&recipe.ingredients.equals(bundled.ingredients)&&recipe.method.equals(bundled.method)){
                recipe.translations=bundled.translations;recipe.language="en";
            }
            return recipe;
        } catch (RuntimeException ignored) { return null; }
    }

    private void setSpinning(boolean busy) {
        spinning = busy;
        loadout.lock(busy);
        binding.btnDislike.setEnabled(!busy);
        if (simpleHome != null) simpleHome.setBusy(busy);
    }

    private void renderRecipe() {
        if(currentRecipe!=null)currentRecipe=currentRecipe.forLanguage(InterfaceSettings.language(this));
        binding.cardMealResult.setVisibility(View.VISIBLE);
        binding.btnCloseResult.setText(R.string.back_home);
        binding.btnDislike.setText(R.string.try_another);
        styleResult();
        UiTypography.apply(binding.cardMealResult);
        binding.cardMealResult.scrollTo(0,0);
        binding.tvResultEyebrow.setText(currentRecipe.language==null||!currentRecipe.language.equals(InterfaceSettings.language(this))?R.string.saved_original_language:currentRecipe.aiGenerated?R.string.ai_result_eyebrow:R.string.result_eyebrow);
        if(currentRecipe.shared)binding.tvResultEyebrow.setText(R.string.shared_recipe_label);
        binding.tvMealTitle.setText(currentRecipe.title);
        binding.tvMealDescription.setText(currentRecipe.description);
        binding.tvMealMeta.setText(getString(R.string.result_metadata, UiLabels.option(this,currentRecipe.meal), KitchenScreens.meta(this,currentRecipe)));
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
        if(currentRecipe.shared){binding.tvMatch.setText(R.string.shared_recipe_note);binding.tvMissing.setVisibility(View.GONE);}
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
    private void styleResult(){
        android.view.ViewGroup page=(android.view.ViewGroup)binding.tvMealTitle.getParent();
        for(int i=0;i<page.getChildCount();i++){
            View v=page.getChildAt(i);
            if(v instanceof TextView){TextView t=(TextView)v;t.setLetterSpacing(0);t.setTypeface(android.graphics.Typeface.create("sans",android.graphics.Typeface.NORMAL));}
        }
        binding.tvMealTitle.setTypeface(android.graphics.Typeface.create(InterfaceSettings.language(this).equals("ar")?"sans":"serif",android.graphics.Typeface.NORMAL));
        binding.btnCloseResult.setBackgroundColor(UiColors.color(this,R.color.surface));
        binding.tvResultSection.setText(R.string.recipe_ready);
        binding.tvMealTitle.setGravity(android.view.Gravity.CENTER);
        binding.tvResultEyebrow.setGravity(android.view.Gravity.CENTER);
        binding.tvResultSection.setGravity(android.view.Gravity.CENTER);
        binding.btnCloseResult.setAllCaps(false);binding.btnRecipe.setAllCaps(false);binding.btnRecipe.setText(R.string.view_recipe);binding.btnRecipe.setCornerRadius(Math.round(16*getResources().getDisplayMetrics().density));binding.btnLike.setCornerRadius(Math.round(16*getResources().getDisplayMetrics().density));
        if(resultPreferences==null){
            resultPreferences=new com.google.android.material.button.MaterialButton(this);resultPreferences.setText(R.string.edit_preferences);
            resultPreferences.setOnClickListener(v->{binding.cardMealResult.setVisibility(View.GONE);showFilters();});
            binding.resultActions.addView(resultPreferences,new LinearLayout.LayoutParams(-1,-2));
        }
        if(resultShare==null){
            resultShare=new com.google.android.material.button.MaterialButton(this);resultShare.setId(R.id.shareRecipe);resultShare.setText(R.string.share_recipe);
            resultShare.setOnClickListener(v->shareRecipe(currentRecipe));binding.resultActions.addView(resultShare,new LinearLayout.LayoutParams(-1,-2));
        }
        styleSecondaryResultAction(binding.btnDislike);
        styleSecondaryResultAction((com.google.android.material.button.MaterialButton)resultPreferences);
        styleSecondaryResultAction((com.google.android.material.button.MaterialButton)resultShare);
        UiIcons.button(binding.btnRecipe,R.drawable.ic_bowl);UiIcons.button(binding.btnLike,R.drawable.ic_bookmark);
        binding.simpleRecipeDetails.setVisibility(View.VISIBLE);binding.simpleRecipeDetails.removeAllViews();
        recipeDetailText(getString(R.string.ingredients_title),true);recipeDetailText(currentRecipe.ingredients,false);recipeDetailText(getString(R.string.method_title),true);recipeDetailText(currentRecipe.method,false);
    }
    private void styleSecondaryResultAction(com.google.android.material.button.MaterialButton button){
        float density=getResources().getDisplayMetrics().density;
        button.setAllCaps(false);button.setTextSize(15);button.setTextColor(UiColors.color(this,R.color.ink));
        button.setTypeface(UiTypography.face(this,"sans-serif",android.graphics.Typeface.BOLD));
        button.setGravity(android.view.Gravity.CENTER);button.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(UiColors.color(this,R.color.surface)));
        button.setStrokeColor(android.content.res.ColorStateList.valueOf(androidx.core.graphics.ColorUtils.setAlphaComponent(UiColors.color(this,R.color.accent),110)));
        button.setStrokeWidth(Math.max(1,Math.round(density)));button.setCornerRadius(Math.round(16*density));
        button.setRippleColor(android.content.res.ColorStateList.valueOf(androidx.core.graphics.ColorUtils.setAlphaComponent(UiColors.color(this,R.color.accent),32)));
        button.setInsetTop(0);button.setInsetBottom(0);button.setMinHeight(Math.round(56*density));
        button.setPadding(Math.round(16*density),Math.round(12*density),Math.round(16*density),Math.round(12*density));button.setElevation(0);
        LinearLayout.LayoutParams layout=(LinearLayout.LayoutParams)button.getLayoutParams();layout.topMargin=Math.round(10*density);button.setLayoutParams(layout);
    }
    private void recipeDetailText(String text,boolean title){
        TextView view=new TextView(this);view.setText(text);view.setTextSize(title?21:16);view.setTextColor(UiColors.color(this,title?R.color.ink:R.color.muted));view.setLineSpacing(6,1);androidx.core.view.ViewCompat.setAccessibilityHeading(view,title);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=Math.round(16*getResources().getDisplayMetrics().density);binding.simpleRecipeDetails.addView(view,lp);
    }
    void refreshConnection(){if(simpleHome!=null)simpleHome.refresh();if(kitchenScreens!=null&&kitchenScreens.screen().equals("settings"))kitchenScreens.settings();}
    void showGeminiSettings(){if(!spinning)GeminiSetupDialog.show(this);}
    void showSettings(){if(!spinning)kitchenScreens.settings();}

    private void updateSaveButton() {
        boolean saved = currentRecipe != null && savedIds.contains(currentRecipe.id);
        UiIcons.button(binding.btnLike,saved?R.drawable.ic_bookmark_filled:R.drawable.ic_bookmark);
        binding.btnLike.setText(saved ? R.string.saved_generic : R.string.save_generic);
        binding.btnLike.setContentDescription(getString(saved ? R.string.remove_recipe : R.string.save_recipe));
    }

    void rememberBilingual(RecipeCatalog.Recipe recipe){
        if(savedIds.contains(recipe.id))preferences.edit().putString("recipe_json_"+recipe.id,gson.toJson(recipe)).apply();
        if(currentRecipe!=null && currentRecipe.id.equals(recipe.id))currentRecipe=recipe;
    }
    void shareRecipe(RecipeCatalog.Recipe recipe){if(!spinning&&recipe!=null)sharing.share(recipe);}
    void importRecipe(){if(!spinning)sharing.importCode();}
    boolean saveSharedRecipe(RecipeCatalog.Recipe recipe){
        if(profile.requiresReview||profile.blocks(recipe)){
            new MaterialAlertDialogBuilder(this).setTitle(recipe.title).setMessage(profile.requiresReview?R.string.review_filters:R.string.allergy_rejected).setPositiveButton(R.string.close,null).show();return false;
        }
        savedIds.add(recipe.id);persistSaved(recipe,false);kitchenScreens.close();currentRecipe=recipe;resultPantry=java.util.Collections.emptySet();renderRecipe();
        android.widget.Toast.makeText(this,R.string.import_saved,android.widget.Toast.LENGTH_SHORT).show();return true;
    }

    void showSavedRecipes() {
        List<RecipeCatalog.Recipe> saved = new ArrayList<>();
        for (String id : savedIds) {
            RecipeCatalog.Recipe recipe = decodeRecipe(preferences.getString("recipe_json_" + id, null));
            if (recipe == null) recipe = RecipeCatalog.find(id);
            if (recipe != null) saved.add(recipe.forLanguage(InterfaceSettings.language(this)));
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
        kitchenScreens.cook(recipe.forLanguage(InterfaceSettings.language(this)));
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

    void showFilters() {
        // Resolve controls from this dialog's layout, independently of generated bindings.
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_preferences, null, false);
        UiTypography.apply(dialogView);
        ChipGroup mealChips = dialogView.findViewById(R.id.mealChips);
        String[] draftMeal = {meal};
        for (String option : SpinOptions.MEALS) {
            Chip chip = selectableChip(UiLabels.option(this, option));
            mealChips.addView(chip);
            chip.setChecked(option.equals(meal));
            chip.setOnCheckedChangeListener((button, checked) -> { if (checked) draftMeal[0] = option; });
        }
        MaterialSwitch switchMotion = dialogView.findViewById(R.id.switchMotion);
        switchMotion.setChecked(preferences.getBoolean("reduce_motion",false));
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
                .setView(dialogView).setPositiveButton(AdaptiveDialogs.compact(this)?R.string.save_preferences_short:R.string.apply_filters, null)
                .setNegativeButton(R.string.cancel, null).create();
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
            draft.mode = profile.mode;
            draft.useFavorites = switchFavorites.isChecked();
            draft.requiresReview = false;
            meal = draftMeal[0];
            preferences.edit().putBoolean("reduce_motion", switchMotion.isChecked())
                    .putString("selected_meal", meal).apply();
            profile = draft;
            profile.save(preferences, gson);
            refreshHome();
            if (currentRecipe != null && binding.cardMealResult.getVisibility()==View.VISIBLE) renderRecipe();
            dialog.dismiss();
        }));
        dialog.show();dialog.getButton(AlertDialog.BUTTON_POSITIVE).setContentDescription(getString(R.string.apply_filters));AdaptiveDialogs.fit(this,dialog);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        photoIngredients.save(state);
        sharing.save(state);
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
        photoIngredients.dispose();
        if(sharing!=null)sharing.dispose();
        // Cancel delivery and release the Activity when leaving or rotating the screen.
        kitchenScreens.dispose();
        if(uiAudio!=null)uiAudio.close();
        if (pendingCall != null) pendingCall.cancel();
        super.onDestroy();
    }
}
