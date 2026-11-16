package com.example.dishroulette;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Recipe data, pantry-name normalization and the bilingual library retained for older saved dishes.
 */
public final class RecipeCatalog {
    private RecipeCatalog() {
    }

    public static final class Recipe {
        @SerializedName("translations")
        public java.util.Map<String, com.example.dishroulette.core.RecipeTranslation> translations;

        public boolean bilingual() {
            try {
                com.example.dishroulette.core.RecipeTranslation.validate(translations);
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        }

        public Recipe forLanguage(String target) {
            final java.util.Map<String, com.example.dishroulette.core.RecipeTranslation> pair;
            try {
                pair = com.example.dishroulette.core.RecipeTranslation.validate(translations);
            } catch (RuntimeException invalid) {
                return this;
            }
            com.example.dishroulette.core.RecipeTranslation text = pair.get(target);
            if (text == null)
                return this;
            return new Recipe(this, text, target, pair);
        }

        /**
         * Copy recipe facts explicitly, while replacing only the selected language's text.
         */
        private Recipe(Recipe source, com.example.dishroulette.core.RecipeTranslation text, String target,
                       java.util.Map<String, com.example.dishroulette.core.RecipeTranslation> pair) {
            id = source.id;
            meal = source.meal;
            minutes = source.minutes;
            servings = source.servings;
            title = text.title;
            description = text.description;
            ingredients = String.join("\n", text.ingredients);
            method = text.method();
            language = target;
            keys = source.keys == null ? null : new ArrayList<>(source.keys);
            translations = pair;
            caloriesPerServing = source.caloriesPerServing;
            proteinGramsPerServing = source.proteinGramsPerServing;
            carbohydratesGramsPerServing = source.carbohydratesGramsPerServing;
            fatGramsPerServing = source.fatGramsPerServing;
            fiberGramsPerServing = source.fiberGramsPerServing;
            sugarGramsPerServing = source.sugarGramsPerServing;
            sodiumMilligramsPerServing = source.sodiumMilligramsPerServing;
            goal = source.goal;
            cuisine = source.cuisine;
            discovery = source.discovery;
            shared = source.shared;
            aiGenerated = source.aiGenerated;
            additionalIngredients =
                source.additionalIngredients == null ? null : new ArrayList<>(source.additionalIngredients);
            // Personal explanations are authored in the generation language.
            whyThisDish = target.equals(source.language) ? source.whyThisDish : "";
            goalFit = target.equals(source.language) ? source.goalFit : "";
        }

        @SerializedName("id")
        public final String id;
        @SerializedName("title")
        public final String title;
        @SerializedName("meal")
        public final String meal;
        @SerializedName("description")
        public final String description;
        @SerializedName("ingredients")
        public final String ingredients;
        @SerializedName("method")
        public final String method;
        @SerializedName("minutes")
        public final int minutes;
        @SerializedName("servings")
        public final int servings;
        @SerializedName("caloriesPerServing")
        public Integer caloriesPerServing;
        @SerializedName("proteinGramsPerServing")
        public Double proteinGramsPerServing;
        @SerializedName("carbohydratesGramsPerServing")
        public Double carbohydratesGramsPerServing;
        @SerializedName("fatGramsPerServing")
        public Double fatGramsPerServing;
        @SerializedName("fiberGramsPerServing")
        public Double fiberGramsPerServing;
        @SerializedName("sugarGramsPerServing")
        public Double sugarGramsPerServing;
        @SerializedName("sodiumMilligramsPerServing")
        public Double sodiumMilligramsPerServing;

        @SerializedName("keys")
        public final List<String> keys;
        @SerializedName("language")
        public String language = "";
        @SerializedName("goal")
        public String goal = "";
        @SerializedName("cuisine")
        public String cuisine = "";
        @SerializedName("whyThisDish")
        public String whyThisDish = "";
        @SerializedName("goalFit")
        public String goalFit = "";
        @SerializedName("discovery")
        public boolean discovery;
        @SerializedName("shared")
        public boolean shared;
        @SerializedName("aiGenerated")
        public boolean aiGenerated;
        @SerializedName("additionalIngredients")
        public List<String> additionalIngredients = new ArrayList<>();

        Recipe(String id, String title, String meal, int minutes, int servings,
               String description, String keys, String ingredients, String method) {
            this.id = id;
            this.title = title;
            this.meal = meal;
            this.minutes = minutes;
            this.servings = servings;
            this.description = description;
            this.keys =
                keys.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(Arrays.asList(keys.split(",")));
            this.ingredients = ingredients;
            this.method = method;
        }

        public int matches(Set<String> pantry) {
            int count = 0;
            for (String key : keys)
                if (pantry.contains(key))
                    count++;
            return count;
        }

        public List<String> missing(Set<String> pantry) {
            List<String> missing = new ArrayList<>();
            for (String key : keys)
                if (!pantry.contains(key))
                    missing.add(key);
            return missing;
        }
    }

