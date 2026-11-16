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

/**
 * Full-screen native recipe library and cooking workspace; progress uses existing local storage.
 */
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
        this.activity = activity;
        this.host = host;
        this.prefs = prefs;
        this.actions = actions;
    }

    String screen() {
        return screen;
    }

    RecipeCatalog.Recipe cookingRecipe() {
        return cooking;
    }

    boolean isOpen() {
        return host.getVisibility() == View.VISIBLE;
    }

    void close() {
        host.setKeepScreenOn(false);
        stopTick();
        host.removeAllViews();
        host.setVisibility(View.GONE);
        screen = "";
    }

    void dispose() {
        host.setKeepScreenOn(false);
        stopTick();
    }

    private void stopTick() {
        if (timerTick != null)
            handler.removeCallbacks(timerTick);
        timerTick = null;
    }

    private int dp(int n) {
        return Math.round(n * activity.getResources().getDisplayMetrics().density);
    }

    private void base(String label, String title, Runnable back) {
        stopTick();
        host.setKeepScreenOn(false);
        host.removeAllViews();
        host.setVisibility(View.VISIBLE);
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(24), dp(16), dp(24), dp(28));
        // Limit long lines on tablets while allowing comfortable phone typography.
        AdaptivePageFrame page = new AdaptivePageFrame(activity, 720);
        FrameLayout.LayoutParams width = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        page.addView(body, width);
        scroll.addView(page);
        host.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        button(body, activity.getString(R.string.back), false, back);
        text(body, label, 11, R.color.accent, 24).setGravity(Gravity.CENTER);
        text(body, title, 34, R.color.ink, 12).setGravity(Gravity.CENTER);
        line(body);
    }

    private void line(LinearLayout parent) {
        View v = new View(activity);
        v.setBackgroundColor(UiColors.color(activity, R.color.line));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(1));
        lp.topMargin = dp(20);
        lp.bottomMargin = dp(12);
        parent.addView(v, lp);
    }

    private TextView text(LinearLayout parent, String value, int size, int color, int margin) {
        TextView t = new TextView(activity);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(UiColors.color(activity, color));
        t.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        UiTypography.apply(t);
        t.setLineSpacing(dp(3), 1);
        androidx.core.view.ViewCompat.setAccessibilityHeading(t, size >= 24);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(margin);
        parent.addView(t, lp);
        return t;
    }

    private Button button(LinearLayout parent, String title, boolean primary, Runnable click) {
        Button b = new androidx.appcompat.widget.AppCompatButton(activity);
        b.setText(title);
        UiTypography.apply(b);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        b.setTextColor(UiColors.color(activity, primary ? R.color.on_accent : R.color.ink));
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setColor(UiColors.color(activity, primary ? R.color.accent : R.color.surface));
        shape.setCornerRadius(dp(14));
        shape.setStroke(dp(1), UiColors.color(activity, R.color.line));
        b.setBackground(shape);
        if (primary)
            b.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(UiColors.color(activity, R.color.accent)));
        b.setMinHeight(dp(52));
        b.setPadding(dp(14), dp(8), dp(14), dp(8));
        b.setOnClickListener(v -> {
            activity.playUiClick();
            click.run();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(10);
        UiTypography.apply(b);
        parent.addView(b, lp);
        return b;
    }

    void saved(List<RecipeCatalog.Recipe> recipes) {
        screen = "saved";
        cooking = null;
        base(activity.getString(R.string.archive_label, recipes.size()), activity.getString(R.string.archive_title),
            this::close);
        Button importRecipe = button(body, activity.getString(R.string.import_recipe), false, activity::importRecipe);
        importRecipe.setId(R.id.importRecipe);
        if (recipes.isEmpty()) {
            text(body, activity.getString(R.string.archive_empty_heading), 22, R.color.ink, 28);
            text(body, activity.getString(R.string.archive_empty_detail), 16, R.color.muted, 12);
            button(body, activity.getString(R.string.home), true, this::close);
            return;
        }
        for (RecipeCatalog.Recipe recipe : recipes)
            addSavedCard(recipe, recipes);
    }

    private void addSavedCard(RecipeCatalog.Recipe recipe, List<RecipeCatalog.Recipe> recipes) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(14));
        android.graphics.drawable.GradientDrawable surface = new android.graphics.drawable.GradientDrawable();
        surface.setColor(UiColors.color(activity, R.color.surface));
        surface.setCornerRadius(dp(24));
        surface.setStroke(dp(1), UiColors.color(activity, R.color.line));
        card.setBackground(surface);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.topMargin = dp(16);
        body.addView(card, cardLp);
        text(card, UiLabels.option(activity, recipe.meal), 12, R.color.accent, 0);
        TextView title = text(card, recipe.title, 25, R.color.ink, 4);
        title.setTypeface(UiTypography.face(activity, "sans-serif", Typeface.BOLD));
        text(card, recipe.description, 15, R.color.muted, 6);
        TextView facts = text(card, RecipeText.overview(activity, recipe), 14, R.color.accent, 12);
        android.graphics.drawable.GradientDrawable band = new android.graphics.drawable.GradientDrawable();
        band.setColor(UiColors.color(activity, R.color.background));
        band.setCornerRadius(dp(12));
        facts.setBackground(band);
        facts.setPadding(dp(12), dp(8), dp(12), dp(8));
        Button open = button(card, activity.getString(R.string.open_recipe), true, () -> {
            close();
            actions.open(recipe);
        });
        open.setElevation(0);
        open.setContentDescription(open.getText() + ", " + recipe.title);
        Button share =
            button(card, activity.getString(R.string.share_recipe), false, () -> activity.shareRecipe(recipe));
        quietAction(share);
        share.setContentDescription(share.getText() + ", " + recipe.title);
        Button nutritionToggle = button(card, activity.getString(R.string.saved_show_nutrition), false, () -> {
        });
        quietAction(nutritionToggle);
        nutritionToggle.setContentDescription(nutritionToggle.getText() + ", " + recipe.title);
        TextView nutrition = text(card, RecipeText.nutrition(activity, recipe), 14, R.color.muted, 4);
        nutrition.setVisibility(View.GONE);
        nutritionToggle.setOnClickListener(v -> {
            boolean expanded = nutrition.getVisibility() != View.VISIBLE;
            nutrition.setVisibility(expanded ? View.VISIBLE : View.GONE);
            nutritionToggle.setText(
                activity.getString(expanded ? R.string.saved_hide_nutrition : R.string.saved_show_nutrition));
            nutritionToggle.setContentDescription(nutritionToggle.getText() + ", " + recipe.title);
            androidx.core.view.ViewCompat.setStateDescription(nutritionToggle,
                activity.getString(expanded ? R.string.saved_nutrition_expanded : R.string.saved_nutrition_collapsed));
        });
        androidx.core.view.ViewCompat.setStateDescription(nutritionToggle,
            activity.getString(R.string.saved_nutrition_collapsed));
        Button remove = button(card, activity.getString(R.string.remove_saved), false, () -> {
            removeSavedCard(recipe, recipes);
        });
        quietAction(remove);
        remove.setContentDescription(remove.getText() + ", " + recipe.title);
    }

    private void removeSavedCard(RecipeCatalog.Recipe recipe, List<RecipeCatalog.Recipe> recipes) {
        actions.remove(recipe);
        List<RecipeCatalog.Recipe> remaining = new ArrayList<>(recipes);
        remaining.remove(recipe);
        saved(remaining);
    }

    private void quietAction(Button button) {
        button.setElevation(0);
        android.util.TypedValue feedback = new android.util.TypedValue();
        activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, feedback, true);
        button.setBackgroundResource(feedback.resourceId);
        button.setTextSize(14);
        button.setTypeface(UiTypography.face(activity, "sans-serif", Typeface.NORMAL));
        ((LinearLayout.LayoutParams) button.getLayoutParams()).topMargin = 0;
    }

    void settings() {
        screen = "settings";
        cooking = null;
        base(activity.getString(R.string.settings), activity.getString(R.string.appearance), this::close);
        text(body, activity.getString(R.string.settings_description), 16, R.color.muted, 6);
        addThemeSettings();
        addLanguageSettings();
        addSoundSettings();
        Button preferences =
            button(body, activity.getString(R.string.recipe_preferences), false, activity::showFilters);
        preferences.setId(R.id.settingsRecipePreferences);
    }


    private void addThemeSettings() {
        text(body, activity.getString(R.string.theme_heading), 21, R.color.ink, 24).setGravity(Gravity.CENTER);
        text(body, activity.getString(R.string.theme_description), 14, R.color.muted, 8);
        String theme = AppearanceSettings.choice(activity);
        Button light =
            button(body, activity.getString(R.string.theme_light), theme.equals("light"), () -> chooseTheme("light"));
        light.setId(R.id.themeLight);
        light.setSelected(theme.equals("light"));
        Button dark =
            button(body, activity.getString(R.string.theme_dark), theme.equals("dark"), () -> chooseTheme("dark"));
        dark.setId(R.id.themeDark);
        dark.setSelected(theme.equals("dark"));
        Button system = button(body, activity.getString(R.string.theme_system), theme.equals("system"),
            () -> chooseTheme("system"));
        system.setId(R.id.themeSystem);
        system.setSelected(theme.equals("system"));
    }

    private void addLanguageSettings() {
        line(body);
        text(body, activity.getString(R.string.language), 21, R.color.ink, 24).setGravity(Gravity.CENTER);
        text(body, activity.getString(R.string.language_description), 14, R.color.muted, 8);
        Button english = button(body, "English", InterfaceSettings.language(activity).equals("en"),
            () -> activity.changeLanguage("en"));
        english.setId(R.id.settingsEnglish);
        Button arabic = button(body, "العربية", InterfaceSettings.language(activity).equals("ar"),
            () -> activity.changeLanguage("ar"));
        arabic.setId(R.id.settingsArabic);
    }

    private void addSoundSettings() {
        line(body);
        com.google.android.material.switchmaterial.SwitchMaterial sounds =
            new com.google.android.material.switchmaterial.SwitchMaterial(activity);
        sounds.setText(R.string.ui_sounds);
        sounds.setTextColor(UiColors.color(activity, R.color.ink));
        UiTypography.apply(sounds);
        sounds.setMinHeight(dp(56));
        sounds.setChecked(InterfaceSettings.prefs(activity).getBoolean("ui_sounds", true));
        sounds.setOnCheckedChangeListener((v, on) -> {
            InterfaceSettings.prefs(activity).edit().putBoolean("ui_sounds", on).apply();
            if (on)
                activity.playUiConfirm();
        });
        body.addView(sounds, new LinearLayout.LayoutParams(-1, -2));
        text(body, activity.getString(R.string.ui_sounds_description), 14, R.color.muted, 4);
    }

    private void chooseTheme(String choice) {
        AppearanceSettings.choose(activity, choice);
        settings();
    }

    void cook(RecipeCatalog.Recipe recipe) {
        screen = "cooking";
        cooking = recipe;
        base(activity.getString(R.string.cook_label), recipe.title, this::close);
        List<String> instructions = CookingInstructions.steps(recipe);
        if (instructions.isEmpty()) {
            text(body, activity.getString(R.string.cooking_instructions_missing), 18, R.color.muted, 12);
            return;
        }
        host.setKeepScreenOn(true);
        new CookingWorkspace(recipe, instructions).show();
    }

    /**
     * Owns one rendered recipe's step controls; KitchenScreens owns its lifecycle/tick cleanup.
     */
    private final class CookingWorkspace {
        private final RecipeCatalog.Recipe recipe;
        private final List<String> instructions;
        private int currentStep;
        private TextView progress, instruction, finish;
        private ProgressBar bar;
        private CheckBox done;
        private CookingTimerView timer;
        private LinearLayout timerActions;
        private Button startTimer, resetTimer, previous, next;
        private String timerPrefix;
        private int timerMinutes;

        CookingWorkspace(RecipeCatalog.Recipe recipe, List<String> instructions) {
            this.recipe = recipe;
            this.instructions = instructions;
            currentStep = Math.max(0, Math.min(instructions.size() - 1, prefs.getInt("cook_step_" + recipe.id, 0)));
        }

        void show() {
            text(body, RecipeText.overview(activity, recipe), 14, R.color.accent, 8);
            createIngredientChecklist();
            if (activity.hasExclusions())
                text(body, activity.getString(R.string.allergy_note), 13, R.color.muted, 12);
            createStepCard();
            createTimerControls();
            createStepNavigation();
            createNutritionDetails();
            button(body, activity.getString(R.string.back_overview), false, KitchenScreens.this::close);
            renderStep();
        }

        private void createIngredientChecklist() {
            LinearLayout checklist = new LinearLayout(activity);
            checklist.setOrientation(LinearLayout.VERTICAL);
            Button checklistToggle = button(body, activity.getString(R.string.checklist_closed), false, () -> {
            });
            checklistToggle.setContentDescription(activity.getString(R.string.checklist_a11y));
            body.addView(checklist);
            checklist.setVisibility(View.GONE);
            checklistToggle.setOnClickListener(v -> {
                boolean open = checklist.getVisibility() != View.VISIBLE;
                checklist.setVisibility(open ? View.VISIBLE : View.GONE);
                checklistToggle.setText(
                    open ? activity.getString(R.string.checklist_open) : activity.getString(R.string.checklist_closed));
            });
            int ingredient = 0;
            for (String value : recipe.ingredients.split("\\n")) {
                if (value.trim().isEmpty())
                    continue;
                String key = "cook_ingredient_" + recipe.id + "_" + ingredient++;
                CheckBox check = new androidx.appcompat.widget.AppCompatCheckBox(activity);
                check.setText(value);
                UiTypography.apply(check);
                check.setTextSize(17);
                check.setTextColor(UiColors.color(activity, R.color.ink));
                check.setMinHeight(dp(52));
                check.setPadding(0, dp(5), 0, dp(5));
                check.setChecked(prefs.getBoolean(key, false));
                check.setOnCheckedChangeListener((v, on) -> prefs.edit().putBoolean(key, on).apply());
                checklist.addView(check, new LinearLayout.LayoutParams(-1, -2));
            }
        }

        private void createStepCard() {
            LinearLayout stepCard = new LinearLayout(activity);
            stepCard.setOrientation(LinearLayout.VERTICAL);
            stepCard.setTag("cooking-step-card");
            stepCard.setPadding(dp(20), dp(18), dp(20), dp(14));
            android.graphics.drawable.GradientDrawable stepSurface = new android.graphics.drawable.GradientDrawable();
            stepSurface.setColor(UiColors.color(activity, R.color.surface));
            stepSurface.setCornerRadius(dp(24));
            stepSurface.setStroke(dp(1), UiColors.color(activity, R.color.line));
            stepCard.setBackground(stepSurface);
            LinearLayout.LayoutParams stepCardLp = new LinearLayout.LayoutParams(-1, -2);
            stepCardLp.topMargin = dp(20);
            body.addView(stepCard, stepCardLp);
            progress = text(stepCard, "", 14, R.color.accent, 0);
            progress.setTypeface(UiTypography.face(activity, "sans-serif", Typeface.BOLD));
            androidx.core.view.ViewCompat.setAccessibilityHeading(progress, true);
            progress.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            bar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
            bar.setMax(instructions.size());
            bar.setProgressTintList(
                android.content.res.ColorStateList.valueOf(UiColors.color(activity, R.color.accent)));
            LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(-1, dp(4));
            barLp.topMargin = dp(14);
            stepCard.addView(bar, barLp);
            instruction = text(stepCard, "", 24, R.color.ink, 18);
            done = new androidx.appcompat.widget.AppCompatCheckBox(activity);
            done.setText(activity.getString(R.string.step_complete));
            UiTypography.apply(done);
            done.setTextSize(16);
            done.setTextColor(UiColors.color(activity, R.color.ink));
            done.setMinHeight(dp(56));
            done.setPadding(0, dp(10), 0, dp(6));
            stepCard.addView(done);
        }

        private void createTimerControls() {
            timer = new CookingTimerView(activity);
            timer.setId(R.id.cookingTimerDial);
            LinearLayout.LayoutParams timerLp = new LinearLayout.LayoutParams(-2, -2);
            timerLp.gravity = Gravity.CENTER_HORIZONTAL;
            timerLp.topMargin = dp(16);
            body.addView(timer, timerLp);
            timerActions = new LinearLayout(activity);
            timerActions.setOrientation(LinearLayout.VERTICAL);
            body.addView(timerActions);
            startTimer = button(timerActions, "", false, () -> {
            });
            startTimer.setId(R.id.cookingTimerStart);
            resetTimer = button(timerActions, activity.getString(R.string.reset_timer), false, () -> {
            });
            resetTimer.setId(R.id.cookingTimerReset);
            text(timerActions, activity.getString(R.string.timer_notice), 12, R.color.muted, 8).setGravity(
                Gravity.CENTER);
            startTimer.setOnClickListener(view -> toggleTimer());
            resetTimer.setOnClickListener(view -> resetTimer());
        }

        private void createStepNavigation() {
            LinearLayout nav = new LinearLayout(activity);
            nav.setOrientation(LinearLayout.HORIZONTAL);
            body.addView(nav);
            previous = button(nav, activity.getString(R.string.previous_step), false, () -> {
            });
            next = button(nav, activity.getString(R.string.next_step), true, () -> {
            });
            LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, -2, 1);
            left.topMargin = dp(18);
            left.setMarginEnd(dp(6));
            previous.setLayoutParams(left);
            LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, -2, 1);
            right.topMargin = dp(18);
            next.setLayoutParams(right);
            finish = text(body, "", 19, R.color.accent, 20);
            finish.setGravity(Gravity.CENTER);
            previous.setOnClickListener(view -> moveStep(-1));
            next.setOnClickListener(view -> moveStep(1));
        }

        private void createNutritionDetails() {
            Button nutritionToggle = button(body, activity.getString(R.string.saved_show_nutrition), false, () -> {
            });
            quietAction(nutritionToggle);
            TextView nutritionDetails = text(body, RecipeText.nutrition(activity, recipe), 14, R.color.muted, 4);
            nutritionDetails.setTag("cooking-nutrition");
            nutritionDetails.setVisibility(View.GONE);
            androidx.core.view.ViewCompat.setStateDescription(nutritionToggle,
                activity.getString(R.string.saved_nutrition_collapsed));
            nutritionToggle.setOnClickListener(v -> {
                boolean expanded = nutritionDetails.getVisibility() != View.VISIBLE;
                nutritionDetails.setVisibility(expanded ? View.VISIBLE : View.GONE);
                nutritionToggle.setText(
                    activity.getString(expanded ? R.string.saved_hide_nutrition : R.string.saved_show_nutrition));
                androidx.core.view.ViewCompat.setStateDescription(nutritionToggle, activity.getString(
                    expanded ? R.string.saved_nutrition_expanded : R.string.saved_nutrition_collapsed));
            });
        }

        private void renderStep() {
            prefs.edit().putInt("cook_step_" + recipe.id, currentStep).apply();
            updateProgress();
            instruction.setText(instructions.get(currentStep));
            updateStepCompletion();
            previous.setEnabled(currentStep > 0);
            next.setEnabled(currentStep < instructions.size() - 1);
            configureStepTimer();
        }

        private void updateProgress() {
            int completed = 0;
            for (int i = 0; i < instructions.size(); i++)
                if (prefs.getBoolean("cook_done_" + recipe.id + "_" + i, false))
                    completed++;
            progress.setText(
                activity.getString(R.string.step_progress, currentStep + 1, instructions.size(), completed));
            bar.setProgress(completed);
            finish.setText(completed == instructions.size() ? activity.getString(R.string.enjoy_meal) : "");
        }

        private void updateStepCompletion() {
            String key = "cook_done_" + recipe.id + "_" + currentStep;
            done.setOnCheckedChangeListener(null);
            done.setChecked(prefs.getBoolean(key, false));
            done.setOnCheckedChangeListener((view, checked) -> {
                if (checked)
                    activity.playUiConfirm();
                prefs.edit().putBoolean(key, checked).apply();
                renderStep();
            });
        }

        private void moveStep(int direction) {
            int target = currentStep + direction;
            if (target < 0 || target >= instructions.size())
                return;
            currentStep = target;
            renderStep();
        }

        private void configureStepTimer() {
            stopTick();
            timerMinutes = CookingInstructions.suggestedMinutes(instructions.get(currentStep));
            timerActions.setVisibility(timerMinutes > 0 ? View.VISIBLE : View.GONE);
            timer.setVisibility(timerMinutes > 0 ? View.VISIBLE : View.GONE);
            if (timerMinutes <= 0)
                return;
            timerPrefix = "cook_timer_" + recipe.id + "_" + currentStep;
            timerTick = this::updateCountdown;
            timerTick.run();
        }

        private void updateCountdown() {
            long deadline = prefs.getLong(timerPrefix + "_end", 0);
            long duration = timerMinutes * 60000L;
            long remaining =
                deadline > 0 ? Math.max(0, deadline - System.currentTimeMillis()) : prefs.getLong(timerPrefix + "_left",
                    duration);
            boolean running = deadline > 0 && remaining > 0;
            timer.update(remaining, duration, running);
            startTimer.setText(running ? activity.getString(R.string.pause_timer) :
                remaining == 0 ? activity.getString(R.string.restart_timer, timerMinutes) : activity.getString(
                    R.string.start_timer));
            if (deadline > 0 && remaining == 0) {
                prefs.edit().putLong(timerPrefix + "_end", 0).putLong(timerPrefix + "_left", 0).apply();
                activity.playTimerComplete();
            }
            if (running)
                handler.postDelayed(timerTick, 1000);
        }

        private void toggleTimer() {
            long now = System.currentTimeMillis();
            long end = prefs.getLong(timerPrefix + "_end", 0);
            SharedPreferences.Editor edit = prefs.edit();
            if (end > now)
                edit.putLong(timerPrefix + "_left", end - now).putLong(timerPrefix + "_end", 0);
            else {
                long remaining = prefs.getLong(timerPrefix + "_left", timerMinutes * 60000L);
                if (remaining <= 0)
                    remaining = timerMinutes * 60000L;
                edit.putLong(timerPrefix + "_end", now + remaining);
            }
            edit.apply();
            activity.playUiClick();
            renderStep();
        }

        private void resetTimer() {
            prefs.edit().remove(timerPrefix + "_end").remove(timerPrefix + "_left").apply();
            renderStep();
        }
    }
}
