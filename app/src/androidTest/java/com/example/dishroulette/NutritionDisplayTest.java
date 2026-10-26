package com.example.dishroulette;

import android.content.Context;
import android.content.res.Configuration;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.Locale;
import org.junit.Test;
import static org.junit.Assert.*;

public class NutritionDisplayTest {
    @Test public void localizedMetadataShowsPerServingEstimatesAndUnknownHistoricalValues() {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        for (String language : new String[]{"en", "ar"}) {
            Configuration config = new Configuration(base.getResources().getConfiguration());
            config.setLocale(Locale.forLanguageTag(language));
            Context context = base.createConfigurationContext(config);
            RecipeCatalog.Recipe recipe = new RecipeCatalog.Recipe("nutrition", "Eggs", "Breakfast", 10, 2,
                    "Eggs", "eggs", "2 eggs", "Cook eggs.");
            assertTrue(KitchenScreens.meta(context, recipe).contains(context.getString(R.string.nutrition_unavailable)));
            recipe.caloriesPerServing = 210;
            recipe.proteinGramsPerServing = 12.5;
            recipe.carbohydratesGramsPerServing = 20.0;
            recipe.fatGramsPerServing = 9.0;
            recipe.fiberGramsPerServing = 2.0;
            recipe.sugarGramsPerServing = 0.0;
            recipe.sodiumMilligramsPerServing = 300.0;
            java.text.NumberFormat numbers = java.text.NumberFormat.getNumberInstance(Locale.forLanguageTag(language));
            numbers.setMaximumFractionDigits(1);
            String expected = context.getString(R.string.nutrition_estimate, numbers.format(210), numbers.format(12.5));
            assertTrue(KitchenScreens.meta(context, recipe).contains(expected));
            assertTrue(KitchenScreens.meta(context, recipe).contains("~~~"));
            assertTrue(KitchenScreens.meta(context, recipe).contains(context.getString(R.string.nutrition_variation)));
            assertTrue(KitchenScreens.meta(context, recipe).contains(context.getString(R.string.nutrition_sodiumMilligrams, numbers.format(300))));
            assertTrue(KitchenScreens.meta(context, recipe).contains(context.getString(R.string.nutrition_sugarGrams, numbers.format(0))));
            assertFalse(KitchenScreens.meta(context, recipe).contains(context.getString(R.string.nutrition_unavailable)));
        }
    }
}
