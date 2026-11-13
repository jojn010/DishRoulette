package com.example.dishroulette;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** The sole ingredient store. Empty slots are retained so editing never shifts saved ingredient positions. */
public final class IngredientMagazine {
    private final ArrayList<String> slots = new ArrayList<>();
    private int active;
    public IngredientMagazine() { ensure(5); }
    private void ensure(int index) { while (slots.size() <= index) slots.add(""); }
    public int active() { return active; }
    public String text() { return slots.get(active); }
    public void edit(String text) { slots.set(active, text); }
    public void select(int index) { ensure(index); active = index; }
    public String validationError() {
        Set<String> names = new LinkedHashSet<>();
        int chars = 0;
        for (String value : slots) {
            // Check the original text before whitespace normalization erases line breaks.
            if (value.contains(",") || value.contains("،") || value.contains("؛")
                    || value.contains(";") || value.contains("\n") || value.contains("\r")) {
                return "One ingredient per entry. Add the next ingredient separately.";
            }
            String name = value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
            if (name.isEmpty()) continue;
            String canonical = RecipeCatalog.parseIngredients(name).iterator().next();
            if (!names.add(canonical)) return "That ingredient is already loaded. Edit or remove this ingredient.";
            chars += name.length();
            if (name.length() > 80) return "Use at most 80 characters per ingredient.";
        }
        if (names.size() > 20 || chars > 300) return "The recipe service accepts 20 ingredients and 300 characters. Remove or shorten an ingredient before generating.";
        return null;
    }
    /** Commit a validated entry; blank input cannot advance. */
    public boolean commit() {
        if (text().trim().isEmpty() || validationError() != null) return false;
        slots.set(active, text().trim());
        active++;
        ensure((active / 6) * 6 + 5);
        return true;
    }
    public Set<String> ingredients() {
        Set<String> result = new LinkedHashSet<>();
        for (String slot : slots) if (!slot.trim().isEmpty()) result.add(slot.trim());
        return result;
    }
    public int count() { return ingredients().size(); }
    public ArrayList<String> save() { return new ArrayList<>(slots); }
    public void restore(List<String> values, int selected) {
        slots.clear();
        if (values != null) for (String value : values) slots.add(value == null ? "" : value);
        active = Math.max(0, Math.min(selected, Math.max(0, slots.size() - 1)));
        ensure((active / 6) * 6 + 5);
    }
}
