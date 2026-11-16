package com.example.dishroulette;

/**
 * Localized recipe facts and nutrition; shared by results, Saved, import review and cooking.
 */
final class RecipeText {
    private RecipeText() {
    }

    static String overview(android.content.Context context, RecipeCatalog.Recipe r) {
        String value = context.getString(R.string.recipe_metadata, r.minutes, r.servings);
        if (r.cuisine != null && !r.cuisine.trim().isEmpty())
            value += "\n" + UiLabels.option(context, r.cuisine);
        if (r.goal != null && !r.goal.trim().isEmpty())
            value += " · " + UiLabels.option(context, r.goal);
        return value;
    }

    static String meta(android.content.Context context, RecipeCatalog.Recipe r) {
        return overview(context, r) + "\n" + nutrition(context, r);
    }

    static String nutrition(android.content.Context context, RecipeCatalog.Recipe r) {
        String value;
        if (com.example.dishroulette.core.NutritionEstimate.valid(r.caloriesPerServing, r.proteinGramsPerServing)) {
            java.text.NumberFormat numbers = java.text.NumberFormat.getNumberInstance(
                context.getResources().getConfiguration().getLocales().get(0));
            numbers.setMaximumFractionDigits(1);
            value = context.getString(R.string.nutrition_estimate,
                numbers.format(r.caloriesPerServing), numbers.format(r.proteinGramsPerServing));
            if (com.example.dishroulette.core.NutritionEstimate.validNutrient(r.carbohydratesGramsPerServing, 1000))
                value += "\n" + context.getString(R.string.nutrition_carbohydratesGrams,
                    numbers.format(r.carbohydratesGramsPerServing));
            if (com.example.dishroulette.core.NutritionEstimate.validNutrient(r.fatGramsPerServing, 500))
                value += "\n" + context.getString(R.string.nutrition_fatGrams, numbers.format(r.fatGramsPerServing));
            if (com.example.dishroulette.core.NutritionEstimate.validNutrient(r.fiberGramsPerServing, 200))
                value +=
                    "\n" + context.getString(R.string.nutrition_fiberGrams, numbers.format(r.fiberGramsPerServing));
            if (com.example.dishroulette.core.NutritionEstimate.validNutrient(r.sugarGramsPerServing, 1000))
                value +=
                    "\n" + context.getString(R.string.nutrition_sugarGrams, numbers.format(r.sugarGramsPerServing));
            if (com.example.dishroulette.core.NutritionEstimate.validNutrient(r.sodiumMilligramsPerServing, 20000))
                value += "\n" + context.getString(R.string.nutrition_sodiumMilligrams,
                    numbers.format(r.sodiumMilligramsPerServing));
            value += "\n" + context.getString(R.string.nutrition_variation);
        } else
            value = context.getString(R.string.nutrition_unavailable);
        return value;
    }
}
