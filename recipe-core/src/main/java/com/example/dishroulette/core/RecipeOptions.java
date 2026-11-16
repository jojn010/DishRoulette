package com.example.dishroulette.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class RecipeOptions {
    private RecipeOptions() {
    }

    public static final List<String> MEALS =
        Collections.unmodifiableList(Arrays.asList("Any meal", "Breakfast", "Lunch", "Dinner", "Snack", "Dessert"));
    public static final List<String> GOALS =
        Collections.unmodifiableList(Arrays.asList("Balanced", "Cutting", "Bulking"));
    public static final List<String> CUISINES = Collections.unmodifiableList(Arrays.asList(
        "Any cuisine", "Palestinian", "Lebanese", "Turkish", "Moroccan", "Egyptian",
        "Italian", "Greek", "Mexican", "Indian", "Japanese", "Chinese", "Korean", "Thai",
        "Ethiopian", "French", "Spanish", "American"));
    public static final List<String> ALLERGENS = Collections.unmodifiableList(Arrays.asList(
        "Milk", "Eggs", "Peanuts", "Tree nuts", "Soy", "Wheat", "Gluten", "Sesame",
        "Fish", "Shellfish", "Molluscs", "Mustard", "Celery", "Lupin"));
}
