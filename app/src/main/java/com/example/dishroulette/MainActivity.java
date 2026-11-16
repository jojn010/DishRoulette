package com.example.dishroulette;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.dishroulette.core.IngredientExclusions;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.dishroulette.databinding.ActivityMainBinding;
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
    private PantryController pantryController;
    private SimpleHomeView simpleHome;
    PhotoIngredientsController photoIngredients;
    private RecipeSharingController sharing;
    private android.widget.Button resultShare;
    private android.widget.Button resultPreferences;
    private SharedPreferences preferences;
    private final Gson gson = new Gson();
    private RecipeApi recipeApi;
    private Call<RecipeSuggestion> pendingCall;
    private final Set<String> savedIds = new HashSet<>();
    private String meal = "Any meal";
    private RecipeCatalog.Recipe currentRecipe;
    private Set<String> resultPantry = new HashSet<>();
    private boolean generating;
    private UiAudio uiAudio;
    private KitchenScreens kitchenScreens;
    private RecipePreferences profile;

    void scanIngredients() {
        if (!generating && simpleHome.commitPending())
            photoIngredients.open();
    }

    String addPhotoIngredients(String text) {
        List<String> additions = new ArrayList<>();
        for (String line : text.split("\\r?\\n"))
            if (!line.trim().isEmpty())
                additions.add(line.trim());
        if (additions.isEmpty())
            return getString(R.string.photo_review_empty);
        PantryEntries candidate = new PantryEntries();
        candidate.restore(pantryController.entries().save(), 0);
        Set<String> seen = new HashSet<>(readPantry());
        for (String name : additions) {
            Set<String> parsed = RecipeCatalog.parseIngredients(name);
            if (parsed.size() == 1 && seen.containsAll(parsed))
                continue;
            candidate.select(candidate.firstEmptySlot());
            candidate.edit(name);
            PantryEntries.ValidationError error = candidate.validationError();
            if (error != null)
                return IngredientValidation.message(this, error);
            seen.addAll(parsed);
        }
        pantryController.restore(candidate.save(), 0);
        chooseGenerationMode("pantry");
        return null;
    }

    RecipePreferences profile() {
        return profile;
    }

    String meal() {
        return meal;
    }

    void generate(boolean another) {
        requestRecipe(another);
    }

    void chooseServings(int count) {
        if (generating)
            return;
        profile.servings = Math.max(1, Math.min(12, count));
        profile.save(preferences, gson);
        refreshHome();
    }

    void chooseGoal(String goal) {
        selectGoal(goal);
    }

    void chooseGenerationMode(String mode) {
        if (generating)
            return;
        profile.mode = mode;
        profile.save(preferences, gson);
        refreshHome();
    }

    void cancelGeneration() {
        if (pendingCall != null) {
            pendingCall.cancel();
            pendingCall = null;
        }
        setGenerating(false);
    }

    void changeLanguage(String language) {
        if (generating || language.equals(InterfaceSettings.language(this)))
            return;
        preferences.edit().putString(InterfaceSettings.LANGUAGE, language).apply();
        recreate();
    }

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(InterfaceSettings.localized(base));
    }

    void playUiClick() {
        if (uiAudio != null)
            uiAudio.click();
    }

    void playUiConfirm() {
        if (uiAudio != null)
            uiAudio.confirm();
    }

    void playTimerComplete() {
        if (uiAudio != null && hasWindowFocus())
            uiAudio.timerComplete();
    }

    boolean hasExclusions() {
        return profile.hasExclusions();
    }


    @Override
    protected void onCreate(Bundle state) {
        InterfaceSettings.migrateLegacyMode(this);
        setTheme(R.style.Theme_DishRoulette_Simple);
        super.onCreate(state);
        uiAudio = new UiAudio(this);
        photoIngredients = new PhotoIngredientsController(this, state);
        createRootView();
        configureSystemInsets();
        restoreUserState(state);
        restoreIngredients(state);
        createScreenControllers();
        configureBackNavigation();
        createHomeScreen();
        restoreVisibleScreen(state);
    }

    private void createRootView() {
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.getRoot().setLayoutDirection(
            InterfaceSettings.language(this).equals("ar") ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        pantryController = new PantryController();

    }

    private void configureSystemInsets() {
        androidx.core.view.WindowInsetsControllerCompat bars =
            new androidx.core.view.WindowInsetsControllerCompat(getWindow(), binding.getRoot());
        bars.setAppearanceLightStatusBars(!AppearanceSettings.dark(this));
        bars.setAppearanceLightNavigationBars(!AppearanceSettings.dark(this));
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets padding = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(padding.left, padding.top, padding.right, padding.bottom);
            if (simpleHome != null)
                simpleHome.setKeyboardVisible(insets.isVisible(WindowInsetsCompat.Type.ime()));
            return insets;
        });
    }

    private void restoreUserState(Bundle state) {
        preferences = getSharedPreferences("dishroulette_ui", MODE_PRIVATE);
        profile = RecipePreferences.load(preferences, gson);
        meal = preferences.getString("selected_meal", "Any meal");
        savedIds.addAll(preferences.getStringSet("saved_recipes", new HashSet<>()));
        if (state != null) {
            meal = state.getString("meal", "Any meal");
            currentRecipe = decodeRecipe(state.getString("recipe_json"));
            resultPantry = RecipeCatalog.parseIngredients(state.getString("result_pantry", ""));
        }
        GeminiCredentials.importSetup(this);
        recipeApi = RecipeApi.create(this);
    }

    private void restoreIngredients(Bundle state) {
        if (state == null) {
            try {
                String[] slots = gson.fromJson(preferences.getString("loaded_ammunition", "[]"), String[].class);
                pantryController.restore(slots == null ? null : java.util.Arrays.asList(slots),
                    preferences.getInt("loaded_chamber", 0));
            } catch (RuntimeException ignored) {
                pantryController.restore(null, 0);
            }
        }
        if (state != null)
            pantryController.restore(state.getStringArrayList("ammunition"), state.getInt("active_chamber", 0));
        pantryController.setChangedListener(this::persistIngredients);

    }

    private void createScreenControllers() {
        kitchenScreens = new KitchenScreens(this, binding.secondaryScreen, preferences, new KitchenScreens.Actions() {
            public void open(RecipeCatalog.Recipe recipe) {
                sharing.prepare(recipe, MainActivity.this::openRecipeResult, () -> openRecipeResult(recipe));
            }

            public void remove(RecipeCatalog.Recipe recipe) {
                savedIds.remove(recipe.id);
                persistSaved(recipe, true);
                updateSaveButton();
            }
        });
        sharing = new RecipeSharingController(this);
    }

    private void configureBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (kitchenScreens.isOpen())
                    kitchenScreens.close();
                else if (binding.cardMealResult.getVisibility() == View.VISIBLE)
                    binding.cardMealResult.setVisibility(View.GONE);
                else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });
    }

    private void createHomeScreen() {
        setupActions();
        simpleHome = new SimpleHomeView(this, pantryController);
        binding.simpleHome.addView(simpleHome, new android.widget.FrameLayout.LayoutParams(-1, -1));
        ViewCompat.requestApplyInsets(binding.getRoot());
        UiTypography.apply(binding.getRoot());
    }

    private void restoreVisibleScreen(Bundle state) {
        if (currentRecipe != null && (state == null || !state.getBoolean("was_firing")))
            renderRecipe();
        if (state != null) {
            if ("settings".equals(state.getString("secondary_screen")))
                showSettings();
            else if ("saved".equals(state.getString("secondary_screen")))
                showSavedRecipes();
            else if ("cooking".equals(state.getString("secondary_screen"))) {
                RecipeCatalog.Recipe cooking = decodeRecipe(state.getString("cooking_recipe"));
                if (cooking != null && !profile.blocks(cooking) && !profile.requiresReview)
                    kitchenScreens.cook(cooking.forLanguage(InterfaceSettings.language(this)));
            }
            if (!state.getBoolean("result_visible", true))
                binding.cardMealResult.setVisibility(View.GONE);
        }
    }

    private void openRecipeResult(RecipeCatalog.Recipe recipe) {
        currentRecipe = recipe;
        resultPantry = readPantry();
        renderRecipe();
    }

    @Override
    protected void onPostCreate(Bundle state) {
        super.onPostCreate(state);
        photoIngredients.restore();
        sharing.restore(state);
    }

    private void setupActions() {
        configureResultAppearance();
        binding.btnCloseResult.setOnClickListener(v -> binding.cardMealResult.setVisibility(View.GONE));
        binding.btnDislike.setOnClickListener(view -> requestRecipe(true));
        binding.btnLike.setOnClickListener(view -> {
            if (currentRecipe == null)
                return;
            boolean removed = !savedIds.add(currentRecipe.id);
            if (removed)
                savedIds.remove(currentRecipe.id);
            persistSaved(currentRecipe, removed);
            updateSaveButton();
            Snackbar.make(binding.getRoot(), removed ? R.string.removed_feedback
                : R.string.saved_feedback, Snackbar.LENGTH_SHORT).show();
        });
        binding.btnRecipe.setOnClickListener(view -> {
            if (currentRecipe != null)
                showRecipe(currentRecipe);
        });
    }

    private void selectGoal(String goal) {
        if (generating)
            return;
        profile.goal = goal;
        profile.save(preferences, gson);
        refreshHome();
    }

    private void persistIngredients() {
        PantryEntries entries = pantryController.entries();
        // Keep the original storage keys so existing installations retain their pantry.
        preferences.edit().putString("loaded_ammunition", gson.toJson(entries.save()))
            .putInt("loaded_chamber", entries.active()).apply();
        refreshHome();
    }

    private void refreshHome() {
        if (simpleHome != null)
            simpleHome.refresh();
    }

    private Set<String> readPantry() {
        Set<String> pantry = new java.util.LinkedHashSet<>();
        for (String ingredient : pantryController.entries().ingredients())
            pantry.addAll(RecipeCatalog.parseIngredients(ingredient));
        return pantry;
    }

    private void requestRecipe(boolean excludeCurrent) {
        if (generating)
            return;
        Set<String> pantry = profile.mode.equals("discover") ? new HashSet<>() : readPantry();
        if (!validateGeneration(pantry))
            return;
        setGenerating(true);
        binding.cardMealResult.setVisibility(View.GONE);
        String avoid = excludeCurrent && currentRecipe != null ? currentRecipe.title : "";
        RecipePreferences requestProfile = profile.copy();
        List<RecipeApi.Favorite> favorites = new ArrayList<>();
        if (profile.useFavorites)
            for (RecipeCatalog.Recipe recipe : eligibleFavorites())
                favorites.add(new RecipeApi.Favorite(recipe));
        pendingCall = recipeApi.suggest(
            new RecipeApi.Request(pantry, meal, avoid, requestProfile, favorites, InterfaceSettings.language(this)));
        pendingCall.enqueue(new Callback<RecipeSuggestion>() {
            @Override
            public void onResponse(Call<RecipeSuggestion> call, Response<RecipeSuggestion> response) {
                if (call.isCanceled() || isDestroyed() || pendingCall != call)
                    return;
                pendingCall = null;
                setGenerating(false);
                if (!response.isSuccessful()) {
                    showGenerationError(apiError(response));
                    return;
                }
                try {
                    if (response.body() == null)
                        throw new IllegalArgumentException();
                    showGeneratedRecipe(response.body(), pantry, requestProfile);
                } catch (IllegalArgumentException e) {
                    showGenerationError(getString(R.string.invalid_recipe));
                }
            }

            @Override
            public void onFailure(Call<RecipeSuggestion> call, Throwable error) {
                if (call.isCanceled() || isDestroyed() || pendingCall != call)
                    return;
                pendingCall = null;
                setGenerating(false);
                // A transport timeout is not evidence that Gemini itself timed out.
                // Only the backend's TIMEOUT response is presented as a generation timeout.
                showGenerationError(getString(R.string.connection_error));
            }
        });
    }

    private void showGeneratedRecipe(RecipeSuggestion suggestion, Set<String> pantry,
                                     RecipePreferences requestProfile) {
        currentRecipe = suggestion.toRecipe(pantry, requestProfile);
        currentRecipe.language = InterfaceSettings.language(MainActivity.this);
        resultPantry = new HashSet<>(pantry);
        renderRecipe();
        animateRecipeResult();
    }

    private void animateRecipeResult() {
        binding.cardMealResult.setAlpha(0f);
        binding.cardMealResult.setTranslationY(48f * getResources().getDisplayMetrics().density);
        binding.cardMealResult.animate().alpha(1f).translationY(0f)
            .setDuration(Experience.duration(MainActivity.this, 480)).start();
    }

    private boolean validateGeneration(Set<String> pantry) {
        if (profile.requiresReview) {
            showGenerationError(getString(R.string.review_filters));
            return false;
        }
        PantryEntries.ValidationError ingredientError = pantryController.entries().validationError();
        if (profile.mode.equals("pantry") && ingredientError != null) {
            showGenerationError(IngredientValidation.message(this, ingredientError));
            return false;
        }
        if (profile.mode.equals("pantry") && pantry.isEmpty()) {
            android.widget.Toast.makeText(this, R.string.ingredient_error, android.widget.Toast.LENGTH_LONG).show();
            pantryController.focusSelected();
            return false;
        }
        Set<String> conflicts = IngredientExclusions.conflicts(pantry, profile.allergies, profile.customExclusions);
        if (!conflicts.isEmpty()) {
            showGenerationError(getString(R.string.allergy_conflict, localizedNames(conflicts)));
            return false;
        }
        if (profile.hasExclusions() && IngredientExclusions.hasUncertainIngredients(pantry)) {
            showGenerationError(getString(R.string.uncertain_ingredients));
            return false;
        }
        return true;
    }

    private String apiError(Response<?> response) {
        String code = "";
        if (response.errorBody() != null) {
            try (okhttp3.ResponseBody body = response.errorBody()) {
                code = JsonParser.parseString(body.string()).getAsJsonObject().get("code").getAsString();
            } catch (Exception ignored) { /* Fall back to a safe message, not raw server errors. */ }
        }
        switch (code) {
            case "KEY_NOT_CONFIGURED":
                return getString(R.string.key_missing);
            case "PROVIDER_UNAVAILABLE":
                return getString(R.string.provider_unavailable);
            case "PROVIDER_CONFIGURATION":
                return getString(R.string.provider_configuration);
            case "NO_RECIPE":
                return getString(R.string.no_filtered_recipe);
            case "INVALID_FILTERS":
                return getString(R.string.preferences_invalid);
            case "ALLERGY_CONFLICT":
            case "ALLERGY_REJECTED":
                return getString(R.string.allergy_rejected);
            case "UNCERTAIN_INGREDIENTS":
                return getString(R.string.uncertain_ingredients);
            case "INVALID_INGREDIENTS":
                return getString(R.string.invalid_ingredients);
            case "TIMEOUT":
                return getString(R.string.generation_timeout);
            case "RATE_LIMITED":
                return getString(R.string.rate_limited);
            default:
                return getString(response.code() == 429 ? R.string.rate_limited : R.string.invalid_recipe);
        }
    }

    private void showGenerationError(String message) {
        MaterialAlertDialogBuilder dialog =
            new MaterialAlertDialogBuilder(this).setTitle(R.string.kitchen_error).setMessage(message)
                .setPositiveButton(R.string.home, null);
        if (message.equals(getString(R.string.key_missing)) ||
            message.equals(getString(R.string.provider_configuration)))
            dialog.setNeutralButton(R.string.gemini_settings, (d, w) -> showGeminiSettings());
        dialog.show();
    }

    private void persistSaved(RecipeCatalog.Recipe recipe, boolean removed) {
        SharedPreferences.Editor editor = preferences.edit()
            .putStringSet("saved_recipes", new HashSet<>(savedIds));
        if (removed)
            editor.remove("recipe_json_" + recipe.id).remove("liked_at_" + recipe.id);
        else
            editor.putString("recipe_json_" + recipe.id, gson.toJson(recipe))
                .putLong("liked_at_" + recipe.id, System.currentTimeMillis());
        editor.apply();
        refreshHome();
    }

    private RecipeCatalog.Recipe decodeRecipe(String json) {
        if (json == null)
            return null;
        try {
            RecipeCatalog.Recipe recipe = gson.fromJson(json, RecipeCatalog.Recipe.class);
            if (recipe == null || recipe.id == null || recipe.title == null || recipe.meal == null
                || recipe.description == null || recipe.ingredients == null || recipe.method == null
                || recipe.keys == null || recipe.keys.contains(null)
                || recipe.minutes < 1 || recipe.servings < 1)
                return null;
            RecipeCatalog.Recipe bundled = RecipeCatalog.find(recipe.id);
            if (!recipe.bilingual() && bundled != null && recipe.ingredients.equals(bundled.ingredients) &&
                recipe.method.equals(bundled.method)) {
                recipe.translations = bundled.translations;
                recipe.language = "en";
            }
            return recipe;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void setGenerating(boolean busy) {
        generating = busy;
        pantryController.lock(busy);
        binding.btnDislike.setEnabled(!busy);
        if (simpleHome != null)
            simpleHome.setBusy(busy);
    }

    private void renderRecipe() {
        if (currentRecipe != null)
            currentRecipe = currentRecipe.forLanguage(InterfaceSettings.language(this));
        binding.cardMealResult.setVisibility(View.VISIBLE);
        binding.btnCloseResult.setText(R.string.back_home);
        binding.btnDislike.setText(R.string.try_another);
        renderRecipeDetails();
        UiTypography.apply(binding.cardMealResult);
        binding.cardMealResult.scrollTo(0, 0);
        renderRecipeHeading();
        renderPantryMatch();
        renderPersonalization();
        renderDietaryWarning();
        updateSaveButton();
    }

    private void renderRecipeHeading() {
        binding.tvResultEyebrow.setText(currentRecipe.language == null || !currentRecipe.language.equals(
            InterfaceSettings.language(
                this)) ? R.string.saved_original_language : currentRecipe.aiGenerated ? R.string.ai_result_eyebrow : R.string.result_eyebrow);
        if (currentRecipe.shared)
            binding.tvResultEyebrow.setText(R.string.shared_recipe_label);
        binding.tvMealTitle.setText(currentRecipe.title);
        binding.tvMealDescription.setText(currentRecipe.description);
        binding.tvMealMeta.setText(getString(R.string.result_metadata, UiLabels.option(this, currentRecipe.meal),
            RecipeText.meta(this, currentRecipe)));
    }

    private void renderPantryMatch() {
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
        if (currentRecipe.shared) {
            binding.tvMatch.setText(R.string.shared_recipe_note);
            binding.tvMissing.setVisibility(View.GONE);
        }
        binding.resultActions.setVisibility(View.VISIBLE);
    }

    private void renderPersonalization() {
        String explanation = currentRecipe.whyThisDish == null ? "" : currentRecipe.whyThisDish;
        String fit = currentRecipe.goalFit == null ? "" : currentRecipe.goalFit;
        String personalization = getString(R.string.personalization_details,
            explanation.isEmpty() ? "" : getString(R.string.why_recipe, explanation),
            fit.isEmpty() ? "" : getString(R.string.goal_fit, fit));
        binding.tvPersonalization.setText(personalization.trim());
        binding.tvPersonalization.setVisibility(explanation.isEmpty() && fit.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void renderDietaryWarning() {
        boolean blocked = profile.requiresReview || profile.blocks(currentRecipe);
        binding.tvAllergyWarning.setText(blocked ? R.string.allergy_rejected : R.string.allergy_note);
        binding.tvAllergyWarning.setVisibility(profile.hasExclusions() ? View.VISIBLE : View.GONE);
        binding.btnRecipe.setEnabled(!blocked);
    }

    private String localizedNames(java.util.Collection<String> names) {
        List<String> display = new ArrayList<>();
        for (String value : names)
            display.add(UiLabels.option(this, value));
        return android.text.TextUtils.join(", ", display);
    }

    private void configureResultAppearance() {
        android.view.ViewGroup page = (android.view.ViewGroup) binding.tvMealTitle.getParent();
        for (int i = 0; i < page.getChildCount(); i++) {
            View v = page.getChildAt(i);
            if (v instanceof TextView) {
                TextView t = (TextView) v;
                t.setLetterSpacing(0);
                t.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL));
            }
        }
        binding.tvMealTitle.setTypeface(
            android.graphics.Typeface.create(InterfaceSettings.language(this).equals("ar") ? "sans" : "serif",
                android.graphics.Typeface.NORMAL));
        binding.btnCloseResult.setBackgroundColor(UiColors.color(this, R.color.surface));
        binding.tvResultSection.setText(R.string.recipe_ready);
        binding.tvMealTitle.setGravity(android.view.Gravity.CENTER);
        binding.tvResultEyebrow.setGravity(android.view.Gravity.CENTER);
        binding.tvResultSection.setGravity(android.view.Gravity.CENTER);
        binding.btnCloseResult.setAllCaps(false);
        binding.btnRecipe.setAllCaps(false);
        binding.btnRecipe.setText(R.string.view_recipe);
        binding.btnRecipe.setCornerRadius(Math.round(16 * getResources().getDisplayMetrics().density));
        binding.btnLike.setCornerRadius(Math.round(16 * getResources().getDisplayMetrics().density));
        if (resultPreferences == null) {
            resultPreferences = new com.google.android.material.button.MaterialButton(this);
            resultPreferences.setText(R.string.edit_preferences);
            resultPreferences.setOnClickListener(v -> {
                binding.cardMealResult.setVisibility(View.GONE);
                showFilters();
            });
            binding.resultActions.addView(resultPreferences, new LinearLayout.LayoutParams(-1, -2));
        }
        if (resultShare == null) {
            resultShare = new com.google.android.material.button.MaterialButton(this);
            resultShare.setId(R.id.shareRecipe);
            resultShare.setText(R.string.share_recipe);
            resultShare.setOnClickListener(v -> shareRecipe(currentRecipe));
            binding.resultActions.addView(resultShare, new LinearLayout.LayoutParams(-1, -2));
        }
        styleSecondaryResultAction(binding.btnDislike);
        styleSecondaryResultAction((com.google.android.material.button.MaterialButton) resultPreferences);
        styleSecondaryResultAction((com.google.android.material.button.MaterialButton) resultShare);
        UiIcons.button(binding.btnRecipe, R.drawable.ic_bowl);
        UiIcons.button(binding.btnLike, R.drawable.ic_bookmark);
    }

    private void renderRecipeDetails() {
        binding.simpleRecipeDetails.setVisibility(View.VISIBLE);
        binding.simpleRecipeDetails.removeAllViews();
        recipeDetailText(getString(R.string.ingredients_title), true);
        recipeDetailText(currentRecipe.ingredients, false);
        recipeDetailText(getString(R.string.method_title), true);
        recipeDetailText(currentRecipe.method, false);
    }

    private void styleSecondaryResultAction(com.google.android.material.button.MaterialButton button) {
        float density = getResources().getDisplayMetrics().density;
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTextColor(UiColors.color(this, R.color.ink));
        button.setTypeface(UiTypography.face(this, "sans-serif", android.graphics.Typeface.BOLD));
        button.setGravity(android.view.Gravity.CENTER);
        button.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(UiColors.color(this, R.color.surface)));
        button.setStrokeColor(android.content.res.ColorStateList.valueOf(
            androidx.core.graphics.ColorUtils.setAlphaComponent(UiColors.color(this, R.color.accent), 110)));
        button.setStrokeWidth(Math.max(1, Math.round(density)));
        button.setCornerRadius(Math.round(16 * density));
        button.setRippleColor(android.content.res.ColorStateList.valueOf(
            androidx.core.graphics.ColorUtils.setAlphaComponent(UiColors.color(this, R.color.accent), 32)));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setMinHeight(Math.round(56 * density));
        button.setPadding(Math.round(16 * density), Math.round(12 * density), Math.round(16 * density),
            Math.round(12 * density));
        button.setElevation(0);
        LinearLayout.LayoutParams layout = (LinearLayout.LayoutParams) button.getLayoutParams();
        layout.topMargin = Math.round(10 * density);
        button.setLayoutParams(layout);
    }

    private void recipeDetailText(String text, boolean title) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(title ? 21 : 16);
        view.setTextColor(UiColors.color(this, title ? R.color.ink : R.color.muted));
        view.setLineSpacing(6, 1);
        androidx.core.view.ViewCompat.setAccessibilityHeading(view, title);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = Math.round(16 * getResources().getDisplayMetrics().density);
        binding.simpleRecipeDetails.addView(view, lp);
    }

    void refreshConnection() {
        if (simpleHome != null)
            simpleHome.refresh();
        if (kitchenScreens != null && kitchenScreens.screen().equals("settings"))
            kitchenScreens.settings();
    }

    void showGeminiSettings() {
        if (!generating)
            GeminiSetupDialog.show(this);
    }

    void showSettings() {
        if (!generating)
            kitchenScreens.settings();
    }

    private void updateSaveButton() {
        boolean saved = currentRecipe != null && savedIds.contains(currentRecipe.id);
        UiIcons.button(binding.btnLike, saved ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
        binding.btnLike.setText(saved ? R.string.saved_generic : R.string.save_generic);
        binding.btnLike.setContentDescription(getString(saved ? R.string.remove_recipe : R.string.save_recipe));
    }

    void rememberBilingual(RecipeCatalog.Recipe recipe) {
        if (savedIds.contains(recipe.id))
            preferences.edit().putString("recipe_json_" + recipe.id, gson.toJson(recipe)).apply();
        if (currentRecipe != null && currentRecipe.id.equals(recipe.id))
            currentRecipe = recipe;
    }

    void shareRecipe(RecipeCatalog.Recipe recipe) {
        if (!generating && recipe != null)
            sharing.share(recipe);
    }

    void importRecipe() {
        if (!generating)
            sharing.importCode();
    }

    boolean saveSharedRecipe(RecipeCatalog.Recipe recipe) {
        if (profile.requiresReview || profile.blocks(recipe)) {
            new MaterialAlertDialogBuilder(this).setTitle(recipe.title)
                .setMessage(profile.requiresReview ? R.string.review_filters : R.string.allergy_rejected)
                .setPositiveButton(R.string.close, null).show();
            return false;
        }
        savedIds.add(recipe.id);
        persistSaved(recipe, false);
        kitchenScreens.close();
        currentRecipe = recipe;
        resultPantry = java.util.Collections.emptySet();
        renderRecipe();
        android.widget.Toast.makeText(this, R.string.import_saved, android.widget.Toast.LENGTH_SHORT).show();
        return true;
    }

    void showSavedRecipes() {
        List<RecipeCatalog.Recipe> saved = new ArrayList<>();
        for (String id : savedIds) {
            RecipeCatalog.Recipe recipe = decodeRecipe(preferences.getString("recipe_json_" + id, null));
            if (recipe == null)
                recipe = RecipeCatalog.find(id);
            if (recipe != null)
                saved.add(recipe.forLanguage(InterfaceSettings.language(this)));
        }
        saved.sort((a, b) -> a.title.compareToIgnoreCase(b.title));
        kitchenScreens.saved(saved);
    }

    private void showRecipe(RecipeCatalog.Recipe recipe) {
        if (profile.blocks(recipe) || profile.requiresReview) {
            MaterialAlertDialogBuilder blocked = new MaterialAlertDialogBuilder(this).setTitle(recipe.title)
                .setMessage(profile.requiresReview ? R.string.review_filters : R.string.allergy_rejected)
                .setPositiveButton(R.string.close, null);
            if (savedIds.contains(recipe.id))
                blocked.setNeutralButton(R.string.remove_recipe, (dialog, which) -> {
                    savedIds.remove(recipe.id);
                    persistSaved(recipe, true);
                    updateSaveButton();
                });
            blocked.show();
            return;
        }
        kitchenScreens.cook(recipe.forLanguage(InterfaceSettings.language(this)));
    }

    private List<RecipeCatalog.Recipe> eligibleFavorites() {
        List<RecipeCatalog.Recipe> recipes = new ArrayList<>();
        Set<String> titles = new HashSet<>();
        List<String> ids = new ArrayList<>(savedIds);
        ids.sort((a, b) -> {
            int compare =
                Long.compare(preferences.getLong("liked_at_" + b, 0), preferences.getLong("liked_at_" + a, 0));
            return compare != 0 ? compare : a.compareTo(b);
        });
        for (String id : ids) {
            RecipeCatalog.Recipe recipe = decodeRecipe(preferences.getString("recipe_json_" + id, null));
            if (recipe == null)
                recipe = RecipeCatalog.find(id);
            if (recipe != null && !profile.blocks(recipe) && titles.add(recipe.title.toLowerCase(Locale.ROOT)))
                recipes.add(recipe);
            if (recipes.size() == 8)
                break;
        }
        return recipes;
    }

    void showFilters() {
        new RecipePreferencesDialog(this, profile, meal, this::applyRecipePreferences)
            .show(preferences, eligibleFavorites().size());
    }

    private void applyRecipePreferences(RecipePreferences draft, String selectedMeal, boolean reduceMotion) {
        draft.mode = profile.mode;
        profile = draft;
        meal = selectedMeal;
        preferences.edit().putBoolean("reduce_motion", reduceMotion).putString("selected_meal", meal).apply();
        profile.save(preferences, gson);
        refreshHome();
        if (currentRecipe != null && binding.cardMealResult.getVisibility() == View.VISIBLE)
            renderRecipe();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        photoIngredients.save(state);
        sharing.save(state);
        state.putBoolean("was_firing", generating);
        state.putBoolean("result_visible", binding.cardMealResult.getVisibility() == View.VISIBLE);
        state.putString("secondary_screen", kitchenScreens.screen());
        if (kitchenScreens.cookingRecipe() != null)
            state.putString("cooking_recipe", gson.toJson(kitchenScreens.cookingRecipe()));
        state.putStringArrayList("ammunition", pantryController.entries().save());
        state.putInt("active_chamber", pantryController.entries().active());
        state.putString("meal", meal);
        if (currentRecipe != null)
            state.putString("recipe_json", gson.toJson(currentRecipe));
        state.putString("result_pantry", android.text.TextUtils.join(",", resultPantry));
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onDestroy() {
        photoIngredients.dispose();
        if (sharing != null)
            sharing.dispose();
        // Cancel delivery and release the Activity when leaving or rotating the screen.
        kitchenScreens.dispose();
        if (uiAudio != null)
            uiAudio.close();
        if (pendingCall != null)
            pendingCall.cancel();
        super.onDestroy();
    }
}