    public static final List<Recipe> ALL = Collections.unmodifiableList(Arrays.asList(
        new Recipe("eggs_toast", "Soft eggs on golden toast", "Breakfast", 10, 1,
            "A simple, comforting breakfast with a little crunch and plenty of flavor.", "eggs,bread",
            "2 eggs\n2 slices bread\n1 tsp butter or olive oil\nSalt and pepper",
            "1. Toast the bread until golden.\n\n2. Whisk the eggs with a pinch of salt. Warm the butter in a pan over medium-low heat.\n\n3. Add the eggs and stir gently until set, with no visible liquid egg.\n\n4. Spoon onto the toast and finish with pepper."),
        new Recipe("banana_oats", "Banana & cinnamon oats", "Breakfast", 10, 1,
            "Warm, creamy oats with naturally sweet banana and a pinch of cinnamon.", "oats,banana,milk",
            "½ cup rolled oats\n1 cup milk or plant milk\n1 banana\n¼ tsp cinnamon",
            "1. Put the oats and milk in a small saucepan.\n\n2. Bring to a gentle simmer and cook for 5–7 minutes, stirring until creamy.\n\n3. Mash half the banana into the oats. Add a splash of water if needed.\n\n4. Top with the remaining sliced banana and cinnamon."),
        new Recipe("chickpea_salad", "The sunny chickpea bowl", "Lunch", 15, 2,
            "Crisp vegetables, tender chickpeas, and a bright lemon dressing.", "chickpeas,tomato,cucumber,lemon",
            "1 can chickpeas (400 g), drained and rinsed\n2 tomatoes\n1 cucumber\n½ lemon, juiced\n1 tbsp olive oil\nSalt and pepper",
            "1. Dice the tomatoes and cucumber.\n\n2. Add to a bowl with the drained chickpeas.\n\n3. Whisk the lemon juice, olive oil, salt, and pepper.\n\n4. Toss everything together and serve."),
        new Recipe("tomato_toast", "Tomato garden toast", "Lunch", 10, 1,
            "Juicy tomatoes meet crisp toast in this easy pantry favorite.", "tomato,bread",
            "2 slices bread\n2 ripe tomatoes\n1 tbsp olive oil\nA pinch of dried oregano\nSalt and pepper",
            "1. Dice the tomatoes and toss with half the oil, oregano, salt, and pepper.\n\n2. Toast the bread until crisp.\n\n3. Spoon the tomatoes onto the toast.\n\n4. Finish with the remaining olive oil and serve immediately."),
        new Recipe("egg_rice", "Golden egg fried rice", "Dinner", 20, 2,
            "A quick pan of fluffy rice, soft egg, and sweet green peas.", "rice,eggs,peas",
            "2 cups freshly cooked rice\n2 eggs\n½ cup frozen peas\n1 tbsp neutral oil\n1 tbsp soy sauce",
            "1. Heat the oil in a large pan over medium-high heat.\n\n2. Beat the eggs, add to the pan, and scramble until fully set. Transfer to a plate.\n\n3. Add peas and a splash of water to the pan. Cook until hot and tender.\n\n4. Add cooked rice, egg, and soy sauce. Stir-fry until everything is steaming hot."),
        new Recipe("tomato_pasta", "Weeknight tomato pasta", "Dinner", 25, 2,
            "A glossy tomato sauce and a tangle of pasta. Comfort, made simple.", "pasta,tomato,garlic",
            "180 g dry pasta\n1 can chopped tomatoes (400 g)\n2 garlic cloves, minced\n1 tbsp olive oil\nSalt, pepper, and dried basil",
            "1. Cook pasta in boiling salted water following the pack instructions. Reserve half a cup of pasta water.\n\n2. Warm olive oil in a pan. Add garlic and stir for 30 seconds.\n\n3. Add tomatoes and basil. Simmer for 12–15 minutes; season to taste.\n\n4. Toss with the drained pasta, adding a little pasta water to loosen the sauce."),
        new Recipe("chicken_wrap", "Crunchy chicken wraps", "Lunch", 15, 2,
            "A fresh, satisfying wrap with cooked chicken and a tangy yogurt dressing.",
            "chicken,tortilla,cucumber,yogurt",
            "200 g ready-to-eat cooked chicken\n2 large tortillas\n½ cucumber\n3 tbsp plain yogurt\n1 tsp lemon juice\nSalt and pepper",
            "1. Slice the cooked chicken and cucumber.\n\n2. Mix yogurt with lemon juice, salt, and pepper.\n\n3. Warm the tortillas in a dry pan.\n\n4. Spread with dressing, add chicken and cucumber, then fold and serve."),
        new Recipe("chickpea_skillet", "Smoky chickpea skillet", "Dinner", 20, 2,
            "Chickpeas simmered in a rich tomato sauce with warm paprika.", "chickpeas,tomato,garlic",
            "1 can chickpeas (400 g), drained and rinsed\n1 can chopped tomatoes (400 g)\n2 garlic cloves, minced\n1 tbsp olive oil\n1 tsp smoked paprika\nSalt and pepper",
            "1. Warm olive oil in a pan over medium heat. Stir in garlic for 30 seconds.\n\n2. Add paprika, tomatoes, and chickpeas.\n\n3. Simmer for 12–15 minutes, stirring occasionally. Add a splash of water if needed.\n\n4. Season with salt and pepper, and spoon into bowls.")
    ));

