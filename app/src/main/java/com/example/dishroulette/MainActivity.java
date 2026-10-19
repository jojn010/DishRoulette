package com.example.dishroulette;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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

    private void afterFiring(Runnable reveal) {
        if (firingFinished) reveal.run();
        else pendingReveal = reveal;
    }

    private SpinProfile profile;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        androidx.core.view.WindowInsetsControllerCompat bars =
                new androidx.core.view.WindowInsetsControllerCompat(getWindow(), binding.getRoot());
        bars.setAppearanceLightStatusBars(false);
        bars.setAppearanceLightNavigationBars(false);
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets padding = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            view.setPadding(padding.left, padding.top, padding.right, padding.bottom);
            return insets;
        });
        preferences = getSharedPreferences("dishroulette_ui", MODE_PRIVATE);
        profile = SpinProfile.load(preferences, gson);
        savedIds.addAll(preferences.getStringSet("saved_recipes", new HashSet<>()));
        if (state != null) {
            meal = state.getString("meal", "Any meal");
            currentRecipe = decodeRecipe(state.getString("recipe_json"));
            resultPantry = RecipeCatalog.parseIngredients(state.getString("result_pantry", ""));
        }
        recipeApi = RecipeApi.create();
        mechanicalSound = new MechanicalAudio(this);
        binding.tabletop.setAudio(mechanicalSound);
        if (state != null) binding.tabletop.restore(state.getStringArrayList("ammunition"), state.getInt("active_chamber", 0));
        binding.tabletop.setChangedListener(this::updateMagazineStatus);

        createChips();
        setupActions();
        updateMode();
        updateFilterSummary();
        updateSpinCounter();
        if (currentRecipe != null && (state == null || !state.getBoolean("was_firing"))) renderRecipe();
        if (state != null && state.getBoolean("was_firing"))
            binding.tvSceneHelp.setText(R.string.shot_interrupted);
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
        for (String label : new String[]{"Any meal", "Breakfast", "Lunch", "Dinner"}) {
            Chip chip = new Chip(this);
            chip.setId(View.generateViewId());
            chip.setText(label);
            chip.setCheckable(true);
            chip.setEnsureMinTouchTargetSize(true);
            chip.setCheckedIconVisible(false);
            // The selected meal is restored explicitly; generated IDs change on recreation.
            chip.setSaveEnabled(false);
            binding.mealChips.addView(chip);
            chip.setChecked(label.equals(meal));
            chip.setOnCheckedChangeListener((button, checked) -> {
                if (checked) meal = label;
            });
        }
    }

    private void setupActions() {
        binding.btnPreviousLoad.setOnClickListener(v -> binding.tabletop.page(-1));
        binding.btnNextLoad.setOnClickListener(v -> binding.tabletop.page(1));
        binding.btnClearChamber.setOnClickListener(v -> binding.tabletop.clearChamber());
        binding.btnClearAll.setOnClickListener(v -> binding.tabletop.clearAll());
        binding.btnCloseResult.setOnClickListener(v -> binding.cardMealResult.setVisibility(View.GONE));
        binding.btnBalanced.setOnClickListener(v -> selectGoal("Balanced"));
        binding.btnCut.setOnClickListener(v -> selectGoal("Cutting"));
        binding.btnBulk.setOnClickListener(v -> selectGoal("Bulking"));
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
        binding.btnFilters.setOnClickListener(view -> showFilters());
    }

    private void selectGoal(String goal) {
        if (spinning || binding.tabletop.isLoading()) return;
        profile.goal = goal;
        profile.save(preferences, gson);
        updateFilterSummary();
    }

    private void updateMagazineStatus() {
        IngredientMagazine magazine = binding.tabletop.magazine();
        binding.tvMagazineStatus.setText(getString(R.string.magazine_status, magazine.page() + 1,
                magazine.active() % 6 + 1, magazine.count()));
    }

    private Set<String> readPantry() {
        Set<String> pantry = new java.util.LinkedHashSet<>();
        for (String ingredient : binding.tabletop.magazine().ingredients())
            pantry.addAll(RecipeCatalog.parseIngredients(ingredient));
        return pantry;
    }

    private void spin(boolean excludeCurrent) {
        if (spinning || binding.tabletop.isLoading()) return;
        if (profile.requiresReview) { showGenerationError(getString(R.string.review_filters)); return; }
        String ingredientError = binding.tabletop.magazine().validationError();
        if (profile.mode.equals("pantry") && ingredientError != null) { showGenerationError(ingredientError); return; }
        Set<String> pantry = profile.mode.equals("discover") ? new HashSet<>() : readPantry();
        if (profile.mode.equals("pantry") && pantry.isEmpty()) {
            android.widget.Toast.makeText(this, R.string.ingredient_error, android.widget.Toast.LENGTH_LONG).show();
            binding.tabletop.focusCartridge();
            return;
        }
        Set<String> conflicts = IngredientExclusions.conflicts(pantry, profile.allergies, profile.customExclusions);
        if (!conflicts.isEmpty()) {
            showGenerationError(getString(R.string.allergy_conflict, android.text.TextUtils.join(", ", conflicts)));
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
        binding.tabletop.fire(() -> {
            firingFinished = true;
            binding.tvSceneHelp.setText(R.string.waiting_for_recipe);
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
        pendingCall = recipeApi.suggest(new RecipeApi.Request(pantry, meal, avoid, requestProfile, favorites));
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
                        resultPantry = new HashSet<>(pantry);
                        updateSpinCounter();
                        preferences.edit().putInt("spin_count", preferences.getInt("spin_count", 0) + 1).apply();
                        updateSpinCounter();
                        renderRecipe();
                        binding.cardMealResult.setAlpha(0f);
                        binding.cardMealResult.setTranslationY(48f * getResources().getDisplayMetrics().density);
                        binding.cardMealResult.animate().alpha(1f).translationY(0f).setDuration(480).start();

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
        new MaterialAlertDialogBuilder(this).setTitle(R.string.generation_error_title)
                .setMessage(message).setPositiveButton(R.string.close, null).show();
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
        if (busy) binding.tvSceneHelp.setText(R.string.firing_recipe);
        else updateMode();
        if (!busy) {
            binding.tabletop.finish();

        }
        binding.btnSpin.setEnabled(!busy);
        binding.btnDislike.setEnabled(!busy);
        binding.btnFilters.setEnabled(!busy);
        for (int i = 0; i < binding.modeChips.getChildCount(); i++) binding.modeChips.getChildAt(i).setEnabled(!busy);
        binding.tabletop.lock(busy);
        binding.btnPreviousLoad.setEnabled(!busy);
        binding.btnNextLoad.setEnabled(!busy);
        binding.btnClearChamber.setEnabled(!busy);
        binding.btnClearAll.setEnabled(!busy);
        binding.btnBalanced.setEnabled(!busy);
        binding.btnCut.setEnabled(!busy);
        binding.btnBulk.setEnabled(!busy);
        binding.btnSpin.setText(busy ? R.string.spinning : R.string.spin);
        binding.spinProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
        for (int i = 0; i < binding.mealChips.getChildCount(); i++) {
            binding.mealChips.getChildAt(i).setEnabled(!busy);
        }
    }

    private void renderRecipe() {
        binding.cardMealResult.setVisibility(View.VISIBLE);
        binding.tvResultEyebrow.setText(currentRecipe.aiGenerated ? R.string.ai_result_eyebrow : R.string.result_eyebrow);
        binding.tvMealTitle.setText(currentRecipe.title);
        binding.tvMealDescription.setText(currentRecipe.description);
        binding.tvMealMeta.setText(getString(R.string.meal_metadata, currentRecipe.meal,
                recipeMeta(currentRecipe)));
        binding.tvMatch.setText(currentRecipe.discovery ? getString(R.string.shopping_recipe)
                : getString(R.string.match_summary, currentRecipe.matches(resultPantry), currentRecipe.keys.size()));
        List<String> missing = currentRecipe.missing(resultPantry);
        if (currentRecipe.discovery) {
            binding.tvMissing.setText(R.string.shopping_note);
        } else if (currentRecipe.aiGenerated) {
            List<String> extras = currentRecipe.additionalIngredients;
            binding.tvMissing.setText(extras == null || extras.isEmpty() ? getString(R.string.no_extras)
                    : getString(R.string.staples, android.text.TextUtils.join(", ", extras)));
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

    private void updateSaveButton() {
        boolean saved = currentRecipe != null && savedIds.contains(currentRecipe.id);
        binding.btnLike.setText(saved ? R.string.recipe_saved : R.string.save_recipe);
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

    private void showSavedRecipes() {
        List<RecipeCatalog.Recipe> saved = new ArrayList<>();
        for (String id : savedIds) {
            RecipeCatalog.Recipe recipe = decodeRecipe(preferences.getString("recipe_json_" + id, null));
            if (recipe == null) recipe = RecipeCatalog.find(id);
            if (recipe != null) saved.add(recipe);
        }
        saved.sort((a, b) -> a.title.compareToIgnoreCase(b.title));
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.saved_title).setPositiveButton(R.string.close, null);
        if (saved.isEmpty()) dialog.setMessage(R.string.saved_empty);
        else {
            String[] titles = new String[saved.size()];
            for (int i = 0; i < saved.size(); i++) titles[i] = saved.get(i).title;
            dialog.setItems(titles, (d, index) -> showRecipe(saved.get(index)));
        }
        dialog.show();
    }

    private String recipeMeta(RecipeCatalog.Recipe recipe) {
        String servings = getResources().getQuantityString(
                R.plurals.servings, recipe.servings, recipe.servings);
        String timing = getString(R.string.time_servings, recipe.minutes, servings);
        return recipe.cuisine == null || recipe.cuisine.isEmpty() ? timing
                : getString(R.string.meal_metadata, recipe.cuisine, timing);
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
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding / 2, padding, padding);
        addRecipeText(content, recipeMeta(recipe), false);
        if (profile.hasExclusions()) addRecipeText(content, getString(R.string.allergy_note), false);
        addRecipeText(content, getString(R.string.ingredients_title), true);
        addRecipeText(content, recipe.ingredients, false);
        addRecipeText(content, getString(R.string.method_title), true);
        addRecipeText(content, recipe.method, false);
        scroll.addView(content);
        new MaterialAlertDialogBuilder(this).setTitle(recipe.title).setView(scroll)
                .setNeutralButton(savedIds.contains(recipe.id) ? R.string.remove_recipe : R.string.save_recipe,
                        (dialog, which) -> {
                            boolean removed = !savedIds.add(recipe.id);
                            if (removed) savedIds.remove(recipe.id);
                            persistSaved(recipe, removed);
                            updateSaveButton();
                            Snackbar.make(binding.getRoot(), removed ? R.string.removed_feedback
                                    : R.string.saved_feedback, Snackbar.LENGTH_SHORT).show();
                        })
                .setPositiveButton(R.string.close, null).show();
    }

    private void addRecipeText(LinearLayout parent, String text, boolean heading) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(heading ? 19 : 16);
        view.setTextColor(getColor(heading ? R.color.ink : R.color.muted));
        view.setLineSpacing(6, 1);
        if (heading) view.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = Math.round(16 * getResources().getDisplayMetrics().density);
        parent.addView(view, params);
    }


    private Chip selectableChip(String label) {
        Chip chip = new Chip(this);
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setEnsureMinTouchTargetSize(true);
        chip.setSaveEnabled(false);
        return chip;
    }

    private void updateMode() {
        binding.tvSceneHelp.setText(profile.mode.equals("pantry") ? R.string.ammo_help : R.string.discovery_help);
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
        binding.tabletop.setGoal(profile.goal);
        binding.btnBalanced.setAlpha(profile.goal.equals("Balanced") ? 1f : .5f);
        binding.btnCut.setAlpha(profile.goal.equals("Cutting") ? 1f : .5f);
        binding.btnBulk.setAlpha(profile.goal.equals("Bulking") ? 1f : .5f);
        int count = eligibleFavorites().size();
        String taste = !profile.useFavorites ? getString(R.string.taste_off) : count == 0 ? getString(R.string.taste_empty)
                : getResources().getQuantityString(R.plurals.taste_count, count, count);
        String summary = getString(R.string.filter_summary, profile.goal, profile.cuisine, taste);
        List<String> exclusions = new ArrayList<>(profile.allergies); exclusions.addAll(profile.customExclusions);
        if (profile.requiresReview) summary = getString(R.string.review_filters);
        if (!exclusions.isEmpty()) summary += "\n" + getString(R.string.exclusions_summary, android.text.TextUtils.join(", ", exclusions));
        binding.tvFilterSummary.setText(summary);
    }

    private void showFilters() {
        // Resolve controls from this dialog's layout, independently of generated bindings.
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_preferences, null, false);
        android.view.ViewGroup settingsHome = (android.view.ViewGroup) binding.settingsDrawer.getParent();
        settingsHome.removeView(binding.settingsDrawer);
        LinearLayout dialogContent = (LinearLayout) ((android.view.ViewGroup) dialogView).getChildAt(0);
        dialogContent.addView(binding.settingsDrawer, 0);
        binding.settingsDrawer.setVisibility(View.VISIBLE);
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
            Chip chip = selectableChip(goal);
            goalChips.addView(chip); chip.setChecked(goal.equals(draft.goal));
            chip.setOnCheckedChangeListener((button, checked) -> { if (checked) draft.goal = goal; });
        }
        cuisineDropdown.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, SpinOptions.CUISINES));
        cuisineDropdown.setText(draft.cuisine, false);
        for (String allergy : SpinOptions.ALLERGENS) {
            Chip chip = selectableChip(allergy);
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
            binding.settingsDrawer.setVisibility(View.GONE);
            settingsHome.addView(binding.settingsDrawer);
        });
        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            try {
                Editable custom = etCustomExclusions.getText();
                draft.customExclusions = IngredientExclusions.parseCustom(custom == null ? "" : custom.toString());
                draft.cuisine = cuisineDropdown.getText().toString();
                if (!SpinOptions.CUISINES.contains(draft.cuisine)) throw new IllegalArgumentException();
            } catch (IllegalArgumentException e) {
                customLayout.setError(getString(R.string.custom_invalid)); return;
            }
            draft.mode = profile.mode; // The source chips update the live profile inside this drawer.
            draft.useFavorites = switchFavorites.isChecked();
            draft.requiresReview = false;
            profile = draft;
            profile.save(preferences, gson);
            updateFilterSummary();
            if (currentRecipe != null) renderRecipe();
            dialog.dismiss();
        }));
        dialog.show();
    }

    @Override protected void onResume() { super.onResume(); binding.tabletop.resume(); }
    @Override protected void onPause() { binding.tabletop.pause(); super.onPause(); }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("was_firing", spinning);
        state.putStringArrayList("ammunition", binding.tabletop.magazine().save());
        state.putInt("active_chamber", binding.tabletop.magazine().active());
        state.putString("meal", meal);
        if (currentRecipe != null) state.putString("recipe_json", gson.toJson(currentRecipe));
        state.putString("result_pantry", android.text.TextUtils.join(",", resultPantry));
        super.onSaveInstanceState(state);
    }

    @Override protected void onDestroy() {
        // Cancel delivery and release the Activity when leaving or rotating the screen.
        pendingReveal = null;
        binding.tabletop.dispose();
        if (mechanicalSound != null) mechanicalSound.close();
        if (pendingCall != null) pendingCall.cancel();
        super.onDestroy();
    }
}
