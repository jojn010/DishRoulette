package com.example.dishroulette;

import android.content.SharedPreferences;
import android.text.Editable;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AlertDialog;

import com.example.dishroulette.core.IngredientExclusions;
import com.example.dishroulette.core.RecipeOptions;
import com.example.dishroulette.databinding.DialogPreferencesBinding;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Edits a private draft; only a valid Apply action changes the activity's preferences.
 */
final class RecipePreferencesDialog {
    interface Listener {
        void apply(RecipePreferences profile, String meal, boolean reduceMotion);
    }

    private final MainActivity activity;
    private final RecipePreferences draft;
    private final Listener listener;
    private final DialogPreferencesBinding controls;
    private final List<String> cuisineLabels = new ArrayList<>();
    private String selectedMeal;

    RecipePreferencesDialog(MainActivity activity, RecipePreferences profile, String meal, Listener listener) {
        this.activity = activity;
        this.draft = profile.copy();
        this.selectedMeal = meal;
        this.listener = listener;
        controls = DialogPreferencesBinding.inflate(activity.getLayoutInflater());
    }

    void show(SharedPreferences preferences, int favoriteCount) {
        createMealChoices();
        createGoalChoices();
        createAllergyChoices();
        configureCuisineDropdown();
        configureFavorites(favoriteCount);
        controls.switchMotion.setChecked(preferences.getBoolean("reduce_motion", false));
        controls.etCustomExclusions.setText(android.text.TextUtils.join(", ", draft.customExclusions));
        UiTypography.apply(controls.getRoot());

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.filter_title).setView(controls.getRoot())
            .setPositiveButton(
                AdaptiveDialogs.compact(activity) ? R.string.save_preferences_short : R.string.apply_filters, null)
            .setNegativeButton(R.string.cancel, null).create();
        dialog.setOnShowListener(unused -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener(view -> applyDraft(dialog)));
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setContentDescription(activity.getString(R.string.apply_filters));
        AdaptiveDialogs.fit(activity, dialog);
    }

    private void createMealChoices() {
        for (String meal : RecipeOptions.MEALS) {
            Chip chip = choice(UiLabels.option(activity, meal));
            controls.mealChips.addView(chip);
            chip.setChecked(meal.equals(selectedMeal));
            chip.setOnCheckedChangeListener((button, checked) -> {
                if (checked)
                    selectedMeal = meal;
            });
        }
    }

    private void createGoalChoices() {
        for (String goal : RecipeOptions.GOALS) {
            Chip chip = choice(UiLabels.option(activity, goal));
            controls.goalChips.addView(chip);
            chip.setChecked(goal.equals(draft.goal));
            chip.setOnCheckedChangeListener((button, checked) -> {
                if (checked)
                    draft.goal = goal;
            });
        }
    }

    private void createAllergyChoices() {
        for (String allergy : RecipeOptions.ALLERGENS) {
            Chip chip = choice(UiLabels.option(activity, allergy));
            controls.allergyChips.addView(chip);
            chip.setChecked(draft.allergies.contains(allergy));
            chip.setOnCheckedChangeListener((button, checked) -> {
                if (checked && !draft.allergies.contains(allergy))
                    draft.allergies.add(allergy);
                else if (!checked)
                    draft.allergies.remove(allergy);
            });
        }
    }

    private void configureCuisineDropdown() {
        for (String cuisine : RecipeOptions.CUISINES)
            cuisineLabels.add(UiLabels.option(activity, cuisine));
        controls.cuisineDropdown.setAdapter(
            new ArrayAdapter<>(activity, android.R.layout.simple_dropdown_item_1line, cuisineLabels));
        controls.cuisineDropdown.setText(UiLabels.option(activity, draft.cuisine), false);
        controls.cuisineDropdown.setOnItemClickListener(
            (parent, view, position, id) -> draft.cuisine = RecipeOptions.CUISINES.get(position));
    }

    private void configureFavorites(int count) {
        controls.switchFavorites.setChecked(draft.useFavorites);
        controls.tvTasteSummary.setText(count == 0 ? activity.getString(R.string.taste_empty)
            : activity.getResources().getQuantityString(R.plurals.taste_count, count, count));
    }

    private void applyDraft(AlertDialog dialog) {
        if (!validateDraft())
            return;
        draft.useFavorites = controls.switchFavorites.isChecked();
        draft.requiresReview = false;
        listener.apply(draft, selectedMeal, controls.switchMotion.isChecked());
        dialog.dismiss();
    }

    private boolean validateDraft() {
        try {
            Editable custom = controls.etCustomExclusions.getText();
            draft.customExclusions = IngredientExclusions.parseCustom(custom == null ? "" : custom.toString());
            int cuisine = cuisineLabels.indexOf(controls.cuisineDropdown.getText().toString());
            if (cuisine < 0)
                throw new IllegalArgumentException();
            draft.cuisine = RecipeOptions.CUISINES.get(cuisine);
            return true;
        } catch (IllegalArgumentException error) {
            controls.customLayout.setError(activity.getString(R.string.custom_invalid));
            return false;
        }
    }

    private Chip choice(String label) {
        Chip chip = new Chip(activity);
        chip.setId(View.generateViewId());
        chip.setText(label);
        UiTypography.apply(chip);
        chip.setTextSize(12);
        float density = activity.getResources().getDisplayMetrics().density;
        chip.setChipCornerRadius(3 * density);
        chip.setChipStrokeWidth(density);
        chip.setChipBackgroundColor(new android.content.res.ColorStateList(
            new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
            new int[]{UiColors.color(activity, R.color.accent), UiColors.color(activity, R.color.surface)}));
        chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(UiColors.color(activity, R.color.line)));
        chip.setTextColor(new android.content.res.ColorStateList(
            new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}},
            new int[]{UiColors.color(activity, R.color.on_accent), UiColors.color(activity, R.color.muted)}));
        chip.setCheckedIconVisible(false);
        chip.setCheckable(true);
        chip.setEnsureMinTouchTargetSize(true);
        chip.setSaveEnabled(false);
        return chip;
    }
}