    static {
        OfflineRecipeTranslations.apply(ALL);
    }

    public static Set<String> parseIngredients(String input) {
        Set<String> result = new LinkedHashSet<>();
        if (input == null)
            return result;
        for (String token : input.toLowerCase(Locale.ROOT).split("[,;،؛\\n]+")) {
            String key = normalize(token.trim());
            if (!key.isEmpty())
                result.add(key);
        }
        return result;
    }

    private static String normalize(String key) {
        key = com.example.dishroulette.core.FoodNames.canonical(key);
        switch (key) {
            case "egg":
                return "eggs";
            case "tomatoes":
            case "tomatos":
            case "canned tomatoes":
                return "tomato";
            case "chickpea":
            case "canned chickpeas":
            case "garbanzo beans":
                return "chickpeas";
            case "bananas":
                return "banana";
            case "cucumbers":
                return "cucumber";
            case "lemons":
                return "lemon";
            case "tortillas":
            case "wraps":
                return "tortilla";
            case "yoghurt":
            case "plain yogurt":
                return "yogurt";
            case "rolled oats":
            case "oatmeal":
                return "oats";
            case "cooked rice":
                return "rice";
            case "cooked chicken":
                return "chicken";
            case "frozen peas":
            case "pea":
                return "peas";
            default:
                return key;
        }
    }

    public static List<Recipe> matching(Set<String> pantry, String meal) {
        List<Recipe> result = new ArrayList<>();
        for (Recipe recipe : ALL) {
            if ((meal.equals("Any meal") || recipe.meal.equals(meal)) && recipe.matches(pantry) > 0) {
                result.add(recipe);
            }
        }
        return result;
    }

    public static Recipe find(String id) {
        for (Recipe recipe : ALL)
            if (recipe.id.equals(id))
                return recipe;
        return null;
    }
}
